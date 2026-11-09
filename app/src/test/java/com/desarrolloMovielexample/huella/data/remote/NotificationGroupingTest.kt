package com.desarrolloMovielexample.huella.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

/** Las notificaciones se agrupan por el día local de created_at, no por el group_label fijo de la BD. */
class NotificationGroupingTest {
    private val bogota = ZoneId.of("America/Bogota")
    private val now = parseSupabaseDate("2026-11-08T17:00:00Z")!! // 12:00 en Bogotá

    private fun group(createdAt: String) = notificationGroup(parseSupabaseDate(createdAt), now, bogota)

    @Test fun `mismo dia local es Hoy`() = assertEquals("Hoy", group("2026-11-08T06:00:00+00:00"))
    @Test fun `antes de medianoche local es Ayer aunque en UTC sea hoy`() = assertEquals("Ayer", group("2026-11-08T03:00:00+00:00"))
    @Test fun `entre 2 y 6 dias es Esta semana`() = assertEquals("Esta semana", group("2026-11-03T15:00:00+00:00"))
    @Test fun `7 dias o mas es Antes`() = assertEquals("Antes", group("2026-11-01T15:00:00+00:00"))
    @Test fun `sin fecha es Antes`() = assertEquals("Antes", notificationGroup(null, now, bogota))
    @Test fun `reloj del servidor adelantado sigue siendo Hoy`() = assertEquals("Hoy", group("2026-11-08T18:00:00+00:00"))
}
