# Justificación de Necesidad Adicional y Arquitectura — Proyecto Huella

Documento de soporte para la entrega y actualización del informe PDF del proyecto Huella.

---

## 1. Identificación y Justificación de la Necesidad Adicional

### Necesidad adicional del proyecto: 
> **"Sistema Integral de Gestión de Solicitudes de Adopción / Hogar Temporal con Mensajería y Chat en Tiempo Real entre Adoptante y Publicador"**

### Justificación:
En las aplicaciones convencionales de rescate animal o adopción, la interacción suele limitarse a una publicación estática con un número de teléfono o enlace externo a WhatsApp. Esto provoca:
1. **Falta de trazabilidad:** El publicador y la comunidad no saben si la mascota ya fue adoptada, cuántas personas están interesadas o en qué estado se encuentra el proceso.
2. **Pérdida de privacidad y seguridad:** Exponer públicamente números personales expone a rescatistas y adoptantes a spam, estafas o intermediarios comerciales no deseados.
3. **Descoordinación en la selección:** Un rescatista recibe decenas de mensajes dispersos sin un formato estructurado de postulación (nivel de compromiso, condiciones del hogar, experiencia previa).

### Cómo lo resuelve Huella:
- **Flujo de Solicitudes:**
  - El adoptante postula formalmente con un mensaje de motivación (`sendAdoptionRequest`), quedando registrado en su bandeja de "Solicitudes Enviadas".
  - El publicador recibe agrupadas las solicitudes en su bandeja de "Solicitudes Recibidas" (`ReceivedRequestGroup`), pudiendo revisar el perfil, nivel de confiabilidad e insignias del interesado.
  - El publicador puede **Aceptar** o **Rechazar** la solicitud directamente.
- **Canal de Chat Seguro y Contextual:**
  - Al abrirse el interés mutuo, se genera una conversación (`Conversation`) enlazada al ID de la publicación.
  - La mensajería interna (`ChatMessage`) permite coordinar visitas, resolver dudas sobre comportamiento o vacunas y compartir fotos de avance sin salir de la plataforma.
- **Cierre del ciclo de vida:**
  - Una vez concretada la adopción en el chat, el publicador marca la publicación como **Finalizada**, otorgando puntos de gamificación e insignias automáticas (`ADOPCION_EXITOSA` / `HOGAR_TEMPORAL`).

---

## 2. Mejoras de Arquitectura Implementadas

1. **Migración a Rutas Type-Safe (`@Serializable`):**
   - Se eliminaron las cadenas String propensas a errores de tipeo y los bundles manuales con `navArgument`.
   - Se implementó `kotlinx.serialization` en `Routes`:
     - `Routes.Detail(val id: String)`
     - `Routes.CreatePost(val editPublicationId: String? = null)`
     - `Routes.Requests(val tab: Int = 0)`
     - `Routes.Chat(val id: String)`
     - `Routes.PublicProfile(val id: String)`
     - `Routes.ModerationDetail(val id: String)`
   - Se utilizan métodos nativos de Navigation Compose `composable<T>` y `entry.toRoute<T>()`.

2. **Capa de Casos de Uso (`domain/usecase`):**
   - Se separó la lógica de negocio y validaciones de los ViewModels mediante Use Cases con `operator fun invoke()`:
     - `CreatePublicationUseCase`, `UpdatePublicationUseCase`, `DeletePublicationUseCase`
     - `AddCommentUseCase`, `DeleteCommentUseCase`, `GetCommentsUseCase`
     - `VotePublicationUseCase`
     - `ModeratePublicationUseCase`
     - `DeleteAccountUseCase`

3. **Corrección del Rol Moderador:**
   - Se creó el `enum class Role { USUARIO, MODERADOR }`.
   - El panel de moderación en `ProfileScreen` y las rutas de moderación quedan estrictamente condicionadas a `user.role == Role.MODERADOR`.
   - En el backend se preparó el script de protección para evitar escalación de privilegios desde el cliente.

4. **Sincronización del Feed con Moderación:**
   - Se corrigió el problema en `FakeRepository`: ahora existe una única fuente de publicaciones (`allPublications`).
   - Al ejecutar `approve(itemId)`, el estado pasa a `PostStatus.APROBADA` y se refleja inmediatamente tanto en `myPublications` como en el feed público (`publications`), además de notificar al autor.

5. **Nuevas Funcionalidades Completadas:**
   - **Comentarios:** lectura, adición y eliminación con actualización en tiempo real y notificación al dueño del post.
   - **Votos:** sistema de valoración upvote/downvote (`publication.votesCount` y `userVote`).
   - **Edición y Eliminación:** posibilidad de editar publicaciones (vuelven a revisión) y eliminarlas mediante diálogo de confirmación.
   - **Eliminación de Cuenta:** opción en Ajustes con diálogo de advertencia irreversible.
   - **Estadísticas Detalladas:** desglose de publicaciones activas, finalizadas y en revisión en el perfil.
   - **Insignias:** modelo `Badge` visible en el perfil según los logros del usuario.
   - **Notificaciones:** soporte de lectura individual al tocar y soporte para tipos `NEW_COMMENT`, `NEW_VOTE` y `BADGE_EARNED`.
