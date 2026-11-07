package com.desarrolloMovielexample.huella.data.remote

import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Suite de tests unitarios locales simulando el consumo de endpoints con [MockSupabaseApiService].
 */
class SupabaseEndpointsMockTest {

    private lateinit var api: MockSupabaseApiService

    @Before
    fun setUp() {
        api = MockSupabaseApiService()
    }

    @Test
    fun testAuthSignInSuccess() = runBlocking {
        val payload = JsonObject().apply {
            addProperty("email", "admin@huella.org")
            addProperty("password", "secret123")
        }
        val response = api.signIn(payload)
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertNotNull(body)
        assertEquals("fake_mock_token_123", body?.get("access_token")?.asString)
        assertEquals("user_mock_1", body?.getAsJsonObject("user")?.get("id")?.asString)
    }

    @Test
    fun testAuthSignInFailure() = runBlocking {
        api.shouldFailAuth = true
        val payload = JsonObject().apply {
            addProperty("email", "wrong@user.com")
            addProperty("password", "badpass")
        }
        val response = api.signIn(payload)
        assertFalse(response.isSuccessful)
        assertEquals(400, response.code())
    }

    @Test
    fun testGetPublicationsFiltering() = runBlocking {
        // Sembrar 2 aprobadas y 1 en revisión
        api.mockPublications.add(JsonObject().apply {
            addProperty("id", "p1")
            addProperty("title", "Max")
            addProperty("status", "APROBADA")
        })
        api.mockPublications.add(JsonObject().apply {
            addProperty("id", "p2")
            addProperty("title", "Luna")
            addProperty("status", "APROBADA")
        })
        api.mockPublications.add(JsonObject().apply {
            addProperty("id", "p3")
            addProperty("title", "Rocky")
            addProperty("status", "EN_REVISION")
        })

        // Consultar solo las aprobadas (Feed principal)
        val response = api.getPublications(select = "*", status = "eq.APROBADA")
        assertTrue(response.isSuccessful)
        val list = response.body()
        assertNotNull(list)
        assertEquals(2, list?.size)
        assertTrue(list?.all { it.get("status").asString == "APROBADA" } == true)
    }

    @Test
    fun testCreatePublicationEndpoint() = runBlocking {
        val newPost = JsonObject().apply {
            addProperty("id", "p_new")
            addProperty("title", "Michi en adopción")
            addProperty("status", "EN_REVISION")
            addProperty("species", "Gato")
        }
        val response = api.createPublication(newPost)
        assertTrue(response.isSuccessful)
        assertEquals(1, api.mockPublications.size)
        assertEquals("Michi en adopción", api.mockPublications.first().get("title").asString)
    }

    @Test
    fun testModeratePublicationRpcEndpoint() = runBlocking {
        val post = JsonObject().apply {
            addProperty("id", "p_mod_1")
            addProperty("title", "Perro rescatado")
            addProperty("status", "EN_REVISION")
        }
        api.mockPublications.add(post)

        // Ejecutar llamada al RPC de moderación
        val rpcPayload = JsonObject().apply {
            addProperty("p_publication_id", "p_mod_1")
            addProperty("p_status", "APROBADA")
        }
        val res = api.moderatePublication(rpcPayload)
        assertTrue(res.isSuccessful)

        // Verificar que el estado del post cambió en la lista
        val updatedPost = api.mockPublications.first { it.get("id").asString == "p_mod_1" }
        assertEquals("APROBADA", updatedPost.get("status").asString)
        assertEquals("p_mod_1", api.lastModeratedPayload?.get("p_publication_id")?.asString)
    }

    @Test
    fun testAdoptionRequestsAndChatMessagesFlow() = runBlocking {
        // 1. Enviar solicitud
        val reqPayload = JsonObject().apply {
            addProperty("publication_id", "p1")
            addProperty("applicant_id", "u_user2")
            addProperty("message", "Quiero adoptarlo")
        }
        val reqRes = api.createAdoptionRequest(reqPayload)
        assertTrue(reqRes.isSuccessful)
        assertEquals(1, api.mockRequests.size)

        // 2. Enviar mensaje de chat
        val msgPayload = JsonObject().apply {
            addProperty("conversation_id", "conv_123")
            addProperty("sender_id", "u_user2")
            addProperty("text", "¿Cuándo puedo ir a verlo?")
        }
        val msgRes = api.sendMessage(msgPayload)
        assertTrue(msgRes.isSuccessful)
        assertEquals(1, api.mockMessages.size)
        assertEquals("¿Cuándo puedo ir a verlo?", api.mockMessages.first().get("text").asString)
    }

    @Test
    fun testNotificationsMarkAllReadEndpoint() = runBlocking {
        api.mockNotifications.add(JsonObject().apply {
            addProperty("id", "n1")
            addProperty("read", false)
        })
        api.mockNotifications.add(JsonObject().apply {
            addProperty("id", "n2")
            addProperty("read", false)
        })

        val res = api.markNotificationsRead()
        assertTrue(res.isSuccessful)
        assertTrue(api.mockNotifications.all { it.get("read").asBoolean })
    }
}
