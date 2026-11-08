# Seguridad del backend: qué cambia para el cliente

Migraciones `supabase/migrations/20261109010000` a `20261109010500` (ítems 1, 2, 3, 4, 17, 18, 23, 24 y 51 de la auditoría).
Pruebas: `supabase/tests/10_security.sql` (pgTAP) y `RlsSecurityE2eTest`.

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
- `PATCH profiles` solo acepta `name, city, phone, bio, avatar_url, initials`. Con `Prefer: return=representation`
  hay que pasar `select=` con columnas públicas. `points`, `level`, `is_moderator`, contadores y `email` devuelven 42501.
- El email se cambia con `PUT auth/v1/user {"email": ...}`; al confirmarse, un trigger lo copia a `profiles.email`.
- El signup ignora `is_moderator` del metadata. Los moderadores se promueven con service_role.

## publications
- El autor crea y edita; el servidor fuerza `status = EN_REVISION` al crear y cada vez que cambia el contenido.
  `author_id`, `created_at`, `lifecycle`, `votes_count` y `comments_count` no se pueden cambiar desde el cliente.
- Aprobar o rechazar solo por `rpc/moderate_publication`; un `PATCH status` del moderador no afecta filas.
- `DELETE publications?id=eq.<id>` del autor borra también sus fotos de `pet-images` citadas en la fila.
- `lifecycle` (`ACTIVA` | `FINALIZADA`), por defecto `ACTIVA`.

## reports
- Un reporte por usuario y publicación: el segundo devuelve 409 (`23505`). El cliente solo envía
  `publication_id, reporter_id, reason, detail`.
- Solo moderadores ven la cola y la cierran con `rpc/resolve_report {"p_report_id": "..."}` (`status` pasa a `RESUELTO`).

## Storage `pet-images`
- Lectura pública. Subir, sobrescribir y borrar solo en `<auth.uid()>/...`; máximo 5 MB; `image/jpeg`, `image/png`, `image/webp`.

## Otros
- `anon` solo lee `publications` (aprobadas, por RLS) y las columnas públicas de `profiles`.
- `adoption_requests`: el autor de la publicación solo puede actualizar `status`.
- `notifications`: el dueño puede marcar `read` y borrar las suyas (`DELETE notifications?id=eq.<id>`).
- `notification_type` incluye `REQUEST_REJECTED, NEW_COMMENT, NEW_VOTE, BADGE_EARNED, SIGHTING`.
- Todas las tablas con `updated_at` lo mantienen por trigger (`set_updated_at`).
- Funciones internas que deban escribir campos protegidos: `SECURITY DEFINER` (corren como su dueño) o
  `set_config('huella.internal', '1', true)`. Las RPC nuevas deben hacer `REVOKE ALL ... FROM PUBLIC, anon`:
  los default privileges del proyecto dan EXECUTE a `anon` directamente, no vía PUBLIC.
