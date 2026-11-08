package com.desarrolloMovielexample.huella.data.remote

import android.content.Context
import android.net.Uri
import com.desarrolloMovielexample.huella.data.remote.api.SupabaseApiService
import com.desarrolloMovielexample.huella.domain.model.*
import com.desarrolloMovielexample.huella.domain.repository.HuellaRepository
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import java.net.URLDecoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
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
    /** Deep link de los correos de Auth (debe estar en las Redirect URLs del proyecto). */
    private const val AUTH_REDIRECT = "huella://auth/confirm"
    private const val FEED_PAGE = 20
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
    private val authorPublications = mutableMapOf<String, List<Publication>>()
    /** Las mismas acciones y puntos que award_points en el servidor; la UI las traduce con pointsActionLabel. */
    override val pointsActions = listOf(
        PointsAction("Crear una publicación", 5),
        PointsAction("Publicación aprobada", 10),
        PointsAction("Comentar una publicación", 2),
        PointsAction("Reportar un avistamiento", 5),
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
        // Con sesión guardada, el Splash llama a restoreSession() y esa carga perfil y datos.
        if (SupabaseSession.accessToken == null) scope.launch { quietly { refreshFeed() } }
    }

    /**
     * Splash (ítem 25): true si hay token guardado y rpc/get_my_profile responde (apiCall refresca el
     * token si hace falta). Si no, la app va a Login aunque quede un token viejo en disco.
     */
    suspend fun restoreSession(): Boolean {
        if (!SupabaseConfig.isConfigured || SupabaseSession.accessToken == null) return false
        val profile = try {
            apiCall { api.getMyProfile() }.firstOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return false
        _currentUser.value = profile.toUser()
        startRefreshAll()
        return true
    }

    private val refreshMutex = Mutex()
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Emite cuando el refresh token ya no sirve: AppNavigation vuelve a Login (ítem 26). */
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

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
        val profile = apiCall { api.getMyProfile() }.firstOrNull()
            ?: error("No encontramos el perfil asociado a esta cuenta")
        val user = profile.toUser()
        _currentUser.value = user
        startRefreshAll()
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
            addPkceChallenge()
        }
        val response = apiCall { api.signUp(body, AUTH_REDIRECT) }
        val token = response.string("access_token")
            ?: error("Cuenta creada. Confirma tu correo y luego inicia sesión.")
        val userJson = response.obj("user") ?: error("Supabase no devolvió el usuario")
        val id = userJson.string("id") ?: error("El usuario no tiene un identificador")
        SupabaseSession.save(appContext, token, id, response.string("refresh_token"))
        val profile = apiCall { api.getMyProfile() }.firstOrNull()
            ?: error("La cuenta se creó, pero aún no aparece su perfil")
        val user = profile.toUser()
        _currentUser.value = user
        startRefreshAll()
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

        params["error_description"]?.let { error(it) } // p. ej. enlace vencido

        // Ítem 36: solo PKCE. huella:// lo puede registrar otra app; un code interceptado no sirve sin el
        // code_verifier que guarda esta app, pero un token_hash o un access_token sí darían sesión: se rechazan.
        val code = params["code"]
        if (code.isNullOrBlank()) error("Este enlace no es válido. Solicita uno nuevo desde la app")
        val verifier = SupabaseSession.codeVerifier
            ?: error("Abre el enlace en el mismo teléfono donde lo solicitaste")
        val session = apiCall { api.exchangeCodeForSession(json {
            addProperty("auth_code", code)
            addProperty("code_verifier", verifier)
        }) }
        SupabaseSession.saveCodeVerifier(appContext, null)
        val accessToken = session.string("access_token")
        if (accessToken.isNullOrBlank()) error("No se encontraron credenciales de autenticación en el enlace")
        val refreshToken = session.string("refresh_token")
        val userId = session.obj("user")?.string("id")
            ?: apiCall { api.getUser("Bearer $accessToken") }.string("id")
            ?: error("No se pudo obtener el usuario autenticado")

        SupabaseSession.save(appContext, accessToken, userId, refreshToken)

        var profile: JsonObject? = null
        for (i in 0..4) {
            profile = apiCall { api.getMyProfile() }.firstOrNull()
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
        startRefreshAll()
        user
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = result {
        requireConfigured()
        val body = json {
            addProperty("email", email.trim())
            addPkceChallenge()
        }
        // type=recovery viaja en el redirect para que AppNavigation abra SetPassword tras canjear el code.
        apiCall { api.recoverPassword(body, "$AUTH_REDIRECT?type=recovery") }
        Unit
    }

    override suspend fun updatePassword(password: String): Result<Unit> = result {
        requireUserId()
        apiCall { api.updateAuthUser(json { addProperty("password", password) }) }
        Unit
    }

    /**
     * PKCE S256: guarda un code_verifier aleatorio en SupabaseSession y manda su challenge.
     * GoTrue lee code_challenge del body (no de la URL) en signup y recover.
     */
    private fun JsonObject.addPkceChallenge() {
        val verifier = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
        SupabaseSession.saveCodeVerifier(appContext, verifier)
        val challenge = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
        addProperty("code_challenge", challenge)
        addProperty("code_challenge_method", "s256")
    }

    override suspend fun updateProfile(name: String, email: String, city: String, phone: String, bio: String): Result<User> = result {
        val id = requireUserId()
        val values = json {
            addProperty("name", name.trim())
            addProperty("city", city.substringBefore(",").trim())
            addProperty("phone", phone.trim())
            addProperty("bio", bio.trim())
        }
        apiCall { api.updateProfile("eq.$id", values) }
        // profiles.email no es editable por el cliente: el cambio pasa por Auth (correo de confirmación)
        // y un trigger en auth.users lo copia al perfil cuando se confirma.
        val newEmail = email.trim()
        if (newEmail.isNotBlank() && !newEmail.equals(_currentUser.value?.email, ignoreCase = true)) {
            // PKCE también aquí: el enlace de confirmación vuelve con ?code= (handleAuthCallback solo acepta code).
            apiCall { api.updateAuthUser(json {
                addProperty("email", newEmail)
                addPkceChallenge()
            }) }
        }
        val profile = apiCall { api.getMyProfile() }.firstOrNull()
            ?: error("No se pudo actualizar el perfil")
        profile.toUser().also { _currentUser.value = it }
    }

    override fun logout() {
        // Un refresh en vuelo de esta cuenta no debe escribir después del logout (ni en la sesión siguiente).
        refreshJob?.cancel()
        refreshJob = null
        _currentUser.value = null
        SupabaseSession.clear(appContext)
        _sentRequests.value = emptyList()
        _receivedRequests.value = emptyList()
        _notifications.value = emptyList()
        _myPublications.value = emptyList()
        cachedPointsHistory = emptyList()
        userCache.clear()
        authorPublications.clear()
        messageFlows.clear()
        commentFlows.clear()
        conversations.clear()
        clearModeration()
    }

    private fun clearModeration() {
        _moderationQueue.value = emptyList()
        _moderationReports.value = emptyList()
        _moderationStats.value = ModerationStats(0, 0, 0)
    }

    override fun getUser(userId: String): User? =
        _currentUser.value?.takeIf { it.id == userId } ?: userCache[userId]

    /** Perfil público (solo columnas públicas) y publicaciones aprobadas de [userId], en caché (ítem 28). */
    override suspend fun refreshUser(userId: String): Result<User> = result {
        val user = apiCall { api.getProfile(userId = "eq.$userId") }.firstOrNull()?.toUser()
            ?: error("No encontramos este perfil")
        val publications = apiCall { api.getPublications(authorId = "eq.$userId") }.map { it.toPublication() }
        userCache[userId] = user
        authorPublications[userId] = publications
        user
    }

    /** Filtros de servidor del feed (categoría, especie, ciudad sin departamento) que reutilizan las páginas siguientes. */
    private var feedFilter: Triple<Category?, String?, String?> = Triple(null, null, null)

    override suspend fun loadFeed(category: Category?, species: String?, city: String?, reset: Boolean): Result<Boolean> = result {
        if (reset) feedFilter = Triple(category, species, city?.substringBefore(",")?.trim())
        val page = fetchFeed(offset = if (reset) 0 else _publications.value.size, limit = FEED_PAGE)
        _publications.value = if (reset) page else (_publications.value + page).distinctBy { it.id }
        page.size == FEED_PAGE
    }

    private suspend fun fetchFeed(offset: Int, limit: Int): List<Publication> = apiCall {
        val (category, species, city) = feedFilter
        api.getPublications(
            category = category?.let { "eq.${it.name}" },
            species = species?.let { "eq.$it" },
            city = city?.let { "eq.$it" },
            order = "created_at.desc,id.desc", // orden total: las páginas no se pisan ni saltan filas
            limit = limit,
            offset = offset,
        )
    }.map { it.toPublication() }

    override fun getPublication(id: String): Publication? =
        (_publications.value + _myPublications.value + _moderationQueue.value.map { it.publication })
            .firstOrNull { it.id == id }

    override fun publicationsByAuthor(userId: String): List<Publication> =
        authorPublications[userId] ?: (_publications.value + _myPublications.value).distinctBy { it.id }
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
        created.toPublication()
    }.onSuccess { quietly { refreshMyPublications() } }

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
        Unit
    }.onSuccess { quietly { refreshRequests() } }

    /** [applicantId] es el id de la solicitud (Applicant.id); las notificaciones las crea el servidor (ítem 7). */
    override suspend fun respondToRequest(applicantId: String, accept: Boolean): Result<Unit> = result {
        apiCall { api.respondToRequest(json {
            addProperty("p_request_id", applicantId)
            addProperty("p_accept", accept)
        }) }
        Unit
    }.onSuccess {
        quietly { refreshRequests() }
        if (accept) quietly { refreshConversations() }
    }

    override suspend fun closePublication(publicationId: String): Result<Unit> = result {
        requireUserId()
        apiCall { api.closePublication(json { addProperty("p_publication_id", publicationId) }) }
        val close = { p: Publication -> if (p.id == publicationId) p.copy(lifecycle = LifecycleStatus.FINALIZADA) else p }
        _myPublications.update { it.map(close) }
        _publications.update { it.map(close) }
        Unit
    }.onSuccess {
        // El servidor suma puntos y actualiza contadores del perfil.
        quietly { refreshMyProfile() }
        quietly { refreshPointsHistory() }
    }

    private suspend fun refreshMyProfile() {
        apiCall { api.getMyProfile() }.firstOrNull()?.let { _currentUser.value = it.toUser() }
    }

    override fun getConversation(id: String): Conversation? = conversations[id]

    override suspend fun openConversation(contactUserId: String, publicationId: String?): Result<String> = result {
        val currentId = requireUserId()
        require(contactUserId != currentId) { "No puedes iniciar un chat contigo mismo" }
        // El servidor filtra el par (en cualquier orden) y la publicación: no se descargan todas las conversaciones.
        val existing = apiCall {
            api.getConversations(
                or = "(and(participant_one.eq.$currentId,participant_two.eq.$contactUserId)," +
                    "and(participant_one.eq.$contactUserId,participant_two.eq.$currentId))",
                publicationId = publicationId?.let { "eq.$it" },
            )
        }.firstOrNull()
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
            val hydrated = apiCall { api.getConversations(id = "eq.$createdId") }.firstOrNull()
            cacheConversation(hydrated ?: created, currentId)
            createdId
        }
    }

    override fun messages(conversationId: String): StateFlow<List<ChatMessage>> {
        val flow = messageFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
        scope.launch {
            if (conversations[conversationId] == null) quietly { refreshConversations() }
            quietly { loadContactPhone(conversationId) }
            quietly { flow.value = fetchMessages(conversationId) }
        }
        return flow.asStateFlow()
    }

    /** El teléfono del contacto no viaja en el embed de profiles: solo lo entrega rpc/get_contact_phone a participantes. */
    private suspend fun loadContactPhone(conversationId: String) {
        val conversation = conversations[conversationId] ?: return
        if (conversation.phone.isNotBlank()) return
        val phone = apiCall { api.getContactPhone(json { addProperty("p_conversation_id", conversationId) }) }
            .takeUnless { it.isJsonNull }?.asString.orEmpty()
        conversations[conversationId] = conversation.copy(phone = phone)
    }

    override suspend fun sendMessage(conversationId: String, text: String): Result<Unit> = result {
        val senderId = requireUserId()
        apiCall { api.sendMessage(json {
            addProperty("conversation_id", conversationId)
            addProperty("sender_id", senderId)
            addProperty("text", text.trim())
        }) }
        Unit
    }.onSuccess {
        quietly { messageFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }.value = fetchMessages(conversationId) }
    }


    override suspend fun deleteAccount(): Result<Unit> = result {
        requireUserId()
        apiCall { api.deleteMyAccount() }
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
            draft.imageUrl?.let { addProperty("image_url", it) }
        }
        // El servidor devuelve la fila editada de vuelta en EN_REVISION: el autor no elige el estado.
        val updated = apiCall { api.updatePublication("eq.$publicationId", body) }.firstOrNull()
            ?: error("No se pudo actualizar la publicación")
        val publication = updated.toPublication()
        _myPublications.update { list -> list.map { if (it.id == publicationId) publication else it } }
        _publications.update { list ->
            if (publication.status == PostStatus.APROBADA) list.map { if (it.id == publicationId) publication else it }
            else list.filterNot { it.id == publicationId }
        }
        publication
    }

    override suspend fun deletePublication(publicationId: String): Result<Unit> = result {
        val deleted = apiCall { api.deletePublication("eq.$publicationId") }
        check(deleted.isNotEmpty()) { "No se pudo eliminar la publicación" }
        _publications.update { list -> list.filterNot { it.id == publicationId } }
        _myPublications.update { list -> list.filterNot { it.id == publicationId } }
        _moderationQueue.update { list -> list.filterNot { it.id == publicationId } }
        _moderationReports.update { list -> list.filterNot { it.id == publicationId } }
        commentFlows.remove(publicationId)
        Unit
    }.onSuccess { quietly { refreshRequests() } }

    /** Mismo botón otra vez = quitar el voto (igual que FakeRepository). */
    override suspend fun votePublication(publicationId: String, value: Int): Result<Unit> = result {
        val userId = requireUserId()
        require(value in -1..1) { "Voto inválido" }
        val newVote = if (value == getPublication(publicationId)?.userVote) 0 else value
        if (newVote == 0) {
            apiCall { api.deleteVote("eq.$publicationId", "eq.$userId") }
        } else {
            apiCall { api.upsertVote(json {
                addProperty("publication_id", publicationId)
                addProperty("user_id", userId)
                addProperty("value", newVote)
            }) }
        }
        Unit
    }.onSuccess { refreshPublication(publicationId) }

    /** Relee votes_count, comments_count y el voto propio, y los aplica en las listas en caché. */
    override suspend fun refreshPublication(publicationId: String): Result<Unit> = result {
        val counts = apiCall { api.getPublicationCounts("eq.$publicationId") }.firstOrNull() ?: return@result Unit
        val userId = SupabaseSession.userId
        val myVote = if (userId == null) 0 else {
            apiCall { api.getMyVote("eq.$publicationId", "eq.$userId") }.firstOrNull()?.int("value") ?: 0
        }
        val apply = { p: Publication ->
            if (p.id != publicationId) p
            else p.copy(votesCount = counts.int("votes_count"), commentsCount = counts.int("comments_count"), userVote = myVote)
        }
        _publications.update { it.map(apply) }
        _myPublications.update { it.map(apply) }
        Unit
    }

    private val commentFlows = mutableMapOf<String, MutableStateFlow<List<Comment>>>()

    override fun comments(publicationId: String): StateFlow<List<Comment>> {
        val flow = commentFlows.getOrPut(publicationId) { MutableStateFlow(emptyList()) }
        scope.launch { quietly { flow.value = fetchComments(publicationId) } }
        return flow.asStateFlow()
    }

    override suspend fun addComment(publicationId: String, text: String): Result<Comment> = result {
        val authorId = requireUserId()
        val created = apiCall { api.createComment(json {
            addProperty("publication_id", publicationId)
            addProperty("author_id", authorId)
            addProperty("text", text.trim())
        }) }.firstOrNull() ?: error("Supabase no devolvió el comentario")
        created.toComment()
    }.onSuccess { comment ->
        commentFlows.getOrPut(publicationId) { MutableStateFlow(emptyList()) }.update { it + comment }
        quietly { commentFlows.getValue(publicationId).value = fetchComments(publicationId) }
        refreshPublication(publicationId)
    }

    override suspend fun deleteComment(publicationId: String, commentId: String): Result<Unit> = result {
        val deleted = apiCall { api.deleteComment("eq.$commentId") }
        check(deleted.isNotEmpty()) { "No se pudo eliminar el comentario" }
        commentFlows[publicationId]?.update { list -> list.filterNot { it.id == commentId } }
        Unit
    }.onSuccess { refreshPublication(publicationId) }

    override suspend fun reportSighting(publicationId: String, text: String): Result<Unit> = result {
        requireUserId()
        apiCall { api.reportSighting(json {
            addProperty("p_publication_id", publicationId)
            addProperty("p_text", text.trim())
        }) }
        Unit
    }

    private suspend fun fetchComments(publicationId: String): List<Comment> =
        apiCall { api.getComments("eq.$publicationId") }.map { it.toComment() }

    private fun JsonObject.toComment(): Comment {
        val author = obj("author")
        val authorId = string("author_id").orEmpty()
        return Comment(
            id = string("id").orEmpty(),
            publicationId = string("publication_id").orEmpty(),
            author = Author(
                userId = authorId,
                name = author?.string("name") ?: "Usuario Huella",
                level = level(author?.string("level")),
                initials = author?.string("initials").orEmpty(),
            ),
            text = string("text").orEmpty(),
            timeAgo = relativeTime(string("created_at")),
            isMine = authorId == SupabaseSession.userId,
        )
    }

    override fun markNotificationRead(id: String) {
        _notifications.update { list -> list.map { if (it.id == id) it.copy(read = true) else it } }
        scope.launch {
            runCatching { apiCall { api.updateNotification("eq.$id", json { addProperty("read", true) }) } }
                .onFailure { quietly { refreshNotifications() } }
        }
    }

    override fun deleteNotification(id: String) {
        _notifications.update { list -> list.filterNot { it.id == id } }
        scope.launch {
            runCatching { apiCall { api.deleteNotification("eq.$id") } }
                .onFailure { quietly { refreshNotifications() } }
        }
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

    override suspend fun resolveReport(reportId: String): Result<Unit> = result {
        apiCall { api.resolveReport(json { addProperty("p_report_id", reportId) }) }
        Unit
    }.onSuccess { quietly { refreshModeration() } }

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
        Unit
    }.onSuccess {
        quietly { refreshModeration() }
        quietly { refreshFeed() }
    }

    @Volatile
    private var refreshJob: Job? = null

    /** Lanza refreshAll() cancelando el de la sesión anterior (login/logout rápidos no mezclan cuentas). */
    private fun startRefreshAll() {
        refreshJob?.cancel()
        refreshJob = scope.launch { refreshAll() }
    }

    /** Recarga todo lo visible; cada parte falla por separado sin tumbar a las demás. */
    private suspend fun refreshAll() {
        quietly { refreshFeed() }
        quietly { refreshPointsHistory() }
        quietly { refreshMyPublications() }
        quietly { refreshRequests() }
        quietly { refreshNotifications() }
        quietly { refreshConversations() }
        if (_currentUser.value?.isModerator == true) quietly { refreshModeration() } else clearModeration()
    }

    /** Refrescos después de una escritura: si fallan, la escritura sigue siendo un éxito (ítem 27). */
    private suspend fun quietly(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    /** Relee desde el principio tantas filas como ya estaban cargadas (mínimo una página) con los filtros vigentes. */
    private suspend fun refreshFeed() {
        _publications.value = fetchFeed(offset = 0, limit = maxOf(FEED_PAGE, _publications.value.size))
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
        val now = System.currentTimeMillis()
        _notifications.value = apiCall { api.getNotifications() }.map { row ->
            AppNotification(
                id = row.string("id").orEmpty(),
                // group_label en BD es texto fijo ("Hoy") que caduca: el grupo se calcula con created_at.
                group = notificationGroup(parseSupabaseDate(row.string("created_at")), now),
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
                        id = row.string("id").orEmpty(),
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
            phone = conversations[id]?.phone.orEmpty(),
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
        val tokenUsed = SupabaseSession.accessToken
        var response = call()
        if (response.code() == 401 && tokenUsed != null && refreshAccessToken(tokenUsed)) {
            response = call()
        }
        if (!response.isSuccessful) {
            val serverText = runCatching { response.errorBody()?.string() }.getOrNull()
            throw IllegalStateException(parseError(serverText) ?: "Error de Supabase (${response.code()})")
        }
        return response.body() ?: Unit as T
    }

    /**
     * Un solo refresh a la vez (ítem 26): si dos llamadas reciben 401 juntas, la segunda reutiliza el
     * token nuevo en vez de gastar otra vez el mismo refresh token (Supabase revocaría la familia).
     * Sin red no se cierra la sesión; si Supabase rechaza el refresh token, se cierra y se avisa.
     */
    private suspend fun refreshAccessToken(staleToken: String): Boolean = refreshMutex.withLock {
        val current = SupabaseSession.accessToken
        if (current != null && current != staleToken) return@withLock true
        val refreshToken = SupabaseSession.refreshToken
        val userId = SupabaseSession.userId
        if (refreshToken != null && userId != null) {
            val response = try {
                api.refreshSession(json { addProperty("refresh_token", refreshToken) })
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return@withLock false
            }
            val newToken = response.body()?.string("access_token")
            if (response.isSuccessful && newToken != null) {
                SupabaseSession.save(appContext, newToken, userId, response.body()?.string("refresh_token") ?: refreshToken)
                return@withLock true
            }
        }
        logout()
        _sessionExpired.tryEmit(Unit)
        false
    }

    private fun errorMessage(error: Exception): String =
        error.message?.takeIf { it.isNotBlank() } ?: "No pudimos conectar con Supabase"

    private fun parseError(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val json = com.google.gson.JsonParser.parseString(raw).asJsonObject
            // 23505: restricción UNIQUE (reporte o solicitud repetidos).
            if (json.string("code") == "23505") "Ya lo habías enviado antes"
            else json.string("message") ?: json.string("msg") ?: json.string("error_description")
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
        stats = ProfileStats(
            activeCount = int("active_count"),
            finishedCount = (int("publications_count") - int("active_count")).coerceAtLeast(0),
            adoptionsCount = int("adoptions_count"),
            fosterCount = int("foster_count"),
        ),
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
            lifecycle = runCatching { LifecycleStatus.valueOf(string("lifecycle").orEmpty()) }.getOrDefault(LifecycleStatus.ACTIVA),
            votesCount = int("votes_count"),
            commentsCount = int("comments_count"),
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
    private fun notificationType(value: String?) =
        runCatching { NotificationType.valueOf(value.orEmpty()) }.getOrDefault(NotificationType.POST_APPROVED)

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

/** Sección de la lista de notificaciones según el día local de created_at: Hoy, Ayer, Esta semana o Antes. */
internal fun notificationGroup(createdAt: Long?, now: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    if (createdAt == null) return "Antes"
    val day = Instant.ofEpochMilli(createdAt).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(day, Instant.ofEpochMilli(now).atZone(zone).toLocalDate())
    return when {
        days <= 0 -> "Hoy"
        days == 1L -> "Ayer"
        days < 7 -> "Esta semana"
        else -> "Antes"
    }
}
