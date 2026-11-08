-- Supabase ya no expone tablas nuevas de public a la Data API sin GRANT explícito
-- (auto_expose_new_tables se retiró el 2026-10-30). Sin esto PostgREST responde
-- 42501 "permission denied" a anon, authenticated y service_role en todas las tablas.
-- Estos GRANT reproducen el comportamiento previo; las políticas RLS siguen decidiendo qué filas.
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO anon;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO service_role;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO authenticated, service_role;
