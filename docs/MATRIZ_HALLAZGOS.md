# Matriz de hallazgos: auditoría 2026-11

Estado de los 63 ítems de la auditoría técnica del 2026-11-08 (commit base `04713a4`) en la rama
`fix/auditoria-2026-11`. Commits según `git log` (sha corto). Pruebas:
pgTAP = `supabase/tests/*.sql` (`tests.test_*`), E2E = `app/src/test/.../e2e/*E2eTest` (contra Supabase),
y el resto son pruebas JVM/Robolectric de `app/src/test`. "—" = sin prueba automatizada.

Resumen: **51 resueltos, 11 parciales, 1 pendiente.**

Cobertura (2026-11-08, `scripts/test-all.sh` contra el hosted): pgTAP 14/14 tests (170/170 aserciones) y JVM
86/86 (0 saltados, 0 fallos; e2e incluidos). Todo ítem Alta/Media tiene al menos una prueba salvo el 32
(pendiente, no hay paginación que probar) y los visuales 42 y 47 (insets y cambio de icono: revisión visual).

| # | Hallazgo | Estado | Commits | Prueba que lo cubre |
|---|---|---|---|---|
| 1 | Registro como moderador por metadata | Resuelto | fe98070 | pgTAP `test_12_handle_new_user`; E2E `RlsSecurityE2eTest` (c) |
| 2 | Usuario edita sus puntos, nivel y rol | Resuelto | fe98070 | pgTAP `test_11_proteccion_perfil`; E2E `RlsSecurityE2eTest` (b) |
| 3 | Autor autoaprueba su publicación | Resuelto | 68e8259 | pgTAP `test_13_publicaciones`; E2E `RlsSecurityE2eTest` (d) |
| 4 | Email y teléfono expuestos | Resuelto | 63a1f8a | pgTAP `test_10_grants_y_perfiles`; E2E `RlsSecurityE2eTest` (a) |
| 5 | Credenciales de admin en el seed | Parcial | 3510bef | `RepoHygieneTest` (migraciones sin usuarios de auth) · El seed ya es solo local, pero el hosted conserva `admin@huella.app` con la contraseña publicada: rotarla o borrar la cuenta |
| 6 | Gamificación inexistente en servidor | Resuelto | ca63b9b | pgTAP `test_20_puntos_y_moderacion` |
| 7 | Notificaciones de solicitud nunca se generan | Resuelto | 58f17c3 504dbba | pgTAP `test_30_solicitudes` |
| 8 | No se puede eliminar la cuenta | Resuelto | 13a908b 1529b06 | pgTAP `test_32_eliminar_cuenta`; E2E `BackendFlowsE2eTest`; E2E `RepositoryFlowsE2eTest` |
| 9 | Editar publicación crea otra | Resuelto | 88770ff | pgTAP `test_13_publicaciones` (edición vuelve a revisión); E2E `RepositoryFlowsE2eTest` |
| 10 | Eliminar publicación no elimina | Resuelto | 88770ff | pgTAP `test_13_publicaciones` (el autor borra); E2E `RepositoryFlowsE2eTest`; E2E `StorageAuthReportsE2eTest` (un tercero no borra) |
| 11 | Recuperar contraseña sin salida | Resuelto | 0b436ba 76799ba | `SupabaseRepositoryAuthTest` (PKCE); E2E `RepositoryFlowsE2eTest` (cambio de contraseña); E2E `StorageAuthReportsE2eTest` (canje PKCE); E2E `RlsSecurityE2eTest` (c, signup con code_challenge) |
| 12 | Horas desfasadas 5 h | Resuelto | 60c95cd | `DateParsingTest`; E2E `RepositoryFlowsE2eTest` ("ahora" en una publicación recién creada) |
| 13 | Comentarios, votos, avistamientos simulados | Resuelto | ace9317 1178f6f | pgTAP `test_21_comentarios_votos_avistamientos`; E2E `BackendFlowsE2eTest`; E2E `RepositoryFlowsE2eTest` |
| 14 | Fotos aleatorias de picsum | Parcial | dab166d 414a1cc | `UxScreensRobolectricTest` (solo foto real) · El asistente sube todas las fotos pero solo se guarda la primera (no hay columna para varias) |
| 15 | "Finalizar adopción" siempre falla | Resuelto | b90e536 1d9643c | pgTAP `test_20_puntos_y_moderacion` (finalizar); E2E `BackendFlowsE2eTest`; E2E `RepositoryFlowsE2eTest` |
| 16 | Seed inconsistente con el trigger | Resuelto | 3510bef | `RepoHygieneTest` (cada perfil del seed con DO UPDATE) |
| 17 | Reportes sin ciclo de vida | Resuelto | 51a8d76 f79bd2f 4678d05 | pgTAP `test_15_reportes`; E2E `StorageAuthReportsE2eTest`; E2E `RepositoryFlowsE2eTest` (resolver) |
| 18 | Storage sin límites | Resuelto | 0713158 | pgTAP `test_14_storage`; E2E `RlsSecurityE2eTest` (e); E2E `StorageAuthReportsE2eTest` (MIME, 5 MB, anónimo); E2E `RepositoryFlowsE2eTest` (subida de la app) |
| 19 | Sin índices | Resuelto | a380c37 | pgTAP `test_31_chat` (índices; toda FK con índice) |
| 20 | `conversations.updated_at` y Realtime | Parcial | 13a908b | pgTAP `test_31_chat` · Trigger y publicación Realtime listos; la app no se suscribe |
| 21 | Datos semánticos como texto | Parcial | b90e536 | pgTAP `test_31_chat` (`group_label` sin default) · `event_date`, `schedule`, `age`, `member_since` siguen TEXT |
| 22 | Ubicación (0,0) y mapa falso | Parcial | b90e536 | pgTAP `test_20_puntos_y_moderacion` (latitud NULL); E2E `RepositoryFlowsE2eTest` · El asistente no captura ubicación y `MapScreen.toLatLng()` sigue inventando coordenadas por ciudad |
| 23 | `handle_new_user` frágil, email divergente | Resuelto | fe98070 83b97dd | pgTAP `test_12_handle_new_user`; E2E `RlsSecurityE2eTest` (f) |
| 24 | Enum `notification_type` incompleto | Resuelto | 51a8d76 a262fbf 8029365 | pgTAP `test_16_esquema` |
| 25 | Sesión persistente ignorada | Resuelto | 88a8f31 | E2E `RepositoryFlowsE2eTest` (`restoreSession` con el token guardado) |
| 26 | Expulsión silenciosa y refresh concurrente | Resuelto | 88a8f31 0b436ba | `SupabaseRepositoryAuthTest` (dos 401, refresh rechazado) |
| 27 | `refreshAll()` dentro de cada escritura | Resuelto | ae8b8e5 | `SupabaseRepositoryWritesTest` |
| 28 | Perfil público con datos falsos | Resuelto | dfdcaa4 | E2E `RepositoryFlowsE2eTest` (`refreshUser`: nombre y puntos reales) |
| 29 | Notificaciones solo en memoria | Resuelto | 2622725 8029365 | `NotificationGroupingTest`; pgTAP `test_30_solicitudes` (target_id); E2E `RepositoryFlowsE2eTest` (leída y borrada persisten) |
| 30 | Textos corruptos (mojibake) | Resuelto | 95629a6 489a95f | `RepoHygieneTest` (sin mojibake en `app/src/main`) |
| 31 | Términos inexistentes | Resuelto | f9e7ad7 76799ba | `UxScreensRobolectricTest` (registro sin términos) |
| 32 | Sin paginación | Pendiente | — | — |
| 33 | "cerca de Medellín" y ubicación sin uso | Parcial | bdfc2bd | `RepoHygieneTest` (sin "cerca de Medellín") · Filtro de distancia decorativo y permiso de ubicación sin uso siguen |
| 34 | Sin `.env.example` ni fail-fast | Resuelto | 2f6420e e079499 | `SupabaseConfigTest` |
| 35 | Token en backup | Resuelto | e7e7b3f | `RepoHygieneTest` (allowBackup y exclusiones) |
| 36 | Deep link custom sin verificación | Resuelto | 0b436ba | `SupabaseRepositoryAuthTest`; E2E `StorageAuthReportsE2eTest` (canje PKCE) · Mitigado con PKCE (el `code` interceptado no sirve sin el verifier); sigue el esquema `huella://` |
| 37 | i18n a medias | Resuelto | 79acb44 0175dcc b27c6db 34e90dc 972c9eb b352dc4 db2b772 7dbd5e2 70d1fdd 4748168 | `EnglishLocaleRobolectricTest` |
| 38 | Build no reproducible | Resuelto | b3ae855 e079499 | `RepoHygieneTest` (sin `org.gradle.java.home`); build con `scripts/gradle-native.sh` |
| 39 | Tests no cubren el código real | Resuelto | 1bccf9a abf84fa c995e5b 0b436ba bd80e58 5afc6bb | `SupabaseRepositoryAuthTest`, `SupabaseRepositorySessionTest`, `SupabaseRepositoryWritesTest`, E2E `RepositoryFlowsE2eTest`, E2E, pgTAP |
| 40 | Logging siempre activo | Resuelto | 5b8c43e | `RepoHygieneTest` (logging solo con `BuildConfig.DEBUG`) |
| 41 | Listas cortadas por la barra inferior | Resuelto | bdfc2bd | `UxScreensRobolectricTest` (margen inferior) |
| 42 | Título del detalle bajo la barra de estado | Resuelto | b66e5c1 | — |
| 43 | Caption de depuración en fotos | Resuelto | dab166d 0b85310 | `UxScreensRobolectricTest` (sin caption) |
| 44 | Snackbars tapan el CTA | Resuelto | b66e5c1 d43644a 0b85310 | `ModerationDetailViewModelTest` (error inline) |
| 45 | Títulos truncados a una línea | Resuelto | 3997c76 0b85310 | `VisualItemsRobolectricTest` (título de tarjeta a dos líneas) |
| 46 | FAB naranja duplicado | Resuelto | bdfc2bd | `UxScreensRobolectricTest` (home sin FAB) |
| 47 | Iconos repetidos y grillas desiguales | Resuelto | 5e3a826 | — |
| 48 | Error de términos sin mensaje, placeholder, typo | Resuelto | d43644a | `UxScreensRobolectricTest`; `LoginScreenRobolectricTest` |
| 49 | Splash con puntos, onboarding que salta | Resuelto | 6d5053f | `VisualItemsRobolectricTest` (título del onboarding estable) |
| 50 | Botones estrechos y áreas táctiles < 48 dp | Resuelto | 6d5053f 1c6b3bb | `VisualItemsRobolectricTest` (Omitir y Ver perfil de 48 dp) |
| 51 | Sin triggers `updated_at` | Parcial | a725ec6 | pgTAP `test_16_esquema` · `member_since` sigue TEXT |
| 52 | `tone` ARGB en BD | Parcial | b90e536 | — · Marcado obsoleto; la columna sigue |
| 53 | RPC no limpia reportes ni suma puntos | Resuelto | ca63b9b | pgTAP `test_20_puntos_y_moderacion` |
| 54 | `FakeRepository` en producción | Resuelto | 414a1cc | Suite `:app:testDebugUnitTest` (usa `FakeRepository` desde `app/src/test`) |
| 55 | Interfaz que miente y mock espejo | Resuelto | 9a3577f | `SupabaseEndpointsMockTest` |
| 56 | Esperas decorativas | Parcial | d43644a 63f4c1d | — · Login/Registro/Perfil sin esperas; Home mantiene `delay(600)` del skeleton |
| 57 | Ajustes: toggles en memoria, cuenta | Parcial | cf2c0bb 76799ba | `UxScreensRobolectricTest` (cuenta y eliminar cuenta) · Toggles de notificaciones solo en memoria, sin push |
| 58 | Accesibilidad | Resuelto | 1c6b3bb 0b85310 68ea6b5 4748168 | `ModerationDetailScreenRobolectricTest`, `UxScreensRobolectricTest` |
| 59 | Tema solo claro | Resuelto | 8fdd357 | — |
| 60 | Inconsistencias de marca y copy | Parcial | 40d2bf4 | — · Anglicismo "badge" corregido; "Huella"/"Huellas" y formatos de hora mixtos siguen |
| 61 | Google Sign-In "Próximamente" | Resuelto | d43644a | — |
| 62 | Documentación desactualizada | Resuelto | aaaea84 947e1e2 (+ commit de esta matriz) | — |
| 63 | `.gitignore`, `.env.example`, cp1252 | Resuelto | 95629a6 e079499 | — |
