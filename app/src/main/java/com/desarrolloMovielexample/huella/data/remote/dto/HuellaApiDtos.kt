package com.desarrolloMovielexample.huella.data.remote.dto

import com.google.gson.annotations.SerializedName

// ============================================================
// DTOs para el backend propio de Huella (Auth, Publicaciones,
// Solicitudes, Chat, Moderación, Notificaciones)
// ============================================================

// ── Auth & Usuario ──────────────────────────────────────────

data class LoginRequestDto(
    @SerializedName("email") val email: String?,
    @SerializedName("password") val password: String?
)

data class RegisterRequestDto(
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("password") val password: String?
)

data class PasswordResetRequestDto(
    @SerializedName("email") val email: String?
)

data class UpdateProfileRequestDto(
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("bio") val bio: String?
)

data class AuthResponseDto(
    @SerializedName("token") val token: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("user") val user: UserResponseDto? = null
)

data class UserResponseDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("points") val points: Int? = null,
    @SerializedName("initials") val initials: String? = null,
    @SerializedName("member_since") val memberSince: String? = null,
    @SerializedName("publications_count") val publicationsCount: Int? = null,
    @SerializedName("adoptions_count") val adoptionsCount: Int? = null,
    @SerializedName("foster_count") val fosterCount: Int? = null,
    @SerializedName("active_count") val activeCount: Int? = null,
    @SerializedName("is_moderator") val isModerator: Boolean? = null
)

// ── Publicaciones ───────────────────────────────────────────

data class AuthorResponseDto(
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("level") val level: String? = null,
    @SerializedName("initials") val initials: String? = null
)

data class PublicationResponseDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("species") val species: String? = null,
    @SerializedName("breed") val breed: String? = null,
    @SerializedName("size") val size: String? = null,
    @SerializedName("vaccinated") val vaccinated: Boolean? = null,
    @SerializedName("sterilized") val sterilized: Boolean? = null,
    @SerializedName("age") val age: String? = null,
    @SerializedName("sex") val sex: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("neighborhood") val neighborhood: String? = null,
    @SerializedName("time_ago") val timeAgo: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("tone") val tone: Long? = null,
    @SerializedName("photo_label") val photoLabel: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("author") val author: AuthorResponseDto? = null,
    @SerializedName("event_date") val eventDate: String? = null,
    @SerializedName("schedule") val schedule: String? = null,
    @SerializedName("place") val place: String? = null,
    @SerializedName("services") val services: List<String>? = null,
    @SerializedName("capacity") val capacity: Int? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null
)

data class CreatePublicationRequestDto(
    @SerializedName("category") val category: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("species") val species: String?,
    @SerializedName("breed") val breed: String?,
    @SerializedName("size") val size: String?,
    @SerializedName("vaccinated") val vaccinated: Boolean?,
    @SerializedName("sterilized") val sterilized: Boolean?,
    @SerializedName("age") val age: String?,
    @SerializedName("sex") val sex: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("event_date") val eventDate: String? = null
)

data class ReportPublicationRequestDto(
    @SerializedName("reason") val reason: String?,
    @SerializedName("detail") val detail: String?
)

// ── Solicitudes de Adopción ─────────────────────────────────

data class AdoptionRequestDto(
    @SerializedName("publication_id") val publicationId: String?,
    @SerializedName("message") val message: String?
)

data class RespondRequestDto(
    @SerializedName("accept") val accept: Boolean?
)

data class SentRequestResponseDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("publication_id") val publicationId: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("when_label") val whenLabel: String? = null,
    @SerializedName("message") val message: String? = null
)

data class ApplicantResponseDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("level") val level: String? = null,
    @SerializedName("initials") val initials: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("time_ago") val timeAgo: String? = null,
    @SerializedName("status") val status: String? = null
)

data class ReceivedRequestGroupResponseDto(
    @SerializedName("publication_id") val publicationId: String? = null,
    @SerializedName("applicants") val applicants: List<ApplicantResponseDto>? = null
)

// ── Chat & Mensajería ───────────────────────────────────────

data class SendMessageRequestDto(
    @SerializedName("text") val text: String?
)

data class ChatMessageResponseDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("conversation_id") val conversationId: String? = null,
    @SerializedName("from_me") val fromMe: Boolean? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("time") val time: String? = null
)

data class ConversationResponseDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("contact") val contact: AuthorResponseDto? = null,
    @SerializedName("about") val about: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("last_message") val lastMessage: ChatMessageResponseDto? = null
)

// ── Notificaciones & Moderación ─────────────────────────────

data class NotificationResponseDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("group") val group: String? = null,
    @SerializedName("type") val type: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("text") val text: String? = null,
    @SerializedName("time") val time: String? = null,
    @SerializedName("read") val read: Boolean? = null,
    @SerializedName("target_id") val targetId: String? = null
)

data class RejectModerationRequestDto(
    @SerializedName("reason") val reason: String?,
    @SerializedName("comment") val comment: String?
)

data class ModerationStatsResponseDto(
    @SerializedName("pending") val pending: Int? = null,
    @SerializedName("reported") val reported: Int? = null,
    @SerializedName("approved_today") val approvedToday: Int? = null
)
