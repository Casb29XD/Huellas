package com.desarrolloMovielexample.huella.domain.repository

import com.desarrolloMovielexample.huella.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Random photo for a publication (Coil loads it; striped placeholder shows meanwhile). */
fun randomPhotoUrl(seed: String): String = "https://picsum.photos/seed/huella$seed/600/400"

/**
 * In-memory repository seeded with every sample shown in the design.
 * Single source of truth for publications, comments, votes, badges, and roles.
 * Demo login: mariana@correo.com / 12345678 (MODERADOR)
 * Demo normal: andres@correo.com / 12345678 (USUARIO)
 */
object FakeRepository : HuellaRepository {

    private const val LATENCY = 350L

    // ---------- Users ----------
    private val marianaStats = ProfileStats(activeCount = 2, finishedCount = 1, pendingCount = 1, rejectedCount = 1, adoptionsCount = 1, fosterCount = 2, commentsCount = 8, votesReceived = 14)
    private val camilaStats = ProfileStats(activeCount = 2, finishedCount = 6, adoptionsCount = 6)
    private val andresStats = ProfileStats(activeCount = 1, finishedCount = 0)

    private val users = MutableStateFlow(
        listOf(
            User(
                id = "u_mariana",
                name = "Mariana López",
                email = "mariana@correo.com",
                city = "Medellín, Antioquia",
                phone = "+57 300 555 0123",
                bio = "Amante de los animales. He sido hogar temporal de 2 gatos y busco ayudar a más.",
                points = 155,
                initials = "ML",
                role = Role.MODERADOR,
                stats = marianaStats,
                badges = listOf(Badge.PRIMER_POST, Badge.HOGAR_TEMPORAL, Badge.COMUNIDAD_ACTIVA, Badge.MODERADOR)
            ),
            User(
                id = "u_camila",
                name = "Camila Restrepo",
                email = "camila@correo.com",
                city = "Medellín, Antioquia",
                points = 180,
                initials = "CR",
                memberSince = "2024",
                role = Role.USUARIO,
                stats = camilaStats,
                badges = listOf(Badge.PRIMER_POST, Badge.ADOPCION_EXITOSA)
            ),
            User(
                id = "u_andres",
                name = "Andrés Gómez",
                email = "andres@correo.com",
                city = "Bogotá, Cundinamarca",
                points = 20,
                initials = "AG",
                role = Role.USUARIO,
                stats = andresStats,
                badges = listOf(Badge.PRIMER_POST)
            ),
        ),
    )

    private val _currentUser = MutableStateFlow<User?>(users.value.first())
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val mariana get() = Author("u_mariana", "Mariana López", Level.PROTECTOR, "ML")
    private fun author(id: String, name: String, level: Level, initials: String) = Author(id, name, level, initials)

    // ---------- All Master Publications ----------
    // Una sola fuente de verdad para publicaciones. El feed público y mis publicaciones se derivan o sincronizan.
    private val allPublications = MutableStateFlow(
        listOf(
            Publication(
                id = "1", category = Category.ADOPCION, title = "Mía, cachorrita juguetona", species = "Perro", breed = "Criollo",
                size = "Mediano", vaccinated = true, sterilized = false, age = "4 meses", sex = "Hembra", city = "Medellín",
                neighborhood = "Poblado, Medellín", timeAgo = "hace 2 h",
                description = "Mía fue rescatada hace dos semanas en las afueras de Medellín. Es curiosa, alegre, convive bien con otros perros y adora correr en parques.",
                imageUrl = randomPhotoUrl("1"), tone = 0xFFD8E8DD, author = author("u_camila", "Camila Restrepo", Level.PROTECTOR, "CR"),
                status = PostStatus.APROBADA, lifecycle = LifecycleStatus.ACTIVA, votesCount = 8, commentsCount = 3,
                lat = 6.2088, lon = -75.5678,
            ),
            Publication(
                id = "2", category = Category.TEMPORAL, title = "Simba busca hogar temporal por 3 semanas", species = "Gato", breed = "Criollo",
                size = "Pequeño", vaccinated = true, sterilized = true, age = "2 años", sex = "Macho", city = "Envigado",
                neighborhood = "Zona Centro, Envigado", timeAgo = "hace 4 h",
                description = "Simba es un gato dócil y tranquilo. Su rescatista viajará tres semanas y busca una familia temporal sin perros que pueda cuidarlo.",
                imageUrl = randomPhotoUrl("2"), tone = 0xFFD8DDE8, author = author("u_andres", "Andrés Gómez", Level.AMIGO_ANIMAL, "AG"),
                status = PostStatus.APROBADA, lifecycle = LifecycleStatus.ACTIVA, votesCount = 3, commentsCount = 1,
                lat = 6.1759, lon = -75.5917,
            ),
            Publication(
                id = "3", category = Category.PERDIDOS, title = "Lucas se extravió cerca del parque", species = "Perro", breed = "Golden retriever",
                size = "Grande", vaccinated = true, sterilized = true, age = "4 años", sex = "Macho", city = "Medellín",
                neighborhood = "Laureles, Medellín", timeAgo = "hace 5 h",
                description = "Lucas se asustó con pólvora y corrió hacia el Parque Primer Parque de Laureles. Llevaba collar azul sin placa.",
                imageUrl = randomPhotoUrl("3"), tone = 0xFFE8D8D8, author = author("u_sofia", "Sofía Henao", Level.GUARDIAN, "SH"),
                status = PostStatus.APROBADA, lifecycle = LifecycleStatus.ACTIVA, votesCount = 12, commentsCount = 4,
                eventDate = "Hoy · 3:30 p. m.", place = "Parque Primer Parque de Laureles", phone = "+57 300 555 0199",
                lat = 6.2442, lon = -75.5928,
            ),
            Publication(
                id = "101", category = Category.ADOPCION, title = "Max, beagle juguetón", species = "Perro", breed = "Beagle",
                size = "Mediano", vaccinated = true, sterilized = true, age = "3 años", sex = "Macho", city = "Medellín",
                timeAgo = "hace 3 d", description = "Max es un beagle lleno de energía que adora olfatear todo. Busca una familia activa con patio.",
                imageUrl = randomPhotoUrl("101"), tone = 0xFFE5DED0, author = mariana,
                status = PostStatus.APROBADA, lifecycle = LifecycleStatus.ACTIVA, votesCount = 5, commentsCount = 2,
            ),
            Publication(
                id = "102", category = Category.TEMPORAL, title = "Pelusa, hogar de paso 2 semanas", species = "Gato", breed = "Criollo",
                size = "Pequeño", vaccinated = true, sterilized = true, age = "2 años", sex = "Hembra", city = "Medellín",
                timeAgo = "hace 4 h", description = "Pelusa necesita un hogar de paso por dos semanas mientras me mudo. Es tranquila y usa arenero.",
                imageUrl = randomPhotoUrl("102"), tone = 0xFFE4DCEC, author = mariana,
                status = PostStatus.EN_REVISION, lifecycle = LifecycleStatus.ACTIVA, votesCount = 0, commentsCount = 0,
            ),
            Publication(
                id = "103", category = Category.ADOPCION, title = "Kira, pastora mestiza", species = "Perro", breed = "Pastor mestizo",
                size = "Grande", vaccinated = false, sterilized = false, age = "1 año", sex = "Hembra", city = "Medellín",
                timeAgo = "hace 1 sem", description = "Kira es protectora y muy leal. Necesita espacio y paseos largos.",
                imageUrl = randomPhotoUrl("103"), tone = 0xFFDDE3DA, author = mariana,
                status = PostStatus.RECHAZADA, lifecycle = LifecycleStatus.ACTIVA, votesCount = 1, commentsCount = 0,
            ),
        )
    )

    private val _publications = MutableStateFlow<List<Publication>>(emptyList())
    override val publications: StateFlow<List<Publication>> = _publications.asStateFlow()

    private val _myPublications = MutableStateFlow<List<Publication>>(emptyList())
    override val myPublications: StateFlow<List<Publication>> = _myPublications.asStateFlow()

    // Comentarios por publicación
    private val commentsStorage = mutableMapOf<String, MutableStateFlow<List<Comment>>>()

    init {
        syncDerivedPublications()
        seedInitialComments()
    }

    private fun syncDerivedPublications() {
        val currentUserId = _currentUser.value?.id ?: "u_mariana"
        val all = allPublications.value
        _publications.value = all.filter { it.status == PostStatus.APROBADA }
        _myPublications.value = all.filter { it.author.userId == currentUserId }
        recalcStatsForUser(currentUserId)
    }

    private fun recalcStatsForUser(userId: String) {
        val userPosts = allPublications.value.filter { it.author.userId == userId }
        val active = userPosts.count { it.status == PostStatus.APROBADA && it.lifecycle == LifecycleStatus.ACTIVA }
        val finished = userPosts.count { it.lifecycle == LifecycleStatus.FINALIZADA }
        val pending = userPosts.count { it.status == PostStatus.EN_REVISION }
        val rejected = userPosts.count { it.status == PostStatus.RECHAZADA }
        val userVotes = userPosts.sumOf { it.votesCount }

        _currentUser.update { u ->
            if (u?.id == userId) {
                u.copy(
                    stats = u.stats.copy(
                        activeCount = active,
                        finishedCount = finished,
                        pendingCount = pending,
                        rejectedCount = rejected,
                        votesReceived = userVotes
                    )
                )
            } else u
        }
    }

    private fun seedInitialComments() {
        val list1 = listOf(
            Comment("c1", "1", author("u_andres", "Andrés Gómez", Level.AMIGO_ANIMAL, "AG"), "¿Convive bien con gatos?", "hace 1 h"),
            Comment("c2", "1", author("u_camila", "Camila Restrepo", Level.PROTECTOR, "CR"), "¡Sí! En su hogar temporal convive con 2 mininos sin problema.", "hace 45 min", isMine = false),
            Comment("c3", "1", mariana, "Hermosa Mía, compartida en mis redes ❤️", "hace 20 min", isMine = true)
        )
        commentsStorage["1"] = MutableStateFlow(list1)
    }

    // ---------- Requests ----------
    private val _sentRequests = MutableStateFlow(
        listOf(
            SentRequest("s1", "1", RequestStatus.PENDIENTE, "Enviada hace 2 h", "Hola Camila, me encantaría darle un hogar a Mía."),
            SentRequest("s2", "3", RequestStatus.ACEPTADA, "Enviada ayer", "Hola Sofía, creo que vi a Lucas por la 70."),
        )
    )
    override val sentRequests: StateFlow<List<SentRequest>> = _sentRequests.asStateFlow()

    private val _receivedRequests = MutableStateFlow(
        listOf(
            ReceivedRequestGroup(
                publicationId = "101",
                applicants = listOf(
                    Applicant("a1", "u_carlos", "Carlos Pérez", Level.PROTECTOR, "CP", "Tengo casa con jardín amplio y tiempo para paseos.", "hace 1 h"),
                    Applicant("a2", "u_laura", "Laura Martínez", Level.AMIGO_ANIMAL, "LM", "He tenido beagles antes, conozco bien la raza.", "hace 3 h"),
                ),
            ),
        ),
    )
    override val receivedRequests: StateFlow<List<ReceivedRequestGroup>> = _receivedRequests.asStateFlow()

    // ---------- Chat ----------
    private val conversations = listOf(
        Conversation("conv_1", author("u_carlos", "Carlos Pérez", Level.PROTECTOR, "CP"), "Sobre: Max, beagle juguetón", "+57 300 555 0144"),
        Conversation("conv_2", author("u_camila", "Camila Restrepo", Level.PROTECTOR, "CR"), "Sobre: Mía, cachorrita juguetona", "+57 300 555 0188"),
    )
    private val chatFlows = mutableMapOf<String, MutableStateFlow<List<ChatMessage>>>()

    // ---------- Notifications ----------
    private val _notifications = MutableStateFlow(
        listOf(
            AppNotification("n1", "Hoy", NotificationType.NEW_COMMENT, "Nuevo comentario", "Mariana comentó en tu publicación \"Mía, cachorrita juguetona\".", "hace 20 min", false, "1"),
            AppNotification("n2", "Hoy", NotificationType.NEW_REQUEST, "Nueva solicitud para Max", "Carlos Pérez envió una solicitud de adopción.", "hace 1 h", false, "101"),
            AppNotification("n3", "Hoy", NotificationType.POST_APPROVED, "Publicación aprobada", "Max ya está visible para toda la comunidad.", "hace 3 h", true, "101"),
            AppNotification("n4", "Ayer", NotificationType.LEVEL_UP, "¡Subiste de nivel!", "Alcanzaste el nivel Protector con 155 puntos.", "ayer", true, null),
        ),
    )
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    // ---------- Moderation ----------
    private val _moderationQueue = MutableStateFlow(
        listOf(
            ModerationItem(
                publication = allPublications.value.first { it.id == "102" },
                waitTime = "hace 4 h en espera",
                authorApproved = 2,
                authorRejected = 1,
            ),
        ),
    )
    override val moderationQueue: StateFlow<List<ModerationItem>> = _moderationQueue.asStateFlow()

    private val _moderationReports = MutableStateFlow<List<ModerationItem>>(emptyList())
    override val moderationReports: StateFlow<List<ModerationItem>> = _moderationReports.asStateFlow()

    private val _moderationStats = MutableStateFlow(ModerationStats(pending = 1, reported = 0, approvedToday = 3))
    override val moderationStats: StateFlow<ModerationStats> = _moderationStats.asStateFlow()

    override suspend fun refreshModerationData(): Result<Unit> = Result.success(Unit)

    // ---------- Points ----------
    override val pointsHistory: List<PointsEntry> = listOf(
        PointsEntry("Publicación aprobada · Max", 10, "hace 3 d"),
        PointsEntry("Hogar temporal completado", 50, "hace 2 sem"),
        PointsEntry("Registro en Huella", 20, "hace 1 mes"),
    )

    override val pointsActions: List<PointsAction> = listOf(
        PointsAction("Crear una publicación", 5),
        PointsAction("Publicación aprobada", 10),
        PointsAction("Adopción concretada", 50),
        PointsAction("Comentar en la comunidad", 2),
    )

    override val cities = listOf("Medellín, Antioquia", "Envigado, Antioquia", "Bogotá, Cundinamarca", "Cali, Valle del Cauca", "Barranquilla, Atlántico")
    override val species = listOf("Perro", "Gato", "Ave", "Conejo", "Roedor", "Otro")
    override val breedSuggestions = listOf("Criollo", "Mestizo", "Beagle", "Poodle", "Labrador")

    // ---------- Auth Implementations ----------
    override suspend fun login(email: String, password: String): Result<User> {
        delay(LATENCY)
        val user = users.value.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("Credenciales incorrectas"))
        _currentUser.value = user
        syncDerivedPublications()
        return Result.success(user)
    }

    override suspend fun register(name: String, email: String, city: String, password: String): Result<User> {
        delay(LATENCY)
        val newUser = User(
            id = "u_${System.currentTimeMillis()}",
            name = name.trim(),
            email = email.trim(),
            city = city.trim(),
            points = 20,
            initials = name.trim().take(2).uppercase(),
            role = Role.USUARIO, // Nunca moderador al registrarse
            stats = ProfileStats(),
            badges = listOf(Badge.PRIMER_POST)
        )
        users.update { it + newUser }
        _currentUser.value = newUser
        syncDerivedPublications()
        return Result.success(newUser)
    }

    override suspend fun handleAuthCallback(uri: android.net.Uri): Result<User> =
        _currentUser.value?.let { Result.success(it) } ?: login("mariana@correo.com", "12345678")

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        delay(LATENCY)
        return Result.success(Unit)
    }

    override suspend fun updateProfile(name: String, email: String, city: String, phone: String, bio: String): Result<User> {
        delay(LATENCY)
        val current = _currentUser.value ?: return Result.failure(IllegalStateException("No autenticado"))
        val updated = current.copy(name = name.trim(), email = email.trim(), city = city.trim(), phone = phone.trim(), bio = bio.trim())
        _currentUser.value = updated
        users.update { list -> list.map { if (it.id == updated.id) updated else it } }
        return Result.success(updated)
    }

    override suspend fun deleteAccount(): Result<Unit> {
        delay(LATENCY)
        val current = _currentUser.value ?: return Result.failure(IllegalStateException("No autenticado"))
        users.update { list -> list.filterNot { it.id == current.id } }
        allPublications.update { list -> list.filterNot { it.author.userId == current.id } }
        logout()
        syncDerivedPublications()
        return Result.success(Unit)
    }

    override fun logout() {
        _currentUser.value = null
    }

    override fun getUser(userId: String): User? = users.value.firstOrNull { it.id == userId }

    override suspend fun refreshUser(userId: String): Result<User> =
        getUser(userId)?.let { Result.success(it) } ?: Result.failure(IllegalStateException("No encontramos este perfil"))

    // ---------- Publications Implementations ----------
    override fun getPublication(id: String): Publication? =
        allPublications.value.firstOrNull { it.id == id }

    override fun publicationsByAuthor(userId: String): List<Publication> =
        allPublications.value.filter { it.author.userId == userId }

    override suspend fun createPublication(draft: PublicationDraft): Result<Publication> {
        delay(LATENCY)
        val user = _currentUser.value
        val id = "n${System.currentTimeMillis()}"
        val pub = Publication(
            id = id, category = draft.category, title = draft.title.trim(), species = draft.species, breed = draft.breed.trim(),
            size = draft.size, vaccinated = draft.vaccinated, sterilized = draft.sterilized, age = draft.age.trim(),
            sex = draft.sex, city = draft.city.substringBefore(","), neighborhood = draft.city, timeAgo = "ahora",
            description = draft.description.trim(), imageUrl = draft.imageUrl ?: randomPhotoUrl(id), tone = 0xFFD8E8DD,
            author = user?.let { Author(it.id, it.name, it.level, it.initials) } ?: mariana,
            status = PostStatus.EN_REVISION, lifecycle = LifecycleStatus.ACTIVA, eventDate = draft.eventDate,
        )
        allPublications.update { listOf(pub) + it }
        _moderationQueue.update { it + ModerationItem(pub, "Recién enviada", authorApproved = 1, authorRejected = 0) }
        _moderationStats.update { it.copy(pending = it.pending + 1) }
        syncDerivedPublications()
        return Result.success(pub)
    }

    override suspend fun updatePublication(publicationId: String, draft: PublicationDraft): Result<Publication> {
        delay(LATENCY)
        val current = getPublication(publicationId) ?: return Result.failure(IllegalArgumentException("Publicación no encontrada"))
        val updated = current.copy(
            category = draft.category,
            title = draft.title.trim(),
            description = draft.description.trim(),
            species = draft.species,
            breed = draft.breed.trim(),
            size = draft.size,
            vaccinated = draft.vaccinated,
            sterilized = draft.sterilized,
            age = draft.age.trim(),
            sex = draft.sex,
            city = draft.city.substringBefore(","),
            eventDate = draft.eventDate,
            imageUrl = draft.imageUrl ?: current.imageUrl,
            status = PostStatus.EN_REVISION // Al editar vuelve a revisión
        )
        allPublications.update { list -> list.map { if (it.id == publicationId) updated else it } }
        _moderationQueue.update { it + ModerationItem(updated, "Editada por autor", authorApproved = 1, authorRejected = 0) }
        syncDerivedPublications()
        return Result.success(updated)
    }

    override suspend fun deletePublication(publicationId: String): Result<Unit> {
        delay(LATENCY)
        allPublications.update { list -> list.filterNot { it.id == publicationId } }
        removeFromModeration(publicationId)
        syncDerivedPublications()
        return Result.success(Unit)
    }

    override suspend fun votePublication(publicationId: String, value: Int): Result<Unit> {
        delay(100)
        allPublications.update { list ->
            list.map { pub ->
                if (pub.id == publicationId) {
                    val prevVote = pub.userVote
                    val newVote = if (prevVote == value) 0 else value
                    val diff = newVote - prevVote
                    pub.copy(votesCount = (pub.votesCount + diff).coerceAtLeast(0), userVote = newVote)
                } else pub
            }
        }
        syncDerivedPublications()
        return Result.success(Unit)
    }

    // ---------- Comments Implementations ----------
    override fun comments(publicationId: String): StateFlow<List<Comment>> {
        val flow = commentsStorage.getOrPut(publicationId) { MutableStateFlow(emptyList()) }
        return flow.asStateFlow()
    }

    override suspend fun addComment(publicationId: String, text: String): Result<Comment> {
        delay(150)
        val user = _currentUser.value ?: return Result.failure(IllegalStateException("Inicia sesión para comentar"))
        val comment = Comment(
            id = "c_${System.currentTimeMillis()}",
            publicationId = publicationId,
            author = Author(user.id, user.name, user.level, user.initials),
            text = text.trim(),
            timeAgo = "ahora",
            isMine = true
        )
        val flow = commentsStorage.getOrPut(publicationId) { MutableStateFlow(emptyList()) }
        flow.update { it + comment }

        // Actualizar contador en la publicación
        allPublications.update { list ->
            list.map { if (it.id == publicationId) it.copy(commentsCount = it.commentsCount + 1) else it }
        }
        syncDerivedPublications()

        // Generar notificación al autor del post
        val pub = getPublication(publicationId)
        if (pub != null && pub.author.userId != user.id) {
            val notif = AppNotification(
                id = "n_${System.currentTimeMillis()}",
                group = "Hoy",
                type = NotificationType.NEW_COMMENT,
                title = "Nuevo comentario",
                text = "${user.name} comentó en tu publicación \"${pub.title}\"",
                time = "ahora",
                read = false,
                targetId = publicationId
            )
            _notifications.update { listOf(notif) + it }
        }

        return Result.success(comment)
    }

    override suspend fun deleteComment(publicationId: String, commentId: String): Result<Unit> {
        delay(150)
        val flow = commentsStorage[publicationId] ?: return Result.success(Unit)
        flow.update { list -> list.filterNot { it.id == commentId } }
        allPublications.update { list ->
            list.map { if (it.id == publicationId) it.copy(commentsCount = (it.commentsCount - 1).coerceAtLeast(0)) else it }
        }
        syncDerivedPublications()
        return Result.success(Unit)
    }

    override suspend fun refreshPublication(publicationId: String): Result<Unit> = Result.success(Unit)

    override suspend fun reportSighting(publicationId: String, text: String): Result<Unit> {
        delay(LATENCY)
        return if (text.isBlank()) Result.failure(IllegalArgumentException("Cuéntanos dónde y cuándo lo viste")) else Result.success(Unit)
    }

    override suspend fun reportPublication(publicationId: String, reason: String, detail: String): Result<Unit> {
        delay(LATENCY)
        return if (reason.isBlank()) Result.failure(IllegalArgumentException("Selecciona un motivo")) else Result.success(Unit)
    }

    override suspend fun sendAdoptionRequest(publicationId: String, message: String): Result<Unit> {
        delay(LATENCY)
        if (_sentRequests.value.any { it.publicationId == publicationId }) {
            return Result.failure(IllegalStateException("Ya enviaste una solicitud para esta publicación"))
        }
        _sentRequests.update { listOf(SentRequest("s${System.currentTimeMillis()}", publicationId, RequestStatus.PENDIENTE, "Enviada ahora", message)) + it }
        return Result.success(Unit)
    }

    override suspend fun respondToRequest(applicantId: String, accept: Boolean): Result<Unit> {
        delay(LATENCY)
        val status = if (accept) RequestStatus.ACEPTADA else RequestStatus.RECHAZADA
        _receivedRequests.update { groups ->
            groups.map { g -> g.copy(applicants = g.applicants.map { if (it.id == applicantId) it.copy(status = status) else it }) }
        }
        return Result.success(Unit)
    }

    override suspend fun closePublication(publicationId: String): Result<Unit> {
        delay(LATENCY)
        // Marcar como finalizada sin eliminarla de la historia
        allPublications.update { list ->
            list.map { if (it.id == publicationId) it.copy(lifecycle = LifecycleStatus.FINALIZADA) else it }
        }
        _currentUser.update { it?.copy(points = it.points + 50) }
        syncDerivedPublications()
        return Result.success(Unit)
    }

    override fun getConversation(id: String): Conversation? = conversations.firstOrNull { it.id == id }

    override suspend fun openConversation(contactUserId: String, publicationId: String?): Result<String> =
        Result.success(conversations.firstOrNull { it.contact.userId == contactUserId }?.id ?: conversations.first().id)

    override fun messages(conversationId: String): StateFlow<List<ChatMessage>> =
        chatFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }.asStateFlow()

    override suspend fun sendMessage(conversationId: String, text: String): Result<Unit> {
        if (text.isBlank()) return Result.failure(IllegalArgumentException("Escribe un mensaje"))
        delay(200)
        val flow = chatFlows.getOrPut(conversationId) { MutableStateFlow(emptyList()) }
        flow.update { it + ChatMessage("m${System.currentTimeMillis()}", conversationId, true, text.trim(), "ahora") }
        return Result.success(Unit)
    }

    override fun markAllNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(read = true) } }
    }

    override fun markNotificationRead(id: String) {
        _notifications.update { list -> list.map { if (it.id == id) it.copy(read = true) else it } }
    }

    override fun deleteNotification(id: String) {
        _notifications.update { list -> list.filterNot { it.id == id } }
    }

    override fun getModerationItem(id: String): ModerationItem? =
        (_moderationQueue.value + _moderationReports.value).firstOrNull { it.id == id }

    /**
     * ¡REQUISITO CORREGIDO!
     * Al aprobar una publicación pendiente en FakeRepository, su status cambia a APROBADA
     * y se actualiza allPublications, provocando que pase a _publications (Feed público).
     */
    override suspend fun approve(itemId: String): Result<Unit> {
        delay(LATENCY)
        removeFromModeration(itemId)
        allPublications.update { list ->
            list.map { if (it.id == itemId) it.copy(status = PostStatus.APROBADA) else it }
        }
        _moderationStats.update { it.copy(approvedToday = it.approvedToday + 1) }
        syncDerivedPublications()

        // Notificar al autor
        val pub = getPublication(itemId)
        if (pub != null) {
            _notifications.update {
                listOf(
                    AppNotification("n_${System.currentTimeMillis()}", "Hoy", NotificationType.POST_APPROVED, "Publicación aprobada", "${pub.title} ya está visible para todos.", "ahora", false, itemId)
                ) + it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun reject(itemId: String, reason: String, comment: String): Result<Unit> {
        delay(LATENCY)
        if (reason.isBlank()) return Result.failure(IllegalArgumentException("Selecciona un motivo de rechazo"))
        removeFromModeration(itemId)
        allPublications.update { list ->
            list.map { if (it.id == itemId) it.copy(status = PostStatus.RECHAZADA) else it }
        }
        syncDerivedPublications()

        val pub = getPublication(itemId)
        if (pub != null) {
            _notifications.update {
                listOf(
                    AppNotification("n_${System.currentTimeMillis()}", "Hoy", NotificationType.POST_REJECTED, "Publicación rechazada", "${pub.title} fue rechazada. Motivo: $reason", "ahora", false, itemId)
                ) + it
            }
        }
        return Result.success(Unit)
    }

    override suspend fun resolveReport(reportId: String): Result<Unit> {
        delay(LATENCY)
        _moderationReports.update { list ->
            list.map { item -> item.copy(reports = item.reports.filterNot { it.id == reportId }) }.filter { it.isReported }
        }
        _moderationStats.update { it.copy(reported = _moderationReports.value.size) }
        return Result.success(Unit)
    }

    private fun removeFromModeration(itemId: String) {
        _moderationQueue.update { list -> list.filterNot { it.id == itemId } }
        _moderationReports.update { list -> list.filterNot { it.id == itemId } }
        _moderationStats.update {
            it.copy(pending = _moderationQueue.value.size, reported = _moderationReports.value.size)
        }
    }
}
