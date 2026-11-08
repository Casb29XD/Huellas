-- Ítem 22 (backend): una publicación tiene ubicación completa y real, o no tiene (NULL). El (0,0) era el
-- default viejo, no un lugar; tampoco se acepta media coordenada ni valores fuera de rango.
UPDATE public.publications SET latitude = NULL, longitude = NULL
 WHERE (latitude IS NULL) <> (longitude IS NULL) OR (latitude = 0 AND longitude = 0)
    OR abs(latitude) > 90 OR abs(longitude) > 180;

ALTER TABLE public.publications DROP CONSTRAINT IF EXISTS publications_location_check;
ALTER TABLE public.publications ADD CONSTRAINT publications_location_check CHECK (
    (latitude IS NULL AND longitude IS NULL)
    OR (latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180 AND NOT (latitude = 0 AND longitude = 0))
);
