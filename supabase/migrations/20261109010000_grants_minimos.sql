-- Corrige 20261104000000_data_api_grants (SELECT total a anon, ALL a authenticated) y cierra el ítem 4:
-- privilegios mínimos por tabla/columna para la Data API. RLS sigue decidiendo las filas.
-- Efecto en PostgREST: select=* sobre profiles FALLA con 42501 ("permission denied for table profiles")
-- para anon y authenticated, porque * incluye email/phone/bio. Hay que pedir columnas explícitas
-- o usar rpc/get_my_profile (perfil propio completo) y rpc/get_contact_phone (teléfono del otro participante).
-- ponytail: tablas nuevas de otras migraciones conceden sus propios GRANT; aquí solo las del esquema inicial.

DO $$
DECLARE t text;
BEGIN
    FOREACH t IN ARRAY ARRAY['profiles','publications','adoption_requests','conversations',
                             'chat_messages','reports','notifications','points_history'] LOOP
        EXECUTE format('REVOKE ALL ON public.%I FROM anon, authenticated', t);
        EXECUTE format('GRANT ALL ON public.%I TO service_role', t);
    END LOOP;
END $$;

-- profiles: columnas públicas para todos; UPDATE propio solo en campos editables por el usuario.
GRANT SELECT (id, name, city, level, initials, points, member_since, avatar_url, is_moderator,
              publications_count, adoptions_count, foster_count, active_count, created_at)
    ON public.profiles TO anon, authenticated;
GRANT UPDATE (name, city, phone, bio, avatar_url, initials) ON public.profiles TO authenticated;

-- anon solo lee publicaciones (RLS: solo APROBADA) y las columnas públicas de profiles.
GRANT SELECT ON public.publications TO anon;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.publications TO authenticated;

GRANT SELECT, INSERT ON public.adoption_requests TO authenticated;
GRANT UPDATE (status) ON public.adoption_requests TO authenticated;

GRANT SELECT, INSERT ON public.conversations TO authenticated;
GRANT SELECT, INSERT ON public.chat_messages TO authenticated;

GRANT SELECT ON public.reports TO authenticated;
GRANT INSERT (publication_id, reporter_id, reason, detail) ON public.reports TO authenticated;

GRANT SELECT, DELETE ON public.notifications TO authenticated;
GRANT UPDATE (read) ON public.notifications TO authenticated;
DROP POLICY IF EXISTS "Usuarios pueden borrar sus notificaciones" ON public.notifications;
CREATE POLICY "Usuarios pueden borrar sus notificaciones"
    ON public.notifications FOR DELETE TO authenticated USING (auth.uid() = user_id);

GRANT SELECT ON public.points_history TO authenticated;

-- Las funciones nuevas de public reciben EXECUTE para anon por default privileges (no vía PUBLIC).
REVOKE EXECUTE ON FUNCTION public.moderate_publication(UUID, public.publication_status, TEXT, TEXT) FROM PUBLIC, anon;

-- Perfil propio completo (incluye email, phone, bio).
CREATE OR REPLACE FUNCTION public.get_my_profile()
RETURNS SETOF public.profiles
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    SELECT * FROM public.profiles WHERE id = auth.uid();
$$;
REVOKE ALL ON FUNCTION public.get_my_profile() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_my_profile() TO authenticated;

-- Teléfono del OTRO participante; NULL si quien llama no participa en la conversación.
CREATE OR REPLACE FUNCTION public.get_contact_phone(p_conversation_id UUID)
RETURNS TEXT
LANGUAGE sql STABLE SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    SELECT p.phone
      FROM public.conversations c
      JOIN public.profiles p
        ON p.id = CASE WHEN c.participant_one = auth.uid() THEN c.participant_two ELSE c.participant_one END
     WHERE c.id = p_conversation_id
       AND auth.uid() IN (c.participant_one, c.participant_two);
$$;
REVOKE ALL ON FUNCTION public.get_contact_phone(UUID) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.get_contact_phone(UUID) TO authenticated;
