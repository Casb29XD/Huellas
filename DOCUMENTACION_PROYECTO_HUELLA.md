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

---

## 3. Estrategia y Funcionamiento de los Tests

El proyecto cuenta con una infraestructura de pruebas automatizadas diseñada para ejecutarse de manera **local, determinista y segura** (sin depender de red externa ni modificar la base de datos de producción de Supabase).

Se implementaron **19 pruebas unitarias y de integración** distribuidas en 3 suites principales:

```
app/src/test/java/com/desarrolloMovielexample/huella/
├── HuellaDomainIntegrationTest.kt             # Pruebas de integración de reglas de dominio
├── domain/usecase/
│   └── PublicationUseCasesTest.kt             # Pruebas unitarias de casos de uso y validaciones
└── data/remote/
    ├── MockSupabaseApiService.kt              # Doble de prueba (Mock) de la API REST / RPC
    └── SupabaseEndpointsMockTest.kt           # Pruebas de consumo y contratos de endpoints
```

---

### 3.1. Pruebas de Integración de Dominio (`HuellaDomainIntegrationTest`)
Verifican la interacción entre los modelos, repositorios y reglas de negocio globales de la aplicación:

1. **`testModeratorRoleAndNormalUserRole`**:
   - **Propósito:** Garantizar que el rol de moderador esté restringido.
   - **Funcionamiento:** Inicia sesión con credenciales de usuario moderador (`mariana@correo.com`) y comprueba que `role == Role.MODERADOR` y `isModerator == true`. Luego inicia sesión con usuario estándar (`andres@correo.com`) y confirma que `role == Role.USUARIO` y `isModerator == false`, asegurando que la UI del panel de moderación no sea accesible para usuarios convencionales.
2. **`testApprovePublicationReachesFeed`**:
   - **Propósito:** Resolver la desconexión histórica entre moderación y feed.
   - **Funcionamiento:** Crea un borrador en estado inicial `EN_REVISION`. Verifica que la publicación **no** aparece en el flujo público `publications`. Ejecuta `ModeratePublicationUseCase.approve()` como moderador y comprueba que inmediatamente el estado cambia a `APROBADA` y se propaga en tiempo real a la lista pública del feed.
3. **`testCommentsAndNotificationGeneration`**:
   - **Propósito:** Validar el sistema de retroalimentación interactiva y notificaciones.
   - **Funcionamiento:** Un usuario añade un comentario a la publicación de otro rescatista mediante `AddCommentUseCase`. Comprueba que el comentario aparece en el StateFlow de comentarios y que se genera automáticamente una notificación de tipo `NEW_COMMENT` para el autor del post.
4. **`testVotingMechanism`**:
   - **Propósito:** Verificar el algoritmo de conteo y conmutación (toggle) de votos.
   - **Funcionamiento:** Emite un voto favorable (+1) y verifica que el contador incrementa en 1 y `userVote == 1`. Al volver a presionar el mismo botón, el sistema retira el voto (toggle a 0), restaurando el conteo original sin duplicar registros.
5. **`testDetailedStatsCalculation`**:
   - **Propósito:** Asegurar el cálculo correcto del desglose de estadísticas e insignias.
   - **Funcionamiento:** Recupera las estadísticas del perfil del usuario autenticado y valida que los contadores desglosados (`activeCount`, `pendingCount`, `finishedCount`) no sean conteos genéricos y que la colección de insignias (`badges`) contenga los logros obtenidos.

---

### 3.2. Pruebas Unitarias de Casos de Uso (`PublicationUseCasesTest`)
Prueban la lógica defensiva, reglas de negocio atómicas y validaciones antes de llegar a la capa de persistencia:

1. **`createPublicationValidation`**:
   - Comprueba que un intento de creación sin título o sin descripción es rechazado inmediatamente con `Result.failure` y mensajes de error descriptivos.
   - Valida que un borrador completo sea aceptado y guardado en estado `EN_REVISION`.
2. **`updatePublicationValidation`**:
   - Impide actualizaciones que dejen el título en blanco.
   - Valida la actualización de campos existentes y persistencia de cambios.
3. **`commentValidationAndDeletion`**:
   - Rechaza comentarios vacíos o compuestos únicamente por espacios en blanco.
   - Aplica la restricción de longitud máxima de 500 caracteres, fallando si se supera el límite.
   - Verifica la eliminación exitosa de un comentario por su autor o moderador.
4. **`deletePublicationFlow`**:
   - Verifica el ciclo de vida de eliminación de una publicación, comprobando que ya no esté disponible en el repositorio tras invocar el caso de uso.
5. **`moderationRejectFlow`**:
   - Valida el flujo en el que un moderador rechaza una publicación especificando el motivo (`reason`) y la retroalimentación para el rescatista (`comment`), marcando el estado en `RECHAZADA`.
6. **`deleteAccountFlow`**:
   - Comprueba que `DeleteAccountUseCase` ejecuta la baja de la cuenta y desautentica la sesión actual (`currentUser == null`).

---

### 3.3. Pruebas de Endpoints y Red (`SupabaseEndpointsMockTest`)
Simulan las llamadas HTTP REST y procedimientos remotos (RPC) hacia el backend utilizando un doble de prueba ([`MockSupabaseApiService`]) implementado sobre la misma interfaz Retrofit del proyecto:

1. **`testAuthSignInSuccess` y `testAuthSignInFailure`**:
   - Simula el endpoint `/auth/v1/token?grant_type=password`.
   - Verifica que ante credenciales válidas se reciba código 200 con `access_token` JWT y `user.id`.
   - Verifica que ante credenciales inválidas se retorne código 400 con cuerpo de error deserializado mediante `ResponseBody.create`.
2. **`testGetPublicationsFiltering`**:
   - Simula la consulta PostgREST `/rest/v1/publications?select=*&status=eq.APROBADA`.
   - Comprueba que el filtro de estado excluya publicaciones `EN_REVISION` o `RECHAZADA`, entregando únicamente las aprobadas.
3. **`testCreatePublicationEndpoint`**:
   - Simula la inserción `POST /rest/v1/publications` con encabezado `Prefer: return=representation`.
4. **`testModeratePublicationRpcEndpoint`**:
   - Simula la llamada RPC de Supabase `/rest/v1/rpc/moderate_publication` enviando `p_publication_id` y `p_status`.
   - Comprueba que la mutación atómica se aplique correctamente en el almacén de datos.
5. **`testAdoptionRequestsAndChatMessagesFlow`**:
   - Simula la inserción de solicitudes (`POST /rest/v1/adoption_requests`) y el envío de mensajes de mensajería (`POST /rest/v1/messages`), validando la estructura del payload y la persistencia en memoria.
6. **`testNotificationsMarkAllReadEndpoint`**:
   - Simula la actualización en bloque `PATCH /rest/v1/notifications?read=eq.false` marcando todas como leídas.

---

### 3.4. Cómo Ejecutar los Tests Localmente

Desde la terminal en la raíz del proyecto:

```powershell
# En Windows (usando el JBR de Android Studio)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --no-configuration-cache --console=plain
```

```bash
# En macOS / Linux
./gradlew testDebugUnitTest
```

#### Reportes HTML de Resultados:
Al finalizar la ejecución, Gradle genera un informe interactivo con el detalle de tiempos y aserciones en:
`app/build/reports/tests/testDebugUnitTest/index.html`

