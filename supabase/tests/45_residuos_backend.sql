-- Cierres de backend: políticas de solicitudes (20261109350000), purga de fotos fail-closed y su reconciliación
-- (20261109350100) y funciones de trigger sin EXECUTE para clientes (20261109350200).

CREATE OR REPLACE FUNCTION tests.test_45_politicas_solicitudes() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
BEGIN
    RETURN NEXT is((SELECT array_agg(policyname || ':' || cmd ORDER BY policyname) FROM pg_policies
                     WHERE schemaname = 'public' AND tablename = 'adoption_requests'),
        ARRAY['Usuarios pueden enviar solicitudes de adopción:INSERT', 'Ver solicitudes enviadas o recibidas:SELECT'],
        'adoption_requests tiene exactamente las políticas INSERT y SELECT previstas');
    RETURN NEXT is((SELECT count(*)::int FROM information_schema.column_privileges
                     WHERE table_schema = 'public' AND table_name = 'adoption_requests'
                       AND grantee IN ('PUBLIC', 'anon', 'authenticated') AND privilege_type = 'UPDATE'),
        0, 'ningún UPDATE (ni de columna) para clientes: el estado cambia solo con respond_to_request');
    RETURN NEXT is((SELECT array_agg(column_name::text ORDER BY column_name::text) FROM information_schema.column_privileges
                     WHERE table_schema = 'public' AND table_name = 'adoption_requests'
                       AND grantee = 'authenticated' AND privilege_type = 'INSERT'),
        ARRAY['applicant_id', 'message', 'publication_id'],
        'authenticated solo inserta publication_id, applicant_id y message');
    RETURN NEXT is((SELECT array_agg(privilege_type::text ORDER BY privilege_type) FROM information_schema.table_privileges
                     WHERE table_schema = 'public' AND table_name = 'adoption_requests'
                       AND grantee IN ('PUBLIC', 'anon', 'authenticated')),
        ARRAY['SELECT'], 'a nivel de tabla los clientes solo tienen SELECT (authenticated)');
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_46_purga_storage() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    a UUID := tests.tap_user('{"name":"tap A"}');
    ins TEXT := 'INSERT INTO public.publications (id, author_id, title, species, breed, size, city, description, status, image_url) '
                'VALUES (%L, %L, ''tap'', ''Perro'', ''Criollo'', ''Mediano'', ''Medellín'', ''tap'', ''APROBADA'', %L)';
    base TEXT := 'https://tap.supabase.co/storage/v1/object/public/pet-images/';
    p UUID := gen_random_uuid();
BEGIN
    -- Con o sin Vault (según el entorno): la fila desaparece en la misma transacción.
    INSERT INTO storage.objects (bucket_id, name) VALUES ('pet-images', a || '/tap46.jpg');
    EXECUTE format(ins, p, a, base || a || '/tap46.jpg');
    PERFORM tests.tap_as(a);
    DELETE FROM public.publications WHERE id = p;
    RESET ROLE;
    RETURN NEXT is((SELECT count(*)::int FROM storage.objects WHERE name = a || '/tap46.jpg'), 0,
        'borrar la publicación borra la fila de storage.objects (entorno actual)');

    -- Con Vault: además se encola el DELETE y queda registrado para la reconciliación.
    PERFORM tests.tap_vault();
    p := gen_random_uuid();
    INSERT INTO storage.objects (bucket_id, name) VALUES ('pet-images', a || '/tap46v.jpg');
    EXECUTE format(ins, p, a, base || a || '/tap46v.jpg');
    PERFORM tests.tap_as(a);
    DELETE FROM public.publications WHERE id = p;
    RESET ROLE;
    RETURN NEXT is((SELECT count(*)::int FROM storage.objects WHERE name = a || '/tap46v.jpg'), 0,
        'con Vault la fila también se borra en la misma transacción');
    RETURN NEXT ok(EXISTS (SELECT 1 FROM public.storage_purge_failures f JOIN net.http_request_queue q ON q.id = f.request_id
                            WHERE f.names = ARRAY[a || '/tap46v.jpg'] AND q.url LIKE '%/storage/v1/object/pet-images'),
        'el DELETE encolado queda registrado en storage_purge_failures');

    -- Reconciliación con respuestas simuladas (ids negativos: no chocan con los de pg_net).
    INSERT INTO public.storage_purge_failures (request_id, names, created_at) VALUES
        (-4601, '{ok}', now()), (-4602, '{500}', now()), (-4603, '{timeout}', now()),
        (-4604, '{perdida}', now() - interval '1 day');
    INSERT INTO net._http_response (id, status_code, timed_out, error_msg) VALUES
        (-4601, 200, false, NULL), (-4602, 500, false, NULL), (-4603, NULL, true, 'Timeout was reached');
    PERFORM public.reconcile_storage_purges();
    RETURN NEXT results_eq(
        'SELECT request_id, status_code, error_msg FROM public.storage_purge_failures WHERE request_id < 0 ORDER BY request_id DESC',
        $q$VALUES (-4602::bigint, 500, NULL::text), (-4603, NULL, 'timeout'), (-4604, NULL, 'sin respuesta de pg_net')$q$,
        'la reconciliación borra los 2xx y registra 500, timeout y peticiones sin respuesta');

    RETURN NEXT is((SELECT schedule FROM cron.job WHERE jobname = 'huellas_reconciliacion_storage'), '15 * * * *',
        'el job huellas_reconciliacion_storage existe en cron.job');
    RETURN NEXT is((SELECT count(*)::int FROM information_schema.table_privileges
                     WHERE table_schema = 'public' AND table_name = 'storage_purge_failures'
                       AND grantee IN ('PUBLIC', 'anon', 'authenticated')), 0,
        'storage_purge_failures no es accesible para clientes');
END;
$$;
