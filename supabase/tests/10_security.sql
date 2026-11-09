-- Seguridad del backend (ítems 1, 2, 3, 4, 17, 18, 23, 24, 51). Lo carga y ejecuta scripts/run-pgtap-remote.sh.
-- runtests() revierte cada test_* al terminar: los usuarios tap_*, publicaciones, objetos de storage y los
-- GRANT temporales no quedan en la BD compartida.

-- Los helpers se llaman ya con SET ROLE anon/authenticated; el schema tests no está expuesto en la Data API.
GRANT USAGE ON SCHEMA tests TO anon, authenticated;

-- Usuario de prueba en auth.users (handle_new_user crea su perfil). Email tap_<uuid>@huella.test.
CREATE OR REPLACE FUNCTION tests.tap_user(p_meta JSONB DEFAULT '{}') RETURNS UUID
LANGUAGE plpgsql AS $$
DECLARE v UUID := gen_random_uuid();
BEGIN
    INSERT INTO auth.users (id, instance_id, aud, role, email, raw_user_meta_data, raw_app_meta_data, created_at, updated_at)
    VALUES (v, '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated',
            'tap_' || v || '@huella.test', p_meta, '{"provider":"email","providers":["email"]}', NOW(), NOW());
    RETURN v;
END;
$$;

-- Simula la petición de PostgREST: JWT con sub (NULL = anónimo) y el rol correspondiente.
CREATE OR REPLACE FUNCTION tests.tap_as(p_uid UUID) RETURNS VOID
LANGUAGE plpgsql AS $$
BEGIN
    RESET ROLE;
    PERFORM set_config('request.jwt.claims',
        json_build_object('sub', p_uid, 'role', CASE WHEN p_uid IS NULL THEN 'anon' ELSE 'authenticated' END)::text, true);
    IF p_uid IS NULL THEN SET LOCAL ROLE anon; ELSE SET LOCAL ROLE authenticated; END IF;
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_10_grants_y_perfiles() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    a UUID := tests.tap_user('{"name":"tap A"}');
    b UUID := tests.tap_user('{"name":"tap B"}');
    c UUID := tests.tap_user('{"name":"tap C"}');
    conv UUID;
BEGIN
    UPDATE public.profiles SET phone = '3001112233' WHERE id = b;
    INSERT INTO public.conversations (participant_one, participant_two) VALUES (a, b) RETURNING id INTO conv;

    RETURN NEXT is(
        (SELECT count(*)::int FROM information_schema.role_table_grants
          WHERE grantee = 'anon' AND table_schema = 'public' AND privilege_type <> 'SELECT'
            AND table_name IN ('profiles','publications','adoption_requests','conversations',
                               'chat_messages','reports','notifications','points_history')),
        0, 'anon no tiene INSERT/UPDATE/DELETE/TRUNCATE en las tablas del esquema');

    PERFORM tests.tap_as(NULL);
    RETURN NEXT throws_ok('SELECT email FROM public.profiles', '42501', NULL, 'anon no lee profiles.email');
    RETURN NEXT throws_ok('SELECT * FROM public.profiles', '42501', NULL, 'anon: select=* sobre profiles falla');
    RETURN NEXT lives_ok('SELECT id, name, level, initials, points FROM public.profiles', 'anon lee columnas públicas de profiles');
    RETURN NEXT throws_ok('SELECT * FROM public.adoption_requests', '42501', NULL, 'anon no lee adoption_requests');
    RETURN NEXT throws_ok(
        'INSERT INTO public.publications (author_id, title, species, breed, size, city, description)
         VALUES (gen_random_uuid(), ''x'', ''x'', ''x'', ''x'', ''x'', ''x'')',
        '42501', NULL, 'anon no crea publicaciones');
    RETURN NEXT throws_ok('SELECT * FROM public.get_my_profile()', '42501', NULL, 'anon no ejecuta get_my_profile');

    PERFORM tests.tap_as(a);
    RETURN NEXT throws_ok('SELECT phone FROM public.profiles', '42501', NULL, 'authenticated no lee profiles.phone');
    RETURN NEXT throws_ok('SELECT * FROM public.profiles', '42501', NULL, 'authenticated: select=* sobre profiles falla');
    RETURN NEXT is((SELECT email FROM public.get_my_profile()), 'tap_' || a || '@huella.test', 'get_my_profile devuelve el email propio');
    RETURN NEXT is(public.get_contact_phone(conv), '3001112233', 'participante obtiene el teléfono del otro');

    PERFORM tests.tap_as(c);
    RETURN NEXT is(public.get_contact_phone(conv), NULL::text, 'no participante no obtiene el teléfono');
    RESET ROLE;
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_11_proteccion_perfil() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE a UUID := tests.tap_user('{"name":"tap A"}');
BEGIN
    PERFORM tests.tap_as(a);
    RETURN NEXT throws_ok('UPDATE public.profiles SET points = 99999 WHERE id = auth.uid()', '42501', NULL, 'no sube sus puntos');
    RETURN NEXT throws_ok('UPDATE public.profiles SET is_moderator = TRUE WHERE id = auth.uid()', '42501', NULL, 'no se hace moderador');
    RETURN NEXT throws_ok('UPDATE public.profiles SET email = ''x@huella.test'' WHERE id = auth.uid()', '42501', NULL, 'no cambia su email sin auth');
    RETURN NEXT lives_ok('UPDATE public.profiles SET name = ''tap nuevo'', bio = ''hola'' WHERE id = auth.uid()', 'edita nombre y bio');

    -- Segunda barrera: aun con privilegio de columna (concedido solo dentro del test), el trigger rechaza.
    RESET ROLE;
    GRANT UPDATE (points, is_moderator) ON public.profiles TO authenticated;
    PERFORM tests.tap_as(a);
    RETURN NEXT throws_ok('UPDATE public.profiles SET points = 99999 WHERE id = auth.uid()', '42501',
        'No puedes modificar puntos, nivel, rol, contadores ni email del perfil', 'protect_profile_fields rechaza points');
    RETURN NEXT throws_ok('UPDATE public.profiles SET is_moderator = TRUE WHERE id = auth.uid()', '42501',
        'No puedes modificar puntos, nivel, rol, contadores ni email del perfil', 'protect_profile_fields rechaza is_moderator');
    PERFORM set_config('huella.internal', '1', true);
    RETURN NEXT lives_ok('UPDATE public.profiles SET points = 60 WHERE id = auth.uid()', 'huella.internal=1 deja pasar código interno');
    PERFORM set_config('huella.internal', '', true);
    RESET ROLE;

    RETURN NEXT is((SELECT level::text FROM public.profiles WHERE id = a), 'PROTECTOR', 'el nivel se recalcula con los puntos');
    RETURN NEXT is((SELECT name FROM public.profiles WHERE id = a), 'tap nuevo', 'el nombre sí se actualizó');
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_12_handle_new_user() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    m UUID := tests.tap_user('{"name":"tap mod","is_moderator":true}');
    n UUID := gen_random_uuid();
BEGIN
    RETURN NEXT is((SELECT is_moderator FROM public.profiles WHERE id = m), FALSE, 'metadata is_moderator=true no crea moderador');

    RETURN NEXT lives_ok(format(
        'INSERT INTO auth.users (id, instance_id, aud, role, phone, raw_user_meta_data, created_at, updated_at)
         VALUES (%L, ''00000000-0000-0000-0000-000000000000'', ''authenticated'', ''authenticated'', %L, ''{}'', NOW(), NOW())',
        n, '57' || (3000000000 + floor(random() * 99999999))::bigint), 'alta sin email (teléfono/OAuth) no falla');
    RETURN NEXT is((SELECT name FROM public.profiles WHERE id = n), 'Usuario Huella', 'perfil sin email recibe nombre por defecto');

    UPDATE auth.users SET email = 'tap_cambio_' || m || '@huella.test' WHERE id = m;
    RETURN NEXT is((SELECT email FROM public.profiles WHERE id = m), 'tap_cambio_' || m || '@huella.test',
        'profiles.email sigue a auth.users.email');
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_13_publicaciones() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    a UUID := tests.tap_user('{"name":"tap autor"}');
    b UUID := tests.tap_user('{"name":"tap otro"}');
    md UUID := tests.tap_user('{"name":"tap mod"}');
    p UUID := gen_random_uuid();
    p2 UUID := gen_random_uuid();
    img TEXT;
BEGIN
    UPDATE public.profiles SET is_moderator = TRUE WHERE id = md;
    img := 'https://tap.supabase.co/storage/v1/object/public/pet-images/' || a || '/tap.jpg';
    INSERT INTO storage.objects (bucket_id, name) VALUES ('pet-images', a || '/tap.jpg'), ('pet-images', a || '/tap_otra.jpg');

    PERFORM tests.tap_as(a);
    RETURN NEXT lives_ok(format(
        'INSERT INTO public.publications (id, author_id, title, species, breed, size, city, description, image_url)
         VALUES (%L, %L, ''tap'', ''Perro'', ''Criollo'', ''Mediano'', ''Medellín'', ''tap'', %L)', p, a, img),
        'el autor crea su publicación');
    RETURN NEXT lives_ok(format(
        'INSERT INTO public.publications (id, author_id, title, species, breed, size, city, description, status)
         VALUES (%L, %L, ''tap'', ''Gato'', ''Criollo'', ''Mediano'', ''Medellín'', ''tap'', ''APROBADA'')', p2, a),
        'crear con status APROBADA no falla...');
    RETURN NEXT throws_ok(format(
        'INSERT INTO public.publications (author_id, title, species, breed, size, city, description)
         VALUES (%L, ''tap'', ''Gato'', ''Criollo'', ''Mediano'', ''Medellín'', ''tap'')', b),
        '42501', NULL, 'no crea publicaciones a nombre de otro');
    RETURN NEXT lives_ok(format('UPDATE public.publications SET status = ''APROBADA'' WHERE id = %L', p), 'PATCH status no da error...');
    RESET ROLE;
    RETURN NEXT is((SELECT status::text FROM public.publications WHERE id = p2), 'EN_REVISION', '...pero entra EN_REVISION');
    RETURN NEXT is((SELECT status::text FROM public.publications WHERE id = p), 'EN_REVISION', '...pero el autor no se autoaprueba');

    PERFORM tests.tap_as(md);
    RETURN NEXT lives_ok(format('UPDATE public.publications SET status = ''RECHAZADA'' WHERE id = %L', p), 'UPDATE directo del moderador...');
    RESET ROLE;
    RETURN NEXT is((SELECT status::text FROM public.publications WHERE id = p), 'EN_REVISION', '...no cambia nada: moderar solo por RPC');
    PERFORM tests.tap_as(md);
    RETURN NEXT lives_ok(format('SELECT public.moderate_publication(%L, ''APROBADA'')', p), 'el moderador aprueba por RPC');

    PERFORM tests.tap_as(a);
    RETURN NEXT lives_ok(format('UPDATE public.publications SET title = ''tap editado'' WHERE id = %L', p), 'el autor edita el contenido');
    RESET ROLE;
    RETURN NEXT is((SELECT status::text FROM public.publications WHERE id = p), 'EN_REVISION', 'editar contenido vuelve a EN_REVISION');

    PERFORM tests.tap_as(b);
    RETURN NEXT lives_ok(format('UPDATE public.publications SET title = ''hack'' WHERE id = %L', p), 'UPDATE de un tercero...');
    PERFORM tests.tap_as(NULL);
    RETURN NEXT is((SELECT count(*)::int FROM public.publications WHERE id = p), 0, 'anon no ve publicaciones en revisión');
    RESET ROLE;
    RETURN NEXT is((SELECT title FROM public.publications WHERE id = p), 'tap editado', '...no modifica la publicación ajena');

    PERFORM tests.tap_as(a);
    RETURN NEXT lives_ok(format('DELETE FROM public.publications WHERE id = %L', p), 'el autor borra su publicación');
    RESET ROLE;
    -- tap_image_purged: fila borrada (sin Vault) o DELETE encolado a la Storage API (con Vault). Ver 40_.
    RETURN NEXT ok(tests.tap_image_purged(a || '/tap.jpg'), 'borrar la publicación borra su imagen de storage');
    RETURN NEXT ok(NOT tests.tap_image_purged(a || '/tap_otra.jpg'), 'las imágenes no citadas por la publicación se conservan');
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_14_storage() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    a UUID := tests.tap_user('{"name":"tap A"}');
    b UUID := tests.tap_user('{"name":"tap B"}');
BEGIN
    RETURN NEXT is((SELECT file_size_limit FROM storage.buckets WHERE id = 'pet-images'), 5242880::bigint, 'pet-images limita a 5 MB');
    RETURN NEXT is((SELECT allowed_mime_types FROM storage.buckets WHERE id = 'pet-images'),
        ARRAY['image/jpeg', 'image/png', 'image/webp'], 'pet-images solo acepta JPEG/PNG/WebP');

    INSERT INTO storage.objects (bucket_id, name) VALUES ('pet-images', b || '/tap_b.jpg');

    PERFORM tests.tap_as(a);
    RETURN NEXT lives_ok(format('INSERT INTO storage.objects (bucket_id, name) VALUES (''pet-images'', %L)', a || '/tap_x.jpg'),
        'sube en su carpeta <uid>/');
    RETURN NEXT throws_ok(format('INSERT INTO storage.objects (bucket_id, name) VALUES (''pet-images'', %L)', b || '/tap_x.jpg'),
        '42501', NULL, 'no sube en la carpeta de otro');
    RETURN NEXT throws_ok('INSERT INTO storage.objects (bucket_id, name) VALUES (''pet-images'', ''tap_raiz.jpg'')',
        '42501', NULL, 'no sube fuera de una carpeta');
    RETURN NEXT lives_ok(format('UPDATE storage.objects SET name = %L WHERE name = %L', a || '/robada.jpg', b || '/tap_b.jpg'),
        'UPDATE sobre objeto ajeno...');
    PERFORM set_config('storage.allow_delete_query', 'true', true);
    RETURN NEXT lives_ok(format('DELETE FROM storage.objects WHERE name IN (%L, %L)', a || '/tap_x.jpg', b || '/tap_b.jpg'),
        'DELETE sobre objeto propio y ajeno...');
    RESET ROLE;
    RETURN NEXT is((SELECT count(*)::int FROM storage.objects WHERE name = b || '/tap_b.jpg'), 1, '...no toca el objeto ajeno');
    RETURN NEXT is((SELECT count(*)::int FROM storage.objects WHERE name = a || '/tap_x.jpg'), 0, '...y borra el propio');
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_15_reportes() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    a UUID := tests.tap_user('{"name":"tap autor"}');
    r UUID := tests.tap_user('{"name":"tap reporta"}');
    md UUID := tests.tap_user('{"name":"tap mod"}');
    p UUID;
    rid UUID;
BEGIN
    UPDATE public.profiles SET is_moderator = TRUE WHERE id = md;
    INSERT INTO public.publications (author_id, title, species, breed, size, city, description)
    VALUES (a, 'tap', 'Perro', 'Criollo', 'Mediano', 'Medellín', 'tap') RETURNING id INTO p;

    PERFORM tests.tap_as(r);
    RETURN NEXT lives_ok(format('INSERT INTO public.reports (publication_id, reporter_id, reason) VALUES (%L, %L, ''tap'')', p, r),
        'el usuario reporta una publicación');
    RETURN NEXT throws_ok(format('INSERT INTO public.reports (publication_id, reporter_id, reason) VALUES (%L, %L, ''tap'')', p, r),
        '23505', NULL, 'no puede reportar dos veces la misma publicación');
    RETURN NEXT throws_ok(format('INSERT INTO public.reports (publication_id, reporter_id, reason, status) VALUES (%L, %L, ''tap'', ''RESUELTO'')', p, r),
        '42501', NULL, 'no fija el estado del reporte');
    RESET ROLE;
    SELECT id INTO rid FROM public.reports WHERE publication_id = p;
    PERFORM tests.tap_as(r);
    RETURN NEXT is((SELECT count(*)::int FROM public.reports WHERE id = rid), 0, 'un usuario normal no ve la cola de reportes');
    RETURN NEXT throws_ok(format('SELECT public.resolve_report(%L)', rid), '42501', NULL, 'un usuario normal no resuelve reportes');

    PERFORM tests.tap_as(md);
    RETURN NEXT is((SELECT status FROM public.reports WHERE id = rid), 'ABIERTO', 'el moderador ve el reporte ABIERTO');
    RETURN NEXT lives_ok(format('SELECT public.resolve_report(%L)', rid), 'el moderador lo resuelve');
    RESET ROLE;
    RETURN NEXT results_eq(format('SELECT status, resolved_by FROM public.reports WHERE id = %L', rid),
        format('VALUES (''RESUELTO''::text, %L::uuid)', md), 'queda RESUELTO y con resolved_by');
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_16_esquema() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE a UUID := tests.tap_user('{"name":"tap A"}');
BEGIN
    RETURN NEXT ok(enum_range(NULL::public.notification_type)::text[]
                   @> ARRAY['REQUEST_REJECTED','NEW_COMMENT','NEW_VOTE','BADGE_EARNED','SIGHTING'],
                   'notification_type tiene los valores del contrato');
    RETURN NEXT has_column('public', 'publications', 'lifecycle', 'publications.lifecycle');
    INSERT INTO public.publications (author_id, title, species, breed, size, city, description)
    VALUES (a, 'tap', 'Perro', 'Criollo', 'Mediano', 'Medellín', 'tap');
    RETURN NEXT is((SELECT lifecycle FROM public.publications WHERE author_id = a), 'ACTIVA', 'lifecycle por defecto ACTIVA');
    RETURN NEXT has_column('public', 'reports', 'status', 'reports.status');
    RETURN NEXT has_column('public', 'reports', 'resolved_by', 'reports.resolved_by');
    RETURN NEXT has_column('public', 'reports', 'resolved_at', 'reports.resolved_at');
    RETURN NEXT col_is_null('public', 'profiles', 'email', 'profiles.email admite NULL como auth.users');
    RETURN NEXT has_trigger('auth', 'users', 'on_auth_user_email_updated', 'trigger de sincronización de email');
    RETURN NEXT has_trigger('public', 'profiles', 'protect_profile_fields', 'trigger protect_profile_fields');
    RETURN NEXT has_trigger('public', 'publications', 'guard_publication_author', 'trigger guard_publication_author');
    RETURN NEXT has_trigger('public', 'profiles', 'set_updated_at', 'set_updated_at en profiles');
    RETURN NEXT has_trigger('public', 'publications', 'set_updated_at', 'set_updated_at en publications');
    RETURN NEXT has_trigger('public', 'adoption_requests', 'set_updated_at', 'set_updated_at en adoption_requests');
    RETURN NEXT has_trigger('public', 'conversations', 'set_updated_at', 'set_updated_at en conversations');

    UPDATE public.profiles SET updated_at = '2000-01-01', city = 'Cali' WHERE id = a;
    RETURN NEXT ok((SELECT updated_at > '2000-01-02' FROM public.profiles WHERE id = a), 'set_updated_at pisa updated_at con NOW()');
END;
$$;
