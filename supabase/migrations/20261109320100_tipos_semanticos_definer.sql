-- Corrige 20261109320000: el trigger corría como el cliente (authenticated) y los parseadores no tienen
-- EXECUTE para él, así que todo INSERT/UPDATE de publications desde la app fallaba con 42501.
-- El trigger pasa a SECURITY DEFINER (solo escribe en NEW; los parseadores siguen sin exponerse por RPC).
ALTER FUNCTION public.publications_typed_fields() SECURITY DEFINER;
