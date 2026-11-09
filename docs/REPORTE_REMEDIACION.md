# Reporte de remediación: auditoría 2026-11

Rama `fix/auditoria-2026-11` sobre el commit base `00f9cdc` (auditoría técnica del 2026-11-08, 63 hallazgos).
Fecha del reporte: 2026-11-08. La prueba que cubre cada ítem está en `docs/MATRIZ_HALLAZGOS.md`.

## Resumen ejecutivo

- **Alcance:** 131 commits (`git log fix/auditoria-2026-11 ^00f9cdc`, contando este) y 177 archivos cambiados,
  con unas +10 600 y −2 800 líneas. Se añadieron 24 migraciones append-only a las 3 que ya existían, además de pruebas
  pgTAP, E2E contra Supabase y Robolectric, y las Edge Functions `moderate-ai` y `verify-image`.
- **Estado final de los 63 ítems:** 58 resueltos, 5 parciales (18, 20, 21, 22, 36) y 0 pendientes.
  - El ítem 5 quedó resuelto al rotar la contraseña de `admin@huella.app` en el hosted (2026-11-09).
  - El parcial de backend (18) ya no depende de Vault (configurado el 2026-11-09): queda el binario huérfano en S3
    al borrar, que exige una decisión de diseño (pendiente 3).
  - Los demás parciales se resuelven en la app: Realtime (20), columnas tipadas (21), ubicación y mapa (22) y
    App Links (36).
- **Verificación independiente del backend:** una revisión en solo lectura contra el hosted encontró tres hallazgos
  nuevos: solicitudes autoaceptadas por la Data API (7), nombre de perfil sin límite (23) y el moderador semilla con
  la contraseña publicada (5). Los tres están corregidos y probados en vivo; el tercero, rotando la contraseña
  (la anterior responde 400 y la nueva 200).
- **Funciones simuladas que ahora son reales:** editar, eliminar, finalizar adopción, comentarios, votos,
  avistamientos, puntos, notificaciones y eliminar cuenta pasan por PostgREST, RPC y triggers. Las pruebas E2E las
  ejercitan con el `SupabaseRepository` real.
- **Pruebas:** la corrida final contra el hosted (`scripts/test-all.sh` sobre `871f26a`, exit 0, tras los cierres de
  backend de la sección "Pendiente"; repetida el 2026-11-09 con Vault configurado y la clave rotada, mismo resultado) dio:
  - pgTAP: 22/22 tests y 225/225 aserciones.
  - `deno test supabase/functions/`: 10/10 (`moderate-ai` y `verify-image`).
  - JVM: 119/119 en 30 clases, con 24 E2E contra el hosted, sin saltos ni fallos.
- **Pendiente operativo:**
  - Ninguno. Hechos el 2026-11-09: `DEEPSEEK_API_KEY` configurada (`moderate-ai` devuelve `status: ok` con revisión), contraseña de `admin@huella.app` rotada (ítem 5) y secretos de Vault cargados con
    `scripts/configure-storage-vault.sh` (ítem 18).

### Cómo se llegó aquí

1. Remediación por bloques: backend y RLS, gamificación, front, i18n, visual y documentación. Cada commit es atómico
   y cita el ítem que resuelve.
2. Verificación adversarial en solo lectura sobre `2c90ffe`. Encontró que el ítem 9 seguía roto: "Editar" abría el
   asistente vacío y creaba otra publicación. También dejó parciales los ítems 14, 15, 32, 33, 36, 37, 44, 49 y 56-60.
   Los problemas eran esperas fijas, interruptores que solo vivían en memoria, un diálogo de cierre con información
   falsa y fotos huérfanas.
3. Reparación de esos ítems (`90f312d`..`94ff6fd`), endurecimiento de `moderate-ai` (`93dd941`) y pruebas para
   los ítems 42 y 47 (`95cdc62`).
4. Corrida completa contra el hosted y matriz actualizada (`5f1ce1d`, `4bf9039`).
5. Verificación independiente del backend (sección siguiente).
6. Reparación del backend (`486ed27`..`1858a2d`) y corrida E2E final (exit 0, sin cambios).

## Verificación independiente del backend

Revisión en solo lectura contra el hosted, sin commits ni migraciones. Usó el esquema real (`pg_policies`,
`column_privileges`, `information_schema`, `pg_enum`), pgTAP, la suite E2E y una sonda propia de 65 comprobaciones
negativas y positivas por REST, RPC, Auth y Storage. La sonda usó usuarios `qa+<nanoTime>@huella.test`, borrados al
terminar. Punto de partida: 16 migraciones registradas, pgTAP 14/14 (170/170), E2E 20/20 y `deno test` 4/4.

**Veredicto por ítem de backend:**

| Ítem | Veredicto | Evidencia principal |
|---|---|---|
| 1, 2, 3, 4, 6, 8, 16, 17, 19, 24, 53 | Resuelto | Exploits de la auditoría rechazados en vivo: signup con `is_moderator`, PATCH de `points`/`is_moderator`, autoaprobación, `select=email,phone` (403/401), `award_points` sin EXECUTE. La moderación suma +10 y resuelve reportes. `delete_my_account` borra en cascada |
| 5 | Resuelto | `admin@huella.app` tenía la contraseña publicada (HTTP 200, moderador). Rotada el 2026-11-09: la anterior responde 400 |
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
| `486ed27` | Ítem 7: `INSERT` solo de `publication_id`, `applicant_id` y `message`; sin `UPDATE` directo; se elimina la política UPDATE del autor. Las respuestas pasan solo por `respond_to_request` | pgTAP `test_30` (+4 casos); E2E `RlsSecurityE2eTest` (g) |
| `7fa2454` | Scripts SQL remotos en UTF-8 y reparación del mojibake que habían dejado en el hosted | pgTAP `test_42_codificacion` |
| `a9e229b` | Ítem 23: CHECK de longitud en `name`, `city`, `phone` y `bio` | pgTAP `test_40`; E2E `RlsSecurityE2eTest` (h) |
| `7c9387b` | Ítems 21, 51 y 52: `event_at`, `event_end_at` y `age_months` tipadas, rellenadas por trigger con backfill; `member_since` generado desde `created_at`; se borra `tone` | pgTAP `test_41` |
| `ab3cc8a` | Ítem 5: el moderador de `seed.sql` nace con contraseña aleatoria | `RepoHygieneTest` |
| `4c4cde6` | Ítem 18: sin listado público; triggers que borran por la Storage API y Edge Function `verify-image` (magic bytes) | pgTAP `test_43`; `deno test` de `verify-image`; E2E `StorageAuthReportsE2eTest` |
| `f54f8d9` | E2E de los ítems 7 y 23 contra el hosted | — |
| `a49d1a6` | Ítem 22: CHECK que exige coordenadas completas, en rango y distintas de (0,0), o NULL | pgTAP `test_44` |
| `1858a2d` | `docs/SEGURIDAD_BACKEND.md` con el contrato nuevo | — |

**Corrida E2E final** (`scripts/test-all.sh`, HEAD `1858a2d`): verde a la primera.

- Las 24 migraciones locales (`20261101000000`..`20261109340100`) están registradas en el hosted.
- Tras la corrida no quedaron datos de prueba: 0 usuarios e2e/tap, 0 publicaciones `tap_`/`e2e` y 0 objetos `e2e-*`.

## Los 63 ítems

Estado final y commits que resuelven cada ítem. La matriz indica la prueba que lo cubre.

| # | Hallazgo | Estado | Commits | Nota |
|---|---|---|---|---|
| 1 | Registro como moderador por metadata | Resuelto | 3525bd7 |  |
| 2 | Usuario edita sus puntos, nivel y rol | Resuelto | 3525bd7 |  |
| 3 | Autor autoaprueba su publicación | Resuelto | 4ff72f5 |  |
| 4 | Email y teléfono expuestos | Resuelto | 1165bfe |  |
| 5 | Credenciales de admin en el seed | Resuelto | 3e3311f ab3cc8a | `seed.sql` es solo local y su moderador nace con contraseña aleatoria. En el hosted la contraseña de `admin@huella.app` se rotó el 2026-11-09 a una aleatoria fuera del repo (la publicada responde 400) |
| 6 | Gamificación inexistente en servidor | Resuelto | 1db9dc9 |  |
| 7 | Notificaciones de solicitud nunca se generan | Resuelto | fee4d9f 82e523a 486ed27 f54f8d9 | La verificación independiente encontró que se podía autoaceptar por INSERT o por PATCH directo. Corregido: el estado solo cambia por `respond_to_request` |
| 8 | No se puede eliminar la cuenta | Resuelto | 75faf12 b7afaa8 |  |
| 9 | Editar publicación crea otra | Resuelto | cca090f 68b9bbc |  |
| 10 | Eliminar publicación no elimina | Resuelto | cca090f |  |
| 11 | Recuperar contraseña sin salida | Resuelto | b9f45ad 4117b91 |  |
| 12 | Horas desfasadas 5 h | Resuelto | f90c927 |  |
| 13 | Comentarios, votos, avistamientos simulados | Resuelto | 6f78a60 b808b14 |  |
| 14 | Fotos aleatorias de picsum | Resuelto | 1b7a182 d7a56da 90f312d | Opción mínima: una foto por publicación (`image_url`); varias fotos piden `image_urls TEXT[]` |
| 15 | "Finalizar adopción" siempre falla | Resuelto | 548a53d 16b1dc3 3b7943e | El diálogo dice que se marca adopción concretada y suma 50 puntos |
| 16 | Seed inconsistente con el trigger | Resuelto | 3e3311f |  |
| 17 | Reportes sin ciclo de vida | Resuelto | 064a938 796b40b 23f3a85 |  |
| 18 | Storage sin límites | Parcial | 2dfea7a 4c4cde6 | Carpeta, MIME, 5 MB y listado privado activos. Vault configurado el 2026-11-09: `verify-image` y el DELETE por la Storage API están activos (verificado en vivo: al borrar una publicación con foto su fila de `storage.objects` desaparece en la misma transacción y `net._http_response` registra el DELETE con 200). Sigue parcial porque la API responde `[]` (la fila ya no existe) y el binario queda huérfano en S3, sin URL que lo sirva (pendiente 3) |
| 19 | Sin índices | Resuelto | 625f7a6 |  |
| 20 | `conversations.updated_at` y Realtime | Parcial | 75faf12 | Trigger y publicación Realtime listos; la app no se suscribe |
| 21 | Datos semánticos como texto | Parcial | 548a53d 7c9387b | La BD ya tiene `event_at`, `event_end_at` y `age_months` tipadas (con trigger y backfill). Falta que la app las escriba, las lea y ordene o filtre por ellas |
| 22 | Ubicación (0,0) y mapa falso | Parcial | 548a53d a49d1a6 | La BD solo acepta coordenadas completas, en rango y distintas de (0,0), o NULL. El asistente no captura la ubicación y `MapScreen.toLatLng()` sigue inventando coordenadas por ciudad |
| 23 | `handle_new_user` frágil, email divergente | Resuelto | 3525bd7 a54fe96 a9e229b f54f8d9 | La verificación independiente encontró `name` sin límite al editar. Corregido con CHECK de longitud en `name`, `city`, `phone` y `bio` |
| 24 | Enum `notification_type` incompleto | Resuelto | 064a938 05ce1cb 7e0a5e6 |  |
| 25 | Sesión persistente ignorada | Resuelto | 27b568e |  |
| 26 | Expulsión silenciosa y refresh concurrente | Resuelto | 27b568e b9f45ad |  |
| 27 | `refreshAll()` dentro de cada escritura | Resuelto | 12eb6a6 |  |
| 28 | Perfil público con datos falsos | Resuelto | f2c3317 |  |
| 29 | Notificaciones solo en memoria | Resuelto | 1124d8e 7e0a5e6 |  |
| 30 | Textos corruptos (mojibake) | Resuelto | d5f9aa6 c4971ce 7fa2454 | `7fa2454` repara también el mojibake que quedó en los datos del hosted |
| 31 | Términos inexistentes | Resuelto | 43db8fd 4117b91 |  |
| 32 | Sin paginación | Resuelto | 9a58459 | Categoría, especie y ciudad se filtran en el servidor; texto, tamaño y vacunado/esterilizado en memoria sobre las páginas cargadas |
| 33 | "cerca de Medellín" y ubicación sin uso | Resuelto | 4334a8d 6867817 |  |
| 34 | Sin `.env.example` ni fail-fast | Resuelto | eb0326b 4b6d5c2 |  |
| 35 | Token en backup | Resuelto | 8ddc46a |  |
| 36 | Deep link custom sin verificación | Parcial | b9f45ad efd15c1 | Solo se acepta `code` + verifier; sigue el esquema `huella://`: App Links `https` con `autoVerify` y `assetlinks.json` requieren un dominio |
| 37 | i18n a medias | Resuelto | b7f4602 457aca6 97926ee d6bd609 a3af53e d7e67b7 8b0093f 971e524 ddd8211 0b46cea ff83497 3b7943e | Errores propios con `AppError(@StringRes)`; el texto que genera el servidor (mensajes de error de PostgREST, títulos de notificaciones) se muestra tal cual |
| 38 | Build no reproducible | Resuelto | bf7dcc3 4b6d5c2 |  |
| 39 | Tests no cubren el código real | Resuelto | e6ace74 4551da4 3371241 b9f45ad 7510689 e523f02 95cdc62 |  |
| 40 | Logging siempre activo | Resuelto | 2a07acd |  |
| 41 | Listas cortadas por la barra inferior | Resuelto | 4334a8d |  |
| 42 | Título del detalle bajo la barra de estado | Resuelto | 9c6ea0d |  |
| 43 | Caption de depuración en fotos | Resuelto | 1b7a182 6864d2a |  |
| 44 | Snackbars tapan el CTA | Resuelto | 9c6ea0d ca1c6cb 6864d2a a940b53 |  |
| 45 | Títulos truncados a una línea | Resuelto | 8f0cd92 6864d2a |  |
| 46 | FAB naranja duplicado | Resuelto | 4334a8d |  |
| 47 | Iconos repetidos y grillas desiguales | Resuelto | f927d92 | `icon_attr_castrado.xml` quedó con finales de línea CRLF (cosmético) |
| 48 | Error de términos sin mensaje, placeholder, typo | Resuelto | ca1c6cb |  |
| 49 | Splash con puntos, onboarding que salta | Resuelto | 1f48095 457a7cc |  |
| 50 | Botones estrechos y áreas táctiles < 48 dp | Resuelto | 1f48095 510d00a |  |
| 51 | Sin triggers `updated_at` | Resuelto | a9c1f16 7c9387b | `member_since` es un `smallint` generado desde `created_at`. Opcional en la app: derivarlo de `created_at` |
| 52 | `tone` ARGB en BD | Resuelto | 548a53d 7c9387b | La columna se borró. La app aún lee `tone` con un valor por defecto: falta un mapa categoría→color (limpieza de front) |
| 53 | RPC no limpia reportes ni suma puntos | Resuelto | 1db9dc9 |  |
| 54 | `FakeRepository` en producción | Resuelto | d7a56da |  |
| 55 | Interfaz que miente y mock espejo | Resuelto | 173689d |  |
| 56 | Esperas decorativas | Resuelto | ca1c6cb ec8d0f4 9a58459 ce9fc9a | Las confirmaciones se muestran en la pantalla de destino |
| 57 | Ajustes: toggles en memoria, cuenta | Resuelto | b63d561 4117b91 273334c |  |
| 58 | Accesibilidad | Resuelto | 510d00a 6864d2a c223304 0b46cea 3dfb54c |  |
| 59 | Tema solo claro | Resuelto | 0a1c668 9c2e126 | El fondo de mapa estático es una ilustración decorativa y queda clara en ambos temas |
| 60 | Inconsistencias de marca y copy | Resuelto | d4ffd44 aeb9ebb ff83497 | Excepciones deliberadas: botón atrás circular sobre la foto del detalle (necesita contraste sobre la imagen) y hora `HH:mm` en el chat |
| 61 | Google Sign-In "Próximamente" | Resuelto | ca1c6cb |  |
| 62 | Documentación desactualizada | Resuelto | c6cbb90 f31665c 849f26e 2c90ffe 94ff6fd 5f1ce1d 4bf9039 1858a2d | Estado final en `docs/REPORTE_REMEDIACION.md` |
| 63 | `.gitignore`, `.env.example`, cp1252 | Resuelto | d5f9aa6 4b6d5c2 |  |

Hay dos temas fuera de la numeración, que salieron de la revisión de seguridad de commits recientes:

- **Refresh en vuelo tras logout/login:** resuelto. Lo cubre `SupabaseRepositorySessionTest`.
- **Prompt injection en `moderate-ai`:** resuelto (`93dd941`) y desplegado: el hosted corre la versión 2.

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
| Edge Function `moderate-ai` | `ACTIVE`, **versión 3** (la de `04fcc63`), con `verify_jwt=true`. Sin JWT responde 401 y con JWT de un moderador de prueba `{"status":"ok","review":{...}}` (modelo `deepseek-chat`) |
| Edge Function `verify-image` | `ACTIVE`, versión 2, con `verify_jwt=true`. Solo acepta el service role: anon recibe 403 |
| Secrets de funciones | Los que Supabase crea por defecto y `DEEPSEEK_API_KEY` (configurada el 2026-11-09). `moderate-ai` genera la sugerencia con `deepseek-chat`; el veredicto sigue siendo solo una sugerencia en la UI |
| Vault | `huellas_project_url` y `huellas_service_role_key` cargados el 2026-11-09 con `scripts/configure-storage-vault.sh`. Los triggers llaman a la Storage API y a `verify-image` (ítem 18) |
| pg_cron | Tres jobs: `huellas_cierre_automatico` (`0 8 * * *`), `huellas_limpieza_notificaciones` (`30 8 * * *`) y `huellas_reconciliacion_storage` (`15 * * * *`) |
| Storage | Bucket `pet-images`, público, con límite de 5 MB y MIME `image/jpeg`, `image/png` o `image/webp`. Cuatro políticas, todas limitadas a la carpeta propia (ver, subir, actualizar y borrar). Ya no hay listado público |
| `adoption_requests` | Dos políticas: INSERT del solicitante y SELECT de solicitudes enviadas o recibidas. La política UPDATE del autor ya no existe. Re-aseguradas sin depender de datos por `20261109350000` (`test_45`) |
| Realtime | La publicación `supabase_realtime` incluye `chat_messages` y `notifications`. La app todavía no se suscribe (ítem 20) |
| Auth | 6 usuarios: 4 de semilla (`admin@huella.app`, que es moderador y tiene la contraseña rotada, y laura, carlos y sofia) y 2 cuentas gmail del desarrollador. La confirmación de correo está activa |

## Pendiente o que requiere decisión humana

1. **Moderador semilla del hosted (ítem 5): hecho el 2026-11-09.** Contraseña de `admin@huella.app` rotada con
   `PUT /auth/v1/admin/users/<id>` a una aleatoria de 24 caracteres guardada fuera del repo; la publicada responde 400
   y la nueva 200. Ninguna prueba del repo usa esa credencial.
   - Laura, carlos y sofia tienen el mismo problema, pero sin rol de moderador. Las 2 cuentas gmail del desarrollador
     no se tocaron.
2. **Secretos de Vault (ítem 18): hechos el 2026-11-09** con `scripts/configure-storage-vault.sh`. Verificado en
   vivo borrando una publicación de prueba con foto: la fila de `storage.objects` desaparece en la misma transacción
   y `net._http_response` registra el DELETE a la Storage API (200). El binario sigue el límite del punto 3.
3. **Seguimiento de la revisión automática de dos migraciones: cerrado, con un límite abierto.**
   - `20261109300100_reparar_codificacion.sql`: `20261109350000` (`f1d0457`, aplicada) re-asegura sin depender de
     datos los privilegios (solo SELECT e INSERT de `publication_id`, `applicant_id`, `message`) y exactamente las
     dos políticas. `test_45_politicas_solicitudes` lo exige. La conversión de datos no se repite: en un entorno
     donde fallara, `apply-migrations` se detiene en `300100` y una copia posterior no llegaría a correr.
   - `20261109330000_storage_por_api.sql`: `20261109350100` (`3b256a3`, aplicada) borra también las filas de
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
   - `moderate-ai` (`04fcc63`, desplegada como versión 3): ya no envía `phone`, un `risk` ausente o inválido queda en
     0.5 y la heurística detecta "venta", "vendo", "mil pesos", "COP", "$" con cifra y "precio" sin marcar
     "ventana". Lo cubre `review_test.ts`.
   - `20261109350200` (`871f26a`, aplicada) retira EXECUTE a PUBLIC, anon y authenticated de todas las funciones de
     trigger SECURITY DEFINER de public. Lo exige `test_47_triggers_sin_execute`.
5. **Key de DeepSeek: configurada el 2026-11-09.** Verificado con un moderador temporal (borrado al terminar):
   `POST /functions/v1/moderate-ai` devuelve `{"status":"ok","review":{...}}` y `ModerateAiE2eTest` pasa 3/3.
   - Se desactiva con `supabase secrets unset DEEPSEEK_API_KEY --project-ref <ref>`.
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
   `scripts/configure-storage-vault.sh` para cargar los secretos de Vault (en el hosted de pruebas ya están).
2. Desplegar `moderate-ai` y `verify-image` en cada entorno nuevo (en el hosted de pruebas ya están).
3. Rotar las cuentas semilla que siguen con contraseña publicada (laura, carlos y sofia; la de admin ya está rotada).
4. Generar el APK de release con `app/.env` apuntando al proyecto real.

Son 131 commits, demasiados para revisarlos uno por uno. Es más práctico recorrerlos por ítem con la matriz.
