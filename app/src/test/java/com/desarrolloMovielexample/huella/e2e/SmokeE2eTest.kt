package com.desarrolloMovielexample.huella.e2e

import androidx.test.core.app.ApplicationProvider
import com.desarrolloMovielexample.huella.data.remote.SupabaseRepository
import com.desarrolloMovielexample.huella.data.remote.SupabaseSession
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Camino feliz contra Supabase real con el SupabaseRepository y el RetrofitClient de la app. */
@RunWith(RobolectricTestRunner::class)
class SmokeE2eTest {
    @Before
    fun setUp() {
        E2eEnv.assumeConfigured()
        SupabaseSession.clear()
    }

    @Test
    fun `login de laura y getProfile devuelve su perfil`() = runBlocking {
        SupabaseRepository.initialize(ApplicationProvider.getApplicationContext())
        val (email, password) = E2eEnv.LAURA
        val user = SupabaseRepository.login(email, password).getOrThrow()
        assertEquals(email, user.email)
        assertEquals("Laura Gómez", user.name)

        // getProfile con el token de la sesión que acaba de guardar login().
        val profile = E2eEnv.api().getProfile(userId = "eq.${user.id}")
        assertTrue("getProfile ${profile.code()}: ${profile.errorBody()?.string()}", profile.isSuccessful)
        assertEquals(listOf(user.id), profile.body()!!.map { it.get("id").asString })
    }

    @Test
    fun `feed devuelve solo publicaciones aprobadas`() = runBlocking {
        val feed = E2eEnv.api().getPublications()
        assertTrue("getPublications ${feed.code()}: ${feed.errorBody()?.string()}", feed.isSuccessful)
        val rows = feed.body()!!
        assertTrue("el feed semilla no debería estar vacío", rows.isNotEmpty())
        assertTrue(rows.all { it.get("status").asString == "APROBADA" })

        // Sin filtro de status, RLS tampoco debe dejar ver al anónimo nada que no esté aprobado.
        val all = E2eEnv.rest("rest/v1/publications?select=status")
        assertTrue("publications anon ${all.code}: ${all.body}", all.ok)
        assertTrue(all.rows().all { it.get("status").asString == "APROBADA" })
    }
}
