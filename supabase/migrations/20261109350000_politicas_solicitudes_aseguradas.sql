-- Ítem 7 (re-aseguramiento): 20261109300100 mezcló una conversión de datos (mojibake) con el DROP/CREATE de las
-- políticas de adoption_requests en la misma transacción. Esta migración no depende de datos y deja, de forma
-- idempotente, el estado final de 20261109300000: solo SELECT e INSERT(publication_id, applicant_id, message);
-- ningún UPDATE directo (el estado cambia solo por respond_to_request) y exactamente dos políticas.
-- ponytail: la conversión de datos de 300100 no se repite aquí: ya está aplicada en el hosted, y en un entorno
-- donde fallara, apply-migrations se detiene en 300100 y una copia posterior nunca llegaría a correr.

REVOKE ALL ON public.adoption_requests FROM PUBLIC, anon, authenticated;
GRANT SELECT ON public.adoption_requests TO authenticated;
GRANT INSERT (publication_id, applicant_id, message) ON public.adoption_requests TO authenticated;

-- Fuera cualquier política que no sea una de las dos previstas (p. ej. la vieja de UPDATE con otro nombre).
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN SELECT policyname FROM pg_policies
              WHERE schemaname = 'public' AND tablename = 'adoption_requests'
                AND policyname NOT IN ('Usuarios pueden enviar solicitudes de adopción', 'Ver solicitudes enviadas o recibidas') LOOP
        EXECUTE format('DROP POLICY %I ON public.adoption_requests', r.policyname);
    END LOOP;
END $$;

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

DROP POLICY IF EXISTS "Ver solicitudes enviadas o recibidas" ON public.adoption_requests;
CREATE POLICY "Ver solicitudes enviadas o recibidas"
    ON public.adoption_requests FOR SELECT
    USING (auth.uid() = applicant_id
           OR auth.uid() IN (SELECT author_id FROM public.publications WHERE id = publication_id));
