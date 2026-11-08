-- Flujo de solicitudes, chat y eliminación de cuenta (ítems 7, 8, 19, 20). Usa helpers de 10_ y 20_.
-- runtests() revierte cada test_*: nada queda en la BD compartida.

CREATE OR REPLACE FUNCTION tests.test_30_solicitudes() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    a UUID := tests.tap_user('{"name":"tap autor"}');
    b UUID := tests.tap_user('{"name":"tap adoptante"}');
    c UUID := tests.tap_user('{"name":"tap rechazado"}');
    p UUID := gen_random_uuid();
    rb UUID;
    rc UUID;
    conv UUID;
BEGIN
    INSERT INTO public.publications (id, author_id, category, title, species, breed, size, city, description, status)
    VALUES (p, a, 'ADOPCION', 'tap Max', 'Perro', 'Criollo', 'Mediano', 'Medellín', 'tap', 'APROBADA');

    PERFORM tests.tap_as(b);
    RETURN NEXT lives_ok(format('INSERT INTO public.adoption_requests (publication_id, applicant_id, message) VALUES (%L, %L, ''tap'')', p, b),
        'b envía una solicitud');
    PERFORM tests.tap_as(c);
    RETURN NEXT lives_ok(format('INSERT INTO public.adoption_requests (publication_id, applicant_id, message) VALUES (%L, %L, ''tap'')', p, c),
        'c envía una solicitud');
    RESET ROLE;
    SELECT id INTO rb FROM public.adoption_requests WHERE publication_id = p AND applicant_id = b;
    SELECT id INTO rc FROM public.adoption_requests WHERE publication_id = p AND applicant_id = c;
    RETURN NEXT is(tests.tap_notifs(a, 'NEW_REQUEST'), 2, 'el autor recibe NEW_REQUEST por cada solicitud');
    RETURN NEXT is((SELECT target_id FROM public.notifications WHERE user_id = a AND type = 'NEW_REQUEST' LIMIT 1), p::text,
        'NEW_REQUEST apunta a la publicación');

    PERFORM tests.tap_as(b);
    RETURN NEXT throws_ok(format('SELECT public.respond_to_request(%L, TRUE)', rb), '42501', NULL, 'el solicitante no se autoacepta');
    PERFORM tests.tap_as(a);
    conv := public.respond_to_request(rb, TRUE);
    RETURN NEXT isnt(conv, NULL::uuid, 'aceptar devuelve la conversación');
    RETURN NEXT throws_ok(format('SELECT public.respond_to_request(%L, FALSE)', rb), '22023', NULL, 'no se responde dos veces');
    RETURN NEXT is(public.respond_to_request(rc, FALSE), NULL::uuid, 'rechazar devuelve NULL');
    RESET ROLE;

    RETURN NEXT results_eq(format('SELECT participant_one, participant_two, publication_id FROM public.conversations WHERE id = %L', conv),
        format('VALUES (%L::uuid, %L::uuid, %L::uuid)', a, b, p), 'conversación entre autor y solicitante sobre la publicación');
    RETURN NEXT is((SELECT target_id FROM public.notifications WHERE user_id = b AND type = 'REQUEST_ACCEPTED'), conv::text,
        'REQUEST_ACCEPTED lleva target_id = conversación');
    RETURN NEXT is(tests.tap_notifs(c, 'REQUEST_REJECTED'), 1, 'el rechazado recibe REQUEST_REJECTED');
    RETURN NEXT results_eq(format('SELECT status::text FROM public.adoption_requests WHERE id IN (%L, %L) ORDER BY applicant_id = %L DESC', rb, rc, b),
        $v$VALUES ('ACEPTADA'), ('RECHAZADA')$v$, 'estados ACEPTADA y RECHAZADA');

    -- Otra solicitud aceptada entre los mismos usuarios reutiliza la conversación.
    INSERT INTO public.publications (id, author_id, title, species, breed, size, city, description, status)
    VALUES (gen_random_uuid(), a, 'tap 2', 'Gato', 'Criollo', 'Mediano', 'Medellín', 'tap', 'APROBADA')
    RETURNING id INTO p;
    INSERT INTO public.adoption_requests (publication_id, applicant_id, message) VALUES (p, b, 'tap') RETURNING id INTO rb;
    PERFORM tests.tap_as(a);
    RETURN NEXT is(public.respond_to_request(rb, TRUE), conv, 'se reutiliza la conversación existente');
    RESET ROLE;
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_31_chat() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    a UUID := tests.tap_user('{"name":"tap A"}');
    b UUID := tests.tap_user('{"name":"tap B"}');
    conv UUID;
BEGIN
    INSERT INTO public.conversations (participant_one, participant_two, created_at, updated_at)
    VALUES (a, b, '2000-01-01', '2000-01-01') RETURNING id INTO conv;
    PERFORM tests.tap_as(b);
    RETURN NEXT lives_ok(format('INSERT INTO public.chat_messages (conversation_id, sender_id, text) VALUES (%L, %L, ''hola'')', conv, b),
        'b envía un mensaje');
    RESET ROLE;
    RETURN NEXT ok((SELECT updated_at > '2000-01-02' FROM public.conversations WHERE id = conv), 'el mensaje actualiza conversations.updated_at');
    RETURN NEXT ok(EXISTS (SELECT 1 FROM pg_publication_tables WHERE pubname = 'supabase_realtime' AND tablename = 'chat_messages'),
        'Realtime publica chat_messages');
    RETURN NEXT ok(EXISTS (SELECT 1 FROM pg_publication_tables WHERE pubname = 'supabase_realtime' AND tablename = 'notifications'),
        'Realtime publica notifications');
    RETURN NEXT ok(to_regclass('public.publications_author_id_idx') IS NOT NULL, 'índice en publications.author_id');
    RETURN NEXT ok(to_regclass('public.chat_messages_conversation_created_idx') IS NOT NULL, 'índice en chat_messages.conversation_id');
    RETURN NEXT ok(to_regclass('public.notifications_user_created_idx') IS NOT NULL, 'índice en notifications.user_id');
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_32_eliminar_cuenta() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    a UUID := tests.tap_user('{"name":"tap autor"}');
    b UUID := tests.tap_user('{"name":"tap se va"}');
    p UUID := gen_random_uuid();
    pb UUID := gen_random_uuid();
BEGIN
    INSERT INTO public.publications (id, author_id, title, species, breed, size, city, description, status) VALUES
        (p, a, 'tap', 'Perro', 'Criollo', 'Mediano', 'Medellín', 'tap', 'APROBADA'),
        (pb, b, 'tap b', 'Gato', 'Criollo', 'Mediano', 'Medellín', 'tap', 'APROBADA');
    INSERT INTO public.adoption_requests (publication_id, applicant_id, message) VALUES (p, b, 'tap');
    INSERT INTO public.comments (publication_id, author_id, text) VALUES (p, b, 'tap');
    INSERT INTO public.conversations (participant_one, participant_two) VALUES (a, b);
    INSERT INTO storage.objects (bucket_id, name) VALUES ('pet-images', b || '/tap.jpg');

    PERFORM tests.tap_as(NULL);
    RETURN NEXT throws_ok('SELECT public.delete_my_account()', '42501', NULL, 'anon no ejecuta delete_my_account');
    PERFORM tests.tap_as(b);
    RETURN NEXT lives_ok('SELECT public.delete_my_account()', 'b elimina su cuenta');
    RESET ROLE;

    RETURN NEXT is((SELECT count(*)::int FROM auth.users WHERE id = b), 0, 'auth.users borrado');
    RETURN NEXT is((SELECT count(*)::int FROM public.profiles WHERE id = b), 0, 'perfil borrado');
    RETURN NEXT is((SELECT count(*)::int FROM public.publications WHERE author_id = b), 0, 'sus publicaciones borradas');
    RETURN NEXT is((SELECT count(*)::int FROM public.adoption_requests WHERE applicant_id = b), 0, 'sus solicitudes borradas');
    RETURN NEXT is((SELECT count(*)::int FROM public.conversations WHERE b IN (participant_one, participant_two)), 0, 'sus conversaciones borradas');
    RETURN NEXT is((SELECT count(*)::int FROM storage.objects WHERE bucket_id = 'pet-images' AND name LIKE b || '/%'), 0, 'sus fotos borradas');
    RETURN NEXT is((SELECT comments_count FROM public.publications WHERE id = p), 0, 'el contador de comentarios ajeno se recalcula');
    RETURN NEXT is((SELECT count(*)::int FROM public.profiles WHERE id = a), 1, 'el otro usuario sigue intacto');
END;
$$;
