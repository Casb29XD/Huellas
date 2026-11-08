-- Ítem 13: comentarios, votos y avistamientos reales. Contadores en publications por trigger;
-- +2 al comentar y NEW_COMMENT al autor; report_sighting suma +5 y notifica SIGHTING.

ALTER TABLE public.publications
    ADD COLUMN IF NOT EXISTS votes_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS comments_count INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS public.comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publication_id UUID NOT NULL REFERENCES public.publications(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    text TEXT NOT NULL CHECK (char_length(btrim(text)) BETWEEN 1 AND 500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.publication_votes (
    publication_id UUID NOT NULL REFERENCES public.publications(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    value SMALLINT NOT NULL CHECK (value IN (-1, 1)),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (publication_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.sightings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publication_id UUID NOT NULL REFERENCES public.publications(id) ON DELETE CASCADE,
    reporter_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    text TEXT NOT NULL CHECK (char_length(btrim(text)) BETWEEN 1 AND 500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.publication_votes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sightings ENABLE ROW LEVEL SECURITY;

-- comments: visibles si la publicación lo es para quien consulta (la subconsulta aplica la RLS de publications).
DROP POLICY IF EXISTS "Comentarios de publicaciones visibles" ON public.comments;
CREATE POLICY "Comentarios de publicaciones visibles"
    ON public.comments FOR SELECT TO anon, authenticated
    USING (EXISTS (SELECT 1 FROM public.publications p WHERE p.id = publication_id));
DROP POLICY IF EXISTS "Usuarios comentan en su nombre" ON public.comments;
CREATE POLICY "Usuarios comentan en su nombre"
    ON public.comments FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = author_id AND EXISTS (SELECT 1 FROM public.publications p WHERE p.id = publication_id));
DROP POLICY IF EXISTS "Autor o moderador borra comentarios" ON public.comments;
CREATE POLICY "Autor o moderador borra comentarios"
    ON public.comments FOR DELETE TO authenticated
    USING (auth.uid() = author_id
           OR EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND is_moderator = TRUE));

DROP POLICY IF EXISTS "Votos visibles para autenticados" ON public.publication_votes;
CREATE POLICY "Votos visibles para autenticados"
    ON public.publication_votes FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS "Usuarios votan en su nombre" ON public.publication_votes;
CREATE POLICY "Usuarios votan en su nombre"
    ON public.publication_votes FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS "Usuarios cambian su voto" ON public.publication_votes;
CREATE POLICY "Usuarios cambian su voto"
    ON public.publication_votes FOR UPDATE TO authenticated
    USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
DROP POLICY IF EXISTS "Usuarios retiran su voto" ON public.publication_votes;
CREATE POLICY "Usuarios retiran su voto"
    ON public.publication_votes FOR DELETE TO authenticated USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Usuarios reportan avistamientos en su nombre" ON public.sightings;
CREATE POLICY "Usuarios reportan avistamientos en su nombre"
    ON public.sightings FOR INSERT TO authenticated WITH CHECK (auth.uid() = reporter_id);
DROP POLICY IF EXISTS "Avistamientos para autor, reportante y moderadores" ON public.sightings;
CREATE POLICY "Avistamientos para autor, reportante y moderadores"
    ON public.sightings FOR SELECT TO authenticated
    USING (auth.uid() = reporter_id
           OR EXISTS (SELECT 1 FROM public.publications p WHERE p.id = publication_id AND p.author_id = auth.uid())
           OR EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND is_moderator = TRUE));

REVOKE ALL ON public.comments, public.publication_votes, public.sightings FROM anon, authenticated;
GRANT ALL ON public.comments, public.publication_votes, public.sightings TO service_role;
GRANT SELECT ON public.comments TO anon, authenticated;
GRANT INSERT (publication_id, author_id, text), DELETE ON public.comments TO authenticated;
-- UPDATE de tabla completa: el upsert de PostgREST (on_conflict) reescribe todas las columnas del cuerpo.
GRANT SELECT, INSERT, UPDATE, DELETE ON public.publication_votes TO authenticated;
GRANT SELECT ON public.sightings TO authenticated;
GRANT INSERT (publication_id, reporter_id, text) ON public.sightings TO authenticated;

CREATE OR REPLACE FUNCTION public.comments_after_change()
RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    pub_id UUID := CASE WHEN TG_OP = 'DELETE' THEN OLD.publication_id ELSE NEW.publication_id END;
    pub RECORD;
BEGIN
    UPDATE public.publications
       SET comments_count = (SELECT count(*) FROM public.comments WHERE publication_id = pub_id)
     WHERE id = pub_id
    RETURNING author_id, title INTO pub;

    IF TG_OP = 'INSERT' AND pub.author_id IS DISTINCT FROM NEW.author_id THEN
        -- Puntos solo por el primer comentario en cada publicación ajena: comentar en bucle no suma.
        IF NOT EXISTS (SELECT 1 FROM public.comments
                        WHERE publication_id = pub_id AND author_id = NEW.author_id AND id <> NEW.id) THEN
            PERFORM public.award_points(NEW.author_id, 'Comentar una publicación', 2);
        END IF;
        INSERT INTO public.notifications (user_id, type, title, text, target_id)
        SELECT pub.author_id, 'NEW_COMMENT', 'Nuevo comentario',
               COALESCE(name, 'Alguien') || ' comentó en ' || pub.title || '.', pub_id::TEXT
          FROM public.profiles WHERE id = NEW.author_id;
    END IF;
    RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS comments_after_change ON public.comments;
CREATE TRIGGER comments_after_change
    AFTER INSERT OR DELETE ON public.comments
    FOR EACH ROW EXECUTE FUNCTION public.comments_after_change();

-- votes_count = suma de votos (+1/-1). Retirar el voto (DELETE) es el "toggle".
CREATE OR REPLACE FUNCTION public.votes_after_change()
RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    pub_id UUID := CASE WHEN TG_OP = 'DELETE' THEN OLD.publication_id ELSE NEW.publication_id END;
BEGIN
    UPDATE public.publications
       SET votes_count = (SELECT COALESCE(sum(value), 0) FROM public.publication_votes WHERE publication_id = pub_id)
     WHERE id = pub_id;
    RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS votes_after_change ON public.publication_votes;
CREATE TRIGGER votes_after_change
    AFTER INSERT OR UPDATE OR DELETE ON public.publication_votes
    FOR EACH ROW EXECUTE FUNCTION public.votes_after_change();

CREATE OR REPLACE FUNCTION public.report_sighting(p_publication_id UUID, p_text TEXT)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    uid UUID := auth.uid();
    pub RECORD;
    clean TEXT := btrim(COALESCE(p_text, ''));
    first_one BOOLEAN;
BEGIN
    IF uid IS NULL THEN
        RAISE EXCEPTION 'Inicia sesión para reportar un avistamiento' USING ERRCODE = '42501';
    END IF;
    IF char_length(clean) NOT BETWEEN 1 AND 500 THEN
        RAISE EXCEPTION 'El avistamiento debe tener entre 1 y 500 caracteres' USING ERRCODE = '22023';
    END IF;

    SELECT author_id, title INTO pub FROM public.publications WHERE id = p_publication_id AND status = 'APROBADA';
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La publicación no existe' USING ERRCODE = 'P0002';
    END IF;
    IF pub.author_id = uid THEN
        RAISE EXCEPTION 'No puedes reportar avistamientos de tu propia publicación' USING ERRCODE = '22023';
    END IF;

    first_one := NOT EXISTS (SELECT 1 FROM public.sightings WHERE publication_id = p_publication_id AND reporter_id = uid);
    INSERT INTO public.sightings (publication_id, reporter_id, text) VALUES (p_publication_id, uid, clean);

    INSERT INTO public.notifications (user_id, type, title, text, target_id)
    VALUES (pub.author_id, 'SIGHTING', 'Nuevo avistamiento',
            'Alguien vio a ' || pub.title || ': ' || LEFT(clean, 200), p_publication_id::TEXT);
    -- Puntos solo por el primer avistamiento de cada publicación: repetirlo no suma.
    IF first_one THEN
        PERFORM public.award_points(uid, 'Reportar un avistamiento', 5);
    END IF;
END;
$$;

REVOKE ALL ON FUNCTION public.comments_after_change() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.votes_after_change() FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.report_sighting(UUID, TEXT) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.report_sighting(UUID, TEXT) TO authenticated;
