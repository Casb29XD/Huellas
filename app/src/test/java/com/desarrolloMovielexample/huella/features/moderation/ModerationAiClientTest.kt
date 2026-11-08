package com.desarrolloMovielexample.huella.features.moderation

import com.desarrolloMovielexample.huella.data.remote.ModerationAiClient
import com.desarrolloMovielexample.huella.data.remote.SupabaseConfig
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ModerationAiClientTest {
    private val server = MockWebServer()
    private var oldUrl = ""
    private var oldKey = ""

    @Before fun setUp() {
        server.start()
        oldUrl = SupabaseConfig.url
        oldKey = SupabaseConfig.publishableKey
        SupabaseConfig.override(server.url("/").toString(), "test-key")
    }

    @After fun tearDown() {
        SupabaseConfig.override(oldUrl, oldKey)
        server.shutdown()
    }

    @Test
    fun `ok parsea la sugerencia`() = runTest {
        server.enqueue(MockResponse().setBody(
            """{"status":"ok","review":{"verdict":"RECHAZAR","risk":0.82,"reasons":["Pide dinero"],"suggested_reason":"Venta de animales"}}""",
        ))
        val review = ModerationAiClient.analyze("p1")!!
        assertEquals("RECHAZAR", review.verdict)
        assertEquals(0.82, review.risk, 0.0001)
        assertEquals(listOf("Pide dinero"), review.reasons)
        assertEquals("Venta de animales", review.suggestedReason)
        val request = server.takeRequest()
        assertEquals("/functions/v1/moderate-ai", request.path)
        assertTrue(request.body.readUtf8().contains("\"publication_id\":\"p1\""))
        assertEquals("test-key", request.getHeader("apikey"))
    }

    @Test
    fun `not_configured devuelve null aunque llegue con error HTTP`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("""{"status":"not_configured"}"""))
        assertNull(ModerationAiClient.analyze("p1"))
    }

    @Test
    fun `error HTTP lanza con el mensaje del servidor`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody("""{"error":"Solo moderadores"}"""))
        val e = runCatching { ModerationAiClient.analyze("p1") }.exceptionOrNull()
        assertEquals("Solo moderadores", e?.message)
    }
}
