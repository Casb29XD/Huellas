-- Ítem 18: pet-images solo acepta escrituras en la carpeta <auth.uid()>/, máx. 5 MB y JPEG/PNG/WebP.
-- La lectura sigue pública (bucket public + política SELECT existente).

UPDATE storage.buckets
   SET file_size_limit = 5242880,
       allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp']
 WHERE id = 'pet-images';

DROP POLICY IF EXISTS "Usuarios autenticados pueden subir fotos" ON storage.objects;
DROP POLICY IF EXISTS "pet-images: subir en carpeta propia" ON storage.objects;
DROP POLICY IF EXISTS "pet-images: actualizar en carpeta propia" ON storage.objects;
DROP POLICY IF EXISTS "pet-images: borrar en carpeta propia" ON storage.objects;

CREATE POLICY "pet-images: subir en carpeta propia"
    ON storage.objects FOR INSERT TO authenticated
    WITH CHECK (bucket_id = 'pet-images' AND (storage.foldername(name))[1] = auth.uid()::text);

CREATE POLICY "pet-images: actualizar en carpeta propia"
    ON storage.objects FOR UPDATE TO authenticated
    USING (bucket_id = 'pet-images' AND (storage.foldername(name))[1] = auth.uid()::text)
    WITH CHECK (bucket_id = 'pet-images' AND (storage.foldername(name))[1] = auth.uid()::text);

CREATE POLICY "pet-images: borrar en carpeta propia"
    ON storage.objects FOR DELETE TO authenticated
    USING (bucket_id = 'pet-images' AND (storage.foldername(name))[1] = auth.uid()::text);
