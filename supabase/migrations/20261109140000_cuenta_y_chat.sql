-- Ítems 8 y 20: eliminar la cuenta de verdad; conversations.updated_at sigue al último mensaje y Realtime
-- emite chat_messages y notifications.

-- Ítem 8: borra las fotos del usuario y su fila de auth.users; profiles y todo lo suyo cae por ON DELETE CASCADE.
-- ponytail: como delete_publication_images, borra la fila de storage.objects; el binario queda huérfano en S3.
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
    PERFORM set_config('storage.allow_delete_query', 'true', true);
    DELETE FROM storage.objects WHERE bucket_id = 'pet-images' AND (storage.foldername(name))[1] = uid::text;
    DELETE FROM auth.users WHERE id = uid;
END;
$$;
REVOKE ALL ON FUNCTION public.delete_my_account() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.delete_my_account() TO authenticated;

-- Ítem 20: la lista de chats ordena por updated_at.desc.
CREATE OR REPLACE FUNCTION public.touch_conversation()
RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    UPDATE public.conversations SET updated_at = NOW() WHERE id = NEW.conversation_id;
    RETURN NULL;
END;
$$;
REVOKE ALL ON FUNCTION public.touch_conversation() FROM PUBLIC, anon, authenticated;

DROP TRIGGER IF EXISTS touch_conversation ON public.chat_messages;
CREATE TRIGGER touch_conversation
    AFTER INSERT ON public.chat_messages
    FOR EACH ROW EXECUTE FUNCTION public.touch_conversation();

-- Realtime respeta la RLS de SELECT: cada usuario solo recibe sus mensajes y notificaciones.
DO $$
DECLARE t TEXT;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime') THEN
        CREATE PUBLICATION supabase_realtime;
    END IF;
    FOREACH t IN ARRAY ARRAY['chat_messages', 'notifications'] LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_publication_tables
                        WHERE pubname = 'supabase_realtime' AND schemaname = 'public' AND tablename = t) THEN
            EXECUTE format('ALTER PUBLICATION supabase_realtime ADD TABLE public.%I', t);
        END IF;
    END LOOP;
END $$;
