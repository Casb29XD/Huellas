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
  - El publicador recibe agrupadas las solicitudes en su bandeja de "Solicitudes Recibidas" (`ReceivedRequestGroup`), pudiendo revisar el perfil público, nivel y puntos del interesado.
  - El publicador puede **Aceptar** o **Rechazar** la solicitud directamente (RPC `respond_to_request`, que notifica al solicitante).
- **Canal de Chat Seguro y Contextual:**
  - Al aceptar la solicitud se crea una conversación (`Conversation`) enlazada a la publicación.
  - La mensajería interna (`ChatMessage`, texto) permite coordinar visitas y resolver dudas sin exponer el teléfono; el teléfono del otro participante solo se obtiene con `get_contact_phone`.
  - Estado actual: los mensajes se recargan al enviar o abrir el chat; `chat_messages` ya está publicada en Supabase Realtime, pero la app todavía no se suscribe.
- **Cierre del ciclo de vida:**
  - Una vez concretada la adopción en el chat, el publicador marca la publicación como **Finalizada** (RPC `close_publication`) y el servidor suma los puntos de gamificación correspondientes. Las insignias (`Badge`) existen en el modelo de la app, pero el backend todavía no las calcula.

---

## 2. Arquitectura actual

1. **Rutas type-safe (`@Serializable`)** en `navigation/Routes.kt` (`Routes.Detail(id)`, `Routes.CreatePost(editPublicationId)`,
   `Routes.Requests(tab)`, `Routes.Chat(id)`, `Routes.PublicProfile(id)`, `Routes.ModerationDetail(id)`, etc.) con
   `composable<T>` y `toRoute<T>()`.
2. **Casos de uso** en `domain/usecase/PublicationUseCases.kt` (`CreatePublicationUseCase`, `UpdatePublicationUseCase`,
   `DeletePublicationUseCase`, `AddCommentUseCase`, `DeleteCommentUseCase`, `GetCommentsUseCase`, `VotePublicationUseCase`,
   `ModeratePublicationUseCase`, `DeleteAccountUseCase`) con validaciones previas al repositorio.
3. **Datos:** `SupabaseRepository` es la única implementación de `HuellaRepository` en la app (Retrofit contra PostgREST,
   Auth y Storage). `FakeRepository` existe solo en `app/src/test` para pruebas.
4. **Rol moderador:** `enum class Role { USUARIO, MODERADOR }`; el panel de moderación solo aparece si
   `user.role == Role.MODERADOR`. En el servidor, el alta nunca otorga el rol, el cliente no puede cambiar
   `is_moderator`, puntos ni nivel, y el autor no puede aprobar su publicación (migraciones `20261109010*`,
   ver `docs/SEGURIDAD_BACKEND.md`).
5. **Moderación:** el RPC `moderate_publication` cambia el estado, notifica al autor y suma puntos; el feed solo
   muestra publicaciones `APROBADA`. La Edge Function `moderate-ai` da una sugerencia opcional (`docs/MODERACION_IA.md`).
6. **Funciones respaldadas por el servidor:** comentarios y votos (tablas con RLS; un comentario ajeno notifica
   `NEW_COMMENT` al autor), "Lo he visto" (`report_sighting`, notificación `SIGHTING`), edición desde el asistente (PATCH; vuelve a revisión) y
   eliminación de publicaciones, eliminación de cuenta (`delete_my_account`), cierre de publicación, notificaciones
   de solicitudes y estadísticas del perfil.
7. **Pendiente:** los tipos `NEW_VOTE` y `BADGE_EARNED` existen en el enum pero ningún proceso los genera; no hay
   insignias calculadas ni notificaciones push (Ajustes no muestra interruptores de notificaciones). Cada publicación
   guarda una sola foto (`image_url`); no hay geolocalización (ni permisos de ubicación ni filtro de distancia).

---

## 3. Pruebas

Detalle y comandos en [docs/PRUEBAS.md](docs/PRUEBAS.md). Resumen:

| Suite | Dónde | Qué cubre |
|---|---|---|
| Unitarias JVM | `app/src/test/.../data/remote`, `domain/usecase`, `HuellaDomainIntegrationTest` | Parseo de fechas, sesión y auth de `SupabaseRepository` con MockWebServer, configuración, casos de uso sobre `FakeRepository`, endpoints con `MockSupabaseApiService` |
| Robolectric + compose-ui-test | `app/src/test/.../ui`, `features/moderation` | Pantallas sin emulador (login, registro, inicio, detalle, ajustes, moderación, inglés) |
| E2E | `app/src/test/.../e2e/*E2eTest` | Flujos reales y exploits de RLS contra Supabase; se saltan sin variables de entorno |
| pgTAP | `supabase/tests/*.sql` | Seguridad, gamificación, solicitudes, chat y eliminación de cuenta en la BD |

`HuellaDomainIntegrationTest` y `PublicationUseCasesTest` validan reglas de dominio sobre `FakeRepository`; no prueban
el backend. Lo que toca Supabase lo cubren las suites E2E y pgTAP.

```bash
scripts/gradle-native.sh :app:testDebugUnitTest   # unitarias + Robolectric
scripts/test-all.sh                               # batería completa contra el hosted
```

Informe HTML: `app/build/reports/tests/testDebugUnitTest/index.html`.
