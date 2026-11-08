-- Humo del esquema: corre con `supabase test db`.
BEGIN;
SELECT plan(9);

SELECT has_table('public', 'profiles', 'existe profiles');
SELECT has_table('public', 'publications', 'existe publications');
SELECT has_table('public', 'adoption_requests', 'existe adoption_requests');
SELECT has_table('public', 'conversations', 'existe conversations');
SELECT has_table('public', 'chat_messages', 'existe chat_messages');
SELECT has_table('public', 'reports', 'existe reports');
SELECT has_table('public', 'notifications', 'existe notifications');
SELECT has_table('public', 'points_history', 'existe points_history');
SELECT has_function('public', 'moderate_publication', 'existe el RPC moderate_publication');

SELECT * FROM finish();
ROLLBACK;
