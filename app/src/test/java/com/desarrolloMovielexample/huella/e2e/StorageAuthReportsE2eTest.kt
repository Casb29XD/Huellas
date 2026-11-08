package com.desarrolloMovielexample.huella.e2e

import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/** Límites del servidor por REST: canje PKCE (ítems 11/36), storage (18), reportes (17) y borrado ajeno (10). */
class StorageAuthReportsE2eTest {
    companion object {
        @AfterClass @JvmStatic
        fun cleanup() = E2eEnv.cleanup()
    }

    @Before
    fun setUp() = E2eEnv.assumeConfigured()

    private fun publicationOf(author: E2eEnv.TestUser, status: String = "APROBADA"): String {
        val r = E2eEnv.admin("rest/v1/publications", "POST",
            """{"author_id":"${author.id}","title":"E2E límites","species":"Gato","breed":"Criollo","size":"Pequeño","city":"Medellín","description":"e2e","status":"$status"}""")
        assertTrue("crear publicación ${r.code}: ${r.body}", r.ok)
        return r.rows().single().get("id").asString
    }

    @Test
    fun `el canje PKCE existe y rechaza un code sin flow state`() {
        val r = E2eEnv.rest("auth/v1/token?grant_type=pkce", "POST",
            """{"auth_code":"${UUID.randomUUID()}","code_verifier":"${"v".repeat(43)}"}""")
        assertEquals(r.body, 404, r.code)
        assertEquals(r.body, "flow_state_not_found", r.json().get("error_code").asString)
    }

    @Test
    fun `pet-images acepta JPEG propio y rechaza MIME, tamano y anonimo`() {
        val user = E2eEnv.createUser(name = "Storage E2E")
        val base = "${user.id}/e2e-${UUID.randomUUID()}"
        fun upload(name: String, bytes: ByteArray, type: String, token: String? = user.token) =
            E2eEnv.restBytes("storage/v1/object/pet-images/$base-$name", bytes, type, token)
        try {
            val ok = upload("ok.jpg", byteArrayOf(-1, -40, -1, -39), "image/jpeg")
            assertTrue("JPEG en la carpeta propia: ${ok.code} ${ok.body}", ok.ok)
            val text = upload("x.txt", "hola".toByteArray(), "text/plain")
            assertFalse("aceptó text/plain: ${text.code} ${text.body}", text.ok)
            val big = upload("big.jpg", ByteArray(5 * 1024 * 1024 + 1), "image/jpeg")
            assertFalse("aceptó más de 5 MB: ${big.code} ${big.body}", big.ok)
            val anon = upload("anon.jpg", byteArrayOf(-1, -40, -1, -39), "image/jpeg", token = null)
            assertFalse("anónimo subió: ${anon.code} ${anon.body}", anon.ok)
        } finally {
            E2eEnv.admin("storage/v1/object/pet-images", "DELETE",
                """{"prefixes":["$base-ok.jpg","$base-x.txt","$base-big.jpg","$base-anon.jpg"]}""")
        }
    }

    @Test
    fun `reporte unico por usuario, cola solo para moderadores y resolve_report solo moderador`() {
        val author = E2eEnv.createUser(name = "Autor Rep E2E")
        val reporter = E2eEnv.createUser(name = "Reporta E2E")
        val pubId = publicationOf(author)
        val body = """{"publication_id":"$pubId","reporter_id":"${reporter.id}","reason":"Spam","detail":"e2e"}"""
        // Sin return=representation: el usuario no tiene SELECT sobre reports (igual que la app).
        val first = E2eEnv.rest("rest/v1/reports", "POST", body, reporter.token)
        assertTrue("reportar ${first.code}: ${first.body}", first.ok)
        val reportId = E2eEnv.admin("rest/v1/reports?publication_id=eq.$pubId&select=id").rows().single().get("id").asString
        assertEquals("segundo reporte de la misma persona", 409, E2eEnv.rest("rest/v1/reports", "POST", body, reporter.token).code)

        val queue = E2eEnv.rest("rest/v1/reports?id=eq.$reportId&select=id", token = reporter.token)
        assertTrue("un usuario normal ve la cola: ${queue.body}", !queue.ok || queue.rows().isEmpty())
        assertFalse(E2eEnv.rpc("resolve_report", """{"p_report_id":"$reportId"}""", reporter.token).ok)
        assertEquals("ABIERTO", E2eEnv.admin("rest/v1/reports?id=eq.$reportId&select=status").rows().single().get("status").asString)
    }

    @Test
    fun `un tercero no borra ni edita la publicacion ajena`() {
        val author = E2eEnv.createUser(name = "Autor Del E2E")
        val other = E2eEnv.createUser(name = "Tercero E2E")
        val pubId = publicationOf(author)
        E2eEnv.rest("rest/v1/publications?id=eq.$pubId", "DELETE", token = other.token)
        E2eEnv.rest("rest/v1/publications?id=eq.$pubId", "PATCH", """{"title":"hackeado"}""", other.token)
        val row = E2eEnv.admin("rest/v1/publications?id=eq.$pubId&select=title").rows()
        assertEquals("la publicación ajena sigue intacta", listOf("E2E límites"), row.map { it.get("title").asString })
    }
}
