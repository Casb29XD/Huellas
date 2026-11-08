package com.desarrolloMovielexample.huella.data.remote

import android.net.Uri
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.security.MessageDigest
import java.util.Base64
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/** SupabaseRepository real contra MockWebServer: PKCE (ítems 11/36) y refresh serializado (ítem 26). */
@RunWith(RobolectricTestRunner::class)
class SupabaseRepositoryAuthTest {
    private val server = MockWebServer()
    private val requests = Collections.synchronizedList(mutableListOf<RecordedRequest>())
    private val refreshCalls = AtomicInteger()
    private var refreshAccepted = true
    private val context get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val path = request.path.orEmpty()
                return when {
                    path.startsWith("/auth/v1/signup") -> json("""{"id":"u1","email":"ana@huella.test"}""")
                    path.startsWith("/auth/v1/token?grant_type=pkce") -> json(SESSION)
                    path.startsWith("/auth/v1/token?grant_type=refresh_token") -> {
                        refreshCalls.incrementAndGet()
                        Thread.sleep(200) // ventana para que la segunda llamada llegue con 401 mientras tanto
                        if (refreshAccepted) json(SESSION) else json("""{"error_description":"Invalid Refresh Token"}""", 400)
                    }
                    path.startsWith("/rest/v1/rpc/get_my_profile") ->
                        if (request.getHeader("Authorization") == "Bearer new-token") json("""[{"id":"u1","name":"Ana"}]""")
                        else json("""{"message":"JWT expired"}""", 401)
                    else -> json("[]")
                }
            }
        }
        server.start()
        SupabaseConfig.override(server.url("/").toString(), "test-key")
        SupabaseSession.initialize(context)
        SupabaseSession.clear(context)
        SupabaseSession.saveCodeVerifier(context, null)
        SupabaseRepository.initialize(context)
    }

    @After
    fun tearDown() = server.shutdown()

    @Test
    fun `registro envia code_challenge S256 del verifier guardado y el canje usa grant_type pkce`() = runBlocking {
        // Con confirmación de correo activa no llega sesión: la app pide confirmar el correo.
        val result = SupabaseRepository.register("Ana", "ana@huella.test", "Medellín, Antioquia", "secreta123")
        assertTrue(result.isFailure)

        val signup = requests.first { it.path!!.startsWith("/auth/v1/signup") }
        assertTrue(signup.path!!.contains("redirect_to=huella%3A%2F%2Fauth%2Fconfirm"))
        val body = JsonParser.parseString(signup.body.readUtf8()).asJsonObject
        val verifier = SupabaseSession.codeVerifier!!
        assertEquals("s256", body.get("code_challenge_method").asString)
        assertEquals(s256(verifier), body.get("code_challenge").asString)

        val auth = SupabaseRepository.handleAuthCallback(Uri.parse("huella://auth/confirm?code=abc-123"))
        assertTrue(auth.exceptionOrNull()?.message, auth.isSuccess)
        val token = requests.first { it.path!!.startsWith("/auth/v1/token") }
        assertEquals("/auth/v1/token?grant_type=pkce", token.path)
        val tokenBody = JsonParser.parseString(token.body.readUtf8()).asJsonObject
        assertEquals("abc-123", tokenBody.get("auth_code").asString)
        assertEquals(verifier, tokenBody.get("code_verifier").asString)
        assertEquals("new-token", SupabaseSession.accessToken)
        assertNull(SupabaseSession.codeVerifier) // un code, un uso
    }

    @Test
    fun `dos 401 a la vez gastan un solo refresh token`() = runBlocking {
        SupabaseSession.save(context, "old-token", "u1", "refresh-1")
        val restored = (1..2).map { async(Dispatchers.IO) { SupabaseRepository.restoreSession() } }.awaitAll()
        assertEquals(listOf(true, true), restored)
        assertEquals(1, refreshCalls.get())
        assertEquals("new-token", SupabaseSession.accessToken)
    }

    @Test
    fun `refresh rechazado cierra la sesion y avisa`() = runBlocking {
        refreshAccepted = false
        SupabaseSession.save(context, "old-token", "u1", "refresh-1")
        val expired = async(Dispatchers.IO) { withTimeout(5_000) { SupabaseRepository.sessionExpired.first() } }
        Thread.sleep(100) // el colector ya está suscrito antes de que falle el refresh
        assertFalse(SupabaseRepository.restoreSession())
        expired.await()
        assertNull(SupabaseSession.accessToken)
    }

    private fun json(body: String, code: Int = 200) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    private fun s256(verifier: String) = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))

    private companion object {
        const val SESSION = """{"access_token":"new-token","refresh_token":"refresh-2","user":{"id":"u1"}}"""
    }
}
