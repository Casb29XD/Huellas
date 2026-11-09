-- Ítem 18 (respaldo): el DELETE por Storage API de 20261109330000 es asíncrono (pg_net) y fail-open: si la
-- petición falla o nunca sale, la foto sigue servida. Ahora purge_pet_images (la usan delete_publication_images
-- y delete_my_account) además de encolar el DELETE borra las filas de storage.objects en la misma transacción:
-- sin fila, /object/public/... ya no sirve el objeto aunque la petición falle.
-- Límite conocido (verificado en el hosted): la Storage API borra el binario solo de las filas que encuentra;
-- al llegar el DELETE la fila ya no existe, responde [] y el binario queda huérfano en S3 (no servible).
-- La CDN puede seguir sirviendo una copia en caché de la URL pública durante su TTL.

-- Peticiones de purga enviadas por pg_net; la reconciliación borra las que respondieron 2xx y deja las fallidas.
CREATE TABLE IF NOT EXISTS public.storage_purge_failures (
    request_id BIGINT PRIMARY KEY,
    names TEXT[] NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status_code INT,
    error_msg TEXT
);
ALTER TABLE public.storage_purge_failures ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.storage_purge_failures FROM PUBLIC, anon, authenticated;
COMMENT ON TABLE public.storage_purge_failures IS
    'DELETE /storage/v1/object/pet-images enviados por purge_pet_images: pendientes (sin status ni error) o fallidos';

CREATE OR REPLACE FUNCTION public.purge_pet_images(p_names TEXT[])
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    req BIGINT;
BEGIN
    IF coalesce(cardinality(p_names), 0) = 0 THEN
        RETURN;
    END IF;
    -- Sin los secretos de Vault huellas_project_url/huellas_service_role_key no se encola nada (NULL).
    req := public.huella_service_request('DELETE', '/storage/v1/object/pet-images',
                                         jsonb_build_object('prefixes', to_jsonb(p_names)));
    IF req IS NOT NULL THEN
        INSERT INTO public.storage_purge_failures (request_id, names) VALUES (req, p_names);
    END IF;
    -- Fail-closed: la foto deja de servirse al confirmar, haya o no respuesta de la Storage API.
    PERFORM set_config('storage.allow_delete_query', 'true', true);
    DELETE FROM storage.objects WHERE bucket_id = 'pet-images' AND name = ANY (p_names);
END;
$$;
REVOKE ALL ON FUNCTION public.purge_pet_images(TEXT[]) FROM PUBLIC, anon, authenticated;

-- Cruza las peticiones pendientes con net._http_response: 2xx se borran; status fuera de 2xx, timeout o error se
-- guardan; sin respuesta pasado pg_net.ttl (las respuestas se purgan a las 6 h) se marcan "sin respuesta".
-- ponytail: registra y no reintenta: las filas ya no existen y un nuevo DELETE por API tampoco borraría el binario.
CREATE OR REPLACE FUNCTION public.reconcile_storage_purges()
RETURNS VOID
LANGUAGE plpgsql
SET search_path = public, pg_temp
AS $$
BEGIN
    UPDATE public.storage_purge_failures f
       SET status_code = r.status_code,
           error_msg = CASE WHEN r.timed_out THEN 'timeout' ELSE r.error_msg END
      FROM net._http_response r
     WHERE r.id = f.request_id AND f.status_code IS NULL AND f.error_msg IS NULL;
    UPDATE public.storage_purge_failures
       SET error_msg = 'sin respuesta de pg_net'
     WHERE status_code IS NULL AND error_msg IS NULL
       AND created_at < now() - coalesce(current_setting('pg_net.ttl', true), '6 hours')::interval;
    DELETE FROM public.storage_purge_failures WHERE status_code BETWEEN 200 AND 299 AND error_msg IS NULL;
END;
$$;
REVOKE ALL ON FUNCTION public.reconcile_storage_purges() FROM PUBLIC, anon, authenticated;

-- Cada hora (no diario): pg_net borra las respuestas a las 6 h; un job diario ya no las vería.
SELECT cron.schedule('huellas_reconciliacion_storage', '15 * * * *', 'SELECT public.reconcile_storage_purges()');
