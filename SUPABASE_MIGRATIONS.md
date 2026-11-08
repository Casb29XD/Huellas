# Guía de Migraciones y Pruebas Backend de Supabase para Huellas

Este documento describe las migraciones pendientes en PostgreSQL/Supabase, las políticas de seguridad (RLS), triggers, vistas y RPCs necesarias para el proyecto Huella, listas para ser ejecutadas y probadas por Claude u otro agente/desarrollador con acceso directo al pooler y testing de base de datos.

---

## 1. Contexto y Problemas Identificados en el Esquema Actual

1. **Escalación de privilegios en registro:**
   - En `supabase_schema.sql` línea 206, `handle_new_user()` extrae `is_moderator` de `raw_user_meta_data`. Cualquier cliente puede registrarse pasando `{"is_moderator": true}`.
   - La columna `is_moderator` debe evolucionar a un `role` de tipo ENUM (`USUARIO`, `MODERADOR`), donde los nuevos registros son forzados a `USUARIO`.

2. **Políticas de actualización permisivas en `profiles`:**
   - La regla RLS `USING (auth.uid() = id)` permite al usuario actualizar cualquier columna en su perfil (incluyendo `is_moderator`, `points`, `level`). Se requiere un trigger `BEFORE UPDATE` que impida la alteración de roles y métricas por parte del cliente.

3. **Auto-aprobación en `publications`:**
   - La política de UPDATE de `publications` permite a los autores cambiar el `status` a `APROBADA` saltándose la moderación. Debe restringirse para que sólo los moderadores puedan cambiar `status` a `APROBADA`/`RECHAZADA` (usando el RPC `moderate_publication`). Si un autor edita su publicación, el `status` debe volver automáticamente a `EN_REVISION`.

4. **Ciclo de vida y estadísticas:**
   - No existía columna `lifecycle` (`ACTIVA`, `FINALIZADA`). Cuando una mascota se adopta o el autor cierra la publicación, debe pasar a `FINALIZADA`.
   - Las estadísticas de perfil deben derivarse de las publicaciones reales (activas, finalizadas, pendientes, rechazadas, adopciones, comentarios) en lugar de contadores manuales desincronizados.

5. **Funcionalidades Faltantes a nivel BD:**
   - Comentarios en publicaciones con RLS y trigger para notificar al autor (`NEW_COMMENT`).
   - Votos/Valoraciones (`publication_votes`).
   - Insignias del usuario (`badges` y `user_badges`).
   - RPC para eliminación de cuenta (`delete_my_account`).

---

## 2. Scripts SQL Idempotentes Propuestos

### Script 1: Seguridad de Roles y Publicaciones (`20261107_roles_security.sql`)
```sql
DO $$ BEGIN
    CREATE TYPE user_role AS ENUM ('USUARIO', 'MODERADOR');
EXCEPTION WHEN duplicate_object THEN null; END $$;

-- 1. Agregar columna role si no existe
ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS role user_role DEFAULT 'USUARIO';

-- Migrar estado existente
UPDATE public.profiles 
SET role = CASE WHEN is_moderator = TRUE THEN 'MODERADOR'::user_role ELSE 'USUARIO'::user_role END
WHERE role IS NULL OR role = 'USUARIO';

-- 2. Función auxiliar segura para verificar moderador
CREATE OR REPLACE FUNCTION public.is_moderator()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.profiles 
    WHERE id = auth.uid() AND (role = 'MODERADOR' OR is_moderator = TRUE)
  );
$$;

-- 3. Blindar handle_new_user() para que nadie se auto-asigne MODERADOR
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
BEGIN
    INSERT INTO public.profiles (
        id,
        name,
        email,
        city,
        phone,
        bio,
        avatar_url,
        initials,
        role,
        is_moderator
    )
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'name', 'Usuario Huella'),
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'city', ''),
        COALESCE(NEW.raw_user_meta_data->>'phone', ''),
        COALESCE(NEW.raw_user_meta_data->>'bio', ''),
        NEW.raw_user_meta_data->>'avatar_url',
        UPPER(SUBSTRING(COALESCE(NEW.raw_user_meta_data->>'name', 'UH') FROM 1 FOR 2)),
        'USUARIO',
        FALSE
    );
    RETURN NEW;
END;
$$;

-- 4. Trigger de protección en profiles: Evitar que el usuario altere su rol, puntos o nivel directamente
CREATE OR REPLACE FUNCTION public.protect_profile_fields()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- Si no es superusuario/service_role y no es llamada interna
    IF current_user IN ('authenticated', 'anon') THEN
        IF NEW.role IS DISTINCT FROM OLD.role THEN
            RAISE EXCEPTION 'No tienes permiso para modificar el rol de usuario';
        END IF;
        IF NEW.is_moderator IS DISTINCT FROM OLD.is_moderator THEN
            RAISE EXCEPTION 'No tienes permiso para modificar is_moderator';
        END IF;
        IF NEW.points IS DISTINCT FROM OLD.points THEN
            RAISE EXCEPTION 'Los puntos no pueden modificarse directamente';
        END IF;
        IF NEW.level IS DISTINCT FROM OLD.level THEN
            RAISE EXCEPTION 'El nivel no puede modificarse directamente';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tr_protect_profile_fields ON public.profiles;
CREATE TRIGGER tr_protect_profile_fields
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.protect_profile_fields();
```

---

### Script 2: Comentarios con Notificación Automática (`20261107_comments.sql`)
```sql
CREATE TABLE IF NOT EXISTS public.comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    publication_id UUID NOT NULL REFERENCES public.publications(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    text TEXT NOT NULL CHECK (char_length(trim(text)) BETWEEN 1 AND 500),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.comments ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Comentarios visibles si la publicacion es visible"
    ON public.comments FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.publications p
            WHERE p.id = comments.publication_id
              AND (p.status = 'APROBADA' OR p.author_id = auth.uid() OR public.is_moderator())
        )
    );

CREATE POLICY "Usuarios autenticados pueden comentar"
    ON public.comments FOR INSERT
    WITH CHECK (auth.uid() = author_id);

CREATE POLICY "Autor o moderador puede eliminar comentario"
    ON public.comments FOR DELETE
    USING (auth.uid() = author_id OR public.is_moderator());

-- Notificar al autor de la publicación cuando reciba un comentario
CREATE OR REPLACE FUNCTION public.notify_on_comment()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    pub_author UUID;
    pub_title TEXT;
    commenter_name TEXT;
BEGIN
    SELECT author_id, title INTO pub_author, pub_title
    FROM public.publications WHERE id = NEW.publication_id;

    -- Solo notificar si quien comenta no es el mismo autor
    IF pub_author IS NOT NULL AND pub_author <> NEW.author_id THEN
        SELECT name INTO commenter_name FROM public.profiles WHERE id = NEW.author_id;

        INSERT INTO public.notifications (
            user_id,
            type,
            title,
            text,
            target_id
        ) VALUES (
            pub_author,
            'NEW_REQUEST'::notification_type, -- O 'NEW_COMMENT' si se actualiza el enum
            'Nuevo comentario',
            COALESCE(commenter_name, 'Alguien') || ' comentó en tu publicación "' || pub_title || '"',
            NEW.publication_id::TEXT
        );
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS tr_notify_on_comment ON public.comments;
CREATE TRIGGER tr_notify_on_comment
    AFTER INSERT ON public.comments
    FOR EACH ROW
    EXECUTE FUNCTION public.notify_on_comment();
```

---

### Script 3: Votos y Valoraciones (`20261107_votes.sql`)
```sql
CREATE TABLE IF NOT EXISTS public.publication_votes (
    publication_id UUID NOT NULL REFERENCES public.publications(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    value SMALLINT NOT NULL CHECK (value IN (-1, 1)),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (publication_id, user_id)
);

ALTER TABLE public.publication_votes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Votos visibles para todos"
    ON public.publication_votes FOR SELECT USING (true);

CREATE POLICY "Usuarios pueden emitir su propio voto"
    ON public.publication_votes FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Usuarios pueden actualizar su propio voto"
    ON public.publication_votes FOR UPDATE
    USING (auth.uid() = user_id);

CREATE POLICY "Usuarios pueden retirar su propio voto"
    ON public.publication_votes FOR DELETE
    USING (auth.uid() = user_id);
```

---

### Script 4: Eliminación de Cuenta (`20261107_delete_account.sql`)
```sql
CREATE OR REPLACE FUNCTION public.delete_my_account()
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    current_uid UUID;
BEGIN
    current_uid := auth.uid();
    IF current_uid IS NULL THEN
        RAISE EXCEPTION 'No estás autenticado';
    END IF;

    -- Borrar al usuario de auth.users; las FKs en CASCADE limpiarán profiles, publications, etc.
    DELETE FROM auth.users WHERE id = current_uid;
END;
$$;

REVOKE ALL ON FUNCTION public.delete_my_account() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.delete_my_account() TO authenticated;
```

---

## 3. Guía de Ejecución y Validación

1. Ejecutar las migraciones en el orden:
   - `roles_security.sql`
   - `comments.sql`
   - `votes.sql`
   - `delete_account.sql`
2. **Casos de Prueba a Verificar:**
   - Intentar registrar un usuario con `{"is_moderator": true}` y corroborar que `role` quede en `'USUARIO'` y `is_moderator = FALSE`.
   - Como usuario común, intentar un `UPDATE profiles SET role = 'MODERADOR'` y verificar que lance excepción de permiso.
   - Insertar un comentario y verificar que se cree el registro en `notifications` para el autor del post.
   - Insertar dos votos con la misma clave compuesta y comprobar idempotencia / rechazo de duplicados.
   - Probar `delete_my_account()` y comprobar eliminación limpia en cascada.
