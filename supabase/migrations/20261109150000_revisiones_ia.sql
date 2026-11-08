-- Contrato B3: veredictos de la Edge Function moderate-ai. Escribe solo service_role; leen solo moderadores.

CREATE TABLE IF NOT EXISTS public.publication_ai_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    publication_id UUID NOT NULL REFERENCES public.publications(id) ON DELETE CASCADE,
    verdict TEXT NOT NULL CHECK (verdict IN ('APROBAR', 'RECHAZAR', 'REVISAR')),
    risk NUMERIC NOT NULL CHECK (risk BETWEEN 0 AND 1),
    reasons TEXT[] NOT NULL DEFAULT '{}',
    suggested_reason TEXT,
    model TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE public.publication_ai_reviews ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Moderadores ven revisiones IA" ON public.publication_ai_reviews;
CREATE POLICY "Moderadores ven revisiones IA"
    ON public.publication_ai_reviews FOR SELECT TO authenticated
    USING (EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND is_moderator = TRUE));

REVOKE ALL ON public.publication_ai_reviews FROM anon, authenticated;
GRANT SELECT ON public.publication_ai_reviews TO authenticated;
GRANT ALL ON public.publication_ai_reviews TO service_role;
