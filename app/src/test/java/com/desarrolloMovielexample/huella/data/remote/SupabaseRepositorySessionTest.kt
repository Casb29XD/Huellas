package com.desarrolloMovielexample.huella.data.remote

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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

/** Login de un moderador, logout y login de otra cuenta: no queda nada de la primera en memoria. */
@RunWith(RobolectricTestRunner::class)
class SupabaseRepositorySessionTest {
    private val server = MockWebServer()
    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                val moderator = request.getHeader("Authorization") == "Bearer token-a"
                return when {
                    path.startsWith("/auth/v1/token?grant_type=password") -> {
                        val a = request.body.readUtf8().contains("a@huella.test")
                        json(if (a) """{"access_token":"token-a","user":{"id":"ua"}}""" else """{"access_token":"token-b","user":{"id":"ub"}}""")
                    }
                    path.startsWith("/rest/v1/rpc/get_my_profile") ->
                        json(if (moderator) """[{"id":"ua","name":"A","is_moderator":true}]""" else """[{"id":"ub","name":"B"}]""")
                    path.startsWith("/rest/v1/notifications") ->
                        json(if (moderator) """[{"id":"na","type":"NEW_REQUEST","title":"de A"}]""" else """[{"id":"nb","type":"LEVEL_UP","title":"de B"}]""")
                    path.startsWith("/rest/v1/publications") && path.contains("EN_REVISION") && moderator ->
                        json("""[{"id":"p1","title":"pendiente","author_id":"ux"}]""")
                    path.startsWith("/rest/v1/reports") && moderator ->
                        json("""[{"id":"r1","publication_id":"p2","reason":"Spam","publication":{"id":"p2","title":"reportada"}}]""")
                    else -> json("[]")
                }
            }
        }
        server.start()
        SupabaseConfig.override(server.url("/").toString(), "test-key")
        SupabaseSession.initialize(context)
        SupabaseSession.clear(context)
        SupabaseRepository.initialize(context)
    }

    @After
    fun tearDown() {
        SupabaseRepository.logout()
        server.shutdown()
    }

    @Test
    fun `logout de un moderador y login de un usuario no deja datos de la primera cuenta`() = runBlocking {
        assertTrue(SupabaseRepository.login("a@huella.test", "x").isSuccess)
        withTimeout(5_000) { SupabaseRepository.moderationReports.first { it.isNotEmpty() } }
        withTimeout(5_000) { SupabaseRepository.notifications.first { it.isNotEmpty() } }

        SupabaseRepository.logout()
        assertTrue(SupabaseRepository.moderationQueue.value.isEmpty())
        assertTrue(SupabaseRepository.moderationReports.value.isEmpty())
        assertTrue(SupabaseRepository.notifications.value.isEmpty())

        assertTrue(SupabaseRepository.login("b@huella.test", "x").isSuccess)
        val notifications = withTimeout(5_000) { SupabaseRepository.notifications.first { it.isNotEmpty() } }
        assertEquals(listOf("nb"), notifications.map { it.id })
        assertTrue(SupabaseRepository.moderationQueue.value.isEmpty())
        assertTrue(SupabaseRepository.moderationReports.value.isEmpty())
        assertEquals(0, SupabaseRepository.moderationStats.value.pending)
    }

    private fun json(body: String) =
        MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(body)
}
