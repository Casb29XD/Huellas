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
