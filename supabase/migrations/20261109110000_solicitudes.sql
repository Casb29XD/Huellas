-- Ítem 7: notificaciones del flujo de solicitudes. NEW_REQUEST al autor por trigger; el autor responde con
-- respond_to_request, que notifica REQUEST_ACCEPTED/REQUEST_REJECTED y, si acepta, abre la conversación.

CREATE OR REPLACE FUNCTION public.notify_new_request()
RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    INSERT INTO public.notifications (user_id, type, title, text, target_id)
    SELECT p.author_id, 'NEW_REQUEST', 'Nueva solicitud',
           COALESCE(a.name, 'Alguien') || ' quiere ayudar con ' || p.title || '.', p.id::TEXT
      FROM public.publications p
      LEFT JOIN public.profiles a ON a.id = NEW.applicant_id
     WHERE p.id = NEW.publication_id;
    RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS notify_new_request ON public.adoption_requests;
CREATE TRIGGER notify_new_request
    AFTER INSERT ON public.adoption_requests
    FOR EACH ROW EXECUTE FUNCTION public.notify_new_request();

-- Devuelve el id de la conversación si acepta; NULL si rechaza.
CREATE OR REPLACE FUNCTION public.respond_to_request(p_request_id UUID, p_accept BOOLEAN)
RETURNS UUID
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    r RECORD;
    conv UUID;
BEGIN
    SELECT ar.applicant_id, ar.status, p.id AS publication_id, p.author_id, p.title
      INTO r
      FROM public.adoption_requests ar
      JOIN public.publications p ON p.id = ar.publication_id
     WHERE ar.id = p_request_id
     FOR UPDATE OF ar;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'La solicitud no existe' USING ERRCODE = 'P0002';
    END IF;
    IF auth.uid() IS NULL OR r.author_id <> auth.uid() THEN
        RAISE EXCEPTION 'Solo el autor de la publicación responde sus solicitudes' USING ERRCODE = '42501';
    END IF;
    IF r.status <> 'PENDIENTE' THEN
        RAISE EXCEPTION 'La solicitud ya fue respondida' USING ERRCODE = '22023';
    END IF;

    UPDATE public.adoption_requests
       SET status = CASE WHEN p_accept THEN 'ACEPTADA' ELSE 'RECHAZADA' END::public.request_status
     WHERE id = p_request_id;

    IF NOT p_accept THEN
        INSERT INTO public.notifications (user_id, type, title, text, target_id)
        VALUES (r.applicant_id, 'REQUEST_REJECTED', 'Solicitud no aceptada',
                'Tu solicitud para ' || r.title || ' no fue aceptada esta vez.', r.publication_id::TEXT);
        RETURN NULL;
    END IF;

    -- Reutiliza la conversación entre ambos (preferiblemente sobre esta publicación).
    SELECT id INTO conv
      FROM public.conversations
     WHERE (participant_one, participant_two) IN ((r.author_id, r.applicant_id), (r.applicant_id, r.author_id))
     ORDER BY (publication_id = r.publication_id) DESC NULLS LAST, created_at
     LIMIT 1;

    IF conv IS NULL THEN
        INSERT INTO public.conversations (publication_id, participant_one, participant_two, about)
        VALUES (r.publication_id, r.author_id, r.applicant_id, 'Sobre: ' || r.title)
        RETURNING id INTO conv;
    END IF;

    INSERT INTO public.notifications (user_id, type, title, text, target_id)
    VALUES (r.applicant_id, 'REQUEST_ACCEPTED', '¡Solicitud aceptada!',
            'Tu solicitud para ' || r.title || ' fue aceptada. Escríbele al publicador.', conv::TEXT);
    RETURN conv;
END;
$$;

REVOKE ALL ON FUNCTION public.notify_new_request() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.respond_to_request(UUID, BOOLEAN) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.respond_to_request(UUID, BOOLEAN) TO authenticated;
