package com.desarrolloMovielexample.huella.data.remote

import com.desarrolloMovielexample.huella.data.remote.api.SupabaseApiService
import com.google.gson.JsonObject
import okhttp3.RequestBody
import retrofit2.Response

/**
 * Mock implementation of [SupabaseApiService] for local unit testing without calling the real network.
 */
class MockSupabaseApiService : SupabaseApiService {

    var mockProfiles: MutableList<JsonObject> = mutableListOf()
    var mockPublications: MutableList<JsonObject> = mutableListOf()
    var mockReports: MutableList<JsonObject> = mutableListOf()
    var mockRequests: MutableList<JsonObject> = mutableListOf()
    var mockConversations: MutableList<JsonObject> = mutableListOf()
    var mockMessages: MutableList<JsonObject> = mutableListOf()
    var mockNotifications: MutableList<JsonObject> = mutableListOf()
    var mockPointsHistory: MutableList<JsonObject> = mutableListOf()

    var lastModeratedPayload: JsonObject? = null
    var shouldFailAuth: Boolean = false

    override suspend fun uploadPetImage(objectPath: String, body: RequestBody, contentType: String, upsert: String): Response<JsonObject> {
        val res = JsonObject().apply { addProperty("Key", "pet-images/$objectPath") }
        return Response.success(res)
    }

    override suspend fun signIn(body: JsonObject): Response<JsonObject> {
        if (shouldFailAuth) {
            val errBody = okhttp3.ResponseBody.create(okhttp3.MediaType.parse("application/json"), "{\"message\":\"Credenciales invalidas\"}")
            return Response.error(400, errBody)
        }
        val res = JsonObject().apply {
            addProperty("access_token", "fake_mock_token_123")
            addProperty("refresh_token", "fake_mock_refresh_123")
            add("user", JsonObject().apply { addProperty("id", "user_mock_1") })
        }
        return Response.success(res)
    }

    override suspend fun refreshSession(body: JsonObject): Response<JsonObject> {
        val res = JsonObject().apply {
            addProperty("access_token", "refreshed_mock_token")
            addProperty("refresh_token", "refreshed_refresh_token")
        }
        return Response.success(res)
    }

    override suspend fun signUp(body: JsonObject): Response<JsonObject> {
        val res = JsonObject().apply {
            addProperty("access_token", "signup_mock_token")
            addProperty("refresh_token", "signup_refresh_token")
            add("user", JsonObject().apply { addProperty("id", "user_mock_signup") })
        }
        return Response.success(res)
    }

    override suspend fun recoverPassword(body: JsonObject): Response<JsonObject> {
        return Response.success(JsonObject())
    }

    override suspend fun getUser(bearerToken: String): Response<JsonObject> {
        val res = JsonObject().apply { addProperty("id", "user_mock_1") }
        return Response.success(res)
    }

    override suspend fun verifyOtp(tokenHash: String, type: String): Response<JsonObject> {
        val res = JsonObject().apply {
            addProperty("access_token", "otp_mock_token")
            addProperty("refresh_token", "otp_refresh_token")
            add("user", JsonObject().apply { addProperty("id", "user_mock_otp") })
        }
        return Response.success(res)
    }

    override suspend fun getProfile(select: String, userId: String): Response<List<JsonObject>> {
        return Response.success(mockProfiles)
    }

    override suspend fun updateProfile(userId: String, body: JsonObject, prefer: String): Response<List<JsonObject>> {
        return Response.success(listOf(body))
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

    override suspend fun createReport(body: JsonObject): Response<Unit> {
        mockReports.add(body)
        return Response.success(Unit)
    }

    override suspend fun getReportsForModeration(select: String, order: String): Response<List<JsonObject>> {
        return Response.success(mockReports)
    }

    override suspend fun getRequests(select: String, applicantId: String?, publicationId: String?, order: String): Response<List<JsonObject>> {
        return Response.success(mockRequests)
    }

    override suspend fun createAdoptionRequest(body: JsonObject): Response<Unit> {
        mockRequests.add(body)
        return Response.success(Unit)
    }

    override suspend fun updateAdoptionRequest(requestId: String, body: JsonObject, prefer: String): Response<Unit> {
        return Response.success(Unit)
    }

    override suspend fun getConversations(select: String, order: String): Response<List<JsonObject>> {
        return Response.success(mockConversations)
    }

    override suspend fun createConversation(body: JsonObject, prefer: String): Response<List<JsonObject>> {
        mockConversations.add(body)
        return Response.success(listOf(body))
    }

    override suspend fun getMessages(select: String, conversationId: String, order: String): Response<List<JsonObject>> {
        return Response.success(mockMessages)
    }

    override suspend fun sendMessage(body: JsonObject): Response<Unit> {
        mockMessages.add(body)
        return Response.success(Unit)
    }

    override suspend fun getNotifications(select: String, order: String): Response<List<JsonObject>> {
        return Response.success(mockNotifications)
    }

    override suspend fun markNotificationsRead(unread: String, body: JsonObject, prefer: String): Response<Unit> {
        mockNotifications.forEach { it.addProperty("read", true) }
        return Response.success(Unit)
    }

    override suspend fun getPointsHistory(select: String, order: String): Response<List<JsonObject>> {
        return Response.success(mockPointsHistory)
    }
}
