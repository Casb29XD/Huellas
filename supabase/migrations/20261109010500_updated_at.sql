-- Ítem 51: updated_at se mantiene solo en todas las tablas de public que lo tengan.

CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = public, pg_temp
AS $$
BEGIN
    NEW.updated_at := NOW();
    RETURN NEW;
END;
$$;

DO $$
DECLARE t text;
BEGIN
    FOR t IN
        SELECT c.table_name FROM information_schema.columns c
          JOIN information_schema.tables tb USING (table_schema, table_name)
         WHERE c.table_schema = 'public' AND c.column_name = 'updated_at' AND tb.table_type = 'BASE TABLE'
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS set_updated_at ON public.%I', t);
        EXECUTE format('CREATE TRIGGER set_updated_at BEFORE UPDATE ON public.%I
                        FOR EACH ROW EXECUTE FUNCTION public.set_updated_at()', t);
    END LOOP;
END $$;
