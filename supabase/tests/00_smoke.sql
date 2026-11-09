-- Humo del esquema. Lo carga y ejecuta scripts/run-pgtap-remote.sh (runtests('tests', '^test_')).
CREATE OR REPLACE FUNCTION tests.test_00_smoke() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
BEGIN
    RETURN NEXT has_table('public', 'profiles', 'existe profiles');
    RETURN NEXT has_table('public', 'publications', 'existe publications');
    RETURN NEXT has_table('public', 'adoption_requests', 'existe adoption_requests');
    RETURN NEXT has_table('public', 'conversations', 'existe conversations');
    RETURN NEXT has_table('public', 'chat_messages', 'existe chat_messages');
    RETURN NEXT has_table('public', 'reports', 'existe reports');
    RETURN NEXT has_table('public', 'notifications', 'existe notifications');
    RETURN NEXT has_table('public', 'points_history', 'existe points_history');
    RETURN NEXT has_function('public', 'moderate_publication', 'existe el RPC moderate_publication');
END;
$$;
