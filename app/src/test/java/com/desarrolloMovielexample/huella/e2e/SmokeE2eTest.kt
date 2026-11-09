package com.desarrolloMovielexample.huella.e2e

import androidx.test.core.app.ApplicationProvider
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.data.remote.SupabaseSession
import com.desarrolloMovielexample.huella.domain.model.Role
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Camino feliz contra Supabase real con el SupabaseRepository y el RetrofitClient de la app, con usuarios propios. */
@RunWith(RobolectricTestRunner::class)
class SmokeE2eTest {
    companion object {
        @AfterClass @JvmStatic
        fun cleanup() = E2eEnv.cleanup()
    }

    @Before
    fun setUp() {
        E2eEnv.assumeConfigured()
        SupabaseSession.clear()
    }

    @Test
    fun `login de un moderador nuevo y getProfile devuelve su perfil`() = runBlocking {
        val mod = E2eEnv.createUser(name = "Moderadora E2E", city = "Cali")
        E2eEnv.makeModerator(mod)
        SupabaseRepository.initialize(ApplicationProvider.getApplicationContext())
        val user = SupabaseRepository.login(mod.email, E2eEnv.PASSWORD).getOrThrow()
        assertEquals(mod.email, user.email)
        assertEquals("Moderadora E2E", user.name)
        assertEquals(Role.MODERADOR, user.role)

        // getProfile con el token de la sesión que acaba de guardar login().
        val profile = E2eEnv.api().getProfile(userId = "eq.${user.id}")
        assertTrue("getProfile ${profile.code()}: ${profile.errorBody()?.string()}", profile.isSuccessful)
        assertEquals(listOf(user.id), profile.body()!!.map { it.get("id").asString })
        assertEquals("Cali", profile.body()!!.single().get("city").asString)
    }

    @Test
    fun `publicacion aprobada por moderate_publication aparece en el feed y el feed solo trae aprobadas`() = runBlocking {
        val author = E2eEnv.createUser(name = "Autor E2E")
        val mod = E2eEnv.createUser(name = "Mod E2E")
        E2eEnv.makeModerator(mod)
        val created = E2eEnv.rest(
            "rest/v1/publications", "POST",
            """{"author_id":"${author.id}","title":"E2E feed","species":"Gato","breed":"Criollo","size":"Pequeño","city":"Medellín","description":"Prueba e2e"}""",
            author.token, headers = mapOf("Prefer" to "return=representation"),
        )
        assertTrue("crear publicación ${created.code}: ${created.body}", created.ok)
        val pubId = created.rows().single().get("id").asString

        val mod1 = E2eEnv.rpc("moderate_publication", """{"p_publication_id":"$pubId","p_status":"APROBADA"}""", mod.token)
        assertTrue("moderate_publication ${mod1.code}: ${mod1.body}", mod1.ok)

        val feed = E2eEnv.api().getPublications()
        assertTrue("getPublications ${feed.code()}: ${feed.errorBody()?.string()}", feed.isSuccessful)
        val rows = feed.body()!!
        assertTrue("la publicación aprobada no está en el feed", rows.any { it.get("id").asString == pubId })
        assertTrue(rows.all { it.get("status").asString == "APROBADA" })

        // Sin filtro de status, RLS tampoco debe dejar ver al anónimo nada que no esté aprobado.
        val all = E2eEnv.rest("rest/v1/publications?select=status")
        assertTrue("publications anon ${all.code}: ${all.body}", all.ok)
        assertTrue(all.rows().all { it.get("status").asString == "APROBADA" })
    }
}
