-- Cierre de los ítems que el verificador dejó parciales (21, 23, 51, 52) y guarda de codificación.
-- Usa helpers de 10_security.sql; runtests() revierte cada test_*.

CREATE OR REPLACE FUNCTION tests.test_40_longitudes_perfil() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE a UUID := tests.tap_user('{"name":"tap A"}');
BEGIN
    PERFORM tests.tap_as(a);
    RETURN NEXT throws_ok(format('UPDATE public.profiles SET name = %L WHERE id = auth.uid()', repeat('N', 81)),
        '23514', NULL, 'name de 81 caracteres se rechaza');
    RETURN NEXT throws_ok('UPDATE public.profiles SET name = ''   '' WHERE id = auth.uid()', '23514', NULL, 'name en blanco se rechaza');
    RETURN NEXT throws_ok(format('UPDATE public.profiles SET city = %L WHERE id = auth.uid()', repeat('c', 81)),
        '23514', NULL, 'city de 81 caracteres se rechaza');
    RETURN NEXT throws_ok(format('UPDATE public.profiles SET phone = %L WHERE id = auth.uid()', repeat('3', 31)),
        '23514', NULL, 'phone de 31 caracteres se rechaza');
    RETURN NEXT throws_ok(format('UPDATE public.profiles SET bio = %L WHERE id = auth.uid()', repeat('b', 501)),
        '23514', NULL, 'bio de 501 caracteres se rechaza');
    RETURN NEXT lives_ok(format('UPDATE public.profiles SET name = %L, city = %L, phone = %L, bio = %L WHERE id = auth.uid()',
        repeat('N', 80), repeat('c', 80), repeat('3', 30), repeat('b', 500)), 'en el límite exacto se acepta');
    RESET ROLE;
    -- El alta recorta igual que los CHECK: un nombre larguísimo no rompe el registro.
    RETURN NEXT lives_ok(format('SELECT tests.tap_user(%L)', json_build_object('name', repeat('x', 5000))::text),
        'alta con nombre de 5000 caracteres no falla');
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_41_tipos_semanticos() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    a UUID := tests.tap_user('{"name":"tap A"}');
    p UUID;
    e UUID;
BEGIN
    RETURN NEXT col_type_is('public', 'publications', 'event_at', 'timestamp with time zone', 'publications.event_at es timestamptz');
    RETURN NEXT col_type_is('public', 'publications', 'event_end_at', 'timestamp with time zone', 'publications.event_end_at es timestamptz');
    RETURN NEXT col_type_is('public', 'publications', 'age_months', 'integer', 'publications.age_months es int');
    RETURN NEXT col_type_is('public', 'profiles', 'member_since', 'smallint', 'profiles.member_since es smallint');
    RETURN NEXT is((SELECT is_generated FROM information_schema.columns
                     WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'member_since'),
        'ALWAYS', 'member_since se calcula de created_at (sin default de texto)');
    RETURN NEXT hasnt_column('public', 'publications', 'tone', 'publications.tone ya no existe (ítem 52)');

    -- Como la app hoy: solo envía los textos; el servidor deriva los tipados.
    PERFORM tests.tap_as(a);
    INSERT INTO public.publications (author_id, category, title, species, breed, size, city, description, age, event_date)
    VALUES (a, 'PERDIDOS', 'tap', 'Perro', 'Criollo', 'Mediano', 'Medellín', 'tap', '1 año y medio', 'sábado 7 de noviembre a las 2.30 pm')
    RETURNING id INTO p;
    INSERT INTO public.publications (author_id, category, title, species, breed, size, city, description, age, event_date, schedule)
    VALUES (a, 'VETERINARIA', 'tap', 'Perro', 'Todos', 'Todos', 'Bogotá', 'tap', 'Todas', 'Sáb 8 nov', '8:00 a. m. — 1:00 p. m.')
    RETURNING id INTO e;
    RESET ROLE;
    RETURN NEXT is((SELECT age_months FROM public.publications WHERE id = p), 18, '"1 año y medio" = 18 meses');
    RETURN NEXT is((SELECT to_char(event_at AT TIME ZONE 'America/Bogota', 'MM-DD HH24:MI') FROM public.publications WHERE id = p),
        '11-07 14:30', 'event_at desde "sábado 7 de noviembre a las 2.30 pm"');
    RETURN NEXT is((SELECT age_months FROM public.publications WHERE id = e), NULL::int, '"Todas" no es una edad');
    RETURN NEXT is((SELECT to_char(event_at AT TIME ZONE 'America/Bogota', 'MM-DD HH24:MI') || ' / ' ||
                           to_char(event_end_at AT TIME ZONE 'America/Bogota', 'HH24:MI') FROM public.publications WHERE id = e),
        '11-08 08:00 / 13:00', 'jornada: inicio y fin desde event_date + schedule');
    RETURN NEXT ok((SELECT event_at < event_end_at FROM public.publications WHERE id = e), 'se puede ordenar y filtrar por fecha');

    -- Si el cliente envía la columna tipada, se respeta.
    PERFORM tests.tap_as(a);
    UPDATE public.publications SET age = '3 meses', age_months = 4 WHERE id = p;
    RESET ROLE;
    RETURN NEXT is((SELECT age_months FROM public.publications WHERE id = p), 4, 'age_months explícito gana al texto');
    RETURN NEXT throws_ok(format('UPDATE public.publications SET age_months = -1 WHERE id = %L', p), '23514', NULL, 'age_months negativo se rechaza');

    RETURN NEXT is((SELECT member_since::int FROM public.profiles WHERE id = a),
        extract(year FROM now() AT TIME ZONE 'America/Bogota')::int, 'member_since = año de created_at');
    RETURN NEXT throws_ok(format('UPDATE public.profiles SET member_since = 1999 WHERE id = %L', a), '428C9', NULL,
        'member_since no se escribe');
END;
$$;

-- Los scripts remotos llegaron a guardar el SQL con mojibake ("BogotÃ¡"). Nada de public debe tenerlo.
CREATE OR REPLACE FUNCTION tests.test_42_codificacion() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    bad CONSTANT TEXT := chr(195) || '|' || chr(194);
    u UUID := tests.tap_user('{"name":"tap sin ciudad"}');
BEGIN
    RETURN NEXT is((SELECT count(*)::int FROM pg_proc WHERE pronamespace = 'public'::regnamespace AND prosrc ~ bad),
        0, 'funciones de public sin mojibake');
    RETURN NEXT is((SELECT count(*)::int FROM pg_policies WHERE schemaname IN ('public', 'storage') AND policyname ~ bad),
        0, 'políticas sin nombres con mojibake');
    RETURN NEXT is((SELECT count(*)::int FROM pg_description d JOIN pg_class c ON c.oid = d.objoid
                     WHERE c.relnamespace = 'public'::regnamespace AND d.description ~ bad),
        0, 'comentarios sin mojibake');
    RETURN NEXT is((SELECT city FROM public.profiles WHERE id = u),
        'Bogotá', 'la ciudad por defecto del alta es "Bogotá"');
END;
$$;

-- ¿Se borró la foto? Desde 20261109350100 purge_pet_images siempre borra la fila (con Vault además encola el
-- DELETE a la Storage API); se acepta también el DELETE encolado (comportamiento de 20261109330000).
CREATE OR REPLACE FUNCTION tests.tap_image_purged(p_name TEXT) RETURNS BOOLEAN
LANGUAGE sql AS $$
    SELECT NOT EXISTS (SELECT 1 FROM storage.objects WHERE bucket_id = 'pet-images' AND name = p_name)
        OR EXISTS (SELECT 1 FROM net.http_request_queue
                    WHERE method = 'DELETE' AND url LIKE '%/storage/v1/object/pet-images'
                      AND convert_from(body, 'UTF8')::jsonb -> 'prefixes' ? p_name);
$$;

-- Vault de prueba dentro de la transacción del test (runtests la revierte; pg_net no envía nada).
CREATE OR REPLACE FUNCTION tests.tap_vault() RETURNS VOID
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM vault.secrets WHERE name = 'huellas_project_url') THEN
        PERFORM vault.create_secret('https://tap.invalid', 'huellas_project_url');
    END IF;
    IF NOT EXISTS (SELECT 1 FROM vault.secrets WHERE name = 'huellas_service_role_key') THEN
        PERFORM vault.create_secret('tap-key', 'huellas_service_role_key');
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_43_storage_por_api() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    a UUID := tests.tap_user('{"name":"tap A"}');
    b UUID := tests.tap_user('{"name":"tap B"}');
    p UUID := gen_random_uuid();
    q_before BIGINT;
BEGIN
    -- (c) anon ya no lista el bucket; cada usuario ve solo su carpeta.
    INSERT INTO storage.objects (bucket_id, name) VALUES ('pet-images', a || '/tap.jpg'), ('pet-images', a || '/tap_queda.jpg');
    PERFORM tests.tap_as(NULL);
    RETURN NEXT is((SELECT count(*)::int FROM storage.objects WHERE bucket_id = 'pet-images'), 0, 'anon no lista pet-images');
    PERFORM tests.tap_as(b);
    RETURN NEXT is((SELECT count(*)::int FROM storage.objects WHERE name LIKE a || '/%'), 0, 'otro usuario no lista la carpeta ajena');
    PERFORM tests.tap_as(a);
    RETURN NEXT is((SELECT count(*)::int FROM storage.objects WHERE name LIKE a || '/%'), 2, 'el dueño lista su carpeta');
    RESET ROLE;

    -- (a) Con Vault: borrar la publicación pide a la Storage API borrar el archivo.
    PERFORM tests.tap_vault();
    INSERT INTO public.publications (id, author_id, title, species, breed, size, city, description, status, image_url)
    VALUES (p, a, 'tap', 'Perro', 'Criollo', 'Mediano', 'Medellín', 'tap', 'APROBADA',
            'https://tap.supabase.co/storage/v1/object/public/pet-images/' || a || '/tap.jpg');
    PERFORM tests.tap_as(a);
    DELETE FROM public.publications WHERE id = p;
    RESET ROLE;
    RETURN NEXT ok(EXISTS (SELECT 1 FROM net.http_request_queue
                            WHERE method = 'DELETE' AND url LIKE '%/storage/v1/object/pet-images'
                              AND convert_from(body, 'UTF8')::jsonb -> 'prefixes' = jsonb_build_array(a || '/tap.jpg')
                              AND headers ->> 'Authorization' LIKE 'Bearer %'),
        'borrar la publicación encola DELETE /storage/v1/object/pet-images con la foto');
    RETURN NEXT is((SELECT count(*)::int FROM storage.objects WHERE name = a || '/tap.jpg'), 0,
        'además la fila se borra en la misma transacción (fail-closed, 20261109350100)');

    -- delete_my_account encola el borrado de toda la carpeta.
    INSERT INTO storage.objects (bucket_id, name) VALUES ('pet-images', b || '/1.jpg'), ('pet-images', b || '/2.png');
    PERFORM tests.tap_as(b);
    PERFORM public.delete_my_account();
    RESET ROLE;
    RETURN NEXT ok(EXISTS (SELECT 1 FROM net.http_request_queue
                            WHERE method = 'DELETE' AND url LIKE '%/storage/v1/object/pet-images'
                              AND convert_from(body, 'UTF8')::jsonb -> 'prefixes' @> jsonb_build_array(b || '/1.jpg', b || '/2.png')),
        'delete_my_account encola el borrado de su carpeta por la Storage API');

    -- (b) Cada subida en pet-images se manda a verify-image (magic bytes).
    SELECT count(*) INTO q_before FROM net.http_request_queue WHERE url LIKE '%/functions/v1/verify-image';
    PERFORM tests.tap_as(a);
    INSERT INTO storage.objects (bucket_id, name) VALUES ('pet-images', a || '/nueva.jpg');
    RESET ROLE;
    RETURN NEXT ok(EXISTS (SELECT 1 FROM net.http_request_queue
                            WHERE method = 'POST' AND url LIKE '%/functions/v1/verify-image'
                              AND convert_from(body, 'UTF8')::jsonb ->> 'name' = a || '/nueva.jpg'),
        'una subida a pet-images llama a verify-image');
    INSERT INTO storage.buckets (id, name) VALUES ('tap-otro', 'tap-otro');
    INSERT INTO storage.objects (bucket_id, name) VALUES ('tap-otro', a || '/x.jpg');
    RETURN NEXT is((SELECT count(*) FROM net.http_request_queue WHERE url LIKE '%/functions/v1/verify-image'), q_before + 1,
        'otros buckets no pasan por verify-image');
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_44_ubicacion() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    a UUID := tests.tap_user('{"name":"tap A"}');
    ins TEXT := 'INSERT INTO public.publications (author_id, title, species, breed, size, city, description, latitude, longitude) '
                'VALUES (auth.uid(), ''tap'', ''Perro'', ''Criollo'', ''Mediano'', ''Medellín'', ''tap'', %s, %s)';
BEGIN
    PERFORM tests.tap_as(a);
    RETURN NEXT lives_ok(format(ins, 'NULL', 'NULL'), 'sin ubicación: NULL');
    RETURN NEXT lives_ok(format(ins, '6.2442', '-75.5812'), 'ubicación real');
    RETURN NEXT throws_ok(format(ins, '0', '0'), '23514', NULL, '(0,0) no es una ubicación');
    RETURN NEXT throws_ok(format(ins, '6.2442', 'NULL'), '23514', NULL, 'media coordenada se rechaza');
    RETURN NEXT throws_ok(format(ins, '96', '-75'), '23514', NULL, 'latitud fuera de rango se rechaza');
    RESET ROLE;
END;
$$;
