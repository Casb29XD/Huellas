package com.desarrolloMovielexample.huella.features.feed

import com.desarrolloMovielexample.huella.domain.repository.FakeRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceFilterTest {
    private val medellin = 6.2442 to -75.5812

    @Test
    fun `Medellin a Envigado son unos 8 km`() {
        val km = distanceKm(medellin.first, medellin.second, 6.1759, -75.5917)
        assertEquals(7.6, km, 1.0)
    }

    @Test
    fun `cerca de mi deja solo lo que esta dentro del radio y con punto en el mapa`() {
        val base = FakeRepository.publications.value.first()
        val pubs = listOf(
            base.copy(id = "cerca", lat = 6.2500, lon = -75.5700),     // ~1.5 km
            base.copy(id = "lejos", lat = 4.7110, lon = -74.0721),     // Bogotá, ~240 km
            base.copy(id = "sin-mapa", lat = 0.0, lon = 0.0),
        )
        val filters = FeedFilters(maxDistanceKm = 10, origin = medellin)
        assertEquals(listOf("cerca"), filterFeed(pubs, "", null, filters).map { it.id })
        assertEquals(3, filterFeed(pubs, "", null, FeedFilters()).size)
    }
}
