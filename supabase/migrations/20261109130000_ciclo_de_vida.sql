-- Ítems 15, 21, 22 y 52: cierre de publicaciones (manual por RPC y automático por pg_cron), ubicación NULL
-- en vez de (0,0) y columnas de presentación que dejan de ser fuente de verdad.

-- Ítem 15: el autor finaliza su publicación; +50 si fue una adopción u hogar temporal. Idempotente.
CREATE OR REPLACE FUNCTION public.close_publication(p_publication_id UUID)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    pub RECORD;
BEGIN
    SELECT author_id, category, lifecycle INTO pub
      FROM public.publications WHERE id = p_publication_id FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La publicación no existe' USING ERRCODE = 'P0002';
    END IF;
    IF auth.uid() IS NULL OR pub.author_id <> auth.uid() THEN
        RAISE EXCEPTION 'Solo el autor puede finalizar la publicación' USING ERRCODE = '42501';
    END IF;
    IF pub.lifecycle = 'FINALIZADA' THEN
        RETURN;
    END IF;

    -- publications_after_change recalcula los contadores del perfil.
    UPDATE public.publications SET lifecycle = 'FINALIZADA' WHERE id = p_publication_id;
    IF pub.category IN ('ADOPCION', 'TEMPORAL') THEN
        PERFORM public.award_points(pub.author_id, 'Adopción concretada', 50);
    END IF;
END;
$$;
REVOKE ALL ON FUNCTION public.close_publication(UUID) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.close_publication(UUID) TO authenticated;

-- Ítem 22: sin ubicación = NULL. Las filas en (0,0) eran el default, no una ubicación real.
ALTER TABLE public.publications ALTER COLUMN latitude DROP DEFAULT, ALTER COLUMN longitude DROP DEFAULT;
UPDATE public.publications SET latitude = NULL, longitude = NULL WHERE latitude = 0 AND longitude = 0;

-- Ítems 21 y 52: se conservan por compatibilidad, pero la app no debe tratarlos como dato.
ALTER TABLE public.notifications ALTER COLUMN group_label DROP DEFAULT;
COMMENT ON COLUMN public.notifications.group_label IS
    'Obsoleto: el grupo (Hoy/Ayer/Esta semana) se calcula en la app a partir de created_at.';
COMMENT ON COLUMN public.publications.tone IS
    'Obsoleto: color de presentación; la app lo deriva de la categoría. No es fuente de verdad.';

-- pg_cron: cierre automático de perdidos/encontrados y limpieza de notificaciones leídas.
DO $$ BEGIN
    CREATE EXTENSION IF NOT EXISTS pg_cron WITH SCHEMA pg_catalog;
EXCEPTION WHEN OTHERS THEN
    CREATE EXTENSION IF NOT EXISTS pg_cron WITH SCHEMA extensions;
END $$;
GRANT USAGE ON SCHEMA cron TO postgres;

-- cron.schedule(nombre, ...) actualiza el job si ya existe: no se duplica al reaplicar. Horas en UTC (3 a. m. Colombia).
SELECT cron.schedule('huellas_cierre_automatico', '0 8 * * *', $job$
    UPDATE public.publications SET lifecycle = 'FINALIZADA'
     WHERE lifecycle = 'ACTIVA' AND category IN ('PERDIDOS', 'ENCONTRADOS')
       AND created_at < NOW() - INTERVAL '60 days'
$job$);
SELECT cron.schedule('huellas_limpieza_notificaciones', '30 8 * * *', $job$
    DELETE FROM public.notifications WHERE read AND created_at < NOW() - INTERVAL '90 days'
$job$);
