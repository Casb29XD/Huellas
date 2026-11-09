package com.desarrolloMovielexample.huella.data.remote

import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.PublicationDraft
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.Collections

/** Ítem 27: si el refresco posterior falla, la escritura ya hecha se reporta como éxito (sin reintentos duplicados). */
@RunWith(RobolectricTestRunner::class)
class SupabaseRepositoryWritesTest {
    private val server = MockWebServer()
    private val requests = Collections.synchronizedList(mutableListOf<RecordedRequest>())

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                return if (request.method == "POST" && request.path!!.startsWith("/rest/v1/publications")) {
                    MockResponse().setResponseCode(201).setHeader("Content-Type", "application/json")
                        .setBody("""[{"id":"p1","author_id":"u1","title":"Luna","category":"ADOPCION","status":"EN_REVISION","created_at":"2026-11-08T20:00:00+00:00"}]""")
                } else {
                    MockResponse().setResponseCode(500).setBody("""{"message":"caído"}""")
                }
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
        SupabaseSession.clear(RuntimeEnvironment.getApplication())
        server.shutdown()
    }

    @Test
    fun `crear publicacion es exito aunque fallen las lecturas posteriores y no se repite el POST`() = runBlocking {
        val draft = PublicationDraft(Category.ADOPCION, "Luna", "desc", "Perro", "Criollo", "Mediano", true, false, "2 años", "Hembra", "Medellín")
        val result = SupabaseRepository.createPublication(draft)
        assertTrue(result.exceptionOrNull()?.message, result.isSuccess)
        assertEquals("p1", result.getOrThrow().id)
        assertEquals(1, requests.count { it.method == "POST" && it.path!!.startsWith("/rest/v1/publications") })
    }
}
