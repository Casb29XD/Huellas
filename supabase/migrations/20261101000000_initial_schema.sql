-- ========================================================================
-- HUELLA APP - ESQUEMA DE BASE DE DATOS COMPLETO PARA SUPABASE (POSTGRESQL)
-- Incluye: Autenticación con auth.users, Triggers automáticos, RLS, Storage y Funciones
-- ========================================================================

-- 1. Habilitar extensiones necesarias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Creación de Tipos ENUM personalizados
DO $$ BEGIN
    CREATE TYPE user_level AS ENUM ('AMIGO_ANIMAL', 'PROTECTOR', 'GUARDIAN', 'HEROE');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE publication_category AS ENUM ('ADOPCION', 'PERDIDOS', 'ENCONTRADOS', 'TEMPORAL', 'VETERINARIA');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE publication_status AS ENUM ('EN_REVISION', 'APROBADA', 'RECHAZADA');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE request_status AS ENUM ('PENDIENTE', 'ACEPTADA', 'RECHAZADA');
EXCEPTION WHEN duplicate_object THEN null; END $$;

DO $$ BEGIN
    CREATE TYPE notification_type AS ENUM ('NEW_REQUEST', 'REQUEST_ACCEPTED', 'POST_APPROVED', 'POST_REJECTED', 'LEVEL_UP');
EXCEPTION WHEN duplicate_object THEN null; END $$;


-- ========================================================================
-- TABLA: profiles (Vinculada directamente a auth.users de Supabase)
-- ========================================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    city TEXT NOT NULL,
    phone TEXT DEFAULT '',
    bio TEXT DEFAULT '',
    avatar_url TEXT,
    points INT DEFAULT 0 CHECK (points >= 0),
    level user_level DEFAULT 'AMIGO_ANIMAL',
    initials VARCHAR(4) DEFAULT '',
    member_since TEXT DEFAULT TO_CHAR(NOW(), 'YYYY'),
    is_moderator BOOLEAN DEFAULT FALSE,
    publications_count INT DEFAULT 0,
    adoptions_count INT DEFAULT 0,
    foster_count INT DEFAULT 0,
    active_count INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========================================================================
-- TABLA: publications (Mascotas en adopción, perdidos, veterinaria, etc.)
-- ========================================================================
CREATE TABLE IF NOT EXISTS public.publications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    author_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    category publication_category NOT NULL DEFAULT 'ADOPCION',
    title TEXT NOT NULL,
    species TEXT NOT NULL,            -- Perro, Gato, Ave, Conejo, Roedor, Otro
    breed TEXT NOT NULL,              -- Raza o aproximación
    size TEXT NOT NULL,               -- Pequeño, Mediano, Grande, Todos
    vaccinated BOOLEAN NOT NULL DEFAULT FALSE,
    sterilized BOOLEAN DEFAULT FALSE,
    age TEXT,                         -- Ej: "2 años", "6 meses"
    sex TEXT,                         -- Macho, Hembra, Desconocido
    city TEXT NOT NULL,
    neighborhood TEXT,
    description TEXT NOT NULL,
    image_url TEXT,
    tone BIGINT DEFAULT 3632851165,   -- Tono ARGB (ej: 0xFFD8E8DD)
    photo_label TEXT,
    status publication_status NOT NULL DEFAULT 'EN_REVISION',
    -- Campos extras para Perdidos, Encontrados o Jornadas Veterinarias
    event_date TEXT,                  -- Ej: "Sáb 5 sep · 6:30 p. m."
    schedule TEXT,                    -- Ej: "8:00 a. m. – 1:00 p. m."
    place TEXT,                       -- Ej: "Parque El Virrey, Chapinero"
    services TEXT[] DEFAULT '{}',     -- Ej: '{"Vacunación","Esterilización"}'
    capacity INT,
    phone TEXT,
    latitude DOUBLE PRECISION DEFAULT 0.0,
    longitude DOUBLE PRECISION DEFAULT 0.0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========================================================================
-- TABLA: adoption_requests (Solicitudes de adopción / hogar temporal)
-- ========================================================================
CREATE TABLE IF NOT EXISTS public.adoption_requests (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    publication_id UUID NOT NULL REFERENCES public.publications(id) ON DELETE CASCADE,
    applicant_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    status request_status NOT NULL DEFAULT 'PENDIENTE',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(publication_id, applicant_id) -- Un usuario solo envía una solicitud por publicación
);

-- ========================================================================
-- TABLA: conversations (Salas de chat entre adoptante y publicador)
-- ========================================================================
CREATE TABLE IF NOT EXISTS public.conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    publication_id UUID REFERENCES public.publications(id) ON DELETE SET NULL,
    participant_one UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    participant_two UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    about TEXT,                       -- Ej: "Sobre: Max, beagle juguetón"
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT different_participants CHECK (participant_one <> participant_two)
);

-- ========================================================================
-- TABLA: chat_messages (Mensajes de chat en tiempo real)
-- ========================================================================
CREATE TABLE IF NOT EXISTS public.chat_messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL REFERENCES public.conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========================================================================
-- TABLA: reports (Reportes de publicaciones para moderación)
-- ========================================================================
CREATE TABLE IF NOT EXISTS public.reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    publication_id UUID NOT NULL REFERENCES public.publications(id) ON DELETE CASCADE,
    reporter_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    reason TEXT NOT NULL,
    detail TEXT DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========================================================================
-- TABLA: notifications (Notificaciones para los usuarios)
-- ========================================================================
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    type notification_type NOT NULL,
    title TEXT NOT NULL,
    text TEXT NOT NULL,
    group_label TEXT DEFAULT 'Hoy',   -- "Hoy", "Ayer", "Esta semana"
    target_id TEXT,                   -- ID de publicación o conversación relacionada
    read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ========================================================================
-- TABLA: points_history (Historial de puntos y gamificación)
-- ========================================================================
CREATE TABLE IF NOT EXISTS public.points_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    action TEXT NOT NULL,             -- Ej: "Publicación aprobada", "Adopción concretada"
    points INT NOT NULL,              -- Puntos sumados
    created_at TIMESTAMPTZ DEFAULT NOW()
);


-- ========================================================================
-- TRIGGERS Y FUNCIONES AUTOMÁTICAS
-- ========================================================================

-- 1. Disparador automático al registrarse un usuario en auth.users de Supabase
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    user_name TEXT;
    calc_initials VARCHAR(4);
BEGIN
    -- Obtener nombre desde metadata o email
    user_name := COALESCE(NEW.raw_user_meta_data->>'name', split_part(NEW.email, '@', 1));

    -- Calcular iniciales
    calc_initials := UPPER(SUBSTRING(user_name FROM 1 FOR 2));

    INSERT INTO public.profiles (
        id,
        name,
        email,
        city,
        phone,
        bio,
        initials,
        points,
        level,
        is_moderator
    ) VALUES (
        NEW.id,
        user_name,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'city', 'Bogotá'),
        COALESCE(NEW.raw_user_meta_data->>'phone', ''),
        COALESCE(NEW.raw_user_meta_data->>'bio', ''),
        calc_initials,
        0,
        'AMIGO_ANIMAL',
        COALESCE((NEW.raw_user_meta_data->>'is_moderator')::boolean, FALSE)
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();


-- 2. Disparador para actualizar el nivel del usuario según sus puntos
CREATE OR REPLACE FUNCTION public.update_user_level()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.points >= 500 THEN
        NEW.level := 'HEROE';
    ELSIF NEW.points >= 200 THEN
        NEW.level := 'GUARDIAN';
    ELSIF NEW.points >= 50 THEN
        NEW.level := 'PROTECTOR';
    ELSE
        NEW.level := 'AMIGO_ANIMAL';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_user_level ON public.profiles;
CREATE TRIGGER trg_update_user_level
    BEFORE UPDATE OF points ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION public.update_user_level();


-- ========================================================================
-- POLÍTICAS DE SEGURIDAD (ROW LEVEL SECURITY - RLS)
-- ========================================================================
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.publications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.adoption_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.points_history ENABLE ROW LEVEL SECURITY;

-- Profiles: Cualquiera puede ver perfiles públicos; sólo el dueño edita su perfil
CREATE POLICY "Perfiles públicos visibles para todos"
    ON public.profiles FOR SELECT USING (true);

CREATE POLICY "Usuarios pueden actualizar su propio perfil"
    ON public.profiles FOR UPDATE USING (auth.uid() = id);

-- Publications:
-- - Aprobadas visibles para todos los autenticados o anónimos
-- - En revisión o rechazadas visibles solo para su autor o moderadores
CREATE POLICY "Publicaciones aprobadas visibles para todos"
    ON public.publications FOR SELECT
    USING (
        status = 'APROBADA'
        OR auth.uid() = author_id
        OR EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND is_moderator = TRUE)
    );

CREATE POLICY "Usuarios pueden crear publicaciones"
    ON public.publications FOR INSERT
    WITH CHECK (auth.uid() = author_id);

CREATE POLICY "Autores y moderadores pueden actualizar publicaciones"
    ON public.publications FOR UPDATE
    USING (
        auth.uid() = author_id
        OR EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND is_moderator = TRUE)
    );

CREATE POLICY "Autores pueden eliminar sus publicaciones"
    ON public.publications FOR DELETE
    USING (auth.uid() = author_id);

-- Adoption Requests:
CREATE POLICY "Ver solicitudes enviadas o recibidas"
    ON public.adoption_requests FOR SELECT
    USING (
        auth.uid() = applicant_id
        OR auth.uid() IN (SELECT author_id FROM public.publications WHERE id = publication_id)
    );

CREATE POLICY "Usuarios pueden enviar solicitudes de adopción"
    ON public.adoption_requests FOR INSERT
    WITH CHECK (auth.uid() = applicant_id);

CREATE POLICY "Autor de publicación puede responder solicitudes"
    ON public.adoption_requests FOR UPDATE
    USING (auth.uid() IN (SELECT author_id FROM public.publications WHERE id = publication_id));

-- Conversations & Messages:
CREATE POLICY "Participantes pueden ver la conversación"
    ON public.conversations FOR SELECT
    USING (auth.uid() = participant_one OR auth.uid() = participant_two);

CREATE POLICY "Participantes pueden crear conversaciones"
    ON public.conversations FOR INSERT
    WITH CHECK (auth.uid() = participant_one OR auth.uid() = participant_two);

CREATE POLICY "Participantes pueden leer mensajes"
    ON public.chat_messages FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.conversations
            WHERE id = chat_messages.conversation_id
            AND (participant_one = auth.uid() OR participant_two = auth.uid())
        )
    );

CREATE POLICY "Participantes pueden enviar mensajes"
    ON public.chat_messages FOR INSERT
    WITH CHECK (
        auth.uid() = sender_id AND
        EXISTS (
            SELECT 1 FROM public.conversations
            WHERE id = chat_messages.conversation_id
            AND (participant_one = auth.uid() OR participant_two = auth.uid())
        )
    );

-- Notifications:
CREATE POLICY "Usuarios ven solo sus notificaciones"
    ON public.notifications FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Usuarios pueden marcar sus notificaciones"
    ON public.notifications FOR UPDATE
    USING (auth.uid() = user_id);

-- Reports:
CREATE POLICY "Usuarios pueden enviar reportes"
    ON public.reports FOR INSERT
    WITH CHECK (auth.uid() = reporter_id);

CREATE POLICY "Moderadores pueden ver reportes"
    ON public.reports FOR SELECT
    USING (EXISTS (SELECT 1 FROM public.profiles WHERE id = auth.uid() AND is_moderator = TRUE));

-- Points History:
CREATE POLICY "Usuarios ven su propio historial de puntos"
    ON public.points_history FOR SELECT
    USING (auth.uid() = user_id);


-- ========================================================================
-- STORAGE BUCKET: imágenes de mascotas y reportes
-- ========================================================================
INSERT INTO storage.buckets (id, name, public)
VALUES ('pet-images', 'pet-images', true)
ON CONFLICT (id) DO NOTHING;

CREATE POLICY "Imágenes de mascotas públicas"
    ON storage.objects FOR SELECT
    USING (bucket_id = 'pet-images');

CREATE POLICY "Usuarios autenticados pueden subir fotos"
    ON storage.objects FOR INSERT
    WITH CHECK (bucket_id = 'pet-images' AND auth.role() = 'authenticated');
