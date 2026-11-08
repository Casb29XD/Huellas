-- Ítems 1, 2 y 23: alta de usuario segura, campos de perfil protegidos y email sincronizado con auth.

-- Ítem 23: auth.users.email admite NULL (OAuth/teléfono); profiles.email también (UNIQUE admite varios NULL).
ALTER TABLE public.profiles ALTER COLUMN email DROP NOT NULL;

-- Ítem 1: is_moderator NUNCA sale del metadata (lo controla el cliente en el signup); tolera email NULL.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    user_name TEXT := LEFT(COALESCE(NULLIF(BTRIM(NEW.raw_user_meta_data->>'name'), ''),
                                    NULLIF(split_part(NEW.email, '@', 1), ''),
                                    'Usuario Huella'), 80);
BEGIN
    INSERT INTO public.profiles (id, name, email, city, phone, bio, initials, points, level, is_moderator)
    VALUES (
        NEW.id,
        user_name,
        NEW.email,
        LEFT(COALESCE(NULLIF(BTRIM(NEW.raw_user_meta_data->>'city'), ''), 'Bogotá'), 80),
        LEFT(COALESCE(NEW.raw_user_meta_data->>'phone', ''), 30),
        LEFT(COALESCE(NEW.raw_user_meta_data->>'bio', ''), 500),
        UPPER(LEFT(user_name, 2)),
        0,
        'AMIGO_ANIMAL',
        FALSE
    );
    RETURN NEW;
END;
$$;

-- Ítem 23: el email del perfil sigue a auth.users (cambia solo tras confirmar en PUT auth/v1/user).
CREATE OR REPLACE FUNCTION public.sync_profile_email()
RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    UPDATE public.profiles SET email = NEW.email WHERE id = NEW.id;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_email_updated ON auth.users;
CREATE TRIGGER on_auth_user_email_updated
    AFTER UPDATE OF email ON auth.users
    FOR EACH ROW WHEN (OLD.email IS DISTINCT FROM NEW.email)
    EXECUTE FUNCTION public.sync_profile_email();

-- Ítem 2: los privilegios de columna ya impiden el PATCH; este trigger es la segunda barrera.
-- Pasan: service_role, funciones SECURITY DEFINER (corren como su dueño) y código interno que haga
-- set_config('huella.internal', '1', true).
CREATE OR REPLACE FUNCTION public.protect_profile_fields()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = public, pg_temp
AS $$
BEGIN
    IF current_user IN ('anon', 'authenticated')
       AND COALESCE(current_setting('huella.internal', true), '') <> '1'
       AND (NEW.id, NEW.email, NEW.points, NEW.level, NEW.is_moderator, NEW.publications_count,
            NEW.adoptions_count, NEW.foster_count, NEW.active_count)
           IS DISTINCT FROM
           (OLD.id, OLD.email, OLD.points, OLD.level, OLD.is_moderator, OLD.publications_count,
            OLD.adoptions_count, OLD.foster_count, OLD.active_count)
    THEN
        RAISE EXCEPTION 'No puedes modificar puntos, nivel, rol, contadores ni email del perfil'
            USING ERRCODE = '42501';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS protect_profile_fields ON public.profiles;
CREATE TRIGGER protect_profile_fields
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.protect_profile_fields();
