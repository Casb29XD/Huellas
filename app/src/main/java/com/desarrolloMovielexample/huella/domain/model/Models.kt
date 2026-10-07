package com.desarrolloMovielexample.huella.domain.model

enum class Category(val label: String) {
    ADOPCION("Adopción"),
    PERDIDOS("Perdidos"),
    ENCONTRADOS("Encontrados"),
    TEMPORAL("Temporal"),
    VETERINARIA("Veterinaria");

    val isAdoption get() = this == ADOPCION || this == TEMPORAL
    val isLostFound get() = this == PERDIDOS || this == ENCONTRADOS
}

enum class Level(
    val label: String,
    val minPoints: Int,
    val maxPoints: Int,
    val range: String,
    val benefits: List<String>,
) {
    AMIGO_ANIMAL("Amigo Animal", 0, 49, "0 – 49 puntos", listOf("Publicar y solicitar adopciones", "Guardar favoritos")),
    PROTECTOR("Protector", 50, 199, "50 – 199 puntos", listOf("Insignia visible en tus publicaciones", "Hasta 5 publicaciones activas")),
    GUARDIAN("Guardián", 200, 499, "200 – 499 puntos", listOf("Publicaciones destacadas 24 h", "Prioridad en solicitudes")),
    HEROE("Héroe de las Mascotas", 500, 99999, "500+ puntos", listOf("Publicaciones sin límite", "Acceso a jornadas exclusivas", "Reconocimiento en la comunidad"));

    val next: Level? get() = Level.entries.getOrNull(ordinal + 1)

    companion object {
        fun forPoints(points: Int): Level = Level.entries.last { points >= it.minPoints }
    }
}

/** Moderation status of a publication. */
enum class PostStatus(val label: String) { EN_REVISION("En revisión"), APROBADA("Aprobada"), RECHAZADA("Rechazada") }

enum class RequestStatus(val label: String) { PENDIENTE("Pendiente"), ACEPTADA("Aceptada"), RECHAZADA("Rechazada") }

/** Small author snapshot shown on cards / detail. [userId] links to a public profile. */
data class Author(
    val userId: String,
    val name: String,
    val level: Level,
    val initials: String,
)

data class Publication(
    val id: String,
    val category: Category,
    val title: String,
    val species: String,          // Perro, Gato, Ave, Conejo, Roedor, Otro, "Perros y gatos"
    val breed: String,            // raza aproximada
    val size: String,             // Pequeño, Mediano, Grande, Todos
    val vaccinated: Boolean,
    val sterilized: Boolean? = null,
    val age: String? = null,
    val sex: String? = null,      // Macho, Hembra, Desconocido
    val city: String,
    val neighborhood: String = city,
    val timeAgo: String,
    val description: String,
    val imageUrl: String?,        // random photo (picsum); null -> striped placeholder only
    val tone: Long,               // ARGB of the striped placeholder band
    val photoLabel: String = title,
    val author: Author,
    val status: PostStatus = PostStatus.APROBADA,
    // Lost / found / vet extras
    val eventDate: String? = null,      // "Sáb 5 sep · 6:30 p. m." / "Dom 13 sep"
    val schedule: String? = null,       // "8:00 a. m. – 1:00 p. m."
    val place: String? = null,          // "Parque El Virrey, Chapinero"
    val services: List<String> = emptyList(),
    val capacity: Int? = null,
    val phone: String? = null,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
)

data class User(
    val id: String,
    val name: String,
    val email: String,
    val city: String,
    val phone: String = "",
    val bio: String = "",
    val points: Int,
    val initials: String,
    val memberSince: String = "2024",
    val publicationsCount: Int = 0,
    val adoptionsCount: Int = 0,
    val fosterCount: Int = 0,
    val activeCount: Int = 0,
    val isModerator: Boolean = false,
) {
    val level: Level get() = Level.forPoints(points)
    val nextLevel: Level? get() = level.next
    val pointsToNext: Int get() = nextLevel?.let { it.minPoints - points } ?: 0
    /** 0f..1f progress inside the current level (design shows 0.70 for 155 pts). */
    val progress: Float
        get() = nextLevel?.let { (points - level.minPoints).toFloat() / (it.minPoints - level.minPoints) }?.coerceIn(0f, 1f) ?: 1f
}

/** A request I sent for someone else's publication. */
data class SentRequest(
    val id: String,
    val publicationId: String,
    val status: RequestStatus,
    val whenLabel: String,        // "Enviada hace 2 h"
    val message: String = "",
)

/** Someone interested in one of my publications. */
data class Applicant(
    val id: String,
    val userId: String,
    val name: String,
    val level: Level,
    val initials: String,
    val message: String,
    val timeAgo: String,
    val status: RequestStatus = RequestStatus.PENDIENTE,
)

data class ReceivedRequestGroup(
    val publicationId: String,
    val applicants: List<Applicant>,
)

data class Conversation(
    val id: String,
    val contact: Author,
    val about: String,            // "Sobre: Max, beagle juguetón"
    val phone: String = "",
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val fromMe: Boolean,
    val text: String,
    val time: String,
)

enum class NotificationType { NEW_REQUEST, REQUEST_ACCEPTED, POST_APPROVED, POST_REJECTED, LEVEL_UP }

data class AppNotification(
    val id: String,
    val group: String,            // "Hoy", "Ayer", "Esta semana"
    val type: NotificationType,
    val title: String,
    val text: String,
    val time: String,
    val read: Boolean = false,
    val targetId: String? = null, // publication id / conversation id depending on type
)

data class Report(val who: String, val reason: String, val detail: String)

data class ModerationItem(
    val publication: Publication,
    val waitTime: String,         // "3 h en espera"
    val reports: List<Report> = emptyList(),
    val authorApproved: Int,
    val authorRejected: Int,
) {
    val id: String get() = publication.id
    val isReported: Boolean get() = reports.isNotEmpty()
}

data class ModerationStats(val pending: Int, val reported: Int, val approvedToday: Int)

data class PointsEntry(val what: String, val points: Int, val whenLabel: String)

data class PointsAction(val what: String, val points: Int)

/** Data captured by the 3-step "Crear publicación" form. */
data class PublicationDraft(
    val category: Category,
    val title: String,
    val description: String,
    val species: String,
    val breed: String,
    val size: String,
    val vaccinated: Boolean,
    val sterilized: Boolean,
    val age: String,
    val sex: String,
    val city: String,
    val eventDate: String? = null,
)
