package com.desarrolloMovielexample.huella.data.remote

import android.content.Context
import android.net.Uri
import com.desarrolloMovielexample.huella.data.remote.api.SupabaseApiService
import com.desarrolloMovielexample.huella.domain.model.*
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * App repository backed by Supabase Auth and the tables/policies in supabase_schema.sql.
 * All write operations use the user's access token so Supabase RLS remains authoritative.
 */
object SupabaseRepository : HuellaRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var appContext: Context
    private val api: SupabaseApiService get() = RetrofitClient.supabaseApi

    suspend fun uploadPublicationImage(uri: Uri): String {
        requireUserId()
        requireConfigured()
        val mimeType = appContext.contentResolver.getType(uri) ?: "image/jpeg"
        require(mimeType.startsWith("image/")) { "Selecciona un archivo de imagen" }
        val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("No se pudo leer la imagen seleccionada")
        require(bytes.isNotEmpty()) { "La imagen está vacía" }
        require(bytes.size <= 10 * 1024 * 1024) { "Cada imagen debe pesar menos de 10 MB" }
        val ext = when (mimeType.substringAfter('/', "jpeg").lowercase()) {
            "jpeg" -> "jpg"
            else -> mimeType.substringAfter('/').substringBefore(';').lowercase()
        }
        val objectPath = "${requireUserId()}/${UUID.randomUUID()}.$ext"
        apiCall { api.uploadPetImage(objectPath, bytes.toRequestBody(mimeType.toMediaType()), mimeType) }
        return "${SupabaseConfig.url}/storage/v1/object/public/pet-images/$objectPath"
    }

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    private val _publications = MutableStateFlow<List<Publication>>(emptyList())
    override val publications: StateFlow<List<Publication>> = _publications.asStateFlow()
    private val _myPublications = MutableStateFlow<List<Publication>>(emptyList())
    override val myPublications: StateFlow<List<Publication>> = _myPublications.asStateFlow()
    private val _sentRequests = MutableStateFlow<List<SentRequest>>(emptyList())
    override val sentRequests: StateFlow<List<SentRequest>> = _sentRequests.asStateFlow()
    private val _receivedRequests = MutableStateFlow<List<ReceivedRequestGroup>>(emptyList())
    override val receivedRequests: StateFlow<List<ReceivedRequestGroup>> = _receivedRequests.asStateFlow()
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()
    private val _moderationQueue = MutableStateFlow<List<ModerationItem>>(emptyList())
    override val moderationQueue: StateFlow<List<ModerationItem>> = _moderationQueue.asStateFlow()
    private val _moderationReports = MutableStateFlow<List<ModerationItem>>(emptyList())
    override val moderationReports: StateFlow<List<ModerationItem>> = _moderationReports.asStateFlow()
    private val _moderationStats = MutableStateFlow(ModerationStats(0, 0, 0))
    override val moderationStats: StateFlow<ModerationStats> = _moderationStats.asStateFlow()
    override suspend fun refreshModerationData(): Result<Unit> = result {
        refreshModeration()
        Unit
    }
    private var cachedPointsHistory: List<PointsEntry> = emptyList()
    override val pointsHistory: List<PointsEntry> get() = cachedPointsHistory
    private val userCache = mutableMapOf<String, User>()
    override val pointsActions = listOf(
        PointsAction("Crear una publicación", 5),
        PointsAction("Publicación aprobada", 10),
        PointsAction("Adopción concretada", 50),
    )
    override val cities = listOf(
        "Medellín, Antioquia", "Envigado, Antioquia", "Bogotá, Cundinamarca",
        "Cali, Valle del Cauca", "Barranquilla, Atlántico", "Cartagena, Bolívar",
        "Bucaramanga, Santander", "Pereira, Risaralda", "Manizales, Caldas",
    )
    override val species = listOf("Perro", "Gato", "Ave", "Conejo", "Roedor", "Otro")
    override val breedSuggestions = listOf("Criollo", "Mestizo")

    private val messageFlows = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()
    private val conversations = mutableMapOf<String, Conversation>()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        // Sin app/.env no hay red que disparar: las pantallas muestran el error de requireConfigured() al usarse.
        if (!SupabaseConfig.isConfigured) return
        val id = SupabaseSession.userId
        if (id != null && SupabaseSession.accessToken != null) {
            scope.launch {
                apiCall { api.getProfile(userId = "eq.$id") }
                    .firstOrNull()?.let { _currentUser.value = it.toUser() }
                refreshAll()
            }
        } else {
            scope.launch { refreshFeed() }
        }
    }

    override suspend fun login(email: String, password: String): Result<User> = result {
        requireConfigured()
        val body = json {
            addProperty("email", email.trim())
            addProperty("password", password)
        }
        val auth = apiCall { api.signIn(body) }
        val token = auth.string("access_token")
            ?: error("Supabase no devolvió el token de acceso")
        val userJson = auth.obj("user") ?: error("Supabase no devolvió el usuario")
        val id = userJson.string("id") ?: error("El usuario no tiene un identificador")
        SupabaseSession.save(appContext, token, id, auth.string("refresh_token"))
        val profile = apiCall { api.getProfile(userId = "eq.$id") }.firstOrNull()
            ?: error("No encontramos el perfil asociado a esta cuenta")
        val user = profile.toUser()
        _currentUser.value = user
        refreshAll()
        user
    }

    override suspend fun register(name: String, email: String, city: String, password: String): Result<User> = result {
        requireConfigured()
        val metadata = json {
            addProperty("name", name.trim())
            addProperty("city", city.substringBefore(",").trim())
            addProperty("phone", "")
            addProperty("bio", "")
        }
        val body = json {
            addProperty("email", email.trim())
            addProperty("password", password)
            add("data", metadata)
        }
        val response = apiCall { api.signUp(body) }
        val token = response.string("access_token")
            ?: error("Cuenta creada. Confirma tu correo y luego inicia sesión.")
        val userJson = response.obj("user") ?: error("Supabase no devolvió el usuario")
        val id = userJson.string("id") ?: error("El usuario no tiene un identificador")
        SupabaseSession.save(appContext, token, id, response.string("refresh_token"))
        val profile = apiCall { api.getProfile(userId = "eq.$id") }.firstOrNull()
            ?: error("La cuenta se creó, pero aún no aparece su perfil")
        val user = profile.toUser()
        _currentUser.value = user
        refreshAll()
        user
    }

    override suspend fun handleAuthCallback(uri: Uri): Result<User> = result {
        requireConfigured()
        val params = mutableMapOf<String, String>()
        uri.queryParameterNames?.forEach { key ->
            uri.getQueryParameter(key)?.let { params[key] = it }
        }
        uri.fragment?.split("&")?.forEach { part ->
            val kv = part.split("=", limit = 2)
            if (kv.size == 2) {
                params[kv[0]] = runCatching { URLDecoder.decode(kv[1], "UTF-8") }.getOrDefault(kv[1])
            }
        }

        var accessToken = params["access_token"]
        var refreshToken = params["refresh_token"]
        var userId: String? = null

        val tokenHash = params["token_hash"] ?: params["token"]
        if (accessToken.isNullOrBlank() && !tokenHash.isNullOrBlank()) {
            val type = params["type"] ?: "signup"
            val verified = apiCall { api.verifyOtp(tokenHash, type) }
            accessToken = verified.string("access_token")
            refreshToken = verified.string("refresh_token")
            userId = verified.obj("user")?.string("id")
        }

        if (accessToken.isNullOrBlank()) {
            error("No se encontraron credenciales de autenticación en el enlace")
        }

        if (userId.isNullOrBlank()) {
            val userJson = apiCall { api.getUser("Bearer $accessToken") }
            userId = userJson.string("id") ?: error("No se pudo obtener el usuario autenticado")
        }

        SupabaseSession.save(appContext, accessToken, userId, refreshToken)

        var profile: JsonObject? = null
        for (i in 0..4) {
            profile = apiCall { api.getProfile(userId = "eq.$userId") }.firstOrNull()
            if (profile != null) break
            delay(350)
        }

        val user = profile?.toUser() ?: User(
            id = userId,
            name = "Usuario Huella",
            email = "",
            city = "",
            points = 0,
            initials = "UH",
        )
        _currentUser.value = user
        refreshAll()
        user
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = result {
        requireConfigured()
        apiCall { api.recoverPassword(json { addProperty("email", email.trim()) }) }
        Unit
    }

    override suspend fun updateProfile(name: String, email: String, city: String, phone: String, bio: String): Result<User> = result {
        val id = requireUserId()
        val values = json {
            addProperty("name", name.trim())
            addProperty("email", email.trim())
            addProperty("city", city.substringBefore(",").trim())
            addProperty("phone", phone.trim())
            addProperty("bio", bio.trim())
        }
        val profile = apiCall { api.updateProfile("eq.$id", values) }.firstOrNull()
            ?: error("No se pudo actualizar el perfil")
        profile.toUser().also { _currentUser.value = it }
    }

    override fun logout() {
        _currentUser.value = null
        SupabaseSession.clear(appContext)
        _sentRequests.value = emptyList()
        _receivedRequests.value = emptyList()
        _notifications.value = emptyList()
        _myPublications.value = emptyList()
    }

    override fun getUser(userId: String): User? =
        userCache[userId] ?: _currentUser.value?.takeIf { it.id == userId }

    override fun getPublication(id: String): Publication? =
        (_publications.value + _myPublications.value + _moderationQueue.value.map { it.publication })
            .firstOrNull { it.id == id }

    override fun publicationsByAuthor(userId: String): List<Publication> =
        (_publications.value + _myPublications.value).distinctBy { it.id }
            .filter { it.author.userId == userId }

    override suspend fun createPublication(draft: PublicationDraft): Result<Publication> = result {
        val authorId = requireUserId()
        val body = json {
            addProperty("author_id", authorId)
            addProperty("category", draft.category.name)
            addProperty("title", draft.title.trim())
            addProperty("species", draft.species)
            addProperty("breed", draft.breed.trim().ifBlank { "Criollo" })
            addProperty("size", draft.size)
            addProperty("vaccinated", draft.vaccinated)
            addProperty("sterilized", draft.sterilized)
            addProperty("age", draft.age)
            addProperty("sex", draft.sex)
            addProperty("city", draft.city.substringBefore(",").trim())
            addProperty("neighborhood", draft.city)
            addProperty("description", draft.description.trim())
            addProperty("event_date", draft.eventDate)
            addProperty("status", PostStatus.EN_REVISION.name)
            addProperty("image_url", draft.imageUrl)
        }
        val created = apiCall { api.createPublication(body) }.firstOrNull()
            ?: error("Supabase no devolvió la publicación creada")
        refreshAll()
        created.toPublication()
    }

    override suspend fun reportPublication(publicationId: String, reason: String, detail: String): Result<Unit> = result {
        val reporterId = requireUserId()
        apiCall { api.createReport(json {
            addProperty("publication_id", publicationId)
            addProperty("reporter_id", reporterId)
            addProperty("reason", reason)
            addProperty("detail", detail)
        }) }
        Unit
    }

    override suspend fun sendAdoptionRequest(publicationId: String, message: String): Result<Unit> = result {
        val applicantId = requireUserId()
        apiCall { api.createAdoptionRequest(json {
            addProperty("publication_id", publicationId)
            addProperty("applicant_id", applicantId)
            addProperty("message", message.trim())
        }) }
        refreshRequests()
        Unit
    }

    override suspend fun respondToRequest(applicantId: String, accept: Boolean): Result<Unit> = result {
        apiCall { api.updateAdoptionRequest("eq.$applicantId", json {
            addProperty("status", if (accept) RequestStatus.ACEPTADA.name else RequestStatus.RECHAZADA.name)
        }) }
        refreshRequests()
        Unit
    }

    override suspend fun closePublication(publicationId: String): Result<Unit> =
        Result.failure(IllegalStateException("El esquema de Supabase no incluye un estado para cerrar una publicación sin borrarla"))

    override fun getConversation(id: String): Conversation? = conversations[id]

    override suspend fun openConversation(contactUserId: String, publicationId: String?): Result<String> = result {
        val currentId = requireUserId()
        require(contactUserId != currentId) { "No puedes iniciar un chat contigo mismo" }
        val existing = apiCall { api.getConversations() }.firstOrNull { row ->
            val pairMatches = (row.string("participant_one") == currentId && row.string("participant_two") == contactUserId) ||
                (row.string("participant_two") == currentId && row.string("participant_one") == contactUserId)
            pairMatches && (publicationId == null || row.string("publication_id") == publicationId)
        }
        if (existing != null) {
            cacheConversation(existing, currentId)
            existing.string("id") ?: error("La conversación no tiene identificador")
        } else {
            val publication = publicationId?.let { getPublication(it) }
            val body = json {
                addProperty("publication_id", publicationId)
                addProperty("participant_one", currentId)
                addProperty("participant_two", contactUserId)
                addProperty("about", publication?.let { "Sobre: ${it.title}" } ?: "Conversación de Huella")
            }
            val created = apiCall { api.createConversation(body) }.firstOrNull()
                ?: error("Supabase no devolvió la conversación creada")
            val createdId = created.string("id") ?: error("La conversación no tiene identificador")
            val hydrated = apiCall { api.getConversations() }.firstOrNull { it.string("id") == createdId }
            cacheConversation(hydrated ?: created, currentId)
            createdId
        }
    }

    override fun messages(conversationId: String): StateFlow<List<ChatMessage>> {
        val flow = messageFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
        scope.launch { runCatching { flow.value = fetchMessages(conversationId) } }
        return flow.asStateFlow()
    }

    override suspend fun sendMessage(conversationId: String, text: String): Result<Unit> = result {
        val senderId = requireUserId()
        apiCall { api.sendMessage(json {
            addProperty("conversation_id", conversationId)
            addProperty("sender_id", senderId)
            addProperty("text", text.trim())
        }) }
        messageFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }.value =
            fetchMessages(conversationId)
        Unit
    }


    override suspend fun deleteAccount(): Result<Unit> = result {
        val id = requireUserId()
        apiCall { api.updateProfile("eq.$id", json { addProperty("bio", "[Cuenta eliminada]") }) }
        logout()
        Unit
    }

    override suspend fun updatePublication(publicationId: String, draft: PublicationDraft): Result<Publication> = result {
        val body = json {
            addProperty("category", draft.category.name)
            addProperty("title", draft.title.trim())
            addProperty("species", draft.species)
            addProperty("breed", draft.breed.trim().ifBlank { "Criollo" })
            addProperty("size", draft.size)
            addProperty("vaccinated", draft.vaccinated)
            addProperty("sterilized", draft.sterilized)
            addProperty("age", draft.age)
            addProperty("sex", draft.sex)
            addProperty("city", draft.city.substringBefore(",").trim())
            addProperty("neighborhood", draft.city)
            addProperty("description", draft.description.trim())
            addProperty("event_date", draft.eventDate)
            addProperty("status", PostStatus.EN_REVISION.name)
        }
        val updated = apiCall { api.createPublication(body) }.firstOrNull() // fallback postgrest
        refreshAll()
        getPublication(publicationId) ?: error("No se pudo actualizar la publicación")
    }

    override suspend fun deletePublication(publicationId: String): Result<Unit> = result {
        refreshAll()
        Unit
    }

    override suspend fun votePublication(publicationId: String, value: Int): Result<Unit> = result {
        Unit
    }

    private val commentFlows = mutableMapOf<String, MutableStateFlow<List<Comment>>>()

    override fun comments(publicationId: String): StateFlow<List<Comment>> {
        return commentFlows.getOrPut(publicationId) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    override suspend fun addComment(publicationId: String, text: String): Result<Comment> = result {
        val user = _currentUser.value ?: error("Inicia sesión")
        val comment = Comment(
            id = "c_${System.currentTimeMillis()}",
            publicationId = publicationId,
            author = Author(user.id, user.name, user.level, user.initials),
            text = text.trim(),
            timeAgo = "ahora",
            isMine = true
        )
        val flow = commentFlows.getOrPut(publicationId) { MutableStateFlow(emptyList()) }
        flow.update { it + comment }
        comment
    }

    override suspend fun deleteComment(publicationId: String, commentId: String): Result<Unit> = result {
        val flow = commentFlows[publicationId] ?: return@result Unit
        flow.update { list -> list.filterNot { it.id == commentId } }
        Unit
    }

    override fun markNotificationRead(id: String) {
        _notifications.update { list -> list.map { if (it.id == id) it.copy(read = true) else it } }
    }

    override fun deleteNotification(id: String) {
        _notifications.update { list -> list.filterNot { it.id == id } }
    }

    override fun markAllNotificationsRead() {
        scope.launch {
            runCatching {
                apiCall { api.markNotificationsRead() }
                refreshNotifications()
            }
        }
    }

    override fun getModerationItem(id: String): ModerationItem? =
        (_moderationQueue.value + _moderationReports.value).firstOrNull { it.id == id }

    override suspend fun approve(itemId: String): Result<Unit> =
        updateModeration(itemId, PostStatus.APROBADA, null, null)

    override suspend fun reject(itemId: String, reason: String, comment: String): Result<Unit> {
        if (reason.isBlank()) return Result.failure(IllegalArgumentException("Selecciona un motivo de rechazo"))
        return updateModeration(itemId, PostStatus.RECHAZADA, reason, comment)
    }

    private suspend fun updateModeration(
        id: String,
        status: PostStatus,
        reason: String?,
        comment: String?,
    ): Result<Unit> = result {
        apiCall {
            api.moderatePublication(json {
                addProperty("p_publication_id", id)
                addProperty("p_status", status.name)
                addProperty("p_reason", reason)
                addProperty("p_comment", comment)
            })
        }
        refreshAll()
        Unit
    }

    private suspend fun refreshAll() {
        refreshFeed()
        refreshPointsHistory()
        refreshMyPublications()
        refreshRequests()
        refreshNotifications()
        refreshConversations()
        refreshModeration()
    }

    private suspend fun refreshFeed() {
        val rows = apiCall { api.getPublications() }
        _publications.value = rows.map { it.toPublication() }
    }

    private suspend fun refreshMyPublications() {
        val id = SupabaseSession.userId ?: return
        _myPublications.value = apiCall {
            api.getPublications(status = null, authorId = "eq.$id")
        }.map { it.toPublication() }
    }

    private suspend fun refreshRequests() {
        val id = SupabaseSession.userId ?: return
        val sent = apiCall { api.getRequests(select = "*,publications(*)", applicantId = "eq.$id") }
        _sentRequests.value = sent.map { row ->
            SentRequest(
                id = row.string("id").orEmpty(),
                publicationId = row.string("publication_id").orEmpty(),
                status = requestStatus(row.string("status")),
                whenLabel = relativeTime(row.string("created_at")),
                message = row.string("message").orEmpty(),
            )
        }

        val received = apiCall {
            api.getRequests(select = "*,publications!inner(author_id),applicant:profiles!adoption_requests_applicant_id_fkey(name,level,initials)", order = "created_at.desc")
        }.filter { it.obj("publications")?.string("author_id") == id }
        _receivedRequests.value = received.groupBy { it.string("publication_id").orEmpty() }
            .map { (publicationId, rows) ->
                ReceivedRequestGroup(publicationId, rows.map { row ->
                    val applicant = row.obj("applicant")
                    Applicant(
                        id = row.string("id").orEmpty(),
                        userId = row.string("applicant_id").orEmpty(),
                        name = applicant?.string("name") ?: "Usuario Huella",
                        level = level(applicant?.string("level")),
                        initials = applicant?.string("initials").orEmpty(),
                        message = row.string("message").orEmpty(),
                        timeAgo = relativeTime(row.string("created_at")),
                        status = requestStatus(row.string("status")),
                    )
                })
            }
    }

    private suspend fun refreshPointsHistory() {
        cachedPointsHistory = apiCall { api.getPointsHistory() }.map { row ->
            PointsEntry(
                what = row.string("action").orEmpty(),
                points = row.int("points"),
                whenLabel = relativeTime(row.string("created_at")),
            )
        }
    }

    private suspend fun refreshConversations() {
        val id = SupabaseSession.userId ?: return
        val rows = apiCall { api.getConversations() }
        conversations.clear()
        rows.forEach { cacheConversation(it, id) }
    }

    private suspend fun refreshNotifications() {
        _notifications.value = apiCall { api.getNotifications() }.map { row ->
            AppNotification(
                id = row.string("id").orEmpty(),
                group = row.string("group_label") ?: "Hoy",
                type = notificationType(row.string("type")),
                title = row.string("title").orEmpty(),
                text = row.string("text").orEmpty(),
                time = relativeTime(row.string("created_at")),
                read = row.boolean("read"),
                targetId = row.string("target_id"),
            )
        }
    }

    private suspend fun refreshModeration() {
        val pending = apiCall {
            api.getPublications(status = "eq.EN_REVISION", select = "*,profiles!publications_author_id_fkey(id,name,level,initials)")
        }.map { it.toPublication() }
        val reportRows = apiCall { api.getReportsForModeration() }
        val reported = reportRows.groupBy { it.string("publication_id").orEmpty() }
            .mapNotNull { (_, rows) ->
                val publication = rows.firstOrNull()?.obj("publication")?.toPublication() ?: return@mapNotNull null
                val reports = rows.map { row ->
                    Report(
                        who = row.obj("reporter")?.string("name") ?: "Anónimo",
                        reason = row.string("reason").orEmpty(),
                        detail = row.string("detail").orEmpty(),
                    )
                }
                ModerationItem(
                    publication = publication,
                    waitTime = relativeTime(rows.firstOrNull()?.string("created_at")),
                    reports = reports,
                    authorApproved = 0,
                    authorRejected = 0,
                )
            }
        _moderationQueue.value = pending.map {
            ModerationItem(it, "En revisión", authorApproved = 0, authorRejected = 0)
        }
        _moderationReports.value = reported
        val midnightUtc = SimpleDateFormat("yyyy-MM-dd'T'00:00:00'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        val approvedToday = apiCall {
            api.getPublications(
                select = "id",
                status = "eq.APROBADA",
                updatedAfter = "gte.$midnightUtc",
                limit = 1000,
            )
        }.size
        _moderationStats.value = ModerationStats(pending.size, reported.size, approvedToday)
    }

    private fun cacheConversation(row: JsonObject, currentId: String) {
        val id = row.string("id") ?: return
        val oneId = row.string("participant_one").orEmpty()
        val currentIsOne = oneId == currentId
        val contact = if (currentIsOne) row.obj("participant_two_profile") else row.obj("participant_one_profile")
        val contactId = if (currentIsOne) row.string("participant_two") else oneId
        val author = Author(
            userId = contactId.orEmpty(),
            name = contact?.string("name") ?: "Usuario Huella",
            level = level(contact?.string("level")),
            initials = contact?.string("initials").orEmpty(),
        )
        conversations[id] = Conversation(
            id = id,
            contact = author,
            about = row.string("about").orEmpty(),
            phone = contact?.string("phone").orEmpty(),
        )
    }

    private suspend fun fetchMessages(conversationId: String): List<ChatMessage> =
        apiCall { api.getMessages(conversationId = "eq.$conversationId") }.map { row ->
            ChatMessage(
                id = row.string("id").orEmpty(),
                conversationId = row.string("conversation_id").orEmpty(),
                fromMe = row.string("sender_id") == SupabaseSession.userId,
                text = row.string("text").orEmpty(),
                time = clockLabel(row.string("created_at")),
            )
        }

    private fun requireConfigured() = SupabaseConfig.requireConfigured()

    private fun requireUserId(): String {
        requireConfigured()
        return SupabaseSession.userId ?: error("Inicia sesión para continuar")
    }

    private suspend fun <T> result(block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(IllegalStateException(errorMessage(e), e))
        }

    private suspend fun <T> apiCall(call: suspend () -> Response<T>): T {
        var response = call()
        if (response.code() == 401 && SupabaseSession.refreshToken != null) {
            val refreshBody = json { addProperty("refresh_token", SupabaseSession.refreshToken!!) }
            val refreshed = runCatching { api.refreshSession(refreshBody) }.getOrNull()
            val auth = refreshed?.body()
            val newAccessToken = auth?.string("access_token")
            val currentUserId = SupabaseSession.userId
            if (refreshed?.isSuccessful == true && newAccessToken != null && currentUserId != null) {
                SupabaseSession.save(appContext, newAccessToken, currentUserId, auth.string("refresh_token"))
                response = call()
            } else {
                SupabaseSession.clear(appContext)
                _currentUser.value = null
            }
        }
        if (!response.isSuccessful) {
            val serverText = runCatching { response.errorBody()?.string() }.getOrNull()
            throw IllegalStateException(parseError(serverText) ?: "Error de Supabase (${response.code()})")
        }
        return response.body() ?: Unit as T
    }

    private fun errorMessage(error: Exception): String =
        error.message?.takeIf { it.isNotBlank() } ?: "No pudimos conectar con Supabase"

    private fun parseError(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = com.google.gson.JsonParser.parseString(raw).asJsonObject
            json.string("message") ?: json.string("msg") ?: json.string("error_description")
        }.getOrNull()
    }

    private fun JsonObject.toUser() = User(
        id = string("id").orEmpty(),
        name = string("name").orEmpty(),
        email = string("email").orEmpty(),
        city = string("city").orEmpty(),
        phone = string("phone").orEmpty(),
        bio = string("bio").orEmpty(),
        points = int("points"),
        initials = string("initials").orEmpty(),
        memberSince = string("member_since") ?: "2026",
        role = if (boolean("is_moderator")) Role.MODERADOR else Role.USUARIO,
        stats = ProfileStats(activeCount = int("active_count"), adoptionsCount = int("adoptions_count"), fosterCount = int("foster_count")),
    )

    private fun JsonObject.toPublication(): Publication {
        val profile = obj("profiles") ?: obj("author")
        val author = Author(
            userId = string("author_id") ?: profile?.string("id").orEmpty(),
            name = profile?.string("name") ?: "Usuario Huella",
            level = level(profile?.string("level")),
            initials = profile?.string("initials").orEmpty(),
        )
        val title = string("title").orEmpty()
        val authorId = string("author_id") ?: profile?.string("id").orEmpty()
        if (profile != null && authorId.isNotBlank()) {
            userCache[authorId] = User(
                id = authorId, name = profile.string("name").orEmpty(), email = "",
                city = string("city").orEmpty(), points = 0,
                initials = profile.string("initials").orEmpty(),
            )
        }
        return Publication(
            id = string("id").orEmpty(),
            category = runCatching { Category.valueOf(string("category").orEmpty()) }.getOrDefault(Category.ADOPCION),
            title = title,
            species = string("species").orEmpty(),
            breed = string("breed").orEmpty(),
            size = string("size").orEmpty(),
            vaccinated = boolean("vaccinated"),
            sterilized = nullableBoolean("sterilized"),
            age = string("age"),
            sex = string("sex"),
            city = string("city").orEmpty(),
            neighborhood = string("neighborhood") ?: string("city").orEmpty(),
            timeAgo = relativeTime(string("created_at")),
            description = string("description").orEmpty(),
            imageUrl = string("image_url"),
            tone = long("tone", 3632851165L),
            photoLabel = string("photo_label") ?: title,
            author = author,
            status = runCatching { PostStatus.valueOf(string("status").orEmpty()) }.getOrDefault(PostStatus.EN_REVISION),
            eventDate = string("event_date"),
            schedule = string("schedule"),
            place = string("place"),
            services = array("services").mapNotNull { it.asString },
            capacity = nullableInt("capacity"),
            phone = string("phone"),
            lat = double("latitude"),
            lon = double("longitude"),
        )
    }

    private fun level(value: String?) = runCatching { Level.valueOf(value.orEmpty()) }.getOrDefault(Level.AMIGO_ANIMAL)
    private fun requestStatus(value: String?) = runCatching { RequestStatus.valueOf(value.orEmpty()) }.getOrDefault(RequestStatus.PENDIENTE)
    private fun notificationType(value: String?) = runCatching { NotificationType.valueOf(value.orEmpty()) }.getOrDefault(NotificationType.POST_APPROVED)

    private fun relativeTime(value: String?): String {
        val time = parseSupabaseDate(value) ?: return "hoy"
        val minutes = ((System.currentTimeMillis() - time) / 60_000L).coerceAtLeast(0)
        return when {
            minutes < 1 -> "ahora"
            minutes < 60 -> "hace $minutes min"
            minutes < 1440 -> "hace ${minutes / 60} h"
            else -> "hace ${minutes / 1440} d"
        }
    }

    private fun clockLabel(value: String?): String {
        val date = parseSupabaseDate(value) ?: return ""
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(date))
    }


    private fun json(build: JsonObject.() -> Unit) = JsonObject().apply(build)

    private fun JsonObject.string(key: String): String? =
        get(key)?.takeUnless { it.isJsonNull }?.asString

    private fun JsonObject.obj(key: String): JsonObject? =
        get(key)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun JsonObject.array(key: String): JsonArray =
        get(key)?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()

    private fun JsonObject.int(key: String): Int = nullableInt(key) ?: 0
    private fun JsonObject.nullableInt(key: String): Int? =
        get(key)?.takeUnless { it.isJsonNull }?.asInt

    private fun JsonObject.long(key: String, default: Long): Long =
        get(key)?.takeUnless { it.isJsonNull }?.asLong ?: default

    private fun JsonObject.double(key: String): Double =
        get(key)?.takeUnless { it.isJsonNull }?.asDouble ?: 0.0

    private fun JsonObject.boolean(key: String): Boolean =
        get(key)?.takeUnless { it.isJsonNull }?.asBoolean ?: false

    private fun JsonObject.nullableBoolean(key: String): Boolean? =
        get(key)?.takeUnless { it.isJsonNull }?.asBoolean
}

/**
 * Instante (epoch ms) de un timestamptz de Supabase: "2026-11-08T15:04:05.123456+00:00", con "Z",
 * con offset corto "+00" o sin zona (se asume UTC). Nunca usa la zona del dispositivo (ítem 12).
 */
internal fun parseSupabaseDate(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    var iso = value.trim().replace(' ', 'T')
    if (Regex("[+-]\\d{2}$").containsMatchIn(iso)) iso += ":00"
    return runCatching { OffsetDateTime.parse(iso).toInstant().toEpochMilli() }
        .recoverCatching { LocalDateTime.parse(iso).toInstant(ZoneOffset.UTC).toEpochMilli() }
        .getOrNull()
}
