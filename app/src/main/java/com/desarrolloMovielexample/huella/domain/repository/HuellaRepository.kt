package com.desarrolloMovielexample.huella.domain.repository

import com.desarrolloMovielexample.huella.domain.model.AppNotification
import com.desarrolloMovielexample.huella.domain.model.ChatMessage
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
 * Single data contract of the app. Implemented in-memory by [FakeRepository] for this phase.
 * All suspend functions simulate network latency and return [Result] with Spanish error messages.
 */
interface HuellaRepository {
    // Auth / user
    val currentUser: StateFlow<User?>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(name: String, email: String, city: String, password: String): Result<User>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun updateProfile(name: String, email: String, city: String, phone: String, bio: String): Result<User>
    fun logout()
    fun getUser(userId: String): User?

    // Publications
    val publications: StateFlow<List<Publication>>        // public feed
    val myPublications: StateFlow<List<Publication>>      // current user's posts (with status)
    fun getPublication(id: String): Publication?
    fun publicationsByAuthor(userId: String): List<Publication>
    suspend fun createPublication(draft: PublicationDraft): Result<Publication>
    suspend fun reportPublication(publicationId: String, reason: String, detail: String): Result<Unit>

    // Requests
    val sentRequests: StateFlow<List<SentRequest>>
    val receivedRequests: StateFlow<List<ReceivedRequestGroup>>
    suspend fun sendAdoptionRequest(publicationId: String, message: String): Result<Unit>
    suspend fun respondToRequest(applicantId: String, accept: Boolean): Result<Unit>
    suspend fun closePublication(publicationId: String): Result<Unit>

    // Chat
    fun getConversation(id: String): Conversation?
    fun messages(conversationId: String): StateFlow<List<ChatMessage>>
    suspend fun sendMessage(conversationId: String, text: String): Result<Unit>

    // Notifications
    val notifications: StateFlow<List<AppNotification>>
    fun markAllNotificationsRead()

    // Gamification
    val pointsHistory: List<PointsEntry>
    val pointsActions: List<PointsAction>

    // Moderation
    val moderationQueue: StateFlow<List<ModerationItem>>     // pending review
    val moderationReports: StateFlow<List<ModerationItem>>   // reported publications
    val moderationStats: StateFlow<ModerationStats>
    fun getModerationItem(id: String): ModerationItem?
    suspend fun approve(itemId: String): Result<Unit>
    suspend fun reject(itemId: String, reason: String, comment: String): Result<Unit>

    // Catalogs
    val cities: List<String>
    val species: List<String>
    val breedSuggestions: List<String>
}
