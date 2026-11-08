-- Ítems 6 y 53: la gamificación vive en el servidor. award_points es la única vía para sumar puntos
-- (historial + profiles.points + LEVEL_UP); contadores del perfil recalculados por trigger;
-- moderate_publication suma +10 al aprobar y resuelve los reportes al rechazar.

-- Interna: solo la llaman triggers y RPCs SECURITY DEFINER (corren como postgres y pasan protect_profile_fields).
CREATE OR REPLACE FUNCTION public.award_points(p_user UUID, p_action TEXT, p_points INT)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    old_level public.user_level;
    new_level public.user_level;
BEGIN
    SELECT level INTO old_level FROM public.profiles WHERE id = p_user FOR UPDATE;
    IF NOT FOUND THEN
        RETURN;
    END IF;

    INSERT INTO public.points_history (user_id, action, points) VALUES (p_user, p_action, p_points);
    -- trg_update_user_level recalcula level en este UPDATE.
    UPDATE public.profiles SET points = GREATEST(points + p_points, 0) WHERE id = p_user
    RETURNING level INTO new_level;

    IF new_level > old_level THEN
        INSERT INTO public.notifications (user_id, type, title, text)
        VALUES (p_user, 'LEVEL_UP', '¡Subiste de nivel!',
                'Ahora eres ' || CASE new_level
                    WHEN 'PROTECTOR' THEN 'Protector'
                    WHEN 'GUARDIAN' THEN 'Guardián'
                    WHEN 'HEROE' THEN 'Héroe'
                    ELSE 'Amigo Animal' END || '. ¡Gracias por ayudar!');
    END IF;
END;
$$;

-- Interna: contadores del perfil a partir de sus publicaciones (recuento: siempre consistente).
CREATE OR REPLACE FUNCTION public.refresh_profile_counters(p_user UUID)
RETURNS VOID
LANGUAGE sql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    UPDATE public.profiles pr
       SET publications_count = c.total,
           active_count = c.active,
           adoptions_count = c.adoptions,
           foster_count = c.foster
      FROM (SELECT count(*) AS total,
                   count(*) FILTER (WHERE status = 'APROBADA' AND lifecycle = 'ACTIVA') AS active,
                   count(*) FILTER (WHERE lifecycle = 'FINALIZADA' AND category = 'ADOPCION') AS adoptions,
                   count(*) FILTER (WHERE lifecycle = 'FINALIZADA' AND category = 'TEMPORAL') AS foster
              FROM public.publications WHERE author_id = p_user) c
     WHERE pr.id = p_user;
$$;

CREATE OR REPLACE FUNCTION public.publications_after_change()
RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        PERFORM public.award_points(NEW.author_id, 'Crear una publicación', 5);
    END IF;
    IF TG_OP <> 'INSERT' THEN
        PERFORM public.refresh_profile_counters(OLD.author_id);
    END IF;
    IF TG_OP <> 'DELETE' AND (TG_OP = 'INSERT' OR NEW.author_id IS DISTINCT FROM OLD.author_id) THEN
        PERFORM public.refresh_profile_counters(NEW.author_id);
    END IF;
    RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS publications_after_change ON public.publications;
CREATE TRIGGER publications_after_change
    AFTER INSERT OR DELETE OR UPDATE OF status, lifecycle, category, author_id ON public.publications
    FOR EACH ROW EXECUTE FUNCTION public.publications_after_change();

-- Contadores reales para los perfiles existentes (los del seed eran inventados).
SELECT public.refresh_profile_counters(id) FROM public.profiles;

CREATE OR REPLACE FUNCTION public.moderate_publication(
    p_publication_id UUID,
    p_status public.publication_status,
    p_reason TEXT DEFAULT NULL,
    p_comment TEXT DEFAULT NULL
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    publication_author UUID;
    publication_title TEXT;
    notification_kind public.notification_type;
    notification_title TEXT;
    notification_text TEXT;
BEGIN
    IF auth.uid() IS NULL OR NOT EXISTS (
        SELECT 1 FROM public.profiles WHERE id = auth.uid() AND is_moderator = TRUE
    ) THEN
        RAISE EXCEPTION 'Solo un moderador puede revisar publicaciones' USING ERRCODE = '42501';
    END IF;

    IF p_status NOT IN ('APROBADA', 'RECHAZADA') THEN
        RAISE EXCEPTION 'El estado de moderación no es válido' USING ERRCODE = '22023';
    END IF;

    SELECT author_id, title INTO publication_author, publication_title
      FROM public.publications WHERE id = p_publication_id FOR UPDATE;

    IF publication_author IS NULL THEN
        RAISE EXCEPTION 'La publicación no existe' USING ERRCODE = 'P0002';
    END IF;

    UPDATE public.publications SET status = p_status WHERE id = p_publication_id;

    IF p_status = 'APROBADA' THEN
        notification_kind := 'POST_APPROVED';
        notification_title := 'Publicación aprobada';
        notification_text := publication_title || ' ya está visible para todos.';
        -- ponytail: una reaprobación tras editar vuelve a sumar; cada una exige a un moderador.
        PERFORM public.award_points(publication_author, 'Publicación aprobada', 10);
    ELSE
        notification_kind := 'POST_REJECTED';
        notification_title := 'Publicación rechazada';
        notification_text := publication_title || ' fue rechazada. Motivo: ' ||
            COALESCE(NULLIF(BTRIM(p_reason), ''), 'Revisar las normas de la comunidad') ||
            CASE WHEN NULLIF(BTRIM(p_comment), '') IS NULL THEN ''
                 ELSE '. Comentario del moderador: ' || BTRIM(p_comment) END;
        UPDATE public.reports
           SET status = 'RESUELTO', resolved_by = auth.uid(), resolved_at = NOW()
         WHERE publication_id = p_publication_id AND status = 'ABIERTO';
    END IF;

    INSERT INTO public.notifications (user_id, type, title, text, target_id)
    VALUES (publication_author, notification_kind, notification_title, notification_text, p_publication_id::TEXT);
END;
$$;

REVOKE ALL ON FUNCTION public.moderate_publication(UUID, public.publication_status, TEXT, TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.moderate_publication(UUID, public.publication_status, TEXT, TEXT) TO authenticated;
REVOKE ALL ON FUNCTION public.award_points(UUID, TEXT, INT) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.refresh_profile_counters(UUID) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.publications_after_change() FROM PUBLIC, anon, authenticated;
