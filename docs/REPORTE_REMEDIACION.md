# Reporte de remediación: auditoría 2026-11

Rama `fix/auditoria-2026-11` sobre el commit base `04713a4` (auditoría técnica del 2026-11-08, 63 hallazgos).
Fecha del reporte: 2026-11-08. La prueba que cubre cada ítem está en `docs/MATRIZ_HALLAZGOS.md`.

## Resumen ejecutivo

- **Alcance:** 131 commits (`git log fix/auditoria-2026-11 ^04713a4`, contando este) y 177 archivos cambiados,
  con unas +10 600 y −2 800 líneas. Se añadieron 24 migraciones append-only a las 3 que ya existían, además de pruebas
  pgTAP, E2E contra Supabase y Robolectric, y las Edge Functions `moderate-ai` y `verify-image`.
- **Estado final de los 63 ítems:** 57 resueltos, 6 parciales (5, 18, 20, 21, 22, 36) y 0 pendientes.
  - Los parciales de backend dependen de un paso operativo en el hosted: rotar la contraseña de `admin@huella.app`
    (ítem 5) y cargar los secretos de Vault para el borrado y la verificación de fotos (ítem 18).
  - Los demás parciales se resuelven en la app: Realtime (20), columnas tipadas (21), ubicación y mapa (22) y
    App Links (36).
- **Verificación independiente del backend:** una revisión en solo lectura contra el hosted encontró tres hallazgos
  nuevos: solicitudes autoaceptadas por la Data API (7), nombre de perfil sin límite (23) y el moderador semilla con
  la contraseña publicada (5). Los dos primeros ya están corregidos y probados en vivo. El tercero sigue abierto
  (ver la sección propia más abajo).
- **Funciones simuladas que ahora son reales:** editar, eliminar, finalizar adopción, comentarios, votos,
  avistamientos, puntos, notificaciones y eliminar cuenta pasan por PostgREST, RPC y triggers. Las pruebas E2E las
  ejercitan con el `SupabaseRepository` real.
- **Pruebas:** la corrida final contra el hosted (`scripts/test-all.sh` sobre `e16808f`, exit 0, tras los cierres de
  backend de la sección "Pendiente") dio:
  - pgTAP: 22/22 tests y 225/225 aserciones.
  - `deno test supabase/functions/`: 10/10 (`moderate-ai` y `verify-image`).
  - JVM: 119/119 en 30 clases, con 24 E2E contra el hosted, sin saltos ni fallos.
- **Pendiente operativo:**
  - Rotar la contraseña de `admin@huella.app` en el hosted (ítem 5). La sesión de reparación no tenía permiso.
  - Cargar los secretos de Vault con `scripts/configure-storage-vault.sh` (ítem 18).
  - Configurar `DEEPSEEK_API_KEY`, si el dueño lo decide.

### Cómo se llegó aquí

1. Remediación por bloques: backend y RLS, gamificación, front, i18n, visual y documentación. Cada commit es atómico
   y cita el ítem que resuelve.
2. Verificación adversarial en solo lectura sobre `86a4197`. Encontró que el ítem 9 seguía roto: "Editar" abría el
   asistente vacío y creaba otra publicación. También dejó parciales los ítems 14, 15, 32, 33, 36, 37, 44, 49 y 56-60.
   Los problemas eran esperas fijas, interruptores que solo vivían en memoria, un diálogo de cierre con información
   falsa y fotos huérfanas.
3. Reparación de esos ítems (`19ff386`..`de9dac9`), endurecimiento de `moderate-ai` (`e6104ac`) y pruebas para
   los ítems 42 y 47 (`4e4082c`).
4. Corrida completa contra el hosted y matriz actualizada (`5834397`, `1afada6`).
5. Verificación independiente del backend (sección siguiente).
6. Reparación del backend (`5459b5e`..`09b7ff3`) y corrida E2E final (exit 0, sin cambios).

## Verificación independiente del backend

Revisión en solo lectura contra el hosted, sin commits ni migraciones. Usó el esquema real (`pg_policies`,
`column_privileges`, `information_schema`, `pg_enum`), pgTAP, la suite E2E y una sonda propia de 65 comprobaciones
negativas y positivas por REST, RPC, Auth y Storage. La sonda usó usuarios `qa+<nanoTime>@huella.test`, borrados al
terminar. Punto de partida: 16 migraciones registradas, pgTAP 14/14 (170/170), E2E 20/20 y `deno test` 4/4.

**Veredicto por ítem de backend:**

| Ítem | Veredicto | Evidencia principal |
|---|---|---|
| 1, 2, 3, 4, 6, 8, 16, 17, 19, 24, 53 | Resuelto | Exploits de la auditoría rechazados en vivo: signup con `is_moderator`, PATCH de `points`/`is_moderator`, autoaprobación, `select=email,phone` (403/401), `award_points` sin EXECUTE. La moderación suma +10 y resuelve reportes. `delete_my_account` borra en cascada |
| 5 | Parcial | `admin@huella.app` con la contraseña publicada inicia sesión en el hosted (HTTP 200) y es moderador |
| 7 | Parcial, **hallazgo nuevo** | El camino legítimo funcionaba, pero `authenticated` tenía `INSERT(status)` y `UPDATE(status)` sobre `adoption_requests`. Había dos atajos: el solicitante insertaba con `status='ACEPTADA'` (201) y el autor aceptaba con PATCH directo (200). Ninguno creaba conversación ni notificación |
| 18 | Parcial | Límites de carpeta, MIME y tamaño correctos. Quedaban tres residuos: binarios huérfanos en S3 al borrar, MIME validado solo por `Content-Type` (un HTML entraba como `image/jpeg`) y listado anónimo del bucket (`object/list` → 200) |
| 20 | Parcial | Trigger y publicación Realtime listos. La app no se suscribe |
| 21, 51, 52 | Parcial | `event_date`, `schedule`, `age` y `member_since` seguían TEXT. `tone` seguía como columna |
| 22 | Parcial | Sin default (0,0) en la BD. `MapScreen.toLatLng()` sigue inventando coordenadas |
| 23 | Parcial, **hallazgo nuevo** | Sin CHECK de longitud: `PATCH profiles {name: 'N'*5000}` → 204. Igual con `city`, `bio` y `phone` |

`moderate-ai` versión 2 desplegada y verificada: delimitadores `<<<PUBLICACION>>>`, `normalizeReview` y heurística
de venta.

**Reparación** (todas las migraciones aplicadas en el hosted):

| Commit | Qué cambia | Prueba |
|---|---|---|
| `5459b5e` | Ítem 7: `INSERT` solo de `publication_id`, `applicant_id` y `message`; sin `UPDATE` directo; se elimina la política UPDATE del autor. Las respuestas pasan solo por `respond_to_request` | pgTAP `test_30` (+4 casos); E2E `RlsSecurityE2eTest` (g) |
| `0129a56` | Scripts SQL remotos en UTF-8 y reparación del mojibake que habían dejado en el hosted | pgTAP `test_42_codificacion` |
| `e820508` | Ítem 23: CHECK de longitud en `name`, `city`, `phone` y `bio` | pgTAP `test_40`; E2E `RlsSecurityE2eTest` (h) |
| `911ef2d` | Ítems 21, 51 y 52: `event_at`, `event_end_at` y `age_months` tipadas, rellenadas por trigger con backfill; `member_since` generado desde `created_at`; se borra `tone` | pgTAP `test_41` |
| `65e7c13` | Ítem 5: el moderador de `seed.sql` nace con contraseña aleatoria | `RepoHygieneTest` |
| `84945e0` | Ítem 18: sin listado público; triggers que borran por la Storage API y Edge Function `verify-image` (magic bytes) | pgTAP `test_43`; `deno test` de `verify-image`; E2E `StorageAuthReportsE2eTest` |
| `f19fdfe` | E2E de los ítems 7 y 23 contra el hosted | — |
| `c58142b` | Ítem 22: CHECK que exige coordenadas completas, en rango y distintas de (0,0), o NULL | pgTAP `test_44` |
| `09b7ff3` | `docs/SEGURIDAD_BACKEND.md` con el contrato nuevo | — |

**Corrida E2E final** (`scripts/test-all.sh`, HEAD `09b7ff3`): verde a la primera.

- Las 24 migraciones locales (`20261101000000`..`20261109340100`) están registradas en el hosted.
- Tras la corrida no quedaron datos de prueba: 0 usuarios e2e/tap, 0 publicaciones `tap_`/`e2e` y 0 objetos `e2e-*`.

## Los 63 ítems

Estado final y commits que resuelven cada ítem. La matriz indica la prueba que lo cubre.

| # | Hallazgo | Estado | Commits | Nota |
|---|---|---|---|---|
| 1 | Registro como moderador por metadata | Resuelto | fe98070 |  |
| 2 | Usuario edita sus puntos, nivel y rol | Resuelto | fe98070 |  |
| 3 | Autor autoaprueba su publicación | Resuelto | 68e8259 |  |
| 4 | Email y teléfono expuestos | Resuelto | 63a1f8a |  |
| 5 | Credenciales de admin en el seed | Parcial | 3510bef 65e7c13 | `seed.sql` es solo local y su moderador nace con contraseña aleatoria. El hosted conserva `admin@huella.app` con la contraseña publicada (inicia sesión y es moderador): hay que rotarla o borrar la cuenta |
| 6 | Gamificación inexistente en servidor | Resuelto | ca63b9b |  |
| 7 | Notificaciones de solicitud nunca se generan | Resuelto | 58f17c3 504dbba 5459b5e f19fdfe | La verificación independiente encontró que se podía autoaceptar por INSERT o por PATCH directo. Corregido: el estado solo cambia por `respond_to_request` |
| 8 | No se puede eliminar la cuenta | Resuelto | 13a908b 1529b06 |  |
| 9 | Editar publicación crea otra | Resuelto | 88770ff bc28af6 |  |
| 10 | Eliminar publicación no elimina | Resuelto | 88770ff |  |
| 11 | Recuperar contraseña sin salida | Resuelto | 0b436ba 76799ba |  |
| 12 | Horas desfasadas 5 h | Resuelto | 60c95cd |  |
| 13 | Comentarios, votos, avistamientos simulados | Resuelto | ace9317 1178f6f |  |
| 14 | Fotos aleatorias de picsum | Resuelto | dab166d 414a1cc 19ff386 | Opción mínima: una foto por publicación (`image_url`); varias fotos piden `image_urls TEXT[]` |
| 15 | "Finalizar adopción" siempre falla | Resuelto | b90e536 1d9643c bb8993b | El diálogo dice que se marca adopción concretada y suma 50 puntos |
| 16 | Seed inconsistente con el trigger | Resuelto | 3510bef |  |
| 17 | Reportes sin ciclo de vida | Resuelto | 51a8d76 f79bd2f 4678d05 |  |
| 18 | Storage sin límites | Parcial | 0713158 84945e0 | Carpeta, MIME, 5 MB y listado privado activos. El borrado del binario por la Storage API y la verificación de magic bytes (`verify-image`) están desplegados, pero no se activan hasta cargar los secretos de Vault. Mientras tanto se borra la fila de `storage.objects` y el binario queda huérfano en S3 (sin URL que lo sirva) |
| 19 | Sin índices | Resuelto | a380c37 |  |
| 20 | `conversations.updated_at` y Realtime | Parcial | 13a908b | Trigger y publicación Realtime listos; la app no se suscribe |
| 21 | Datos semánticos como texto | Parcial | b90e536 911ef2d | La BD ya tiene `event_at`, `event_end_at` y `age_months` tipadas (con trigger y backfill). Falta que la app las escriba, las lea y ordene o filtre por ellas |
| 22 | Ubicación (0,0) y mapa falso | Parcial | b90e536 c58142b | La BD solo acepta coordenadas completas, en rango y distintas de (0,0), o NULL. El asistente no captura la ubicación y `MapScreen.toLatLng()` sigue inventando coordenadas por ciudad |
| 23 | `handle_new_user` frágil, email divergente | Resuelto | fe98070 83b97dd e820508 f19fdfe | La verificación independiente encontró `name` sin límite al editar. Corregido con CHECK de longitud en `name`, `city`, `phone` y `bio` |
| 24 | Enum `notification_type` incompleto | Resuelto | 51a8d76 a262fbf 8029365 |  |
| 25 | Sesión persistente ignorada | Resuelto | 88a8f31 |  |
| 26 | Expulsión silenciosa y refresh concurrente | Resuelto | 88a8f31 0b436ba |  |
| 27 | `refreshAll()` dentro de cada escritura | Resuelto | ae8b8e5 |  |
| 28 | Perfil público con datos falsos | Resuelto | dfdcaa4 |  |
| 29 | Notificaciones solo en memoria | Resuelto | 2622725 8029365 |  |
| 30 | Textos corruptos (mojibake) | Resuelto | 95629a6 489a95f 0129a56 | `0129a56` repara también el mojibake que quedó en los datos del hosted |
| 31 | Términos inexistentes | Resuelto | f9e7ad7 76799ba |  |
| 32 | Sin paginación | Resuelto | d2c05b6 | Categoría, especie y ciudad se filtran en el servidor; texto, tamaño y vacunado/esterilizado en memoria sobre las páginas cargadas |
| 33 | "cerca de Medellín" y ubicación sin uso | Resuelto | bdfc2bd 74ededd |  |
| 34 | Sin `.env.example` ni fail-fast | Resuelto | 2f6420e e079499 |  |
| 35 | Token en backup | Resuelto | e7e7b3f |  |
| 36 | Deep link custom sin verificación | Parcial | 0b436ba 16e7bce | Solo se acepta `code` + verifier; sigue el esquema `huella://`: App Links `https` con `autoVerify` y `assetlinks.json` requieren un dominio |
| 37 | i18n a medias | Resuelto | 79acb44 0175dcc b27c6db 34e90dc 972c9eb b352dc4 db2b772 7dbd5e2 70d1fdd 4748168 84c7efd bb8993b | Errores propios con `AppError(@StringRes)`; el texto que genera el servidor (mensajes de error de PostgREST, títulos de notificaciones) se muestra tal cual |
| 38 | Build no reproducible | Resuelto | b3ae855 e079499 |  |
| 39 | Tests no cubren el código real | Resuelto | 1bccf9a abf84fa c995e5b 0b436ba bd80e58 5afc6bb 4e4082c |  |
| 40 | Logging siempre activo | Resuelto | 5b8c43e |  |
| 41 | Listas cortadas por la barra inferior | Resuelto | bdfc2bd |  |
| 42 | Título del detalle bajo la barra de estado | Resuelto | b66e5c1 |  |
| 43 | Caption de depuración en fotos | Resuelto | dab166d 0b85310 |  |
| 44 | Snackbars tapan el CTA | Resuelto | b66e5c1 d43644a 0b85310 af44f68 |  |
| 45 | Títulos truncados a una línea | Resuelto | 3997c76 0b85310 |  |
| 46 | FAB naranja duplicado | Resuelto | bdfc2bd |  |
| 47 | Iconos repetidos y grillas desiguales | Resuelto | 5e3a826 | `icon_attr_castrado.xml` quedó con finales de línea CRLF (cosmético) |
| 48 | Error de términos sin mensaje, placeholder, typo | Resuelto | d43644a |  |
| 49 | Splash con puntos, onboarding que salta | Resuelto | 6d5053f bf20643 |  |
| 50 | Botones estrechos y áreas táctiles < 48 dp | Resuelto | 6d5053f 1c6b3bb |  |
| 51 | Sin triggers `updated_at` | Resuelto | a725ec6 911ef2d | `member_since` es un `smallint` generado desde `created_at`. Opcional en la app: derivarlo de `created_at` |
| 52 | `tone` ARGB en BD | Resuelto | b90e536 911ef2d | La columna se borró. La app aún lee `tone` con un valor por defecto: falta un mapa categoría→color (limpieza de front) |
| 53 | RPC no limpia reportes ni suma puntos | Resuelto | ca63b9b |  |
| 54 | `FakeRepository` en producción | Resuelto | 414a1cc |  |
| 55 | Interfaz que miente y mock espejo | Resuelto | 9a3577f |  |
| 56 | Esperas decorativas | Resuelto | d43644a 63f4c1d d2c05b6 561fe4c | Las confirmaciones se muestran en la pantalla de destino |
| 57 | Ajustes: toggles en memoria, cuenta | Resuelto | cf2c0bb 76799ba 1f94f91 |  |
| 58 | Accesibilidad | Resuelto | 1c6b3bb 0b85310 68ea6b5 4748168 a5a3222 |  |
| 59 | Tema solo claro | Resuelto | 8fdd357 8c52bee | El fondo de mapa estático es una ilustración decorativa y queda clara en ambos temas |
| 60 | Inconsistencias de marca y copy | Resuelto | 40d2bf4 9d251c3 84c7efd | Excepciones deliberadas: botón atrás circular sobre la foto del detalle (necesita contraste sobre la imagen) y hora `HH:mm` en el chat |
| 61 | Google Sign-In "Próximamente" | Resuelto | d43644a |  |
| 62 | Documentación desactualizada | Resuelto | aaaea84 947e1e2 5cd2266 86a4197 de9dac9 5834397 1afada6 09b7ff3 | Estado final en `docs/REPORTE_REMEDIACION.md` |
| 63 | `.gitignore`, `.env.example`, cp1252 | Resuelto | 95629a6 e079499 |  |

Hay dos temas fuera de la numeración, que salieron de la revisión de seguridad de commits recientes:

- **Refresh en vuelo tras logout/login:** resuelto. Lo cubre `SupabaseRepositorySessionTest`.
- **Prompt injection en `moderate-ai`:** resuelto (`e6104ac`) y desplegado: el hosted corre la versión 2.

## Pruebas: qué existen y cómo correrlas

| Suite | Dónde vive | Qué cubre |
|---|---|---|
| pgTAP | `supabase/tests/*.sql` (funciones `tests.test_*`: 19 tests, 213 aserciones) | Grants; RLS de perfiles y publicaciones; `handle_new_user`; storage (también el borrado por API, con un Vault de prueba); reportes; esquema; puntos y moderación; comentarios, votos y avistamientos; solicitudes sin atajos; chat e índices; eliminar cuenta; longitudes del perfil; tipos semánticos; codificación; ubicación válida |
| Edge Functions | `supabase/functions/*/*_test.ts` (`deno test supabase/functions/`, 8 pruebas) | `moderate-ai`: delimitadores contra prompt injection, validación del esquema de respuesta, heurística de venta. `verify-image`: magic bytes, ruta del objeto y solo service role |
| JVM unitarias | `app/src/test` | Repositorio (auth/PKCE, sesión, escrituras, paginación), fechas, tiempos relativos, ViewModels, contraste, higiene del repo (`RepoHygieneTest`) |
| Robolectric + compose-ui-test | `app/src/test/.../ui` | Login, Home, detalle, registro, ajustes, moderación, inglés, modo oscuro, ítems visuales |
| E2E (24 pruebas) | `app/src/test/.../e2e/*E2eTest` | `SmokeE2eTest`, `RlsSecurityE2eTest` (comprueba que los exploits de la auditoría están bloqueados), `BackendFlowsE2eTest`, `RepositoryFlowsE2eTest`, `StorageAuthReportsE2eTest`, `ModerateAiE2eTest` |

**Hosted (por defecto, sin Docker).** Las credenciales viven fuera del repo y nunca se versionan.

```bash
source /c/temp/huellas-audit/.env.remote      # o HUELLAS_ENV=<archivo>
scripts/test-all.sh                           # pgTAP remoto + deno test + Gradle (unit, Robolectric, E2E)
scripts/run-pgtap-remote.sh                   # solo pgTAP
scripts/gradle-native.sh :app:testDebugUnitTest                  # JVM (sin variables, las E2E se saltan)
scripts/gradle-native.sh :app:testDebugUnitTest --tests "*E2e*"  # solo E2E
```

La BD hosted es compartida y no se puede resetear, así que cada suite limpia lo que crea:

- Las E2E crean usuarios `e2e+<nanoTime>@huella.test` con la Admin API y los borran al terminar.
- Los tests pgTAP borran sus propios datos.
- Los objetos de storage `<uid>/e2e-*.jpg` se borran dentro de la misma prueba.

Tras la corrida final no quedaron residuos: 0 usuarios e2e/tap, 0 publicaciones `tap_`/`e2e` y 0 objetos `e2e-*`.
Se volvió a comprobar para este reporte: 0 usuarios `@huella.test`.

Herramientas necesarias: `curl`, `python` y `deno` en el PATH, más JDK 17+ y el Android SDK. En lugar del JDK y el
SDK sirve el toolchain portable de `HUELLAS_TOOLS`.

**Local (opcional, con Docker).** `scripts/test-all.sh --local` hace lo siguiente con el Supabase CLI:

1. Corre `supabase db reset`, que también aplica `supabase/seed.sql` (un seed solo para local).
2. Carga pgTAP con `psql`.
3. Corre Gradle en la imagen de `scripts/gradle-docker.sh`.

Más detalle en `docs/PRUEBAS.md`.

## Estado real del proyecto hosted

Consultado en solo lectura el 2026-11-08, después de la corrida final (proyecto `neuvyswxhibgcnezxxql`):

| Recurso | Estado |
|---|---|
| Migraciones | 27 registradas en `supabase_migrations.schema_migrations`; la última es `20261109350200`. Son las mismas 27 de `supabase/migrations/`, no falta ninguna |
| Edge Function `moderate-ai` | `ACTIVE`, **versión 3** (la de `3729555`), con `verify_jwt=true`. Sin JWT responde 401 y con JWT de un moderador de prueba `{"status":"not_configured"}` |
| Edge Function `verify-image` | `ACTIVE`, versión 2, con `verify_jwt=true`. Solo acepta el service role: anon recibe 403 |
| Secrets de funciones | Solo los que Supabase crea por defecto. **No hay `DEEPSEEK_API_KEY`** (decisión del dueño), así que `moderate-ai` responde `not_configured` y la UI lo muestra |
| Vault | **Faltan** `huellas_project_url` y `huellas_service_role_key`. Sin ellos, los triggers de storage usan el modo de respaldo: borran la fila de `storage.objects`, el binario queda huérfano en S3 y no se llama a `verify-image` (ítem 18) |
| pg_cron | Tres jobs: `huellas_cierre_automatico` (`0 8 * * *`), `huellas_limpieza_notificaciones` (`30 8 * * *`) y `huellas_reconciliacion_storage` (`15 * * * *`) |
| Storage | Bucket `pet-images`, público, con límite de 5 MB y MIME `image/jpeg`, `image/png` o `image/webp`. Cuatro políticas, todas limitadas a la carpeta propia (ver, subir, actualizar y borrar). Ya no hay listado público |
| `adoption_requests` | Dos políticas: INSERT del solicitante y SELECT de solicitudes enviadas o recibidas. La política UPDATE del autor ya no existe. Re-aseguradas sin depender de datos por `20261109350000` (`test_45`) |
| Realtime | La publicación `supabase_realtime` incluye `chat_messages` y `notifications`. La app todavía no se suscribe (ítem 20) |
| Auth | 6 usuarios: 4 de semilla (`admin@huella.app`, que es moderador, y laura, carlos y sofia) y 2 cuentas gmail del desarrollador. La confirmación de correo está activa |

## Pendiente o que requiere decisión humana

1. **Rotar el moderador semilla del hosted (ítem 5).** `admin@huella.app` sigue entrando con la contraseña que se
   publicó, y es moderador. La sesión de reparación no tuvo permiso para cambiarla:
   - Rotarla con `PUT /auth/v1/admin/users/<id>` y el service role, usando una clave aleatoria que no quede en el
     repo. Otra opción es borrar la cuenta y crear el moderador desde el dashboard.
   - Después, actualizar "Usuarios semilla" en `ENTORNO_REMOTO.md`. Ninguna prueba del repo usa esa credencial.
   - Laura, carlos y sofia tienen el mismo problema, pero sin rol de moderador. Las 2 cuentas gmail del desarrollador
     no se tocaron.
2. **Cargar los secretos de Vault (ítem 18).** Ejecutar
   `source /c/temp/huellas-audit/.env.remote && scripts/configure-storage-vault.sh`. Con eso quedan activos el
   borrado de binarios por la Storage API y la verificación de magic bytes, sin otros cambios. `test_43` ya prueba
   el cableado con un Vault de prueba dentro de la transacción.
3. **Seguimiento de la revisión automática de dos migraciones: cerrado, con un límite abierto.**
   - `20261109300100_reparar_codificacion.sql`: `20261109350000` (`5659d93`, aplicada) re-asegura sin depender de
     datos los privilegios (solo SELECT e INSERT de `publication_id`, `applicant_id`, `message`) y exactamente las
     dos políticas. `test_45_politicas_solicitudes` lo exige. La conversión de datos no se repite: en un entorno
     donde fallara, `apply-migrations` se detiene en `300100` y una copia posterior no llegaría a correr.
   - `20261109330000_storage_por_api.sql`: `20261109350100` (`8b44dcf`, aplicada) borra también las filas de
     `storage.objects` en la misma transacción (`purge_pet_images`, que usan `delete_publication_images` y
     `delete_my_account`), con o sin Vault. Las peticiones encoladas se registran en `public.storage_purge_failures`
     y el job `huellas_reconciliacion_storage` las cruza con `net._http_response`: borra las 2xx y registra error,
     timeout o falta de respuesta. Es horario y no diario porque `pg_net.ttl` es de 6 h. No reintenta (ver el
     límite). Lo cubren `test_46_purga_storage` y `test_43`.
   - **Límite abierto (verificado en el hosted con un objeto de prueba):** la Storage API borra el binario solo de
     las filas que encuentra. Si la fila ya se borró por SQL, el DELETE responde `[]` y el binario queda huérfano en
     S3. No se sirve (sin fila, `/object/public/...` responde 400), pero sigue almacenado. Además, la CDN puede servir
     una copia en caché de la URL pública durante su TTL. Purgar también el binario exige otro diseño, y decidirlo le
     corresponde al dueño: por ejemplo, guardar `name` y `version` y reinsertar la fila justo antes del DELETE.
4. **Residuos menores del backend: cerrados.**
   - `moderate-ai` (`3729555`, desplegada como versión 3): ya no envía `phone`, un `risk` ausente o inválido queda en
     0.5 y la heurística detecta "venta", "vendo", "mil pesos", "COP", "$" con cifra y "precio" sin marcar
     "ventana". Lo cubre `review_test.ts`.
   - `20261109350200` (`e16808f`, aplicada) retira EXECUTE a PUBLIC, anon y authenticated de todas las funciones de
     trigger SECURITY DEFINER de public. Lo exige `test_47_triggers_sin_execute`.
5. **Key de DeepSeek.** Falta decidir si se activa la sugerencia de IA:
   - Se activa con `supabase secrets set DEEPSEEK_API_KEY=... --project-ref <ref>` y, si se quiere, `DEEPSEEK_MODEL`.
   - Activarla manda el contenido de cada publicación a un tercero (ya sin `phone`), así que conviene revisarlo
     contra la política de datos.
   - Detalles en `docs/MODERACION_IA.md`.
6. **Credenciales del seed.** El moderador de `supabase/seed.sql` ya nace con contraseña aleatoria, pero laura,
   carlos y sofia siguen con contraseñas en claro:
   - Solo se usa en local, con `supabase db reset`.
   - `RepoHygieneTest` impide que las migraciones creen usuarios de auth y que vuelva `Admin1234`.
   - Las contraseñas siguen en el historial de git. Sacarlas exige reescribir el historial, y esa decisión le
     corresponde al dueño del repo.
7. **App Links https (ítem 36).** Hoy solo se acepta `code` PKCE con el verifier local, por el esquema `huella://`.
   Para pasar a App Links verificados hacen falta tres cosas:
   - Un dominio que sirva `/.well-known/assetlinks.json` con el SHA-256 del certificado de firma.
   - `autoVerify=true` en el manifest.
   - Las URL de redirección registradas en Supabase Auth.
8. **Trabajo pendiente en la app** (el backend ya está listo):
   - 20: suscribir el chat abierto y las notificaciones a Realtime (`postgres_changes` filtrado por
     `conversation_id`), con una prueba E2E.
   - 21: escribir y leer `event_at`, `event_end_at` y `age_months`, y ordenar o filtrar por `event_at`.
   - 22: no pintar en el mapa las publicaciones sin coordenadas, en lugar de `MapScreen.toLatLng()`, y capturar la
     ubicación en el asistente.
   - 51 y 52 (limpieza opcional): derivar "miembro desde" de `created_at`, y cambiar `toneColor(publication.tone)`
     por un mapa categoría→color en `SupabaseRepository.kt`, `Images.kt`, `RequestsScreen.kt` y
     `ModerationDetailScreen.kt`.
9. **Limitaciones aceptadas:**
   - Una sola foto por publicación. Permitir varias pide una columna `image_urls TEXT[]`.
   - Los textos que genera el servidor (errores de PostgREST, títulos de notificaciones) se muestran sin traducir.
   - `SupabaseRepository.login()` mantiene un `delay(350)`. No es decorativo: reintenta mientras el trigger crea
     el perfil.
   - `icon_attr_castrado.xml` tiene finales de línea CRLF.
10. **Trailer de los commits.** La rama mezcla dos trailers: 97 commits con `Co-Authored-By: Claude Opus 5.5`, que
    es la atribución que indicó el harness, y 34 con `Claude Fable 5.1`, que pide `ENTORNO_REMOTO.md`. Si se quiere
    uno solo, los trailers se reescriben con un rebase antes de publicar.
11. **Sin CI.** Las pruebas se corren a mano con `scripts/test-all.sh`. Un pipeline necesitaría secretos del hosted
    o un Supabase efímero con Docker.

## Publicar la rama (PR)

No se ha hecho push: todos los commits siguen solo en local.

```bash
cd C:/temp/huellas-audit/Huellas
git status                                   # árbol limpio
source /c/temp/huellas-audit/.env.remote && scripts/test-all.sh   # verde antes de publicar
git push -u origin fix/auditoria-2026-11
gh pr create --base main --head fix/auditoria-2026-11 \
  --title "Remediación de la auditoría técnica 2026-11 (63 hallazgos)" \
  --body-file docs/REPORTE_REMEDIACION.md
```

Después del merge:

1. En cada entorno, correr `scripts/apply-migrations-remote.sh` para confirmar que no falta ninguna migración, y
   `scripts/configure-storage-vault.sh` para cargar los secretos de Vault (pendiente 2).
2. Desplegar `moderate-ai` y `verify-image` en cada entorno nuevo (en el hosted de pruebas ya están).
3. Rotar las cuentas semilla (pendiente 1).
4. Generar el APK de release con `app/.env` apuntando al proyecto real.

Son 131 commits, demasiados para revisarlos uno por uno. Es más práctico recorrerlos por ítem con la matriz.
