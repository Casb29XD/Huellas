package com.desarrolloMovielexample.huella.e2e

import com.google.gson.JsonParser
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Funcionalidad del backend por REST/RPC (ítems 6, 7, 8, 13, 15, 20, 53): puntos, notificaciones, solicitudes,
 * comentarios, votos, avistamientos, cierre y eliminación de cuenta. Usuarios propios borrados en @AfterClass.
 */
class BackendFlowsE2eTest {
    companion object {
        @AfterClass @JvmStatic
        fun cleanup() = E2eEnv.cleanup()
    }

    @Before
    fun setUp() = E2eEnv.assumeConfigured()

    private fun points(userId: String) =
        E2eEnv.admin("rest/v1/profiles?id=eq.$userId&select=points").rows().single().get("points").asInt

    private fun notifications(userId: String, type: String) =
        E2eEnv.admin("rest/v1/notifications?user_id=eq.$userId&type=eq.$type&select=target_id").rows()

    private fun publication(id: String, select: String) =
        E2eEnv.admin("rest/v1/publications?id=eq.$id&select=$select").rows().single()

    private fun ok(what: String, r: E2eEnv.Resp) = assertTrue("$what ${r.code}: ${r.body}", r.ok)

    @Test
    fun `flujo completo de publicacion, solicitud, chat, comentarios, votos, avistamiento y cierre`() {
        val author = E2eEnv.createUser(name = "Autor E2E")
        val applicant = E2eEnv.createUser(name = "Adoptante E2E")
        val mod = E2eEnv.createUser(name = "Moderador E2E")
        E2eEnv.makeModerator(mod)

        // Crear publicación: +5.
        val created = E2eEnv.rest(
            "rest/v1/publications", "POST",
            """{"author_id":"${author.id}","category":"ADOPCION","title":"E2E Max","species":"Perro","breed":"Criollo","size":"Mediano","city":"Medellín","description":"Prueba e2e"}""",
            author.token, headers = mapOf("Prefer" to "return=representation"),
        )
        ok("crear publicación", created)
        val pubId = created.rows().single().get("id").asString
        assertEquals("crear publicación suma +5", 5, points(author.id))
        assertTrue("sin ubicación la latitud es NULL", publication(pubId, "latitude").get("latitude").isJsonNull)

        // Aprobar por RPC: +10 y POST_APPROVED.
        ok("aprobar", E2eEnv.rpc("moderate_publication", """{"p_publication_id":"$pubId","p_status":"APROBADA"}""", mod.token))
        assertEquals("aprobar suma +10", 15, points(author.id))
        assertEquals(1, notifications(author.id, "POST_APPROVED").size)

        // Solicitud: NEW_REQUEST al autor.
        val req = E2eEnv.rest(
            "rest/v1/adoption_requests", "POST",
            """{"publication_id":"$pubId","applicant_id":"${applicant.id}","message":"Quiero adoptarlo"}""",
            applicant.token, headers = mapOf("Prefer" to "return=representation"),
        )
        ok("enviar solicitud", req)
        val reqId = req.rows().single().get("id").asString
        assertEquals("NEW_REQUEST apunta a la publicación", pubId,
            notifications(author.id, "NEW_REQUEST").single().get("target_id").asString)

        // Solo el autor responde; aceptar devuelve la conversación y la notifica al solicitante.
        assertTrue(!E2eEnv.rpc("respond_to_request", """{"p_request_id":"$reqId","p_accept":true}""", applicant.token).ok)
        val accepted = E2eEnv.rpc("respond_to_request", """{"p_request_id":"$reqId","p_accept":true}""", author.token)
        ok("aceptar solicitud", accepted)
        val convId = JsonParser.parseString(accepted.body).asString
        assertEquals("REQUEST_ACCEPTED lleva target_id = conversación", convId,
            notifications(applicant.id, "REQUEST_ACCEPTED").single().get("target_id").asString)
        val conv = E2eEnv.rest("rest/v1/conversations?id=eq.$convId&select=updated_at", token = applicant.token).rows()
        assertEquals("el solicitante ve la conversación", 1, conv.size)

        // Mensaje: updated_at de la conversación avanza.
        val before = conv.single().get("updated_at").asString
        Thread.sleep(50)
        ok("enviar mensaje", E2eEnv.rest("rest/v1/chat_messages", "POST",
            """{"conversation_id":"$convId","sender_id":"${applicant.id}","text":"Hola"}""", applicant.token))
        val after = E2eEnv.admin("rest/v1/conversations?id=eq.$convId&select=updated_at").rows().single().get("updated_at").asString
        assertNotEquals("el mensaje actualiza conversations.updated_at", before, after)

        // Comentario: +2, contador y NEW_COMMENT.
        ok("comentar", E2eEnv.rest("rest/v1/comments", "POST",
            """{"publication_id":"$pubId","author_id":"${applicant.id}","text":"¡Qué lindo!"}""", applicant.token))
        assertEquals(1, publication(pubId, "comments_count").get("comments_count").asInt)
        assertEquals("comentar suma +2", 2, points(applicant.id))
        assertEquals(1, notifications(author.id, "NEW_COMMENT").size)

        // Voto: contador y toggle (DELETE retira el voto).
        ok("votar", E2eEnv.rest("rest/v1/publication_votes", "POST",
            """{"publication_id":"$pubId","user_id":"${applicant.id}","value":1}""", applicant.token))
        assertEquals(1, publication(pubId, "votes_count").get("votes_count").asInt)
        ok("retirar voto", E2eEnv.rest("rest/v1/publication_votes?publication_id=eq.$pubId&user_id=eq.${applicant.id}",
            "DELETE", token = applicant.token))
        assertEquals(0, publication(pubId, "votes_count").get("votes_count").asInt)

        // Avistamiento: +5 y SIGHTING.
        ok("avistamiento", E2eEnv.rpc("report_sighting", """{"p_publication_id":"$pubId","p_text":"Lo vi en el parque"}""", applicant.token))
        assertEquals("avistamiento suma +5", 7, points(applicant.id))
        assertEquals(1, notifications(author.id, "SIGHTING").size)

        // Cierre: +50, FINALIZADA, contador de adopciones y LEVEL_UP (15 + 50 = 65 -> PROTECTOR).
        assertTrue(!E2eEnv.rpc("close_publication", """{"p_publication_id":"$pubId"}""", applicant.token).ok)
        ok("finalizar", E2eEnv.rpc("close_publication", """{"p_publication_id":"$pubId"}""", author.token))
        assertEquals("FINALIZADA", publication(pubId, "lifecycle").get("lifecycle").asString)
        assertEquals("adopción concretada suma +50", 65, points(author.id))
        val profile = E2eEnv.admin("rest/v1/profiles?id=eq.${author.id}&select=level,adoptions_count").rows().single()
        assertEquals("PROTECTOR", profile.get("level").asString)
        assertEquals(1, profile.get("adoptions_count").asInt)
        assertEquals(1, notifications(author.id, "LEVEL_UP").size)
    }

    @Test
    fun `delete_my_account borra el usuario y sus datos en cascada`() {
        val author = E2eEnv.createUser(name = "Autor E2E")
        // Sin track(): la cuenta se borra en la prueba; si falla, el finally la limpia.
        val email = E2eEnv.uniqueEmail()
        val r = E2eEnv.admin("auth/v1/admin/users", "POST",
            """{"email":"$email","password":"${E2eEnv.PASSWORD}","email_confirm":true,"user_metadata":{"name":"Se va E2E"}}""")
        ok("crear usuario", r)
        val goneId = r.json().get("id").asString
        try {
            val token = E2eEnv.login(email).first
            val pub = E2eEnv.admin("rest/v1/publications", "POST",
                """{"author_id":"${author.id}","title":"E2E","species":"Perro","breed":"Criollo","size":"Mediano","city":"Medellín","description":"e2e","status":"APROBADA"}""")
            ok("publicación del autor", pub)
            val pubId = pub.rows().single().get("id").asString
            ok("solicitud", E2eEnv.rest("rest/v1/adoption_requests", "POST",
                """{"publication_id":"$pubId","applicant_id":"$goneId","message":"e2e"}""", token))
            ok("comentario", E2eEnv.rest("rest/v1/comments", "POST",
                """{"publication_id":"$pubId","author_id":"$goneId","text":"e2e"}""", token))

            ok("delete_my_account", E2eEnv.rpc("delete_my_account", token = token))

            assertEquals("auth.users borrado", 404, E2eEnv.admin("auth/v1/admin/users/$goneId").code)
            assertEquals(0, E2eEnv.admin("rest/v1/profiles?id=eq.$goneId&select=id").rows().size)
            assertEquals(0, E2eEnv.admin("rest/v1/adoption_requests?applicant_id=eq.$goneId&select=id").rows().size)
            assertEquals(0, publication(pubId, "comments_count").get("comments_count").asInt)
        } finally {
            E2eEnv.admin("auth/v1/admin/users/$goneId", "DELETE")
        }
    }
}
