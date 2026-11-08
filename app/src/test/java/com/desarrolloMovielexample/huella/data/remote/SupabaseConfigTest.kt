package com.desarrolloMovielexample.huella.data.remote

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Ítem 34: sin app/.env la app no crashea; las llamadas fallan con un mensaje accionable. */
class SupabaseConfigTest {
    private val saved = SupabaseConfig.url to SupabaseConfig.publishableKey
    private val msg = "Configura SUPABASE_URL y SUPABASE_PUBLISHABLE_KEY en app/.env"

    @After
    fun restore() = SupabaseConfig.override(saved.first, saved.second)

    @Test
    fun `sin configuracion supabaseApi lanza IllegalStateException`() {
        SupabaseConfig.override("", "")
        val e = assertThrows(IllegalStateException::class.java) { RetrofitClient.supabaseApi }
        assertEquals(msg, e.message)
    }

    @Test
    fun `sin configuracion login falla con el mensaje en vez de crashear`() = runTest {
        SupabaseConfig.override("", "")
        assertEquals(msg, SupabaseRepository.login("a@b.co", "x").exceptionOrNull()?.message)
    }
}
