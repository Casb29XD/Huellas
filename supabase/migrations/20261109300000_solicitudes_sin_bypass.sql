-- Ítem 7 (cierre): el estado de una solicitud solo cambia por respond_to_request (SECURITY DEFINER), que es
-- quien notifica y abre la conversación. Antes había dos atajos por la Data API:
--   1) el solicitante insertaba con status='ACEPTADA' (privilegio INSERT de tabla) y se autoaceptaba;
--   2) el autor hacía PATCH status (UPDATE(status) + política UPDATE) sin notificación ni conversación.

REVOKE INSERT, UPDATE ON public.adoption_requests FROM anon, authenticated;
-- REVOKE de tabla también retira los privilegios de columna (el antiguo UPDATE(status)).
GRANT INSERT (publication_id, applicant_id, message) ON public.adoption_requests TO authenticated;

DROP POLICY IF EXISTS "Autor de publicación puede responder solicitudes" ON public.adoption_requests;

-- Solo sobre publicaciones aprobadas, activas y ajenas; el estado inicial es siempre PENDIENTE (default de la columna).
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
