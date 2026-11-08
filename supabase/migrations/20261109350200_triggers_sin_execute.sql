-- Residuos de privilegios: las funciones de trigger SECURITY DEFINER de public (handle_new_user,
-- sync_profile_email, delete_publication_images, ...) conservaban EXECUTE para PUBLIC/anon/authenticated.
-- Un trigger no exige EXECUTE al dispararse (solo al crearlo), así que se retira a todos esos roles.
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT p.oid::regprocedure AS fn FROM pg_proc p
              WHERE p.pronamespace = 'public'::regnamespace AND p.prosecdef
                AND p.prorettype = 'trigger'::regtype LOOP
        EXECUTE format('REVOKE EXECUTE ON FUNCTION %s FROM PUBLIC, anon, authenticated', r.fn);
    END LOOP;
END $$;
