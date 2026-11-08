-- ============================================================================
-- HUELLA APP - DATOS DE PRUEBA Y USUARIO ADMINISTRADOR / SUPERVISOR
-- ============================================================================
-- INSTRUCCIONES DE USO:
--   1. Abre el SQL Editor en tu proyecto de Supabase.
--   2. Ejecuta primero supabase_schema.sql si aún no lo has hecho.
--   3. Ejecuta supabase_migrations/20261103_moderation_rpc.sql.
--   4. Ejecuta ESTE archivo (20261106_seed_data.sql).
--
-- CREDENCIALES DEL ADMINISTRADOR:
--   Email   : admin@huella.app
--   Password: Admin1234!
--   Rol     : is_moderator = TRUE (supervisor/moderador de contenidos)
-- ============================================================================

-- ============================================================================
-- PASO 1 · Crear usuarios en auth.users y sus perfiles en public.profiles
-- ============================================================================
-- Supabase no permite insertar directamente en auth.users desde SQL sin ser
-- superusuario. Hay dos opciones:
--
-- Opción A (recomendada para desarrollo): Usa la función interna de Supabase.
-- Opción B: Crea los usuarios desde la UI (Authentication > Users) y luego
--           actualiza sus perfiles con el bloque UPDATE al final.
--
-- Este script usa la función auth.create_user() disponible en Supabase >= 2.x.
-- Si tu versión no la tiene, cambia a Opción B.
-- ============================================================================

DO $$
DECLARE
  v_admin_id    UUID;
  v_user1_id    UUID;
  v_user2_id    UUID;
  v_user3_id    UUID;
  v_pub1_id     UUID;
  v_pub2_id     UUID;
  v_pub3_id     UUID;
  v_pub4_id     UUID;
  v_pub5_id     UUID;
  v_conv_id     UUID;
BEGIN

-- -------------------------------------------------------------------------
-- 1.1 Crear usuario ADMINISTRADOR / SUPERVISOR
-- -------------------------------------------------------------------------
-- Si el usuario ya existe (p.ej. por corridas previas), lo saltamos.
SELECT id INTO v_admin_id FROM auth.users WHERE email = 'admin@huella.app';

IF v_admin_id IS NULL THEN
  v_admin_id := extensions.uuid_generate_v4();
  INSERT INTO auth.users (
    id, instance_id, aud, role,
    email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data,
    created_at, updated_at, confirmation_token, recovery_token,
    email_change_token_new, email_change
  ) VALUES (
    v_admin_id,
    '00000000-0000-0000-0000-000000000000',
    'authenticated', 'authenticated',
    'admin@huella.app',
    crypt('Admin1234!', gen_salt('bf')),
    NOW(),
    '{"provider":"email","providers":["email"]}',
    '{"name":"Administrador Huella","city":"Medellín"}',
    NOW(), NOW(), '', '', '', ''
  );
END IF;

-- Perfil del admin (is_moderator = TRUE)
INSERT INTO public.profiles (
  id, name, email, city, phone, bio,
  points, level, initials, member_since,
  is_moderator, publications_count, adoptions_count, foster_count, active_count
) VALUES (
  v_admin_id,
  'Administrador Huella',
  'admin@huella.app',
  'Medellín',
  '+57 310 000 0001',
  'Supervisor oficial de la plataforma Huella. Revisa y modera publicaciones para mantener la comunidad segura.',
  1500, 'HEROE', 'AH', '2026',
  TRUE, 0, 0, 0, 0
) ON CONFLICT (id) DO UPDATE SET
  is_moderator = TRUE,
  level        = 'HEROE',
  points       = 1500,
  bio          = EXCLUDED.bio;

-- -------------------------------------------------------------------------
-- 1.2 Crear usuario de prueba 1 — Protector
-- -------------------------------------------------------------------------
SELECT id INTO v_user1_id FROM auth.users WHERE email = 'laura@ejemplo.com';
IF v_user1_id IS NULL THEN
  v_user1_id := extensions.uuid_generate_v4();
  INSERT INTO auth.users (
    id, instance_id, aud, role,
    email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data,
    created_at, updated_at, confirmation_token, recovery_token,
    email_change_token_new, email_change
  ) VALUES (
    v_user1_id,
    '00000000-0000-0000-0000-000000000000',
    'authenticated', 'authenticated',
    'laura@ejemplo.com',
    crypt('Laura1234!', gen_salt('bf')),
    NOW(),
    '{"provider":"email","providers":["email"]}',
    '{"name":"Laura Gómez","city":"Medellín"}',
    NOW(), NOW(), '', '', '', ''
  );
END IF;

INSERT INTO public.profiles (
  id, name, email, city, phone, bio,
  points, level, initials, member_since,
  is_moderator, publications_count, adoptions_count, foster_count, active_count
) VALUES (
  v_user1_id,
  'Laura Gómez',
  'laura@ejemplo.com',
  'Medellín',
  '+57 311 222 3344',
  'Amante de los animales. He rescatado 3 perros y 2 gatos desde que descubrí Huella.',
  120, 'PROTECTOR', 'LG', '2026',
  FALSE, 4, 2, 1, 2
) ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------------------
-- 1.3 Crear usuario de prueba 2 — Guardián
-- -------------------------------------------------------------------------
SELECT id INTO v_user2_id FROM auth.users WHERE email = 'carlos@ejemplo.com';
IF v_user2_id IS NULL THEN
  v_user2_id := extensions.uuid_generate_v4();
  INSERT INTO auth.users (
    id, instance_id, aud, role,
    email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data,
    created_at, updated_at, confirmation_token, recovery_token,
    email_change_token_new, email_change
  ) VALUES (
    v_user2_id,
    '00000000-0000-0000-0000-000000000000',
    'authenticated', 'authenticated',
    'carlos@ejemplo.com',
    crypt('Carlos1234!', gen_salt('bf')),
    NOW(),
    '{"provider":"email","providers":["email"]}',
    '{"name":"Carlos Restrepo","city":"Bogotá"}',
    NOW(), NOW(), '', '', '', ''
  );
END IF;

INSERT INTO public.profiles (
  id, name, email, city, phone, bio,
  points, level, initials, member_since,
  is_moderator, publications_count, adoptions_count, foster_count, active_count
) VALUES (
  v_user2_id,
  'Carlos Restrepo',
  'carlos@ejemplo.com',
  'Bogotá',
  '+57 312 555 6677',
  'Veterinario voluntario. Organizo jornadas de vacunación en el barrio.',
  350, 'GUARDIAN', 'CR', '2026',
  FALSE, 8, 5, 3, 3
) ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------------------
-- 1.4 Crear usuario de prueba 3 — Amigo Animal (nuevo)
-- -------------------------------------------------------------------------
SELECT id INTO v_user3_id FROM auth.users WHERE email = 'sofia@ejemplo.com';
IF v_user3_id IS NULL THEN
  v_user3_id := extensions.uuid_generate_v4();
  INSERT INTO auth.users (
    id, instance_id, aud, role,
    email, encrypted_password, email_confirmed_at,
    raw_app_meta_data, raw_user_meta_data,
    created_at, updated_at, confirmation_token, recovery_token,
    email_change_token_new, email_change
  ) VALUES (
    v_user3_id,
    '00000000-0000-0000-0000-000000000000',
    'authenticated', 'authenticated',
    'sofia@ejemplo.com',
    crypt('Sofia1234!', gen_salt('bf')),
    NOW(),
    '{"provider":"email","providers":["email"]}',
    '{"name":"Sofía Martínez","city":"Cali"}',
    NOW(), NOW(), '', '', '', ''
  );
END IF;

INSERT INTO public.profiles (
  id, name, email, city, phone, bio,
  points, level, initials, member_since,
  is_moderator, publications_count, adoptions_count, foster_count, active_count
) VALUES (
  v_user3_id,
  'Sofía Martínez',
  'sofia@ejemplo.com',
  'Cali',
  '+57 313 888 9900',
  'Recién llegué a Huella. Busco adoptar mi primera mascota.',
  10, 'AMIGO_ANIMAL', 'SM', '2026',
  FALSE, 1, 0, 0, 1
) ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- PASO 2 · Publicaciones de prueba
-- ============================================================================

v_pub1_id := extensions.uuid_generate_v4();
v_pub2_id := extensions.uuid_generate_v4();
v_pub3_id := extensions.uuid_generate_v4();
v_pub4_id := extensions.uuid_generate_v4();
v_pub5_id := extensions.uuid_generate_v4();

-- Publicación 1: Adopción aprobada (perro)
INSERT INTO public.publications (
  id, author_id, category, title, species, breed, size,
  vaccinated, sterilized, age, sex,
  city, neighborhood, description, image_url, tone, photo_label,
  status, latitude, longitude, created_at, updated_at
) VALUES (
  v_pub1_id, v_user1_id,
  'ADOPCION', 'Max busca hogar para siempre', 'Perro', 'Beagle', 'Mediano',
  TRUE, TRUE, '2 años', 'Macho',
  'Medellín', 'El Poblado',
  'Max es un beagle de 2 años, cariñoso y muy activo. Vive bien con niños y otros perros. Ha sido maltratado pero está recuperado y listo para un nuevo comienzo. Incluye cartilla de vacunación completa.',
  'https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=600', 3632851165, 'Max, beagle juguetón',
  'APROBADA', 6.2088, -75.5742, NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days'
);

-- Publicación 2: Adopción aprobada (gato)
INSERT INTO public.publications (
  id, author_id, category, title, species, breed, size,
  vaccinated, sterilized, age, sex,
  city, neighborhood, description, image_url, tone, photo_label,
  status, latitude, longitude, created_at, updated_at
) VALUES (
  v_pub2_id, v_user2_id,
  'ADOPCION', 'Luna necesita una familia amorosa', 'Gato', 'Siamés', 'Pequeño',
  TRUE, TRUE, '1 año', 'Hembra',
  'Bogotá', 'Chapinero',
  'Luna es una gatita siamesa de un año. Tranquila, muy limpia y sociable. Lleva vacunas al día y chip de identificación. Busca hogar sin perros o con perros que respeten a los gatos.',
  'https://images.unsplash.com/photo-1529778873920-4da4926a72c2?w=600', 3632851165, 'Luna, gatita siamesa',
  'APROBADA', 4.6097, -74.0817, NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days'
);

-- Publicación 3: Perdido (en revisión — para probar cola de moderación)
INSERT INTO public.publications (
  id, author_id, category, title, species, breed, size,
  vaccinated, sterilized, age, sex,
  city, neighborhood, description, image_url, tone, photo_label,
  status, event_date, latitude, longitude, created_at, updated_at
) VALUES (
  v_pub3_id, v_user1_id,
  'PERDIDOS', 'Se perdió Tobi en El Poblado', 'Perro', 'Criollo', 'Grande',
  TRUE, FALSE, '4 años', 'Macho',
  'Medellín', 'El Poblado',
  'Tobi se perdió el 4 de noviembre cerca al parque de El Poblado. Es de color café con manchas blancas, usa collar rojo. Recompensa. Por favor comuníquese si lo ve.',
  NULL, 3641654400, 'Tobi, se perdió en Poblado',
  'EN_REVISION', 'Mié 4 nov', 6.2088, -75.5700, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'
);

-- Publicación 4: Encontrado aprobado
INSERT INTO public.publications (
  id, author_id, category, title, species, breed, size,
  vaccinated, sterilized, age, sex,
  city, neighborhood, description, image_url, tone, photo_label,
  status, event_date, latitude, longitude, created_at, updated_at
) VALUES (
  v_pub4_id, v_user3_id,
  'ENCONTRADOS', 'Encontré un gato atigrado en el sur', 'Gato', 'Mestizo', 'Pequeño',
  FALSE, FALSE, 'Desconocida', 'Desconocido',
  'Cali', 'Ciudad Jardín',
  'Encontré este gatito atigrado deambulando. Parece manso y en buen estado. Lo tengo en casa mientras aparece su dueño. Llama al número de contacto.',
  'https://images.unsplash.com/photo-1495360010541-f48722b34f7d?w=600', 3641654400, 'Gato atigrado encontrado',
  'APROBADA', 'Jue 5 nov', 3.4516, -76.5320, NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day'
);

-- Publicación 5: Jornada veterinaria aprobada
INSERT INTO public.publications (
  id, author_id, category, title, species, breed, size,
  vaccinated, sterilized, age, sex,
  city, neighborhood, description, image_url, tone, photo_label,
  status, event_date, schedule, place, services, capacity, phone,
  latitude, longitude, created_at, updated_at
) VALUES (
  v_pub5_id, v_user2_id,
  'VETERINARIA', 'Jornada gratuita de vacunación — Bogotá', 'Perro', 'Todos', 'Todos',
  FALSE, FALSE, 'Todas', 'Todos',
  'Bogotá', 'Chapinero',
  'Jornada de salud animal gratuita. Traé tu perro o gato. Cupos limitados, llega temprano. Recuerda llevar el carné de vacunación anterior si lo tienes.',
  'https://images.unsplash.com/photo-1628009368231-7bb7cfcb0def?w=600', 3641654400, 'Jornada veterinaria gratuita',
  'APROBADA', 'Sáb 8 nov · 8:00 a. m.', '8:00 a. m. — 1:00 p. m.', 'Parque El Virrey, Chapinero',
  ARRAY['Vacunación', 'Desparasitación', 'Chip de identificación'], 200, '+57 320 111 2233',
  4.6600, -74.0528, NOW() - INTERVAL '4 days', NOW() - INTERVAL '3 days'
);

-- ============================================================================
-- PASO 3 · Solicitudes de adopción de prueba
-- ============================================================================

-- Sofía solicita adoptar a Max (pub1 de Laura)
INSERT INTO public.adoption_requests (
  id, publication_id, applicant_id, message, status, created_at, updated_at
) VALUES (
  extensions.uuid_generate_v4(),
  v_pub1_id, v_user3_id,
  'Hola Laura! Me enamoré de Max. Tengo patio y mucho tiempo libre. Puedo darle mucho amor y espacio. ¿Podemos coordinar una visita?',
  'PENDIENTE', NOW() - INTERVAL '12 hours', NOW() - INTERVAL '12 hours'
) ON CONFLICT (publication_id, applicant_id) DO NOTHING;

-- Carlos solicita adoptar a Luna (pub2 de Carlos, pero como prueba usamos admin solicitando)
INSERT INTO public.adoption_requests (
  id, publication_id, applicant_id, message, status, created_at, updated_at
) VALUES (
  extensions.uuid_generate_v4(),
  v_pub2_id, v_user1_id,
  'Soy veterinaria y conozco muy bien el cuidado de siameses. Luna estaría en perfectas manos. Tengo hogar tranquilo sin perros.',
  'ACEPTADA', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day'
) ON CONFLICT (publication_id, applicant_id) DO NOTHING;

-- ============================================================================
-- PASO 4 · Historial de puntos de prueba
-- ============================================================================

INSERT INTO public.points_history (user_id, action, points, created_at) VALUES
  (v_user1_id, 'Crear una publicación',  5,  NOW() - INTERVAL '5 days'),
  (v_user1_id, 'Publicación aprobada',   10, NOW() - INTERVAL '4 days'),
  (v_user1_id, 'Crear una publicación',  5,  NOW() - INTERVAL '3 days'),
  (v_user1_id, 'Adopción concretada',    50, NOW() - INTERVAL '2 days'),
  (v_user1_id, 'Publicación aprobada',   10, NOW() - INTERVAL '1 day'),
  (v_user2_id, 'Crear una publicación',  5,  NOW() - INTERVAL '6 days'),
  (v_user2_id, 'Publicación aprobada',   10, NOW() - INTERVAL '5 days'),
  (v_user2_id, 'Adopción concretada',    50, NOW() - INTERVAL '4 days'),
  (v_user3_id, 'Crear una publicación',  5,  NOW() - INTERVAL '1 day');

-- ============================================================================
-- PASO 5 · Notificaciones de prueba
-- ============================================================================

INSERT INTO public.notifications (user_id, type, title, text, target_id, read, created_at) VALUES
  (v_user1_id, 'POST_APPROVED', 'Publicación aprobada',
   'Max busca hogar para siempre ya está visible para todos.',
   v_pub1_id::TEXT, TRUE,  NOW() - INTERVAL '2 days'),
  (v_user1_id, 'NEW_REQUEST', 'Nueva solicitud de adopción',
   'Sofía Martínez está interesada en adoptar a Max.',
   v_pub1_id::TEXT, FALSE, NOW() - INTERVAL '12 hours'),
  (v_user2_id, 'POST_APPROVED', 'Publicación aprobada',
   'Luna necesita una familia amorosa ya está visible para todos.',
   v_pub2_id::TEXT, TRUE,  NOW() - INTERVAL '4 days'),
  (v_user2_id, 'REQUEST_ACCEPTED', 'Solicitud aceptada',
   'Laura Gómez aceptó tu solicitud para Luna.',
   v_pub2_id::TEXT, FALSE, NOW() - INTERVAL '1 day'),
  (v_user3_id, 'POST_APPROVED', 'Publicación aprobada',
   'Encontré un gato atigrado en el sur ya está visible para todos.',
   v_pub4_id::TEXT, FALSE, NOW() - INTERVAL '1 day');

-- ============================================================================
-- PASO 6 · Conversación de prueba entre Laura y Sofía sobre Max
-- ============================================================================

v_conv_id := extensions.uuid_generate_v4();

INSERT INTO public.conversations (
  id, publication_id, participant_one, participant_two, about, created_at, updated_at
) VALUES (
  v_conv_id, v_pub1_id, v_user3_id, v_user1_id,
  'Sobre: Max busca hogar para siempre',
  NOW() - INTERVAL '10 hours', NOW() - INTERVAL '8 hours'
);

INSERT INTO public.chat_messages (conversation_id, sender_id, text, created_at) VALUES
  (v_conv_id, v_user3_id, '¡Hola Laura! Vi la publicación de Max y me encantó. ¿Sigue disponible?',  NOW() - INTERVAL '10 hours'),
  (v_conv_id, v_user1_id, 'Hola Sofía! Sí, Max sigue buscando hogar 🐾 ¿Tienes experiencia con perros?', NOW() - INTERVAL '9 hours 30 minutes'),
  (v_conv_id, v_user3_id, 'Crecí con perros en casa así que me siento cómoda. ¿Podría visitarlo este fin de semana?', NOW() - INTERVAL '9 hours'),
  (v_conv_id, v_user1_id, '¡Claro que sí! El sábado en la tarde estaría perfecto. Te mando la dirección.', NOW() - INTERVAL '8 hours');

-- ============================================================================
-- PASO 7 · Reporte de prueba (para probar la cola de reportes en moderación)
-- ============================================================================

INSERT INTO public.reports (
  publication_id, reporter_id, reason, detail, created_at
) VALUES (
  v_pub3_id, v_user2_id,
  'Contenido inapropiado',
  'La descripción parece sospechosa y la foto no coincide con la mascota descrita.',
  NOW() - INTERVAL '6 hours'
);

END $$;

-- ============================================================================
-- VERIFICACIÓN (opcional — ejecuta por separado para ver los datos insertados)
-- ============================================================================
-- SELECT id, name, email, is_moderator, level, points FROM public.profiles ORDER BY created_at;
-- SELECT id, title, category, status FROM public.publications ORDER BY created_at;
-- SELECT id, status, message FROM public.adoption_requests;
-- SELECT id, type, title, read FROM public.notifications ORDER BY created_at DESC;

