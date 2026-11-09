# Seguridad del backend: qué cambia para el cliente

Migraciones `supabase/migrations/20261109010000` a `20261109010500` (ítems 1, 2, 3, 4, 17, 18, 23, 24 y 51 de la auditoría)
y `20261109300000` a `20261109340100` (cierre de 7, 18, 21, 22, 23, 51 y 52).
Pruebas: `supabase/tests/10_security.sql` y `40_cierre_auditoria.sql` (pgTAP), `RlsSecurityE2eTest` y `StorageAuthReportsE2eTest`.

## profiles
- `anon` y `authenticated` solo leen estas columnas: `id, name, city, level, initials, points, member_since, avatar_url,
  is_moderator, publications_count, adoptions_count, foster_count, active_count, created_at`.
- **`select=*` sobre `profiles` falla** con `42501 permission denied for table profiles` (HTTP 401/403); no excluye
  columnas en silencio. Lo mismo un embed que pida `email`, `phone` o `bio`.
  - Perfil propio completo: `POST rest/v1/rpc/get_my_profile` (devuelve una fila con email, phone y bio).
  - Teléfono del otro participante de un chat: `POST rest/v1/rpc/get_contact_phone {"p_conversation_id": "..."}`
    (`null` si no participas).
  - Embeds que siguen funcionando: `profiles!publications_author_id_fkey(id,name,level,initials)`,
    `profiles!adoption_requests_applicant_id_fkey(name,level,initials)`,
    `participant_one_profile:profiles!conversations_participant_one_fkey(id,name,level,initials)` (sin `phone`).
- `PATCH profiles` solo acepta `name, city, phone, bio, avatar_url, initials`, con tope: `name` 1..80 (sin blancos),
  `city` 80, `phone` 30, `bio` 500 caracteres (`23514` → HTTP 400 si se pasa). Con `Prefer: return=representation`
  hay que pasar `select=` con columnas públicas. `points`, `level`, `is_moderator`, contadores y `email` devuelven 42501.
- El email se cambia con `PUT auth/v1/user {"email": ...}`; al confirmarse, un trigger lo copia a `profiles.email`.
- El signup ignora `is_moderator` del metadata. Los moderadores se promueven con service_role.
- `member_since` es un `smallint` calculado de `created_at` (año, America/Bogota); no se escribe.

## publications
- El autor crea y edita; el servidor fuerza `status = EN_REVISION` al crear y cada vez que cambia el contenido.
  `author_id`, `created_at`, `lifecycle`, `votes_count` y `comments_count` no se pueden cambiar desde el cliente.
- Aprobar o rechazar solo por `rpc/moderate_publication`; un `PATCH status` del moderador no afecta filas.
- `DELETE publications?id=eq.<id>` del autor borra también sus fotos de `pet-images` citadas en la fila.
- `lifecycle` (`ACTIVA` | `FINALIZADA`), por defecto `ACTIVA`.
- Tipados: `event_at`/`event_end_at` (timestamptz) y `age_months` (int). Si el cliente solo envía `event_date`,
  `schedule` o `age` (texto para mostrar), un trigger los deriva; si envía el tipado, se respeta.
- Ubicación: `latitude` y `longitude` juntas, en rango y distintas de (0,0), o ambas `NULL` ("sin ubicación").
- Ya no existe `tone`: el color de la tarjeta lo decide la app según la categoría.

## reports
- Un reporte por usuario y publicación: el segundo devuelve 409 (`23505`). El cliente solo envía
  `publication_id, reporter_id, reason, detail`.
- Solo moderadores ven la cola y la cierran con `rpc/resolve_report {"p_report_id": "..."}` (`status` pasa a `RESUELTO`).

## Storage `pet-images`
- Lectura pública por `/storage/v1/object/public/pet-images/...` (bucket public). Listar: solo la carpeta propia
  (anon no lista nada). Subir, sobrescribir y borrar solo en `<auth.uid()>/...`; máximo 5 MB; `image/jpeg`,
  `image/png`, `image/webp`.
- Cada subida pasa por la Edge Function `verify-image` (trigger `verify_pet_image` + pg_net): si los primeros bytes no
  son JPEG/PNG/WebP real, el objeto se borra unos segundos después aunque el Content-Type dijera `image/jpeg`.
- Borrar una publicación o la cuenta borra sus fotos por la Storage API (fila y binario en S3).
- Ambas cosas necesitan, una vez por proyecto, los secretos de Vault `huellas_project_url` y
  `huellas_service_role_key` (`scripts/configure-storage-vault.sh`) y la función desplegada
  (`supabase functions deploy verify-image --use-api`). Sin ellos solo se borra la fila (el binario queda en S3).

## Otros
- `anon` solo lee `publications` (aprobadas, por RLS) y las columnas públicas de `profiles`.
- `adoption_requests`: el cliente solo inserta `publication_id, applicant_id, message` sobre publicaciones aprobadas,
  activas y ajenas (nace `PENDIENTE`). Nadie actualiza la fila: el autor responde con `rpc/respond_to_request`.
- `notifications`: el dueño puede marcar `read` y borrar las suyas (`DELETE notifications?id=eq.<id>`).
- `notification_type` incluye `REQUEST_REJECTED, NEW_COMMENT, NEW_VOTE, BADGE_EARNED, SIGHTING`.
- Todas las tablas con `updated_at` lo mantienen por trigger (`set_updated_at`).
- Funciones internas que deban escribir campos protegidos: `SECURITY DEFINER` (corren como su dueño) o
  `set_config('huella.internal', '1', true)`. Las RPC nuevas deben hacer `REVOKE ALL ... FROM PUBLIC, anon`:
  los default privileges del proyecto dan EXECUTE a `anon` directamente, no vía PUBLIC.
