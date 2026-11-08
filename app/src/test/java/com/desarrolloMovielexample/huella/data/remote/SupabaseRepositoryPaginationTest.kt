package com.desarrolloMovielexample.huella.data.remote

import com.desarrolloMovielexample.huella.domain.model.Category
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.Collections

/** Ítem 32: feed paginado con filtros de PostgREST y conversación buscada en el servidor. */
@RunWith(RobolectricTestRunner::class)
class SupabaseRepositoryPaginationTest {
    private val server = MockWebServer()
    private val requests = Collections.synchronizedList(mutableListOf<RecordedRequest>())

    private fun rows(from: Int, count: Int) = (from until from + count).joinToString(",", "[", "]") {
        """{"id":"p$it","author_id":"a","title":"T$it","category":"ADOPCION","status":"APROBADA","created_at":"2026-11-08T20:00:00+00:00"}"""
    }

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val url = request.requestUrl!!
                val body = when {
                    url.encodedPath == "/rest/v1/publications" ->
                        if (url.queryParameter("offset") == "0") rows(0, 20) else rows(20, 5)
                    url.encodedPath == "/rest/v1/conversations" ->
                        """[{"id":"c1","participant_one":"u1","participant_two":"u2","publication_id":"p1"}]"""
                    else -> "[]"
                }
                return MockResponse().setHeader("Content-Type", "application/json").setBody(body)
            }
        }
        server.start()
        val context = RuntimeEnvironment.getApplication()
        SupabaseConfig.override(server.url("/").toString(), "test-key")
        SupabaseSession.initialize(context)
        SupabaseSession.save(context, "token", "u1", "refresh")
        SupabaseRepository.initialize(context)
    }

    @After
    fun tearDown() {
        SupabaseRepository.logout()
        server.shutdown()
    }

    @Test
    fun `feed pide paginas de 20 con filtros del servidor y anade la siguiente`() = runBlocking {
        val more = SupabaseRepository.loadFeed(Category.ADOPCION, "Perro", "Medellín, Antioquia", reset = true).getOrThrow()
        assertTrue(more)
        val first = requests.last { it.requestUrl!!.encodedPath == "/rest/v1/publications" }.requestUrl!!
        assertEquals("eq.ADOPCION", first.queryParameter("category"))
        assertEquals("eq.Perro", first.queryParameter("species"))
        assertEquals("eq.Medellín", first.queryParameter("city"))
        assertEquals("eq.APROBADA", first.queryParameter("status"))
        assertEquals("20", first.queryParameter("limit"))
        assertEquals("0", first.queryParameter("offset"))
        assertEquals(20, SupabaseRepository.publications.value.size)

        val stillMore = SupabaseRepository.loadFeed(null, null, null, reset = false).getOrThrow()
        assertFalse(stillMore)
        val second = requests.last { it.requestUrl!!.encodedPath == "/rest/v1/publications" }.requestUrl!!
        assertEquals("20", second.queryParameter("offset"))
        assertEquals("eq.ADOPCION", second.queryParameter("category")) // la página siguiente mantiene los filtros
        assertEquals(25, SupabaseRepository.publications.value.size)
    }

    @Test
    fun `abrir conversacion filtra el par y la publicacion en la consulta`() = runBlocking {
        val id = SupabaseRepository.openConversation("u2", "p1").getOrThrow()
        assertEquals("c1", id)
        val url = requests.single { it.requestUrl!!.encodedPath == "/rest/v1/conversations" }.requestUrl!!
        assertEquals(
            "(and(participant_one.eq.u1,participant_two.eq.u2),and(participant_one.eq.u2,participant_two.eq.u1))",
            url.queryParameter("or"),
        )
        assertEquals("eq.p1", url.queryParameter("publication_id"))
    }
}
