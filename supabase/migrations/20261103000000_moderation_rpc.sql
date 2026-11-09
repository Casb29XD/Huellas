-- Secure moderation endpoint. Apply this migration in the Supabase SQL Editor.
-- The existing publications UPDATE policy also lets authors update their own row,
-- including status, so moderation decisions must go through this checked RPC.

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
        SELECT 1
        FROM public.profiles
        WHERE id = auth.uid()
          AND is_moderator = TRUE
    ) THEN
        RAISE EXCEPTION 'Solo un moderador puede revisar publicaciones'
            USING ERRCODE = '42501';
    END IF;

    IF p_status NOT IN ('APROBADA', 'RECHAZADA') THEN
        RAISE EXCEPTION 'El estado de moderación no es válido'
            USING ERRCODE = '22023';
    END IF;

    SELECT author_id, title
      INTO publication_author, publication_title
      FROM public.publications
     WHERE id = p_publication_id
     FOR UPDATE;

    IF publication_author IS NULL THEN
        RAISE EXCEPTION 'La publicación no existe'
            USING ERRCODE = 'P0002';
    END IF;

    UPDATE public.publications
       SET status = p_status,
           updated_at = NOW()
     WHERE id = p_publication_id;

    IF p_status = 'APROBADA' THEN
        notification_kind := 'POST_APPROVED';
        notification_title := 'Publicación aprobada';
        notification_text := publication_title || ' ya está visible para todos.';
    ELSE
        notification_kind := 'POST_REJECTED';
        notification_title := 'Publicación rechazada';
        notification_text := publication_title || ' fue rechazada. Motivo: ' ||
            COALESCE(NULLIF(BTRIM(p_reason), ''), 'Revisar las normas de la comunidad') ||
            CASE
                WHEN NULLIF(BTRIM(p_comment), '') IS NULL THEN ''
                ELSE '. Comentario del moderador: ' || BTRIM(p_comment)
            END;
    END IF;

    INSERT INTO public.notifications (user_id, type, title, text, target_id)
    VALUES (
        publication_author,
        notification_kind,
        notification_title,
        notification_text,
        p_publication_id::TEXT
    );
END;
$$;

REVOKE ALL ON FUNCTION public.moderate_publication(UUID, public.publication_status, TEXT, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.moderate_publication(UUID, public.publication_status, TEXT, TEXT) TO authenticated;
