package com.desarrolloMovielexample.huella.data.remote

import com.desarrolloMovielexample.huella.R
import com.desarrolloMovielexample.huella.core.utils.toUiText
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Ítems 37 y 60: tiempos relativos y errores del repositorio salen de recursos en el idioma de la UI. */
@RunWith(RobolectricTestRunner::class)
class RelativeTimeTest {
    private val res get() = RuntimeEnvironment.getApplication().resources
    private val now = 1_800_000_000_000L
    private fun ago(minutes: Long) = relativeTimeText(now - minutes * 60_000L, now).resolve(res)

    @Test
    @Config(qualifiers = "es")
    fun `espanol`() {
        assertEquals("ahora", ago(0))
        assertEquals("hace 5 min", ago(5))
        assertEquals("hace 3 h", ago(180))
        assertEquals("hace 1 día", ago(1440))
        assertEquals("hace 4 días", ago(4 * 1440))
        assertEquals("hoy", relativeTimeText(null, now).resolve(res))
    }

    @Test
    @Config(qualifiers = "en")
    fun `ingles`() {
        assertEquals("now", ago(0))
        assertEquals("5 min ago", ago(5))
        assertEquals("3 h ago", ago(180))
        assertEquals("1 day ago", ago(1440))
        assertEquals("4 days ago", ago(4 * 1440))
    }

    @Test
    @Config(qualifiers = "en")
    fun `error propio del repositorio se traduce`() = runBlocking {
        SupabaseConfig.override("http://127.0.0.1:9/", "test-key")
        SupabaseSession.initialize(RuntimeEnvironment.getApplication())
        SupabaseSession.clear(RuntimeEnvironment.getApplication())
        SupabaseRepository.initialize(RuntimeEnvironment.getApplication())
        val error = SupabaseRepository.addComment("p1", "hola").exceptionOrNull()!!
        assertEquals("Sign in to continue", error.toUiText(R.string.error_comment_post).resolve(res))
    }
}
