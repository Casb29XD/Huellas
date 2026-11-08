# Reporte de remediación: auditoría 2026-11

Rama `fix/auditoria-2026-11` sobre el commit base `04713a4` (auditoría técnica del 2026-11-08, 63 hallazgos).
Fecha del reporte: 2026-11-08. La prueba que cubre cada ítem está en `docs/MATRIZ_HALLAZGOS.md`.

## Resumen ejecutivo

- **Alcance:** 116 commits (`git log fix/auditoria-2026-11 ^04713a4`, contando este) y 159 archivos cambiados,
  con unas +9 100 y −2 800 líneas. Se añadieron 13 migraciones append-only a las 3 que ya existían, además de pruebas
  pgTAP, E2E contra Supabase y Robolectric, y la Edge Function `moderate-ai`.
- **Estado final de los 63 ítems:** 56 resueltos, 7 parciales (5, 20, 21, 22, 36, 51, 52) y 0 pendientes.
  Los ítems de seguridad y pérdida de datos del backend (1-8) quedan cerrados en el servidor. La excepción es el 5:
  la cuenta `admin@huella.app` del hosted todavía tiene la contraseña que se publicó.
- **Funciones simuladas que ahora son reales:** editar, eliminar, finalizar adopción, comentarios, votos,
  avistamientos, puntos, notificaciones y eliminar cuenta pasan por PostgREST, RPC y triggers. Las pruebas E2E las
  ejercitan con el `SupabaseRepository` real.
- **Pruebas:** la última corrida completa contra el hosted (`scripts/test-all.sh`, exit 0) dio:
  - pgTAP: 14/14 tests y 170/170 aserciones.
  - `deno test` de `moderate-ai`: 4/4.
  - JVM: 114/114, con las 20 E2E incluidas, sin saltos ni fallos.

  Al cerrar este reporte se repitió `:app:testDebugUnitTest --rerun` sin credenciales: 114 tests, 94 pasan,
  20 E2E saltadas por `Assume` y 0 fallos.
- **Pendiente operativo:**
  - Desplegar la versión endurecida de `moderate-ai`. El hosted sigue con la versión 1.
  - Configurar `DEEPSEEK_API_KEY`.
  - Decidir qué hacer con las cuentas semilla.

### Cómo se llegó aquí

1. Remediación por bloques: backend y RLS, gamificación, front, i18n, visual y documentación. Cada commit es atómico
   y cita el ítem que resuelve.
2. Verificación adversarial en solo lectura sobre `86a4197`. Encontró que el ítem 9 seguía roto: "Editar" abría el
   asistente vacío y creaba otra publicación. También dejó parciales los ítems 14, 15, 32, 33, 36, 37, 44, 49 y 56-60.
   Los problemas eran esperas fijas, interruptores que solo vivían en memoria, un diálogo de cierre con información
   falsa y fotos huérfanas.
3. Reparación de esos ítems (`19ff386`..`de9dac9`), endurecimiento de `moderate-ai` (`e6104ac`) y pruebas para
   los ítems 42 y 47 (`4e4082c`).
4. Corrida completa contra el hosted y matriz actualizada (`5834397`).

## Los 63 ítems

Estado final y commits que resuelven cada ítem. La matriz indica la prueba que lo cubre.

| # | Hallazgo | Estado | Commits | Nota |
|---|---|---|---|---|
| 1 | Registro como moderador por metadata | Resuelto | fe98070 |  |
| 2 | Usuario edita sus puntos, nivel y rol | Resuelto | fe98070 |  |
| 3 | Autor autoaprueba su publicación | Resuelto | 68e8259 |  |
| 4 | Email y teléfono expuestos | Resuelto | 63a1f8a |  |
| 5 | Credenciales de admin en el seed | Parcial | 3510bef | El seed ya es solo local, pero el hosted conserva `admin@huella.app` con la contraseña publicada: rotarla o borrar la cuenta |
| 6 | Gamificación inexistente en servidor | Resuelto | ca63b9b |  |
| 7 | Notificaciones de solicitud nunca se generan | Resuelto | 58f17c3 504dbba |  |
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
| 18 | Storage sin límites | Resuelto | 0713158 |  |
| 19 | Sin índices | Resuelto | a380c37 |  |
| 20 | `conversations.updated_at` y Realtime | Parcial | 13a908b | Trigger y publicación Realtime listos; la app no se suscribe |
| 21 | Datos semánticos como texto | Parcial | b90e536 | `event_date`, `schedule`, `age`, `member_since` siguen TEXT |
| 22 | Ubicación (0,0) y mapa falso | Parcial | b90e536 | El asistente no captura ubicación y `MapScreen.toLatLng()` sigue inventando coordenadas por ciudad |
| 23 | `handle_new_user` frágil, email divergente | Resuelto | fe98070 83b97dd |  |
| 24 | Enum `notification_type` incompleto | Resuelto | 51a8d76 a262fbf 8029365 |  |
| 25 | Sesión persistente ignorada | Resuelto | 88a8f31 |  |
| 26 | Expulsión silenciosa y refresh concurrente | Resuelto | 88a8f31 0b436ba |  |
| 27 | `refreshAll()` dentro de cada escritura | Resuelto | ae8b8e5 |  |
| 28 | Perfil público con datos falsos | Resuelto | dfdcaa4 |  |
| 29 | Notificaciones solo en memoria | Resuelto | 2622725 8029365 |  |
| 30 | Textos corruptos (mojibake) | Resuelto | 95629a6 489a95f |  |
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
| 51 | Sin triggers `updated_at` | Parcial | a725ec6 | `member_since` sigue TEXT |
| 52 | `tone` ARGB en BD | Parcial | b90e536 | Marcado obsoleto; la columna sigue |
| 53 | RPC no limpia reportes ni suma puntos | Resuelto | ca63b9b |  |
| 54 | `FakeRepository` en producción | Resuelto | 414a1cc |  |
| 55 | Interfaz que miente y mock espejo | Resuelto | 9a3577f |  |
| 56 | Esperas decorativas | Resuelto | d43644a 63f4c1d d2c05b6 561fe4c | Las confirmaciones se muestran en la pantalla de destino |
| 57 | Ajustes: toggles en memoria, cuenta | Resuelto | cf2c0bb 76799ba 1f94f91 |  |
| 58 | Accesibilidad | Resuelto | 1c6b3bb 0b85310 68ea6b5 4748168 a5a3222 |  |
| 59 | Tema solo claro | Resuelto | 8fdd357 8c52bee | El fondo de mapa estático es una ilustración decorativa y queda clara en ambos temas |
| 60 | Inconsistencias de marca y copy | Resuelto | 40d2bf4 9d251c3 84c7efd | Excepciones deliberadas: botón atrás circular sobre la foto del detalle (necesita contraste sobre la imagen) y hora `HH:mm` en el chat |
| 61 | Google Sign-In "Próximamente" | Resuelto | d43644a |  |
| 62 | Documentación desactualizada | Resuelto | aaaea84 947e1e2 5cd2266 86a4197 de9dac9 5834397 | Estado final en `docs/REPORTE_REMEDIACION.md` |
| 63 | `.gitignore`, `.env.example`, cp1252 | Resuelto | 95629a6 e079499 |  |

Hay dos temas fuera de la numeración, que salieron de la revisión de seguridad de commits recientes:

- **Refresh en vuelo tras logout/login:** resuelto. Lo cubre `SupabaseRepositorySessionTest`.
- **Prompt injection en `moderate-ai`:** resuelto en el código (`e6104ac`). Falta desplegarlo.

## Pruebas: qué existen y cómo correrlas

| Suite | Dónde vive | Qué cubre |
|---|---|---|
| pgTAP | `supabase/tests/*.sql` (funciones `tests.test_*`: 14 tests, 170 aserciones) | Grants; RLS de perfiles y publicaciones; `handle_new_user`; storage; reportes; esquema; puntos y moderación; comentarios, votos y avistamientos; solicitudes; chat e índices; eliminar cuenta |
| Edge Function | `supabase/functions/moderate-ai/review_test.ts` (`deno test`) | Delimitadores contra prompt injection, validación del esquema de respuesta, heurística de venta |
| JVM unitarias | `app/src/test` | Repositorio (auth/PKCE, sesión, escrituras, paginación), fechas, tiempos relativos, ViewModels, contraste, higiene del repo (`RepoHygieneTest`) |
| Robolectric + compose-ui-test | `app/src/test/.../ui` | Login, Home, detalle, registro, ajustes, moderación, inglés, modo oscuro, ítems visuales |
| E2E (20 pruebas) | `app/src/test/.../e2e/*E2eTest` | `SmokeE2eTest`, `RlsSecurityE2eTest` (comprueba que los exploits de la auditoría están bloqueados), `BackendFlowsE2eTest`, `RepositoryFlowsE2eTest`, `StorageAuthReportsE2eTest`, `ModerateAiE2eTest` |

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

Tras la última corrida no quedaron residuos. Se volvió a comprobar para este reporte: 0 usuarios `@huella.test`
y 0 objetos `e2e-*`.

Herramientas necesarias: `curl`, `python` y `deno` en el PATH, más JDK 17+ y el Android SDK. En lugar del JDK y el
SDK sirve el toolchain portable de `HUELLAS_TOOLS`.

**Local (opcional, con Docker).** `scripts/test-all.sh --local` hace lo siguiente con el Supabase CLI:

1. Corre `supabase db reset`, que también aplica `supabase/seed.sql` (un seed solo para local).
2. Carga pgTAP con `psql`.
3. Corre Gradle en la imagen de `scripts/gradle-docker.sh`.

Más detalle en `docs/PRUEBAS.md`.

## Estado real del proyecto hosted

Consultado en solo lectura el 2026-11-08 (proyecto `neuvyswxhibgcnezxxql`):

| Recurso | Estado |
|---|---|
| Migraciones | 16 registradas en `supabase_migrations.schema_migrations`; la última es `20261109160000`. Son las mismas 16 de `supabase/migrations/`, no falta ninguna |
| Edge Function `moderate-ai` | `ACTIVE`, **versión 1**, con `verify_jwt=true`. Sin token responde 401. No incluye los cambios de `e6104ac` porque el permiso de despliegue se denegó en esta sesión |
| Secrets de funciones | Solo los que Supabase crea por defecto. **No hay `DEEPSEEK_API_KEY`**, así que la función responde `{"status":"not_configured"}` y la UI lo muestra |
| pg_cron | Dos jobs activos: `huellas_cierre_automatico` (`0 8 * * *`) y `huellas_limpieza_notificaciones` (`30 8 * * *`) |
| Storage | Bucket `pet-images`, público, con límite de 5 MB y MIME `image/jpeg`, `image/png` o `image/webp`. Las políticas restringen cada usuario a su propia carpeta |
| Realtime | La publicación `supabase_realtime` incluye `chat_messages` y `notifications`. La app todavía no se suscribe (ítem 20) |
| Auth | 6 usuarios: 4 de semilla (`admin@huella.app`, que es moderador, y laura, carlos y sofia) y 2 cuentas gmail del desarrollador. La confirmación de correo está activa |

## Pendiente o que requiere decisión humana

1. **Desplegar `moderate-ai`.** Alguien con permiso sobre el proyecto tiene que ejecutar
   `supabase functions deploy moderate-ai --use-api --project-ref <ref>`. `ModerateAiE2eTest` pasa con las dos
   versiones de la función.
2. **Key de DeepSeek.** Falta decidir si se activa la sugerencia de IA:
   - Se activa con `supabase secrets set DEEPSEEK_API_KEY=... --project-ref <ref>` y, si se quiere, `DEEPSEEK_MODEL`.
   - Activarla manda el contenido de cada publicación a un tercero, así que conviene revisarlo contra la política
     de datos.
   - Detalles en `docs/MODERACION_IA.md`.
3. **Cuentas de prueba en el hosted (ítem 5).** Las contraseñas de `admin@huella.app` (moderador), laura, carlos
   y sofia estuvieron publicadas en el repo. Hay que rotarlas o borrar las cuentas desde el dashboard antes de
   cualquier uso real. Las 2 cuentas gmail del desarrollador no se tocaron.
4. **Credenciales del seed.** `supabase/seed.sql` todavía tiene contraseñas en claro:
   - Solo se usa en local, con `supabase db reset`.
   - `RepoHygieneTest` impide que las migraciones creen usuarios de auth.
   - Las contraseñas siguen en el historial de git. Sacarlas exige reescribir el historial, y esa decisión le
     corresponde al dueño del repo.
5. **App Links https (ítem 36).** Hoy solo se acepta `code` PKCE con el verifier local, por el esquema `huella://`.
   Para pasar a App Links verificados hacen falta tres cosas:
   - Un dominio que sirva `/.well-known/assetlinks.json` con el SHA-256 del certificado de firma.
   - `autoVerify=true` en el manifest.
   - Las URL de redirección registradas en Supabase Auth.
6. **Parciales de esquema y app:**
   - 20: la app no se suscribe a Realtime.
   - 21 y 51: `event_date`, `schedule`, `age` y `member_since` siguen siendo TEXT.
   - 22: el asistente no captura la ubicación y `MapScreen.toLatLng()` inventa coordenadas por ciudad.
   - 52: la columna `tone` está marcada como obsoleta pero sigue en el esquema.
7. **Limitaciones aceptadas:**
   - Una sola foto por publicación. Permitir varias pide una columna `image_urls TEXT[]`.
   - Los textos que genera el servidor (errores de PostgREST, títulos de notificaciones) se muestran sin traducir.
   - `SupabaseRepository.login()` mantiene un `delay(350)`. No es decorativo: reintenta mientras el trigger crea
     el perfil.
   - `icon_attr_castrado.xml` tiene finales de línea CRLF.
8. **Trailer de los commits.** La rama usa `Co-Authored-By: Claude Opus 5.5`, que es lo que indicó la atribución
   del harness. `ENTORNO_REMOTO.md` pide `Claude Fable 5.1`. Si se prefiere ese texto, los trailers se reescriben
   con un rebase antes de publicar.
9. **Sin CI.** Las pruebas se corren a mano con `scripts/test-all.sh`. Un pipeline necesitaría secretos del hosted
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

1. Desplegar `moderate-ai` (pendiente 1).
2. En cada entorno, correr `scripts/apply-migrations-remote.sh` para confirmar que no falta ninguna migración.
3. Rotar las cuentas semilla (pendiente 3).
4. Generar el APK de release con `app/.env` apuntando al proyecto real.

Son 116 commits, demasiados para revisarlos uno por uno. Es más práctico recorrerlos por ítem con la matriz.
