package com.desarrolloMovielexample.huella.domain.repository

import com.desarrolloMovielexample.huella.domain.model.Applicant
import com.desarrolloMovielexample.huella.domain.model.AppNotification
import com.desarrolloMovielexample.huella.domain.model.Author
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.ChatMessage
import com.desarrolloMovielexample.huella.domain.model.Conversation
import com.desarrolloMovielexample.huella.domain.model.Level
import com.desarrolloMovielexample.huella.domain.model.ModerationItem
import com.desarrolloMovielexample.huella.domain.model.ModerationStats
import com.desarrolloMovielexample.huella.domain.model.NotificationType
import com.desarrolloMovielexample.huella.domain.model.PointsAction
import com.desarrolloMovielexample.huella.domain.model.PointsEntry
import com.desarrolloMovielexample.huella.domain.model.PostStatus
import com.desarrolloMovielexample.huella.domain.model.Publication
import com.desarrolloMovielexample.huella.domain.model.PublicationDraft
import com.desarrolloMovielexample.huella.domain.model.ReceivedRequestGroup
import com.desarrolloMovielexample.huella.domain.model.Report
import com.desarrolloMovielexample.huella.domain.model.RequestStatus
import com.desarrolloMovielexample.huella.domain.model.SentRequest
import com.desarrolloMovielexample.huella.domain.model.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Random photo for a publication (Coil loads it; striped placeholder shows meanwhile). */
fun randomPhotoUrl(seed: String): String = "https://picsum.photos/seed/huella$seed/600/400"


/**
 * In-memory repository seeded with every sample shown in the design.
 * ponytail: process-lifetime state only; swap for a real backend impl of [HuellaRepository] later.
 * Demo login: mariana@correo.com / 12345678
 */
object FakeRepository : HuellaRepository {

    private const val LATENCY = 700L

    // ---------- Users ----------
    private val users = MutableStateFlow(
        listOf(
            User("u_mariana", "Mariana López", "mariana@correo.com", "Medellín, Antioquia", "+57 300 555 0123",
                "Amante de los animales. He sido hogar temporal de 2 gatos y busco ayudar a más.", 155, "ML",
                publicationsCount = 3, adoptionsCount = 1, fosterCount = 2, activeCount = 2, isModerator = true),
            User("u_camila", "Camila Restrepo", "camila@correo.com", "Medellín, Antioquia", points = 180, initials = "CR",
                memberSince = "2024", publicationsCount = 8, adoptionsCount = 6, activeCount = 2),
            User("u_andres", "Andrés Gómez", "andres@correo.com", "Bogotá, Cundinamarca", points = 20, initials = "AG", activeCount = 1),
            User("u_valentina", "Valentina Ruiz", "valentina@correo.com", "Cali, Valle del Cauca", points = 260, initials = "VR", activeCount = 1),
            User("u_juanpablo", "Juan Pablo Mora", "juanpablo@correo.com", "Barranquilla, Atlántico", points = 620, initials = "JM", activeCount = 1),
            User("u_patitas", "Fundación Patitas", "patitas@correo.com", "Bucaramanga, Santander", points = 900, initials = "FP", activeCount = 1),
            User("u_refugio", "Refugio Segunda Oportunidad", "refugio@correo.com", "Pereira, Risaralda", points = 310, initials = "RS", activeCount = 1),
            User("u_sofia", "Sofía Arango", "sofia@correo.com", "Manizales, Caldas", points = 30, initials = "SA", activeCount = 1),
            User("u_mateo", "Mateo Zapata", "mateo@correo.com", "Medellín, Antioquia", points = 90, initials = "MZ", activeCount = 1),
            User("u_santiago", "Santiago Pérez", "santiago@correo.com", "Envigado, Antioquia", points = 15, initials = "SP"),
            User("u_laura", "Laura Cárdenas", "laura@correo.com", "Medellín, Antioquia", points = 240, initials = "LC"),
            User("u_daniel", "Daniel Torres", "daniel@correo.com", "Medellín, Antioquia", points = 120, initials = "DT"),
            User("u_paula", "Paula Henao", "paula@correo.com", "Envigado, Antioquia", points = 110, initials = "PH"),
            User("u_diego", "Diego Salazar", "diego@correo.com", "Cali, Valle del Cauca", points = 10, initials = "DS"),
            User("u_vetcali", "Vet Comunitaria Cali", "vet@correo.com", "Cali, Valle del Cauca", points = 300, initials = "VC"),
            User("u_carlos", "Carlos Mejía", "carlos@correo.com", "Bogotá, Cundinamarca", points = 25, initials = "CM"),
            User("u_nicolas", "Nicolás Vargas", "nicolas@correo.com", "Bogotá, Cundinamarca", points = 5, initials = "NV"),
        )
    )
    private val passwords = mutableMapOf("mariana@correo.com" to "12345678")

    // ponytail: pre-seeded with Mariana so screens work standalone; login() still validates and replaces it.
    private val _currentUser = MutableStateFlow<User?>(users.value.first())
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private fun author(userId: String, name: String, level: Level, initials: String) = Author(userId, name, level, initials)

    private val camila = author("u_camila", "Camila Restrepo", Level.PROTECTOR, "CR")
    private val mariana = author("u_mariana", "Mariana López", Level.PROTECTOR, "ML")

    // ---------- Feed ----------
    private val _publications = MutableStateFlow(
        listOf(
            Publication(
                id = "1", category = Category.ADOPCION, title = "Luna busca un hogar tranquilo",
                species = "Perro", breed = "Criollo", size = "Mediano", vaccinated = true, sterilized = true,
                age = "2 años", sex = "Hembra", city = "Medellín", neighborhood = "Laureles, Medellín", timeAgo = "hace 2 h",
                description = "Luna llegó a la fundación hace tres meses tras ser rescatada en Belén. Es tranquila, se lleva bien con otros perros y le encanta dormir al sol. Ya está esterilizada y con todas sus vacunas al día. Buscamos una familia con paciencia y un espacio donde pueda pasear a diario.",
                imageUrl = randomPhotoUrl("1"), tone = 0xFFD8E8DD, author = camila, lat = 6.2442, lon = -75.5812,
            ),
            Publication(
                id = "2", category = Category.PERDIDOS, title = "Toby se perdió en Chapinero",
                species = "Perro", breed = "Labrador mestizo", size = "Grande", vaccinated = true,
                age = "4 años", sex = "Macho", city = "Bogotá", neighborhood = "Parque El Virrey, Chapinero", timeAgo = "hace 5 h",
                description = "Toby se soltó del collar mientras paseábamos. Es color miel, tiene una mancha blanca en el pecho y lleva collar azul. Es muy dócil y responde a su nombre. Cualquier pista nos ayuda.",
                imageUrl = randomPhotoUrl("2"), tone = 0xFFE8DAD6,
                author = author("u_andres", "Andrés Gómez", Level.AMIGO_ANIMAL, "AG"),
                eventDate = "Sáb 5 sep · 6:30 p. m.", place = "Parque El Virrey, Chapinero", phone = "+57 310 555 0142",
                lat = 4.6733, lon = -74.0533,
            ),
            Publication(
                id = "3", category = Category.ENCONTRADOS, title = "Gatita encontrada en San Fernando",
                species = "Gato", breed = "Criollo", size = "Pequeño", vaccinated = false,
                age = "6 meses aprox.", sex = "Hembra", city = "Cali", neighborhood = "San Fernando, Cali", timeAgo = "hace 1 d",
                description = "La encontramos maullando bajo un carro. Tiene manchas negras y blancas, está sana pero asustada. La tenemos en casa mientras aparece su familia.",
                imageUrl = randomPhotoUrl("3"), tone = 0xFFD8E0EC,
                author = author("u_valentina", "Valentina Ruiz", Level.GUARDIAN, "VR"),
                eventDate = "Vie 4 sep · 9:00 p. m.", place = "Calle 5 con Carrera 36, San Fernando", phone = "+57 315 555 0198",
                lat = 3.4372, lon = -76.5443,
            ),
            Publication(
                id = "4", category = Category.TEMPORAL, title = "Nala necesita hogar de paso por 1 mes",
                species = "Gato", breed = "Siamés", size = "Pequeño", vaccinated = true,
                age = "1 año", sex = "Hembra", city = "Barranquilla", neighborhood = "El Prado, Barranquilla", timeAgo = "hace 3 h",
                description = "Su cuidadora viaja por trabajo durante septiembre. Nala es independiente, usa arenero y come concentrado. Se entrega con todo lo necesario.",
                imageUrl = randomPhotoUrl("4"), tone = 0xFFE4DCEC,
                author = author("u_juanpablo", "Juan Pablo Mora", Level.HEROE, "JM"),
                lat = 11.0041, lon = -74.8070,
            ),
            Publication(
                id = "5", category = Category.VETERINARIA, title = "Jornada de vacunación gratuita",
                species = "Perros y gatos", breed = "Todas las razas", size = "Todos", vaccinated = true,
                city = "Bucaramanga", neighborhood = "Parque San Pío, Bucaramanga", timeAgo = "hace 6 h",
                description = "Trae a tu mascota con correa o guacal. La esterilización requiere inscripción previa y ayuno de 8 horas.",
                imageUrl = randomPhotoUrl("5"), tone = 0xFFD6ECE8, photoLabel = "Jornada de vacunación gratuita",
                author = author("u_patitas", "Fundación Patitas", Level.HEROE, "FP"),
                eventDate = "Dom 13 sep", schedule = "8:00 a. m. – 1:00 p. m.", place = "Parque San Pío, Cabecera",
                services = listOf("Vacunación", "Esterilización", "Desparasitación"), capacity = 40,
                lat = 7.1193, lon = -73.1227,
            ),
            Publication(
                id = "6", category = Category.ADOPCION, title = "Rocky, energía pura para una familia activa",
                species = "Perro", breed = "Pitbull mestizo", size = "Grande", vaccinated = true, sterilized = false,
                age = "3 años", sex = "Macho", city = "Pereira", neighborhood = "Álamos, Pereira", timeAgo = "hace 1 d",
                description = "Rocky adora correr y jugar con pelota. Necesita una familia que lo saque a diario. Es cariñoso con las personas y está esterilizado.",
                imageUrl = randomPhotoUrl("6"), tone = 0xFFE7E1D3,
                author = author("u_refugio", "Refugio Segunda Oportunidad", Level.GUARDIAN, "RS"),
                lat = 4.8133, lon = -75.6961,
            ),
            Publication(
                id = "7", category = Category.ADOPCION, title = "Coco, conejo cabeza de león",
                species = "Conejo", breed = "Cabeza de león", size = "Pequeño", vaccinated = false, sterilized = false,
                age = "8 meses", sex = "Macho", city = "Manizales", neighborhood = "Palermo, Manizales", timeAgo = "hace 2 d",
                description = "Coco es curioso y tranquilo. Come heno, verduras y necesita espacio para saltar. Ideal para un hogar sin perros grandes.",
                imageUrl = randomPhotoUrl("7"), tone = 0xFFEEE3D2,
                author = author("u_sofia", "Sofía Arango", Level.AMIGO_ANIMAL, "SA"),
                lat = 5.0703, lon = -75.5138,
            ),
            Publication(
                id = "8", category = Category.PERDIDOS, title = "Simón, gato naranja perdido en Laureles",
                species = "Gato", breed = "Criollo", size = "Mediano", vaccinated = true,
                age = "5 años", sex = "Macho", city = "Medellín", neighborhood = "Laureles, Medellín", timeAgo = "hace 30 min",
                description = "Simón salió por la ventana esta mañana. Es naranja, tiene collar rojo con placa. Suele esconderse bajo carros.",
                imageUrl = randomPhotoUrl("8"), tone = 0xFFEFE0CF,
                author = author("u_mateo", "Mateo Zapata", Level.PROTECTOR, "MZ"),
                eventDate = "Hoy · 7:15 a. m.", place = "Circular 74 con Calle 39, Laureles", phone = "+57 300 555 0177",
                lat = 6.2450, lon = -75.5900,
            ),
        )
    )
    override val publications: StateFlow<List<Publication>> = _publications.asStateFlow()

    private val _myPublications = MutableStateFlow(
        listOf(
            Publication(
                id = "101", category = Category.ADOPCION, title = "Max, beagle juguetón", species = "Perro", breed = "Beagle",
                size = "Mediano", vaccinated = true, sterilized = true, age = "3 años", sex = "Macho", city = "Medellín",
                timeAgo = "hace 3 d", description = "Max es un beagle lleno de energía que adora olfatear todo. Busca una familia activa con patio.",
                imageUrl = randomPhotoUrl("101"), tone = 0xFFE5DED0, author = mariana, status = PostStatus.APROBADA,
            ),
            Publication(
                id = "102", category = Category.TEMPORAL, title = "Pelusa, hogar de paso 2 semanas", species = "Gato", breed = "Criollo",
                size = "Pequeño", vaccinated = true, sterilized = true, age = "2 años", sex = "Hembra", city = "Medellín",
                timeAgo = "hace 4 h", description = "Pelusa necesita un hogar de paso por dos semanas mientras me mudo. Es tranquila y usa arenero.",
                imageUrl = randomPhotoUrl("102"), tone = 0xFFE4DCEC, author = mariana, status = PostStatus.EN_REVISION,
            ),
            Publication(
                id = "103", category = Category.ADOPCION, title = "Kira, pastora mestiza", species = "Perro", breed = "Pastor mestizo",
                size = "Grande", vaccinated = false, sterilized = false, age = "1 año", sex = "Hembra", city = "Medellín",
                timeAgo = "hace 1 sem", description = "Kira es protectora y muy leal. Necesita espacio y paseos largos.",
                imageUrl = randomPhotoUrl("103"), tone = 0xFFDDE3DA, author = mariana, status = PostStatus.RECHAZADA,
            ),
        )
    )
    override val myPublications: StateFlow<List<Publication>> = _myPublications.asStateFlow()

    // ---------- Moderation ----------
    private val _moderationQueue = MutableStateFlow(
        listOf(
            ModerationItem(
                Publication(
                    id = "p1", category = Category.ADOPCION, title = "Bruno busca familia en Envigado", species = "Perro",
                    breed = "Golden mestizo", size = "Grande", vaccinated = true, sterilized = true, age = "5 años", sex = "Macho",
                    city = "Envigado", timeAgo = "hace 3 h",
                    description = "Bruno tiene 5 años, es tranquilo y convive con niños. Vacunas al día y esterilizado.",
                    imageUrl = randomPhotoUrl("p1"), tone = 0xFFEEE3D2,
                    author = author("u_paula", "Paula Henao", Level.PROTECTOR, "PH"), status = PostStatus.EN_REVISION,
                    lat = 6.1759, lon = -75.5911,
                ),
                waitTime = "3 h en espera", authorApproved = 4, authorRejected = 0,
            ),
            ModerationItem(
                Publication(
                    id = "p2", category = Category.ADOPCION, title = "Camada de 4 gatitos criollos", species = "Gato",
                    breed = "Criollo", size = "Pequeño", vaccinated = false, age = "2 meses", sex = "Desconocido",
                    city = "Cali", timeAgo = "hace 5 h",
                    description = "Cuatro gatitos de dos meses, destetados. Se entregan con primera dosis de desparasitación.",
                    imageUrl = randomPhotoUrl("p2"), tone = 0xFFD8E0EC,
                    author = author("u_diego", "Diego Salazar", Level.AMIGO_ANIMAL, "DS"), status = PostStatus.EN_REVISION,
                    lat = 3.4516, lon = -76.5320,
                ),
                waitTime = "5 h en espera", authorApproved = 1, authorRejected = 0,
            ),
            ModerationItem(
                Publication(
                    id = "p3", category = Category.VETERINARIA, title = "Jornada de esterilización Siloé", species = "Perros y gatos",
                    breed = "Todas", size = "Todos", vaccinated = true, city = "Cali", neighborhood = "Siloé, Cali", timeAgo = "hace 1 d",
                    description = "Jornada gratuita con inscripción previa.",
                    imageUrl = randomPhotoUrl("p3"), tone = 0xFFD6ECE8, photoLabel = "jornada",
                    author = author("u_vetcali", "Vet Comunitaria Cali", Level.GUARDIAN, "VC"), status = PostStatus.EN_REVISION,
                    lat = 3.4200, lon = -76.5500,
                ),
                waitTime = "1 d en espera", authorApproved = 9, authorRejected = 1,
            ),
        )
    )
    override val moderationQueue: StateFlow<List<ModerationItem>> = _moderationQueue.asStateFlow()

    private val _moderationReports = MutableStateFlow(
        listOf(
            ModerationItem(
                Publication(
                    id = "r1", category = Category.ADOPCION, title = "Cachorros pastor alemán, se entregan con aporte", species = "Perro",
                    breed = "Pastor alemán", size = "Grande", vaccinated = false, age = "45 días", sex = "Desconocido",
                    city = "Bogotá", neighborhood = "Suba, Bogotá", timeAgo = "hace 8 h",
                    description = "Cachorros de 45 días, se entregan con un aporte voluntario de \$350.000 para cubrir gastos. Escribir al interno.",
                    imageUrl = randomPhotoUrl("r1"), tone = 0xFFE8DAD6,
                    author = author("u_carlos", "Carlos Mejía", Level.AMIGO_ANIMAL, "CM"), status = PostStatus.EN_REVISION,
                    lat = 4.6533, lon = -74.0836,
                ),
                waitTime = "8 h en espera",
                reports = listOf(
                    Report("Laura C.", "Venta de animales", "Pide \$350.000 por cachorro"),
                    Report("Andrés G.", "Venta de animales", "Aporte obligatorio, es venta encubierta"),
                    Report("Valentina R.", "Información falsa", "Las fotos son de internet"),
                    Report("Anónimo", "Venta de animales", ""),
                ),
                authorApproved = 2, authorRejected = 1,
            ),
            ModerationItem(
                Publication(
                    id = "r2", category = Category.PERDIDOS, title = "Perro perdido en Suba", species = "Perro",
                    breed = "Criollo", size = "Mediano", vaccinated = true, age = "3 años", sex = "Macho",
                    city = "Bogotá", neighborhood = "Suba, Bogotá", timeAgo = "hace 2 d",
                    description = "Se perdió el jueves cerca al portal de Suba.",
                    imageUrl = randomPhotoUrl("r2"), tone = 0xFFDDE3DA,
                    author = author("u_nicolas", "Nicolás Vargas", Level.AMIGO_ANIMAL, "NV"), status = PostStatus.EN_REVISION,
                    lat = 4.7450, lon = -74.0930,
                ),
                waitTime = "2 d en espera",
                reports = listOf(Report("Anónimo", "Spam", "Publicado 3 veces")),
                authorApproved = 0, authorRejected = 0,
            ),
        )
    )
    override val moderationReports: StateFlow<List<ModerationItem>> = _moderationReports.asStateFlow()

    private val _moderationStats = MutableStateFlow(ModerationStats(pending = 3, reported = 2, approvedToday = 12))
    override val moderationStats: StateFlow<ModerationStats> = _moderationStats.asStateFlow()

    // ---------- Requests ----------
    private val _sentRequests = MutableStateFlow(
        listOf(
            SentRequest("s1", "6", RequestStatus.PENDIENTE, "Enviada hace 2 h"),
            SentRequest("s2", "7", RequestStatus.ACEPTADA, "Aceptada ayer"),
            SentRequest("s3", "4", RequestStatus.RECHAZADA, "Rechazada hace 3 d"),
        )
    )
    override val sentRequests: StateFlow<List<SentRequest>> = _sentRequests.asStateFlow()

    private val _receivedRequests = MutableStateFlow(
        listOf(
            ReceivedRequestGroup(
                "101",
                listOf(
                    Applicant("a1", "u_santiago", "Santiago Pérez", Level.AMIGO_ANIMAL, "SP",
                        "Vivo en una casa con patio en Envigado y trabajo desde casa. Tuve un beagle durante 12 años y sé cuánta energía necesitan.", "hace 1 h"),
                    Applicant("a2", "u_laura", "Laura Cárdenas", Level.GUARDIAN, "LC",
                        "Somos una familia con dos niños y ya tenemos una perrita criolla que necesita compañía. Podemos visitarlos este fin de semana.", "hace 5 h"),
                ),
            ),
            ReceivedRequestGroup(
                "102",
                listOf(
                    Applicant("a3", "u_daniel", "Daniel Torres", Level.PROTECTOR, "DT",
                        "Puedo recibir a Pelusa las dos semanas, tengo experiencia con gatos y un apartamento con malla.", "ayer"),
                ),
            ),
        )
    )
    override val receivedRequests: StateFlow<List<ReceivedRequestGroup>> = _receivedRequests.asStateFlow()

    // ---------- Chat ----------
    private val conversations = listOf(
        Conversation("c1", camila, "Sobre: Max, beagle juguetón", phone = "+57 300 555 0101"),
    )
    private val chatFlows = mutableMapOf(
        "c1" to MutableStateFlow(
            listOf(
                ChatMessage("m1", "c1", false, "Hola Mariana, vi tu solicitud para Max. ¿Tienes patio o espacio para que corra?", "10:12"),
                ChatMessage("m2", "c1", true, "¡Hola Camila! Sí, vivimos en una casa con patio en Envigado y salgo a caminar todos los días.", "10:15"),
                ChatMessage("m3", "c1", false, "Perfecto. ¿Podríamos hacer una visita este sábado en la mañana?", "10:16"),
                ChatMessage("m4", "c1", true, "Claro, el sábado a las 10 me funciona.", "10:18"),
            )
        )
    )

    // ---------- Notifications ----------
    private val _notifications = MutableStateFlow(
        listOf(
            AppNotification("n1", "Hoy", NotificationType.NEW_REQUEST, "Nueva solicitud para Max", "Santiago Pérez quiere adoptar a Max.", "hace 1 h", targetId = "101"),
            AppNotification("n2", "Hoy", NotificationType.REQUEST_ACCEPTED, "Solicitud aceptada", "Sofía Arango aceptó tu solicitud por Coco. Ya puedes contactarla.", "hace 4 h", targetId = "c1"),
            AppNotification("n3", "Ayer", NotificationType.POST_APPROVED, "Publicación aprobada", "Max, beagle juguetón ya está visible para todos.", "9:20 a. m.", targetId = "101"),
            AppNotification("n4", "Ayer", NotificationType.POST_REJECTED, "Publicación rechazada", "Kira, pastora mestiza fue rechazada. Motivo: fotos inadecuadas. Puedes editarla y volver a enviarla.", "8:05 a. m.", targetId = "103"),
            AppNotification("n5", "Esta semana", NotificationType.LEVEL_UP, "¡Subiste de nivel!", "Ahora eres Protector. Tus publicaciones muestran la insignia de bronce.", "Lun"),
        )
    )
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    // ---------- Gamification ----------
    override val pointsHistory = listOf(
        PointsEntry("Publicación aprobada · Max", 10, "hace 3 d"),
        PointsEntry("Hogar temporal completado · Nube", 30, "hace 2 sem"),
        PointsEntry("Publicación creada · Pelusa", 5, "hace 4 h"),
        PointsEntry("Reporte de avistamiento · Simón", 5, "hoy"),
        PointsEntry("Adopción concretada · Bruno", 50, "hace 1 mes"),
    )
    override val pointsActions = listOf(
        PointsAction("Crear una publicación", 5),
        PointsAction("Publicación aprobada", 10),
        PointsAction("Reportar avistamiento de mascota perdida", 5),
        PointsAction("Hogar temporal completado", 30),
        PointsAction("Reunir una mascota con su familia", 40),
        PointsAction("Adopción concretada", 50),
    )

    // ---------- Catalogs ----------
    override val cities = listOf(
        "Medellín, Antioquia", "Envigado, Antioquia", "Bogotá, Cundinamarca", "Cali, Valle del Cauca",
        "Barranquilla, Atlántico", "Cartagena, Bolívar", "Bucaramanga, Santander", "Pereira, Risaralda", "Manizales, Caldas",
    )
    override val species = listOf("Perro", "Gato", "Ave", "Conejo", "Roedor", "Otro")
    override val breedSuggestions = listOf("Criollo", "Labrador mestizo", "Golden mestizo")

    // ================= Implementation =================

    override suspend fun login(email: String, password: String): Result<User> {
        delay(LATENCY)
        val key = email.trim().lowercase()
        val user = users.value.firstOrNull { it.email.lowercase() == key }
        return if (user != null && passwords[key] == password) {
            _currentUser.value = user
            Result.success(user)
        } else {
            Result.failure(IllegalArgumentException("Correo o contraseña incorrectos"))
        }
    }

    override suspend fun register(name: String, email: String, city: String, password: String): Result<User> {
        delay(LATENCY)
        val key = email.trim().lowercase()
        if (users.value.any { it.email.lowercase() == key }) {
            return Result.failure(IllegalStateException("Ya existe una cuenta con este correo"))
        }
        val initials = name.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
        val user = User("u_${System.currentTimeMillis()}", name.trim(), key, city, points = 0, initials = initials)
        users.update { it + user }
        passwords[key] = password
        _currentUser.value = user
        return Result.success(user)
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        delay(LATENCY)
        val key = email.trim().lowercase()
        return if (users.value.any { it.email.lowercase() == key }) Result.success(Unit)
        else Result.failure(IllegalArgumentException("No encontramos una cuenta con ese correo"))
    }

    override suspend fun updateProfile(name: String, email: String, city: String, phone: String, bio: String): Result<User> {
        delay(LATENCY)
        val current = _currentUser.value ?: users.value.first()
        val updated = current.copy(name = name.trim(), email = email.trim(), city = city, phone = phone.trim(), bio = bio.trim())
        users.update { list -> list.map { if (it.id == updated.id) updated else it } }
        _currentUser.value = updated
        return Result.success(updated)
    }

    override fun logout() {
        _currentUser.value = null
    }

    override fun getUser(userId: String): User? = users.value.firstOrNull { it.id == userId }

    override fun getPublication(id: String): Publication? =
        (_publications.value + _myPublications.value + _moderationQueue.value.map { it.publication } +
            _moderationReports.value.map { it.publication }).firstOrNull { it.id == id }

    override fun publicationsByAuthor(userId: String): List<Publication> =
        (_publications.value + _myPublications.value).filter { it.author.userId == userId }.let { list ->
            // Design: Camila's public profile shows Luna + Rocky as "Publicaciones activas".
            if (userId == "u_camila") list + listOfNotNull(_publications.value.firstOrNull { it.id == "6" }) else list
        }

    override suspend fun createPublication(draft: PublicationDraft): Result<Publication> {
        delay(LATENCY)
        val user = _currentUser.value
        val id = "n${System.currentTimeMillis()}"
        val pub = Publication(
            id = id, category = draft.category, title = draft.title.trim(), species = draft.species, breed = draft.breed.trim(),
            size = draft.size, vaccinated = draft.vaccinated, sterilized = draft.sterilized, age = draft.age.trim(),
            sex = draft.sex, city = draft.city.substringBefore(","), neighborhood = draft.city, timeAgo = "ahora",
            description = draft.description.trim(), imageUrl = randomPhotoUrl(id), tone = 0xFFD8E8DD,
            author = user?.let { Author(it.id, it.name, it.level, it.initials) } ?: mariana,
            status = PostStatus.EN_REVISION, eventDate = draft.eventDate,
        )
        _myPublications.update { listOf(pub) + it }
        _moderationQueue.update { it + ModerationItem(pub, "Recién enviada", authorApproved = 1, authorRejected = 0) }
        _moderationStats.update { it.copy(pending = it.pending + 1) }
        return Result.success(pub)
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
        _myPublications.update { list -> list.filterNot { it.id == publicationId } }
        _receivedRequests.update { list -> list.filterNot { it.publicationId == publicationId } }
        _currentUser.update { it?.copy(points = it.points + 50, adoptionsCount = it.adoptionsCount + 1) }
        return Result.success(Unit)
    }

    override fun getConversation(id: String): Conversation? = conversations.firstOrNull { it.id == id }

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

    override fun getModerationItem(id: String): ModerationItem? =
        (_moderationQueue.value + _moderationReports.value).firstOrNull { it.id == id }

    override suspend fun approve(itemId: String): Result<Unit> {
        delay(LATENCY)
        removeFromModeration(itemId)
        _myPublications.update { list -> list.map { if (it.id == itemId) it.copy(status = PostStatus.APROBADA) else it } }
        _moderationStats.update { it.copy(approvedToday = it.approvedToday + 1) }
        return Result.success(Unit)
    }

    override suspend fun reject(itemId: String, reason: String, comment: String): Result<Unit> {
        delay(LATENCY)
        if (reason.isBlank()) return Result.failure(IllegalArgumentException("Selecciona un motivo de rechazo"))
        removeFromModeration(itemId)
        _myPublications.update { list -> list.map { if (it.id == itemId) it.copy(status = PostStatus.RECHAZADA) else it } }
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
