package com.desarrolloMovielexample.huella.data.remote.api

import com.desarrolloMovielexample.huella.data.remote.dto.*
import com.desarrolloMovielexample.huella.data.remote.model.ApiResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz Retrofit para el backend propio de Huella.
 * Todos los métodos son funciones de suspensión y retornan Response<ApiResponse<T>>.
 */
interface HuellaApiService {

    // ── Autenticación y Perfil ───────────────────────────────

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<ApiResponse<AuthResponseDto>>

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): Response<ApiResponse<AuthResponseDto>>

    @POST("api/v1/auth/forgot-password")
    suspend fun sendPasswordReset(
        @Body request: PasswordResetRequestDto
    ): Response<ApiResponse<Unit>>

    @GET("api/v1/users/{userId}")
    suspend fun getUserById(
        @Path("userId") userId: String
    ): Response<ApiResponse<UserResponseDto>>

    @PUT("api/v1/users/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequestDto
    ): Response<ApiResponse<UserResponseDto>>

    // ── Publicaciones ────────────────────────────────────────

    @GET("api/v1/publications")
    suspend fun getPublications(
        @Query("category") category: String? = null,
        @Query("species") species: String? = null,
        @Query("city") city: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<List<PublicationResponseDto>>>

    @GET("api/v1/publications/{publicationId}")
    suspend fun getPublicationById(
        @Path("publicationId") publicationId: String
    ): Response<ApiResponse<PublicationResponseDto>>

    @GET("api/v1/users/{userId}/publications")
    suspend fun getPublicationsByAuthor(
        @Path("userId") userId: String
    ): Response<ApiResponse<List<PublicationResponseDto>>>

    @GET("api/v1/users/me/publications")
    suspend fun getMyPublications(): Response<ApiResponse<List<PublicationResponseDto>>>

    @POST("api/v1/publications")
    suspend fun createPublication(
        @Body request: CreatePublicationRequestDto
    ): Response<ApiResponse<PublicationResponseDto>>

    @POST("api/v1/publications/{publicationId}/report")
    suspend fun reportPublication(
        @Path("publicationId") publicationId: String,
        @Body request: ReportPublicationRequestDto
    ): Response<ApiResponse<Unit>>

    @PATCH("api/v1/publications/{publicationId}/close")
    suspend fun closePublication(
        @Path("publicationId") publicationId: String
    ): Response<ApiResponse<Unit>>

    // ── Solicitudes de Adopción ──────────────────────────────

    @GET("api/v1/requests/sent")
    suspend fun getSentRequests(): Response<ApiResponse<List<SentRequestResponseDto>>>

    @GET("api/v1/requests/received")
    suspend fun getReceivedRequests(): Response<ApiResponse<List<ReceivedRequestGroupResponseDto>>>

    @POST("api/v1/requests")
    suspend fun sendAdoptionRequest(
        @Body request: AdoptionRequestDto
    ): Response<ApiResponse<Unit>>

    @PATCH("api/v1/requests/{applicantId}/respond")
    suspend fun respondToRequest(
        @Path("applicantId") applicantId: String,
        @Body request: RespondRequestDto
    ): Response<ApiResponse<Unit>>

    // ── Chat & Mensajería ────────────────────────────────────

    @GET("api/v1/conversations/{conversationId}")
    suspend fun getConversation(
        @Path("conversationId") conversationId: String
    ): Response<ApiResponse<ConversationResponseDto>>

    @GET("api/v1/conversations/{conversationId}/messages")
    suspend fun getMessages(
        @Path("conversationId") conversationId: String
    ): Response<ApiResponse<List<ChatMessageResponseDto>>>

    @POST("api/v1/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body request: SendMessageRequestDto
    ): Response<ApiResponse<ChatMessageResponseDto>>

    // ── Notificaciones ───────────────────────────────────────

    @GET("api/v1/notifications")
    suspend fun getNotifications(): Response<ApiResponse<List<NotificationResponseDto>>>

    @PATCH("api/v1/notifications/mark-read")
    suspend fun markAllNotificationsRead(): Response<ApiResponse<Unit>>

    // ── Moderación ───────────────────────────────────────────

    @GET("api/v1/moderation/queue")
    suspend fun getModerationQueue(): Response<ApiResponse<List<PublicationResponseDto>>>

    @GET("api/v1/moderation/reports")
    suspend fun getModerationReports(): Response<ApiResponse<List<PublicationResponseDto>>>

    @GET("api/v1/moderation/stats")
    suspend fun getModerationStats(): Response<ApiResponse<ModerationStatsResponseDto>>

    @POST("api/v1/moderation/{itemId}/approve")
    suspend fun approveModerationItem(
        @Path("itemId") itemId: String
    ): Response<ApiResponse<Unit>>

    @POST("api/v1/moderation/{itemId}/reject")
    suspend fun rejectModerationItem(
        @Path("itemId") itemId: String,
        @Body request: RejectModerationRequestDto
    ): Response<ApiResponse<Unit>>
}
