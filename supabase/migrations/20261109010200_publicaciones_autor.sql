-- Ítem 3: el autor no puede aprobar su publicación (ni al crearla ni al editarla) ni tocar campos del
-- servidor; editar contenido la devuelve a EN_REVISION. Los moderadores deciden solo vía moderate_publication.

CREATE OR REPLACE FUNCTION public.guard_publication_author()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = public, pg_temp
AS $$
DECLARE
    -- Campos que solo escribe el servidor. Las claves que aún no existan en la tabla se ignoran.
    server_keys TEXT[] := ARRAY['author_id', 'created_at', 'votes_count', 'comments_count', 'lifecycle'];
    ignored TEXT[] := server_keys || ARRAY['status', 'updated_at'];
BEGIN
    IF current_user NOT IN ('anon', 'authenticated')
       OR COALESCE(current_setting('huella.internal', true), '') = '1' THEN
        RETURN NEW;
    END IF;

    IF TG_OP = 'INSERT' THEN
        NEW := jsonb_populate_record(NEW,
            '{"status":"EN_REVISION","votes_count":0,"comments_count":0,"lifecycle":"ACTIVA"}'::jsonb);
        RETURN NEW;
    END IF;

    NEW := jsonb_populate_record(NEW,
        (SELECT COALESCE(jsonb_object_agg(key, value), '{}'::jsonb)
           FROM jsonb_each(to_jsonb(OLD)) WHERE key = ANY (server_keys)));
    NEW.status := CASE
        WHEN (to_jsonb(NEW) - ignored) IS DISTINCT FROM (to_jsonb(OLD) - ignored) THEN 'EN_REVISION'
        ELSE OLD.status
    END;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS guard_publication_author ON public.publications;
CREATE TRIGGER guard_publication_author
    BEFORE INSERT OR UPDATE ON public.publications
    FOR EACH ROW EXECUTE FUNCTION public.guard_publication_author();

DROP POLICY IF EXISTS "Usuarios pueden crear publicaciones" ON public.publications;
CREATE POLICY "Usuarios pueden crear publicaciones"
    ON public.publications FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = author_id AND status = 'EN_REVISION');

DROP POLICY IF EXISTS "Autores y moderadores pueden actualizar publicaciones" ON public.publications;
DROP POLICY IF EXISTS "Autores pueden editar sus publicaciones" ON public.publications;
CREATE POLICY "Autores pueden editar sus publicaciones"
    ON public.publications FOR UPDATE TO authenticated
    USING (auth.uid() = author_id)
    WITH CHECK (auth.uid() = author_id AND status = 'EN_REVISION');

-- Al borrar una publicación se borran sus fotos de pet-images (objetos de la carpeta del autor citados
-- en la fila: image_url o cualquier columna de URLs que se agregue).
-- ponytail: borra la fila de storage.objects; el binario queda huérfano en el backend S3 hasta una
-- limpieza vía Storage API. Ya no es servible (Storage responde 404 sin la fila).
CREATE OR REPLACE FUNCTION public.delete_publication_images()
RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    row_text TEXT := to_jsonb(OLD)::text;
BEGIN
    PERFORM set_config('storage.allow_delete_query', 'true', true);
    DELETE FROM storage.objects o
     WHERE o.bucket_id = 'pet-images'
       AND (storage.foldername(o.name))[1] = OLD.author_id::text
       AND strpos(row_text, '/pet-images/' || o.name) > 0;
    RETURN OLD;
END;
$$;

DROP TRIGGER IF EXISTS delete_publication_images ON public.publications;
CREATE TRIGGER delete_publication_images
    AFTER DELETE ON public.publications
    FOR EACH ROW EXECUTE FUNCTION public.delete_publication_images();
