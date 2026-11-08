package com.desarrolloMovielexample.huella.data.remote

import com.desarrolloMovielexample.huella.data.remote.api.SupabaseApiService
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.lang.reflect.Proxy

/** Cualquier endpoint que el mock no sobrescribe falla con el nombre del método. */
private val unsupported = Proxy.newProxyInstance(
    SupabaseApiService::class.java.classLoader,
    arrayOf(SupabaseApiService::class.java),
) { _, method, _ -> throw UnsupportedOperationException("MockSupabaseApiService.${method.name}") } as SupabaseApiService

/**
 * Doble en memoria de [SupabaseApiService] solo con los endpoints que usa SupabaseEndpointsMockTest.
 * ponytail: el resto delega en un Proxy que lanza; añade un override cuando un test lo necesite.
 */
class MockSupabaseApiService : SupabaseApiService by unsupported {

    val mockPublications: MutableList<JsonObject> = mutableListOf()
    val mockRequests: MutableList<JsonObject> = mutableListOf()
    val mockMessages: MutableList<JsonObject> = mutableListOf()
    val mockNotifications: MutableList<JsonObject> = mutableListOf()

    var lastModeratedPayload: JsonObject? = null
    var shouldFailAuth: Boolean = false

    override suspend fun signIn(body: JsonObject): Response<JsonObject> {
        if (shouldFailAuth) {
            val errBody = "{\"message\":\"Credenciales invalidas\"}".toResponseBody("application/json".toMediaTypeOrNull())
            return Response.error(400, errBody)
        }
        val res = JsonObject().apply {
            addProperty("access_token", "fake_mock_token_123")
            addProperty("refresh_token", "fake_mock_refresh_123")
            add("user", JsonObject().apply { addProperty("id", "user_mock_1") })
        }
        return Response.success(res)
    }

    override suspend fun getPublications(
        select: String,
        status: String?,
        category: String?,
        species: String?,
        city: String?,
        authorId: String?,
        updatedAfter: String?,
        order: String,
        limit: Int
    ): Response<List<JsonObject>> {
        val filtered = if (status != null && status.startsWith("eq.")) {
            val st = status.substringAfter("eq.")
            mockPublications.filter { it.get("status")?.asString == st }
        } else {
            mockPublications
        }
        return Response.success(filtered)
    }

    override suspend fun createPublication(body: JsonObject, prefer: String): Response<List<JsonObject>> {
        mockPublications.add(body)
        return Response.success(listOf(body))
    }

    override suspend fun moderatePublication(body: JsonObject): Response<Unit> {
        lastModeratedPayload = body
        val pubId = body.get("p_publication_id")?.asString
        val newStatus = body.get("p_status")?.asString
        mockPublications.forEach {
            if (it.get("id")?.asString == pubId) {
                it.addProperty("status", newStatus)
            }
        }
        return Response.success(Unit)
    }

    override suspend fun createAdoptionRequest(body: JsonObject): Response<Unit> {
        mockRequests.add(body)
        return Response.success(Unit)
    }

    override suspend fun sendMessage(body: JsonObject): Response<Unit> {
        mockMessages.add(body)
        return Response.success(Unit)
    }

    override suspend fun markNotificationsRead(unread: String, body: JsonObject, prefer: String): Response<Unit> {
        mockNotifications.forEach { it.addProperty("read", true) }
        return Response.success(Unit)
    }
}
