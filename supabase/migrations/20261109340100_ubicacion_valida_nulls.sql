-- Corrige 20261109340000: con media coordenada el CHECK daba NULL (= pasa). Exige ambas o ninguna.
ALTER TABLE public.publications DROP CONSTRAINT IF EXISTS publications_location_check;
ALTER TABLE public.publications ADD CONSTRAINT publications_location_check CHECK (
    (latitude IS NULL AND longitude IS NULL)
    OR (latitude IS NOT NULL AND longitude IS NOT NULL
        AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180 AND NOT (latitude = 0 AND longitude = 0))
);
