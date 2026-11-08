-- Repara el mojibake que dejó scripts/supabase-remote-sql.sh en el hosted: Python en Windows leía el SQL en
-- cp1252, así que todo literal con tildes de las migraciones 20261109* llegó doble codificado ("BogotÃ¡",
-- "Â¡Solicitud aceptada!") y los DROP POLICY con tildes no encontraban la política. En una BD donde las
-- migraciones se aplicaron bien (supabase db reset) esta migración no encuentra nada y no hace nada.
-- Texto doble codificado = UTF-8 leído como WIN1252: convert_to(WIN1252) devuelve los bytes UTF-8 originales.

DO $$
DECLARE
    bad CONSTANT TEXT := chr(195) || '|' || chr(194);   -- 'Ã' o 'Â': no aparecen en texto español correcto
    r RECORD;
    fixed TEXT;
BEGIN
    -- Funciones de public (cuerpos con literales mal codificados).
    FOR r IN SELECT p.oid FROM pg_proc p
              WHERE p.pronamespace = 'public'::regnamespace AND p.prokind = 'f' AND p.prosrc ~ bad LOOP
        EXECUTE convert_from(convert_to(pg_get_functiondef(r.oid), 'WIN1252'), 'UTF8');
    END LOOP;

    -- Políticas creadas con nombre mal codificado: la del nombre correcto es la vieja que el DROP no encontró.
    FOR r IN SELECT schemaname, tablename, policyname FROM pg_policies
              WHERE schemaname IN ('public', 'storage') AND policyname ~ bad LOOP
        fixed := convert_from(convert_to(r.policyname, 'WIN1252'), 'UTF8');
        EXECUTE format('DROP POLICY IF EXISTS %I ON %I.%I', fixed, r.schemaname, r.tablename);
        EXECUTE format('ALTER POLICY %I ON %I.%I RENAME TO %I', r.policyname, r.schemaname, r.tablename, fixed);
    END LOOP;

    -- Comentarios de tablas y columnas.
    FOR r IN SELECT d.objoid::regclass AS rel, a.attname, d.description FROM pg_description d
               JOIN pg_class c ON c.oid = d.objoid AND d.classoid = 'pg_class'::regclass
               LEFT JOIN pg_attribute a ON a.attrelid = d.objoid AND a.attnum = d.objsubid AND d.objsubid > 0
              WHERE c.relnamespace = 'public'::regnamespace AND d.description ~ bad LOOP
        fixed := convert_from(convert_to(r.description, 'WIN1252'), 'UTF8');
        IF r.attname IS NULL THEN
            EXECUTE format('COMMENT ON TABLE %s IS %L', r.rel, fixed);
        ELSE
            EXECUTE format('COMMENT ON COLUMN %s.%I IS %L', r.rel, r.attname, fixed);
        END IF;
    END LOOP;

    -- Perfiles creados con la ciudad por defecto mal codificada por handle_new_user.
    UPDATE public.profiles SET city = convert_from(convert_to(city, 'WIN1252'), 'UTF8') WHERE city ~ bad;
END $$;

-- Ítem 7: los DROP de 20261109300000 con tildes no aplicaron en el hosted; se repiten (idempotentes).
DROP POLICY IF EXISTS "Autor de publicación puede responder solicitudes" ON public.adoption_requests;
DROP POLICY IF EXISTS "Usuarios pueden enviar solicitudes de adopción" ON public.adoption_requests;
CREATE POLICY "Usuarios pueden enviar solicitudes de adopción"
    ON public.adoption_requests FOR INSERT TO authenticated
    WITH CHECK (
        auth.uid() = applicant_id
        AND status = 'PENDIENTE'
        AND EXISTS (SELECT 1 FROM public.publications p
                     WHERE p.id = publication_id AND p.status = 'APROBADA' AND p.lifecycle = 'ACTIVA'
                       AND p.author_id <> auth.uid())
    );
