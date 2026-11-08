package com.desarrolloMovielexample.huella.domain.repository

import com.desarrolloMovielexample.huella.domain.model.AppNotification
import com.desarrolloMovielexample.huella.domain.model.ChatMessage
import com.desarrolloMovielexample.huella.domain.model.Comment
import com.desarrolloMovielexample.huella.domain.model.Conversation
import com.desarrolloMovielexample.huella.domain.model.ModerationItem
import com.desarrolloMovielexample.huella.domain.model.ModerationStats
import com.desarrolloMovielexample.huella.domain.model.PointsAction
import com.desarrolloMovielexample.huella.domain.model.PointsEntry
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.domain.model.PublicationDraft
import com.desarrolloMovielexample.huella.domain.model.ReceivedRequestGroup
import com.desarrolloMovielexample.huella.domain.model.SentRequest
import com.desarrolloMovielexample.huella.domain.model.User
import kotlinx.coroutines.flow.StateFlow

/**
 * Data contract shared by the Supabase implementation and the in-memory preview repository.
 * Suspend functions return [Result] with Spanish error messages.
 */
interface HuellaRepository {
    // Auth / user
    val currentUser: StateFlow<User?>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, city: String, password: String): Result<User>
    suspend fun handleAuthCallback(uri: android.net.Uri): Result<User>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    /** Contraseña nueva para el usuario con sesión (incluida la sesión de recuperación). */
    suspend fun updatePassword(password: String): Result<Unit>
    suspend fun updateProfile(name: String, email: String, city: String, phone: String, bio: String): Result<User>
    suspend fun deleteAccount(): Result<Unit>
    fun logout()
    fun getUser(userId: String): User?
    /** Carga el perfil público de [userId] y sus publicaciones; luego [getUser] y [publicationsByAuthor] los devuelven. */
    suspend fun refreshUser(userId: String): Result<User>

    // Publications
    val publications: StateFlow<List<Publication>>        // public feed
    val myPublications: StateFlow<List<Publication>>      // current user's posts (with status)
    fun getPublication(id: String): Publication?
    fun publicationsByAuthor(userId: String): List<Publication>
    suspend fun createPublication(draft: PublicationDraft): Result<Publication>
    suspend fun updatePublication(publicationId: String, draft: PublicationDraft): Result<Publication>
    suspend fun deletePublication(publicationId: String): Result<Unit>
    suspend fun reportPublication(publicationId: String, reason: String, detail: String): Result<Unit>
    suspend fun votePublication(publicationId: String, value: Int): Result<Unit> // 1 up, -1 down, 0 o el mismo voto otra vez = quitar
    /** Relee contadores y el voto propio de una publicación. */
    suspend fun refreshPublication(publicationId: String): Result<Unit>
    /** "Lo he visto": avisa al autor de una publicación de perdidos/encontrados. */
    suspend fun reportSighting(publicationId: String, text: String): Result<Unit>

    // Comments
    fun comments(publicationId: String): StateFlow<List<Comment>>
    suspend fun addComment(publicationId: String, text: String): Result<Comment>
    suspend fun deleteComment(publicationId: String, commentId: String): Result<Unit>

    // Requests
    val sentRequests: StateFlow<List<SentRequest>>
    val receivedRequests: StateFlow<List<ReceivedRequestGroup>>
    suspend fun sendAdoptionRequest(publicationId: String, message: String): Result<Unit>
    suspend fun respondToRequest(applicantId: String, accept: Boolean): Result<Unit>
    suspend fun closePublication(publicationId: String): Result<Unit>

    // Chat
    fun getConversation(id: String): Conversation?
    suspend fun openConversation(contactUserId: String, publicationId: String? = null): Result<String>
    fun messages(conversationId: String): StateFlow<List<ChatMessage>>
    suspend fun sendMessage(conversationId: String, text: String): Result<Unit>

    // Notifications
    val notifications: StateFlow<List<AppNotification>>
    fun markAllNotificationsRead()
    fun markNotificationRead(id: String)
    fun deleteNotification(id: String)

    // Gamification
    val pointsHistory: List<PointsEntry>
    val pointsActions: List<PointsAction>

    // Moderation
    val moderationQueue: StateFlow<List<ModerationItem>>     // pending review
    val moderationReports: StateFlow<List<ModerationItem>>   // reported publications
    val moderationStats: StateFlow<ModerationStats>
    suspend fun refreshModerationData(): Result<Unit>
    fun getModerationItem(id: String): ModerationItem?
    suspend fun approve(itemId: String): Result<Unit>
    suspend fun reject(itemId: String, reason: String, comment: String): Result<Unit>
    /** Cierra un reporte sin rechazar la publicación (Report.id). */
    suspend fun resolveReport(reportId: String): Result<Unit>

    // Catalogs
    val cities: List<String>
    val species: List<String>
    val breedSuggestions: List<String>
}
