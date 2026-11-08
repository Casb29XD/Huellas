-- Ítems 21, 51 y 52 (cierre): fechas, edades y "miembro desde" con tipos de verdad; fuera el color de la BD.
-- Aditiva y compatible con la app actual: las columnas TEXT siguen (texto para mostrar) y un trigger deriva
-- las tipadas mientras la app no las escriba; si las escribe, se respeta su valor.

ALTER TABLE public.publications
    ADD COLUMN IF NOT EXISTS event_at     TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS event_end_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS age_months   INT;
ALTER TABLE public.publications DROP CONSTRAINT IF EXISTS publications_age_months_check;
ALTER TABLE public.publications ADD CONSTRAINT publications_age_months_check CHECK (age_months BETWEEN 0 AND 600);
ALTER TABLE public.publications DROP CONSTRAINT IF EXISTS publications_event_span_check;
ALTER TABLE public.publications ADD CONSTRAINT publications_event_span_check CHECK (event_end_at >= event_at);
CREATE INDEX IF NOT EXISTS publications_event_at_idx ON public.publications (event_at) WHERE event_at IS NOT NULL;

COMMENT ON COLUMN public.publications.event_at IS 'Inicio del evento o fecha del avistamiento/pérdida (America/Bogota).';
COMMENT ON COLUMN public.publications.event_end_at IS 'Fin del evento (jornadas con horario); NULL si no aplica.';
COMMENT ON COLUMN public.publications.age_months IS 'Edad aproximada en meses; NULL si es desconocida o no aplica.';
COMMENT ON COLUMN public.publications.event_date IS 'Texto para mostrar. Fuente de verdad: event_at.';
COMMENT ON COLUMN public.publications.schedule IS 'Texto para mostrar. Fuente de verdad: event_at/event_end_at.';
COMMENT ON COLUMN public.publications.age IS 'Texto para mostrar. Fuente de verdad: age_months.';

-- "4 años" -> 48, "1 año y medio" -> 18, "3 meses" -> 3, "2" -> 24 (años); "Desconocida" -> NULL.
CREATE OR REPLACE FUNCTION public.parse_age_months(p TEXT)
RETURNS INT
LANGUAGE plpgsql IMMUTABLE
SET search_path = pg_catalog, pg_temp
AS $$
DECLARE
    s TEXT := lower(btrim(coalesce(p, '')));
    m TEXT[];
    months NUMERIC := 0;
    hit BOOLEAN := FALSE;
BEGIN
    IF s ~ '^\d+([.,]\d+)?$' THEN
        RETURN LEAST(round(replace(s, ',', '.')::NUMERIC * 12), 600)::INT;
    END IF;
    m := regexp_match(s, '(\d+(?:[.,]\d+)?)\s*a(ñ|n)os?');
    IF m IS NOT NULL THEN months := months + replace(m[1], ',', '.')::NUMERIC * 12; hit := TRUE; END IF;
    m := regexp_match(s, '(\d+)\s*mes');
    IF m IS NOT NULL THEN months := months + m[1]::NUMERIC; hit := TRUE; END IF;
    m := regexp_match(s, '(\d+)\s*semana');
    IF m IS NOT NULL THEN months := months + m[1]::NUMERIC / 4; hit := TRUE; END IF;
    IF s ~ 'medio' THEN months := months + 6; hit := TRUE; END IF;
    RETURN CASE WHEN hit THEN LEAST(round(months), 600)::INT END;
END;
$$;

-- Hora del día de la n-ésima mención ("8:00 a. m.", "2.30 pm", "14:00") o NULL. "8 nov" no es una hora:
-- se exigen minutos o a. m./p. m.
CREATE OR REPLACE FUNCTION public.parse_time_of_day(p TEXT, n INT DEFAULT 1)
RETURNS INTERVAL
LANGUAGE plpgsql IMMUTABLE
SET search_path = pg_catalog, pg_temp
AS $$
DECLARE
    m TEXT[];
    h INT;
BEGIN
    SELECT t.x INTO m
      FROM regexp_matches(lower(coalesce(p, '')), '(\d{1,2})(?:[:.](\d{2}))?\s*(?:([ap])\.?\s*m\M\.?)?', 'g')
           WITH ORDINALITY AS t(x, i)
     WHERE t.x[2] IS NOT NULL OR t.x[3] IS NOT NULL
     ORDER BY t.i OFFSET n - 1 LIMIT 1;
    IF m IS NULL THEN RETURN NULL; END IF;
    h := m[1]::INT;
    IF m[3] IS NOT NULL THEN h := h % 12 + CASE WHEN m[3] = 'p' THEN 12 ELSE 0 END; END IF;
    IF h > 23 OR coalesce(m[2], '0')::INT > 59 THEN RETURN NULL; END IF;
    RETURN make_interval(hours => h, mins => coalesce(m[2], '0')::INT);
END;
$$;

-- "Sáb 8 nov · 8:00 a. m." / "sabado 7 de noviembre a las 2.30 pm" / ISO 8601. El año sale de p_ref
-- (creación de la publicación); si la fecha queda más de 6 meses antes, es del año siguiente.
CREATE OR REPLACE FUNCTION public.parse_event_at(p TEXT, p_ref TIMESTAMPTZ)
RETURNS TIMESTAMPTZ
LANGUAGE plpgsql STABLE
SET search_path = pg_catalog, pg_temp
AS $$
DECLARE
    s TEXT := lower(btrim(coalesce(p, '')));
    m TEXT[];
    ref TIMESTAMP := coalesce(p_ref, now()) AT TIME ZONE 'America/Bogota';
    d TIMESTAMP;
BEGIN
    IF s ~ '^\d{4}-\d{2}-\d{2}' THEN
        BEGIN RETURN s::TIMESTAMPTZ; EXCEPTION WHEN others THEN RETURN NULL; END;
    END IF;
    m := regexp_match(s, '(\d{1,2})\s*(?:de\s+)?(ene|feb|mar|abr|may|jun|jul|ago|sep|oct|nov|dic)');
    IF m IS NULL THEN RETURN NULL; END IF;
    BEGIN
        d := make_timestamp(extract(year FROM ref)::INT,
                            array_position(ARRAY['ene','feb','mar','abr','may','jun','jul','ago','sep','oct','nov','dic'], m[2]),
                            m[1]::INT, 0, 0, 0);
    EXCEPTION WHEN others THEN RETURN NULL;
    END;
    IF d < ref - INTERVAL '6 months' THEN d := d + INTERVAL '1 year'; END IF;
    RETURN (d + coalesce(public.parse_time_of_day(s), INTERVAL '0')) AT TIME ZONE 'America/Bogota';
END;
$$;

REVOKE ALL ON FUNCTION public.parse_age_months(TEXT) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.parse_time_of_day(TEXT, INT) FROM PUBLIC, anon, authenticated;
REVOKE ALL ON FUNCTION public.parse_event_at(TEXT, TIMESTAMPTZ) FROM PUBLIC, anon, authenticated;

-- Deriva las columnas tipadas del texto cuando el cliente cambia el texto sin enviar la tipada.
-- La hora de inicio sale de event_date o, si no trae, de la primera hora de schedule; el fin, de la segunda.
CREATE OR REPLACE FUNCTION public.publications_typed_fields()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = public, pg_temp
AS $$
DECLARE
    ins BOOLEAN := TG_OP = 'INSERT';
    day TIMESTAMP;
BEGIN
    IF (ins AND NEW.age_months IS NULL)
       OR (NOT ins AND NEW.age IS DISTINCT FROM OLD.age AND NEW.age_months IS NOT DISTINCT FROM OLD.age_months) THEN
        NEW.age_months := public.parse_age_months(NEW.age);
    END IF;
    IF (ins AND NEW.event_at IS NULL)
       OR (NOT ins AND (NEW.event_date, NEW.schedule) IS DISTINCT FROM (OLD.event_date, OLD.schedule)
           AND NEW.event_at IS NOT DISTINCT FROM OLD.event_at) THEN
        NEW.event_at := public.parse_event_at(NEW.event_date, NEW.created_at);
        NEW.event_end_at := NULL;
        IF NEW.event_at IS NOT NULL THEN
            day := date_trunc('day', NEW.event_at AT TIME ZONE 'America/Bogota');
            IF public.parse_time_of_day(NEW.event_date) IS NULL AND public.parse_time_of_day(NEW.schedule) IS NOT NULL THEN
                NEW.event_at := (day + public.parse_time_of_day(NEW.schedule)) AT TIME ZONE 'America/Bogota';
            END IF;
            NEW.event_end_at := (day + public.parse_time_of_day(NEW.schedule, 2)) AT TIME ZONE 'America/Bogota';
            IF NEW.event_end_at < NEW.event_at THEN NEW.event_end_at := NULL; END IF;
        END IF;
    END IF;
    RETURN NEW;
END;
$$;
REVOKE ALL ON FUNCTION public.publications_typed_fields() FROM PUBLIC, anon, authenticated;

DROP TRIGGER IF EXISTS publications_typed_fields ON public.publications;
CREATE TRIGGER publications_typed_fields
    BEFORE INSERT OR UPDATE ON public.publications
    FOR EACH ROW EXECUTE FUNCTION public.publications_typed_fields();

-- Backfill best-effort de las filas existentes (como postgres: guard_publication_author no interviene).
UPDATE public.publications SET age_months = public.parse_age_months(age)
 WHERE age_months IS NULL AND age IS NOT NULL;
WITH parsed AS (
    SELECT id, public.parse_event_at(event_date, created_at) AS at,
           public.parse_time_of_day(event_date) AS t1, public.parse_time_of_day(schedule) AS s1,
           public.parse_time_of_day(schedule, 2) AS s2
      FROM public.publications WHERE event_at IS NULL AND event_date IS NOT NULL
), spans AS (
    SELECT id, CASE WHEN t1 IS NULL AND s1 IS NOT NULL
                    THEN (date_trunc('day', at AT TIME ZONE 'America/Bogota') + s1) AT TIME ZONE 'America/Bogota'
                    ELSE at END AS starts,
           (date_trunc('day', at AT TIME ZONE 'America/Bogota') + s2) AT TIME ZONE 'America/Bogota' AS ends
      FROM parsed WHERE at IS NOT NULL
)
UPDATE public.publications p
   SET event_at = s.starts, event_end_at = CASE WHEN s.ends >= s.starts THEN s.ends END
  FROM spans s WHERE p.id = s.id;

-- Ítem 51: "miembro desde" deriva de created_at; deja de ser un TEXT con default to_char(now()).
-- SMALLINT: la app lo lee con asString ("2026") igual que antes.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'profiles' AND column_name = 'member_since'
                  AND is_generated = 'NEVER') THEN
        ALTER TABLE public.profiles DROP COLUMN member_since;
        ALTER TABLE public.profiles ADD COLUMN member_since SMALLINT
            GENERATED ALWAYS AS (extract(year FROM created_at AT TIME ZONE 'America/Bogota')::SMALLINT) STORED;
    END IF;
END $$;
GRANT SELECT (member_since) ON public.profiles TO anon, authenticated;
COMMENT ON COLUMN public.profiles.member_since IS 'Año de alta, calculado de created_at (no se escribe).';

-- Ítem 52: el color de la tarjeta es presentación; la app lo deriva de la categoría (lee "tone" con default).
ALTER TABLE public.publications DROP COLUMN IF EXISTS tone;
