package com.desarrolloMovielexample.huella.data.remote

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

/** Ítem 12: las horas de Supabase son UTC y no deben desfasarse con la zona del teléfono (Colombia, UTC-5). */
class DateParsingTest {
    private val original = TimeZone.getDefault()
    // 2026-11-08T15:00:00Z
    private val expected = 1_794_150_000_000L

    @Before fun bogota() = TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"))
    @After fun restore() = TimeZone.setDefault(original)

    @Test fun `offset +00 00 de PostgREST`() = assertEquals(expected, parseSupabaseDate("2026-11-08T15:00:00+00:00"))
    @Test fun `microsegundos`() = assertEquals(expected + 123, parseSupabaseDate("2026-11-08T15:00:00.123456+00:00"))
    @Test fun `sufijo Z`() = assertEquals(expected, parseSupabaseDate("2026-11-08T15:00:00Z"))
    @Test fun `sin zona se asume UTC`() = assertEquals(expected, parseSupabaseDate("2026-11-08T15:00:00"))
    @Test fun `offset negativo`() = assertEquals(expected, parseSupabaseDate("2026-11-08T10:00:00-05:00"))
    @Test fun `formato de Postgres con espacio y offset corto`() = assertEquals(expected, parseSupabaseDate("2026-11-08 15:00:00+00"))
    @Test fun `vacio o basura devuelve null`() {
        assertNull(parseSupabaseDate(null))
        assertNull(parseSupabaseDate(""))
        assertNull(parseSupabaseDate("ayer"))
    }
}
