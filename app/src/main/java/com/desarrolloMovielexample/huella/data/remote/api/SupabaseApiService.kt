package com.desarrolloMovielexample.huella.data.remote.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    /** El body lleva code_challenge/code_challenge_method (PKCE); redirect_to es el deep link del correo. */
    @POST("auth/v1/signup")
    suspend fun signUp(@Body body: JsonObject, @Query("redirect_to") redirectTo: String? = null): Response<JsonObject>

    @POST("auth/v1/recover")
    suspend fun recoverPassword(@Body body: JsonObject, @Query("redirect_to") redirectTo: String? = null): Response<JsonObject>

    /** Canjea el ?code= del deep link por una sesión: {auth_code, code_verifier}. */
    @POST("auth/v1/token?grant_type=pkce")
    suspend fun exchangeCodeForSession(@Body body: JsonObject): Response<JsonObject>

    @GET("auth/v1/user")
    suspend fun getUser(@Header("Authorization") bearerToken: String): Response<JsonObject>

    /** Cambia email o contraseña del usuario autenticado (Supabase envía la confirmación del email nuevo). */
    @PUT("auth/v1/user")
    suspend fun updateAuthUser(@Body body: JsonObject): Response<JsonObject>

    /** Perfil de otro usuario: solo columnas públicas (email y phone no son legibles para terceros). */
    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Query("select") select: String = "id,name,city,level,initials,points,member_since,avatar_url,is_moderator,publications_count,adoptions_count,foster_count,active_count,created_at",
        @Query("id") userId: String,
    ): Response<List<JsonObject>>

    /** Fila completa del perfil propio (incluye email, phone y bio). */
    @POST("rest/v1/rpc/get_my_profile")
    suspend fun getMyProfile(@Body body: JsonObject = JsonObject()): Response<List<JsonObject>>

    /** Teléfono del otro participante de la conversación; null si no lo tiene. */
    @POST("rest/v1/rpc/get_contact_phone")
    suspend fun getContactPhone(@Body body: JsonObject): Response<JsonElement>

    /** return=minimal: la respuesta no puede incluir columnas sin privilegio de lectura. */
    @PATCH("rest/v1/profiles")
    suspend fun updateProfile(
        @Query("id") userId: String,
        @Body body: JsonObject,
        @Header("Prefer") prefer: String = "return=minimal",
    ): Response<Unit>

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
        @Query("offset") offset: Int? = null,
    ): Response<List<JsonObject>>

    @POST("rest/v1/publications")
    suspend fun createPublication(
        @Body body: JsonObject,
        @Header("Prefer") prefer: String = "return=representation",
    ): Response<List<JsonObject>>

    @PATCH("rest/v1/publications")
    suspend fun updatePublication(
        @Query("id") id: String,
        @Body body: JsonObject,
        @Query("select") select: String = "*,profiles!publications_author_id_fkey(id,name,level,initials)",
        @Header("Prefer") prefer: String = "return=representation",
    ): Response<List<JsonObject>>

    /** return=representation: si RLS no deja borrar, la lista vuelve vacía en vez de fallar en silencio. */
    @DELETE("rest/v1/publications")
    suspend fun deletePublication(
        @Query("id") id: String,
        @Query("select") select: String = "id",
        @Header("Prefer") prefer: String = "return=representation",
    ): Response<List<JsonObject>>

    @GET("rest/v1/publications")
    suspend fun getPublicationCounts(
        @Query("id") id: String,
        @Query("select") select: String = "votes_count,comments_count",
    ): Response<List<JsonObject>>

    @GET("rest/v1/comments")
    suspend fun getComments(
        @Query("publication_id") publicationId: String,
        @Query("select") select: String = "*,author:profiles(id,name,level,initials)",
        @Query("order") order: String = "created_at.asc",
    ): Response<List<JsonObject>>

    @POST("rest/v1/comments")
    suspend fun createComment(
        @Body body: JsonObject,
        @Query("select") select: String = "*,author:profiles(id,name,level,initials)",
        @Header("Prefer") prefer: String = "return=representation",
    ): Response<List<JsonObject>>

    @DELETE("rest/v1/comments")
    suspend fun deleteComment(
        @Query("id") id: String,
        @Query("select") select: String = "id",
        @Header("Prefer") prefer: String = "return=representation",
    ): Response<List<JsonObject>>

    /** Upsert del voto propio: la PK (publication_id, user_id) resuelve el conflicto. */
    @POST("rest/v1/publication_votes")
    suspend fun upsertVote(
        @Body body: JsonObject,
        @Query("on_conflict") onConflict: String = "publication_id,user_id",
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=minimal",
    ): Response<Unit>

    @DELETE("rest/v1/publication_votes")
    suspend fun deleteVote(
        @Query("publication_id") publicationId: String,
        @Query("user_id") userId: String,
    ): Response<Unit>

    @GET("rest/v1/publication_votes")
    suspend fun getMyVote(
        @Query("publication_id") publicationId: String,
        @Query("user_id") userId: String,
        @Query("select") select: String = "value",
    ): Response<List<JsonObject>>

    /** Inserta el avistamiento, notifica SIGHTING al autor y suma +5 al reporter (servidor). */
    @POST("rest/v1/rpc/report_sighting")
    suspend fun reportSighting(@Body body: JsonObject): Response<Unit>

    /** Solo el autor: lifecycle=FINALIZADA, +50 si es adopción/temporal y contadores (servidor). */
    @POST("rest/v1/rpc/close_publication")
    suspend fun closePublication(@Body body: JsonObject): Response<Unit>

    /** Borra auth.users del usuario (cascada a perfil, publicaciones, solicitudes...) y sus fotos. */
    @POST("rest/v1/rpc/delete_my_account")
    suspend fun deleteMyAccount(@Body body: JsonObject = JsonObject()): Response<Unit>

    @POST("rest/v1/rpc/moderate_publication")
    suspend fun moderatePublication(@Body body: JsonObject): Response<Unit>

    @POST("rest/v1/reports")
    suspend fun createReport(@Body body: JsonObject): Response<Unit>

    @GET("rest/v1/reports")
    suspend fun getReportsForModeration(
        @Query("select") select: String = "*,publication:publications(*,profiles!publications_author_id_fkey(id,name,level,initials)),reporter:profiles!reports_reporter_id_fkey(name)",
        @Query("status") status: String = "eq.ABIERTO",
        @Query("order") order: String = "created_at.desc",
    ): Response<List<JsonObject>>

    /** Solo moderadores: marca el reporte RESUELTO. */
    @POST("rest/v1/rpc/resolve_report")
    suspend fun resolveReport(@Body body: JsonObject): Response<Unit>

    @GET("rest/v1/adoption_requests")
    suspend fun getRequests(
        @Query("select") select: String = "*,publications(*)",
        @Query("applicant_id") applicantId: String? = null,
        @Query("publication_id") publicationId: String? = null,
        @Query("order") order: String = "created_at.desc",
    ): Response<List<JsonObject>>

    @POST("rest/v1/adoption_requests")
    suspend fun createAdoptionRequest(@Body body: JsonObject): Response<Unit>

    /**
     * Solo el autor de la publicación: cambia el estado, notifica al solicitante y, si acepta, crea o
     * devuelve la conversación entre ambos (su id; null al rechazar).
     */
    @POST("rest/v1/rpc/respond_to_request")
    suspend fun respondToRequest(@Body body: JsonObject): Response<JsonElement>

    @GET("rest/v1/conversations")
    suspend fun getConversations(
        @Query("select") select: String = "*,publications(title),participant_one_profile:profiles!conversations_participant_one_fkey(id,name,level,initials),participant_two_profile:profiles!conversations_participant_two_fkey(id,name,level,initials)",
        @Query("order") order: String = "updated_at.desc",
        @Query("id") id: String? = null,
        @Query("publication_id") publicationId: String? = null,
        /** Filtro PostgREST or=(...), p. ej. el par de participantes en cualquier orden. */
        @Query("or") or: String? = null,
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

    @PATCH("rest/v1/notifications")
    suspend fun updateNotification(
        @Query("id") id: String,
        @Body body: JsonObject,
        @Header("Prefer") prefer: String = "return=minimal",
    ): Response<Unit>

    @DELETE("rest/v1/notifications")
    suspend fun deleteNotification(
        @Query("id") id: String,
        @Header("Prefer") prefer: String = "return=minimal",
    ): Response<Unit>

    @GET("rest/v1/points_history")
    suspend fun getPointsHistory(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc",
    ): Response<List<JsonObject>>
}
