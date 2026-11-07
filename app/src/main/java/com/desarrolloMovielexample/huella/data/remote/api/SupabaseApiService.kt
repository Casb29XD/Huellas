package com.desarrolloMovielexample.huella.data.remote.api

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.PUT
import retrofit2.http.Path
import okhttp3.RequestBody

/** Supabase Auth and PostgREST endpoints used by the Huella schema. */
interface SupabaseApiService {
    @PUT("storage/v1/object/pet-images/{objectPath}")
    suspend fun uploadPetImage(
        @Path(value = "objectPath", encoded = true) objectPath: String,
        @Body body: RequestBody,
        @Header("Content-Type") contentType: String,
        @Header("x-upsert") upsert: String = "false",
    ): Response<JsonObject>

    /**
     * Supabase Auth: sign in with email + password.
     * Content-Type MUST be application/json — Supabase rejects form-encoded bodies
     * with "json input" error even on the token endpoint.
     */
    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(@Body body: JsonObject): Response<JsonObject>

    /** Supabase Auth: refresh an expired access token using the refresh token. */
    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshSession(@Body body: JsonObject): Response<JsonObject>

    @POST("auth/v1/signup")
    suspend fun signUp(@Body body: JsonObject): Response<JsonObject>

    @POST("auth/v1/recover")
    suspend fun recoverPassword(@Body body: JsonObject): Response<JsonObject>

    @GET("auth/v1/user")
    suspend fun getUser(@Header("Authorization") bearerToken: String): Response<JsonObject>

    @GET("auth/v1/verify")
    suspend fun verifyOtp(
        @Query("token_hash") tokenHash: String,
        @Query("type") type: String,
    ): Response<JsonObject>

    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Query("select") select: String = "*",
        @Query("id") userId: String,
    ): Response<List<JsonObject>>

    @PATCH("rest/v1/profiles")
    suspend fun updateProfile(
        @Query("id") userId: String,
        @Body body: JsonObject,
        @Header("Prefer") prefer: String = "return=representation",
    ): Response<List<JsonObject>>

    @GET("rest/v1/publications")
    suspend fun getPublications(
        @Query("select") select: String = "*,profiles!publications_author_id_fkey(id,name,level,initials)",
        @Query("status") status: String? = "eq.APROBADA",
        @Query("category") category: String? = null,
        @Query("species") species: String? = null,
        @Query("city") city: String? = null,
        @Query("author_id") authorId: String? = null,
        @Query("updated_at") updatedAfter: String? = null,
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 100,
    ): Response<List<JsonObject>>

    @POST("rest/v1/publications")
    suspend fun createPublication(
        @Body body: JsonObject,
        @Header("Prefer") prefer: String = "return=representation",
    ): Response<List<JsonObject>>

    @POST("rest/v1/rpc/moderate_publication")
    suspend fun moderatePublication(@Body body: JsonObject): Response<Unit>

    @POST("rest/v1/reports")
    suspend fun createReport(@Body body: JsonObject): Response<Unit>

    @GET("rest/v1/reports")
    suspend fun getReportsForModeration(
        @Query("select") select: String = "*,publication:publications(*,profiles!publications_author_id_fkey(id,name,level,initials)),reporter:profiles!reports_reporter_id_fkey(name)",
        @Query("order") order: String = "created_at.desc",
    ): Response<List<JsonObject>>

    @GET("rest/v1/adoption_requests")
    suspend fun getRequests(
        @Query("select") select: String = "*,publications(*)",
        @Query("applicant_id") applicantId: String? = null,
        @Query("publication_id") publicationId: String? = null,
        @Query("order") order: String = "created_at.desc",
    ): Response<List<JsonObject>>

    @POST("rest/v1/adoption_requests")
    suspend fun createAdoptionRequest(@Body body: JsonObject): Response<Unit>

    @PATCH("rest/v1/adoption_requests")
    suspend fun updateAdoptionRequest(
        @Query("id") requestId: String,
        @Body body: JsonObject,
        @Header("Prefer") prefer: String = "return=minimal",
    ): Response<Unit>

    @GET("rest/v1/conversations")
    suspend fun getConversations(
        @Query("select") select: String = "*,publications(title),participant_one_profile:profiles!conversations_participant_one_fkey(id,name,level,initials,phone),participant_two_profile:profiles!conversations_participant_two_fkey(id,name,level,initials,phone)",
        @Query("order") order: String = "updated_at.desc",
    ): Response<List<JsonObject>>

    @POST("rest/v1/conversations")
    suspend fun createConversation(
        @Body body: JsonObject,
        @Header("Prefer") prefer: String = "return=representation",
    ): Response<List<JsonObject>>

    @GET("rest/v1/chat_messages")
    suspend fun getMessages(
        @Query("select") select: String = "*",
        @Query("conversation_id") conversationId: String,
        @Query("order") order: String = "created_at.asc",
    ): Response<List<JsonObject>>

    @POST("rest/v1/chat_messages")
    suspend fun sendMessage(@Body body: JsonObject): Response<Unit>

    @GET("rest/v1/notifications")
    suspend fun getNotifications(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc",
    ): Response<List<JsonObject>>

    @PATCH("rest/v1/notifications")
    suspend fun markNotificationsRead(
        @Query("read") unread: String = "eq.false",
        @Body body: JsonObject = JsonObject().apply { addProperty("read", true) },
        @Header("Prefer") prefer: String = "return=minimal",
    ): Response<Unit>

    @GET("rest/v1/points_history")
    suspend fun getPointsHistory(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc",
    ): Response<List<JsonObject>>
}
