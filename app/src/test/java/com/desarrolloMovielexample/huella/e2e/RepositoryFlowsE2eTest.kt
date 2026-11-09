package com.desarrolloMovielexample.huella.e2e

import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.utils.UiText
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.data.remote.SupabaseSession
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.PostStatus
import com.desarrolloMovielexample.huella.domain.model.PublicationDraft
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * El SupabaseRepository de la app (mapeo JSON, fechas, sesión) contra el Supabase hosted (ítem 39): cada paso usa
 * el método real que llama la UI y se verifica en la BD con service_role. Usuarios propios borrados en @AfterClass.
 */
@RunWith(RobolectricTestRunner::class)
class RepositoryFlowsE2eTest {
    companion object {
        @AfterClass @JvmStatic
        fun cleanup() = E2eEnv.cleanup()
    }

    private val repo = SupabaseRepository
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    private val draft = PublicationDraft(
        category = Category.ADOPCION, title = "E2E Repo Luna", description = "Prueba e2e del repositorio",
        species = "Perro", breed = "Criollo", size = "Mediano", vaccinated = true, sterilized = false,
        age = "2 años", sex = "Hembra", city = "Medellín, Antioquia",
    )

    @Before
    fun setUp() {
        E2eEnv.assumeConfigured()
        SupabaseSession.clear()
        repo.initialize(context)
    }

    @After
    fun tearDown() = repo.logout()

    private suspend fun loginAs(user: E2eEnv.TestUser) {
        repo.logout()
        repo.login(user.email, E2eEnv.PASSWORD).getOrThrow()
    }

    private fun one(path: String) = E2eEnv.admin(path).rows().single()

    /** markNotificationRead/deleteNotification escriben en segundo plano: espera a verlo en la BD. */
    private fun eventually(what: String, check: () -> Boolean) {
        repeat(50) { if (check()) return; Thread.sleep(100) }
        fail(what)
    }

    @Test
    fun `publicacion creada, editada, aprobada, solicitada, aceptada, cerrada y borrada con el repositorio`() = runBlocking {
        val author = E2eEnv.createUser(name = "Autor Repo E2E")
        val applicant = E2eEnv.createUser(name = "Adoptante Repo E2E")
        val mod = E2eEnv.createUser(name = "Mod Repo E2E")
        E2eEnv.makeModerator(mod)

        // Crear (ítem 22: sin ubicación, NULL) y fecha del servidor en UTC (ítem 12: "ahora", no "hace 5 h").
        loginAs(author)
        val created = repo.createPublication(draft).getOrThrow()
        val pubId = created.id
        assertEquals(PostStatus.EN_REVISION, created.status)
        assertTrue(
            "timeAgo de una publicación recién creada: ${created.timeAgo}",
            created.timeAgo in setOf(UiText.Res(R.string.time_now), UiText.Plural(R.plurals.time_minutes_ago, 1)),
        )
        assertTrue(one("rest/v1/publications?id=eq.$pubId&select=latitude").get("latitude").isJsonNull)

        // Editar actualiza la misma fila (ítem 9).
        val edited = repo.updatePublication(pubId, draft.copy(title = "E2E Repo Luna editada")).getOrThrow()
        assertEquals(pubId, edited.id)
        val mine = E2eEnv.admin("rest/v1/publications?author_id=eq.${author.id}&select=id,title").rows()
        assertEquals("editar no crea otra publicación", listOf(pubId), mine.map { it.get("id").asString })
        assertEquals("E2E Repo Luna editada", mine.single().get("title").asString)

        // Moderar con el repositorio (approve -> RPC moderate_publication).
        loginAs(mod)
        repo.approve(pubId).getOrThrow()
        assertEquals("APROBADA", one("rest/v1/publications?id=eq.$pubId&select=status").get("status").asString)

        // Perfil público real del autor (ítem 28): nombre y puntos (+5 crear, +10 aprobada).
        loginAs(applicant)
        val publicAuthor = repo.refreshUser(author.id).getOrThrow()
        assertEquals("Autor Repo E2E", publicAuthor.name)
        assertEquals(15, publicAuthor.points)
        assertEquals(listOf(pubId), repo.publicationsByAuthor(author.id).map { it.id })

        // Solicitud, comentario, voto, avistamiento y reporte reales (ítems 7, 13, 17).
        repo.sendAdoptionRequest(pubId, "Quiero adoptarla").getOrThrow()
        assertTrue(repo.addComment(pubId, "¡Hermosa!").getOrThrow().isMine)
        repo.votePublication(pubId, 1).getOrThrow()
        repo.reportSighting(pubId, "La vi en el parque").getOrThrow()
        repo.reportPublication(pubId, "Contenido inapropiado", "e2e").getOrThrow()
        val counts = one("rest/v1/publications?id=eq.$pubId&select=votes_count,comments_count")
        assertEquals(1, counts.get("votes_count").asInt)
        assertEquals(1, counts.get("comments_count").asInt)
        assertEquals("comentario +2 y avistamiento +5", 7,
            one("rest/v1/profiles?id=eq.${applicant.id}&select=points").get("points").asInt)

        // El autor acepta (RPC), escribe en la conversación y gestiona notificaciones (ítem 29: persisten).
        loginAs(author)
        val reqId = one("rest/v1/adoption_requests?publication_id=eq.$pubId&select=id").get("id").asString
        repo.respondToRequest(reqId, true).getOrThrow()
        val convId = one("rest/v1/conversations?publication_id=eq.$pubId&select=id").get("id").asString
        repo.sendMessage(convId, "Hola, ¿cuándo la conoces?").getOrThrow()
        assertEquals(1, E2eEnv.admin("rest/v1/chat_messages?conversation_id=eq.$convId&select=id").rows().size)

        val notifs = E2eEnv.admin("rest/v1/notifications?user_id=eq.${author.id}&select=id,type,read").rows()
        val newRequest = notifs.single { it.get("type").asString == "NEW_REQUEST" }.get("id").asString
        val newComment = notifs.single { it.get("type").asString == "NEW_COMMENT" }.get("id").asString
        repo.markNotificationRead(newRequest)
        repo.deleteNotification(newComment)
        eventually("NEW_REQUEST no quedó leída en la BD") {
            E2eEnv.admin("rest/v1/notifications?id=eq.$newRequest&select=read").rows().singleOrNull()?.get("read")?.asBoolean == true
        }
        eventually("NEW_COMMENT no se borró de la BD") {
            E2eEnv.admin("rest/v1/notifications?id=eq.$newComment&select=id").rows().isEmpty()
        }

        // Finalizar (ítem 15): +50 y nivel.
        repo.closePublication(pubId).getOrThrow()
        assertEquals("FINALIZADA", one("rest/v1/publications?id=eq.$pubId&select=lifecycle").get("lifecycle").asString)
        val profile = one("rest/v1/profiles?id=eq.${author.id}&select=points,level")
        assertEquals(65, profile.get("points").asInt)
        assertEquals("PROTECTOR", profile.get("level").asString)

        // El moderador resuelve el reporte (ítem 17).
        loginAs(mod)
        val reportId = one("rest/v1/reports?publication_id=eq.$pubId&select=id").get("id").asString
        repo.resolveReport(reportId).getOrThrow()
        assertEquals("RESUELTO", one("rest/v1/reports?id=eq.$reportId&select=status").get("status").asString)

        // Borrar de verdad (ítem 10).
        loginAs(author)
        repo.deletePublication(pubId).getOrThrow()
        assertTrue(E2eEnv.admin("rest/v1/publications?id=eq.$pubId&select=id").rows().isEmpty())
    }

    @Test
    fun `sube una foto a su carpeta de pet-images con el repositorio`() = runBlocking {
        val user = E2eEnv.createUser(name = "Foto Repo E2E")
        loginAs(user)
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())
        val file = File.createTempFile("e2e-", ".jpg").apply { writeBytes(jpeg) }
        val url = repo.uploadPublicationImage(Uri.fromFile(file))
        val objectPath = url.substringAfter("/pet-images/")
        try {
            assertTrue("la foto va a <uid>/: $objectPath", objectPath.startsWith("${user.id}/"))
            assertEquals("lectura pública de la foto", 200, E2eEnv.rest(url.removePrefix(E2eEnv.url)).code)
        } finally {
            E2eEnv.admin("storage/v1/object/pet-images", "DELETE", """{"prefixes":["$objectPath"]}""")
            file.delete()
        }
    }

    @Test
    fun `sesion restaurada, contrasena cambiada y cuenta eliminada con el repositorio`() = runBlocking {
        // Sin track(): la cuenta se borra en la prueba; si falla, el finally la limpia.
        val email = E2eEnv.uniqueEmail()
        val r = E2eEnv.admin("auth/v1/admin/users", "POST",
            """{"email":"$email","password":"${E2eEnv.PASSWORD}","email_confirm":true,"user_metadata":{"name":"Se va Repo E2E"}}""")
        assertTrue("crear usuario ${r.code}: ${r.body}", r.ok)
        val id = r.json().get("id").asString
        try {
            repo.login(email, E2eEnv.PASSWORD).getOrThrow()

            // Ítem 25: con el token guardado, el arranque restaura la sesión sin pasar por Login.
            assertTrue(repo.restoreSession())
            assertEquals(id, repo.currentUser.value?.id)

            // Ítem 11: PUT auth/v1/user {"password"}.
            val newPassword = "E2e-Nueva-5678!"
            repo.updatePassword(newPassword).getOrThrow()
            assertEquals(id, E2eEnv.login(email, newPassword).second)
            assertFalse("la contraseña anterior sigue sirviendo",
                E2eEnv.rest("auth/v1/token?grant_type=password", "POST", """{"email":"$email","password":"${E2eEnv.PASSWORD}"}""").ok)

            // Ítem 8: delete_my_account borra auth.users y cierra la sesión local.
            repo.deleteAccount().getOrThrow()
            assertEquals(404, E2eEnv.admin("auth/v1/admin/users/$id").code)
            assertNull(SupabaseSession.accessToken)
            assertNull(repo.currentUser.value)
        } finally {
            E2eEnv.admin("auth/v1/admin/users/$id", "DELETE")
        }
    }
}
