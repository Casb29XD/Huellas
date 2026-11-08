-- Ítem 18 (cierre): (a) borrar fotos por la Storage API para que también se borre el binario (antes se
-- borraba la fila de storage.objects por SQL y el archivo quedaba huérfano en S3); (b) revisar los magic
-- bytes de cada subida con la Edge Function verify-image; (c) anon ya no lista el bucket.
--
-- Las llamadas salen por pg_net (asíncronas, solo si la transacción confirma) con la service role guardada
-- en Vault. Configuración única por proyecto, fuera del repo (ver docs/SEGURIDAD_BACKEND.md):
--   select vault.create_secret('<SUPABASE_URL>', 'huellas_project_url');
--   select vault.create_secret('<SERVICE_ROLE_KEY>', 'huellas_service_role_key');
-- Sin esos secretos (p. ej. stack local recién creado) se conserva el comportamiento anterior: se borra la
-- fila por SQL (la foto deja de servirse) y no hay revisión de magic bytes.

CREATE EXTENSION IF NOT EXISTS pg_net WITH SCHEMA extensions;

-- POST/DELETE a la API del propio proyecto con la service role. NULL si faltan los secretos.
CREATE OR REPLACE FUNCTION public.huella_service_request(p_method TEXT, p_path TEXT, p_body JSONB DEFAULT NULL)
RETURNS BIGINT
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    base TEXT;
    key TEXT;
    headers JSONB;
BEGIN
    SELECT decrypted_secret INTO base FROM vault.decrypted_secrets WHERE name = 'huellas_project_url';
    SELECT decrypted_secret INTO key FROM vault.decrypted_secrets WHERE name = 'huellas_service_role_key';
    IF base IS NULL OR key IS NULL THEN
        RETURN NULL;
    END IF;
    headers := jsonb_build_object('apikey', key, 'Authorization', 'Bearer ' || key, 'Content-Type', 'application/json');
    IF p_method = 'DELETE' THEN
        RETURN net.http_delete(url := rtrim(base, '/') || p_path, headers := headers, body := p_body,
                               timeout_milliseconds := 10000);
    END IF;
    RETURN net.http_post(url := rtrim(base, '/') || p_path, body := coalesce(p_body, '{}'), headers := headers,
                         timeout_milliseconds := 10000);
END;
$$;
REVOKE ALL ON FUNCTION public.huella_service_request(TEXT, TEXT, JSONB) FROM PUBLIC, anon, authenticated;

-- (a) Borra objetos de pet-images por la Storage API (fila + binario). Sin secretos: solo la fila.
CREATE OR REPLACE FUNCTION public.purge_pet_images(p_names TEXT[])
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    IF coalesce(cardinality(p_names), 0) = 0 THEN
        RETURN;
    END IF;
    IF public.huella_service_request('DELETE', '/storage/v1/object/pet-images',
                                     jsonb_build_object('prefixes', to_jsonb(p_names))) IS NULL THEN
        RAISE WARNING 'Vault sin huellas_project_url/huellas_service_role_key: se borran filas de storage.objects y los binarios quedan en S3';
        PERFORM set_config('storage.allow_delete_query', 'true', true);
        DELETE FROM storage.objects WHERE bucket_id = 'pet-images' AND name = ANY (p_names);
    END IF;
END;
$$;
REVOKE ALL ON FUNCTION public.purge_pet_images(TEXT[]) FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION public.delete_publication_images()
RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    row_text TEXT := to_jsonb(OLD)::text;
BEGIN
    PERFORM public.purge_pet_images(ARRAY(
        SELECT o.name FROM storage.objects o
         WHERE o.bucket_id = 'pet-images'
           AND (storage.foldername(o.name))[1] = OLD.author_id::text
           AND strpos(row_text, '/pet-images/' || o.name) > 0));
    RETURN OLD;
END;
$$;

CREATE OR REPLACE FUNCTION public.delete_my_account()
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    uid UUID := auth.uid();
BEGIN
    IF uid IS NULL THEN
        RAISE EXCEPTION 'Inicia sesión para eliminar tu cuenta' USING ERRCODE = '42501';
    END IF;
    PERFORM public.purge_pet_images(ARRAY(
        SELECT name FROM storage.objects WHERE bucket_id = 'pet-images' AND (storage.foldername(name))[1] = uid::text));
    DELETE FROM auth.users WHERE id = uid;
END;
$$;
REVOKE ALL ON FUNCTION public.delete_my_account() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.delete_my_account() TO authenticated;

-- (b) Cada subida o reemplazo en pet-images pasa por verify-image, que borra lo que no sea JPEG/PNG/WebP real.
-- ponytail: revisión asíncrona (segundos después de la subida); para bloquear antes de servir haría falta
-- subir a través de una Edge Function en lugar de directo a Storage.
CREATE OR REPLACE FUNCTION public.verify_pet_image()
RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    PERFORM public.huella_service_request('POST', '/functions/v1/verify-image', jsonb_build_object('name', NEW.name));
    RETURN NULL;
END;
$$;
REVOKE ALL ON FUNCTION public.verify_pet_image() FROM PUBLIC, anon, authenticated;

DROP TRIGGER IF EXISTS verify_pet_image ON storage.objects;
CREATE TRIGGER verify_pet_image
    AFTER INSERT OR UPDATE OF name, version ON storage.objects
    FOR EACH ROW WHEN (NEW.bucket_id = 'pet-images')
    EXECUTE FUNCTION public.verify_pet_image();

-- (c) El bucket es public: /object/public/... sirve las fotos sin política SELECT. La política pública solo
-- servía para listar el bucket entero con la publishable key. Cada usuario ve (y lista) su carpeta.
DROP POLICY IF EXISTS "Imágenes de mascotas públicas" ON storage.objects;
DROP POLICY IF EXISTS "pet-images: ver carpeta propia" ON storage.objects;
CREATE POLICY "pet-images: ver carpeta propia"
    ON storage.objects FOR SELECT TO authenticated
    USING (bucket_id = 'pet-images' AND (storage.foldername(name))[1] = auth.uid()::text);
