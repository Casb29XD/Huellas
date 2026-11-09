-- Ítems 17 y 24: ciclo de vida de reportes, enum notification_type completo y publications.lifecycle.

ALTER TYPE public.notification_type ADD VALUE IF NOT EXISTS 'REQUEST_REJECTED';
ALTER TYPE public.notification_type ADD VALUE IF NOT EXISTS 'NEW_COMMENT';
ALTER TYPE public.notification_type ADD VALUE IF NOT EXISTS 'NEW_VOTE';
ALTER TYPE public.notification_type ADD VALUE IF NOT EXISTS 'BADGE_EARNED';
ALTER TYPE public.notification_type ADD VALUE IF NOT EXISTS 'SIGHTING';

ALTER TABLE public.publications
    ADD COLUMN IF NOT EXISTS lifecycle TEXT NOT NULL DEFAULT 'ACTIVA'
        CONSTRAINT publications_lifecycle_check CHECK (lifecycle IN ('ACTIVA', 'FINALIZADA'));

ALTER TABLE public.reports
    ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'ABIERTO'
        CONSTRAINT reports_status_check CHECK (status IN ('ABIERTO', 'RESUELTO')),
    ADD COLUMN IF NOT EXISTS resolved_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMPTZ;

-- Un reporte por usuario y publicación: se conserva el más antiguo de cada par duplicado.
DELETE FROM public.reports r
 USING public.reports older
 WHERE r.publication_id = older.publication_id
   AND r.reporter_id = older.reporter_id
   AND (r.created_at, r.id) > (older.created_at, older.id);

DO $$ BEGIN
    ALTER TABLE public.reports
        ADD CONSTRAINT reports_publication_reporter_key UNIQUE (publication_id, reporter_id);
EXCEPTION WHEN duplicate_table OR duplicate_object THEN NULL; END $$;

DROP POLICY IF EXISTS "Moderadores pueden ver reportes" ON public.reports;
CREATE POLICY "Moderadores pueden ver reportes"
    ON public.reports FOR SELECT TO authenticated
    USING (EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND is_moderator = TRUE));

CREATE OR REPLACE FUNCTION public.resolve_report(p_report_id UUID)
RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND is_moderator = TRUE) THEN
        RAISE EXCEPTION 'Solo un moderador puede resolver reportes' USING ERRCODE = '42501';
    END IF;

    UPDATE public.reports
       SET status = 'RESUELTO', resolved_by = auth.uid(), resolved_at = NOW()
     WHERE id = p_report_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'El reporte no existe' USING ERRCODE = 'P0002';
    END IF;
END;
$$;
REVOKE ALL ON FUNCTION public.resolve_report(UUID) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.resolve_report(UUID) TO authenticated;
