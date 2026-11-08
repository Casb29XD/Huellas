-- Gamificación, comentarios, votos, avistamientos, cierre y cron (ítems 6, 13, 15, 22, 53).
-- Usa tests.tap_user/tap_as de 10_security.sql. runtests() revierte cada test_*: nada queda en la BD compartida.

CREATE OR REPLACE FUNCTION tests.tap_points(p_uid UUID) RETURNS INT
LANGUAGE sql AS $$ SELECT points FROM public.profiles WHERE id = p_uid $$;

CREATE OR REPLACE FUNCTION tests.tap_notifs(p_uid UUID, p_type TEXT) RETURNS INT
LANGUAGE sql AS $$ SELECT count(*)::int FROM public.notifications WHERE user_id = p_uid AND type::text = p_type $$;

CREATE OR REPLACE FUNCTION tests.test_20_puntos_y_moderacion() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    a UUID := tests.tap_user('{"name":"tap autor"}');
    b UUID := tests.tap_user('{"name":"tap otro"}');
    md UUID := tests.tap_user('{"name":"tap mod"}');
    p UUID := gen_random_uuid();
    p2 UUID := gen_random_uuid();
BEGIN
    UPDATE public.profiles SET is_moderator = TRUE WHERE id = md;

    PERFORM tests.tap_as(a);
    RETURN NEXT lives_ok(format(
        'INSERT INTO public.publications (id, author_id, category, title, species, breed, size, city, description)
         VALUES (%L, %L, ''ADOPCION'', ''tap'', ''Perro'', ''Criollo'', ''Mediano'', ''Medellín'', ''tap'')', p, a),
        'el autor crea una publicación');
    RETURN NEXT throws_ok(format('SELECT public.award_points(%L, ''hack'', 1000)', a), '42501', NULL,
        'authenticated no ejecuta award_points');
    RESET ROLE;
    RETURN NEXT is(tests.tap_points(a), 5, 'crear publicación suma +5');
    RETURN NEXT is((SELECT action FROM public.points_history WHERE user_id = a), 'Crear una publicación', 'queda en points_history');
    RETURN NEXT is((SELECT publications_count FROM public.profiles WHERE id = a), 1, 'publications_count = 1');
    RETURN NEXT is((SELECT latitude FROM public.publications WHERE id = p), NULL::float8, 'sin ubicación la latitud es NULL, no 0');

    PERFORM tests.tap_as(md);
    RETURN NEXT lives_ok(format('SELECT public.moderate_publication(%L, ''APROBADA'')', p), 'el moderador aprueba');
    RESET ROLE;
    RETURN NEXT is(tests.tap_points(a), 15, 'aprobar suma +10');
    RETURN NEXT is(tests.tap_notifs(a, 'POST_APPROVED'), 1, 'notificación POST_APPROVED');
    RETURN NEXT is((SELECT active_count FROM public.profiles WHERE id = a), 1, 'active_count cuenta la aprobada');

    -- Rechazar resuelve los reportes abiertos de la publicación.
    INSERT INTO public.publications (id, author_id, title, species, breed, size, city, description)
    VALUES (p2, a, 'tap2', 'Gato', 'Criollo', 'Mediano', 'Medellín', 'tap');
    INSERT INTO public.reports (publication_id, reporter_id, reason) VALUES (p2, b, 'tap');
    PERFORM tests.tap_as(md);
    RETURN NEXT lives_ok(format('SELECT public.moderate_publication(%L, ''RECHAZADA'', ''tap'')', p2), 'el moderador rechaza');
    RESET ROLE;
    RETURN NEXT results_eq(format('SELECT status, resolved_by FROM public.reports WHERE publication_id = %L', p2),
        format('VALUES (''RESUELTO''::text, %L::uuid)', md), 'rechazar resuelve los reportes de la publicación');
    RETURN NEXT is(tests.tap_points(a), 20, 'rechazar no suma (solo los +5 de crear p2)');

    -- Cierre: solo el autor; +50 por adopción, LEVEL_UP y contadores; repetir no vuelve a sumar.
    PERFORM tests.tap_as(b);
    RETURN NEXT throws_ok(format('SELECT public.close_publication(%L)', p), '42501', NULL, 'un tercero no finaliza');
    PERFORM tests.tap_as(a);
    RETURN NEXT lives_ok(format('SELECT public.close_publication(%L)', p), 'el autor finaliza su publicación');
    RETURN NEXT lives_ok(format('SELECT public.close_publication(%L)', p), 'finalizar dos veces no falla');
    RESET ROLE;
    RETURN NEXT is((SELECT lifecycle FROM public.publications WHERE id = p), 'FINALIZADA', 'lifecycle = FINALIZADA');
    RETURN NEXT is(tests.tap_points(a), 70, 'adopción concretada suma +50 una sola vez');
    RETURN NEXT is((SELECT level::text FROM public.profiles WHERE id = a), 'PROTECTOR', 'sube a PROTECTOR');
    RETURN NEXT is(tests.tap_notifs(a, 'LEVEL_UP'), 1, 'notificación LEVEL_UP');
    RETURN NEXT results_eq(format('SELECT adoptions_count, active_count FROM public.profiles WHERE id = %L', a),
        'VALUES (1, 0)', 'adoptions_count = 1 y active_count = 0');
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_21_comentarios_votos_avistamientos() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    a UUID := tests.tap_user('{"name":"tap autor"}');
    b UUID := tests.tap_user('{"name":"tap otro"}');
    c UUID := tests.tap_user('{"name":"tap tercero"}');
    p UUID := gen_random_uuid();
    p_rev UUID := gen_random_uuid();
BEGIN
    INSERT INTO public.publications (id, author_id, category, title, species, breed, size, city, description, status)
    VALUES (p, a, 'PERDIDOS', 'tap', 'Perro', 'Criollo', 'Mediano', 'Medellín', 'tap', 'APROBADA'),
           (p_rev, a, 'PERDIDOS', 'tap rev', 'Perro', 'Criollo', 'Mediano', 'Medellín', 'tap', 'EN_REVISION');

    PERFORM tests.tap_as(b);
    RETURN NEXT lives_ok(format('INSERT INTO public.comments (publication_id, author_id, text) VALUES (%L, %L, ''hola'')', p, b),
        'comenta una publicación aprobada');
    RETURN NEXT lives_ok(format('INSERT INTO public.comments (publication_id, author_id, text) VALUES (%L, %L, ''otra'')', p, b),
        'comenta otra vez');
    RETURN NEXT throws_ok(format('INSERT INTO public.comments (publication_id, author_id, text) VALUES (%L, %L, ''x'')', p_rev, b),
        '42501', NULL, 'no comenta una publicación que no ve');
    RETURN NEXT throws_ok(format('INSERT INTO public.comments (publication_id, author_id, text) VALUES (%L, %L, ''x'')', p, c),
        '42501', NULL, 'no comenta a nombre de otro');
    RETURN NEXT throws_ok(format('INSERT INTO public.comments (publication_id, author_id, text) VALUES (%L, %L, ''  '')', p, b),
        '23514', NULL, 'comentario vacío rechazado');
    PERFORM tests.tap_as(NULL);
    RETURN NEXT is((SELECT count(*)::int FROM public.comments WHERE publication_id = p), 2, 'anon lee los comentarios de una aprobada');
    RESET ROLE;
    RETURN NEXT is((SELECT comments_count FROM public.publications WHERE id = p), 2, 'comments_count = 2');
    RETURN NEXT is(tests.tap_points(b), 2, 'comentar suma +2 solo la primera vez por publicación');
    RETURN NEXT is(tests.tap_notifs(a, 'NEW_COMMENT'), 2, 'el autor recibe NEW_COMMENT');

    PERFORM tests.tap_as(c);
    RETURN NEXT lives_ok(format('DELETE FROM public.comments WHERE publication_id = %L', p), 'un tercero intenta borrar comentarios...');
    PERFORM tests.tap_as(b);
    RETURN NEXT lives_ok(format('DELETE FROM public.comments WHERE publication_id = %L AND text = ''otra''', p), 'el autor del comentario lo borra');
    RESET ROLE;
    RETURN NEXT is((SELECT comments_count FROM public.publications WHERE id = p), 1, '...solo se borró el propio: comments_count = 1');

    PERFORM tests.tap_as(b);
    RETURN NEXT lives_ok(format('INSERT INTO public.publication_votes (publication_id, user_id, value) VALUES (%L, %L, 1)', p, b), 'b vota +1');
    PERFORM tests.tap_as(c);
    RETURN NEXT lives_ok(format('INSERT INTO public.publication_votes (publication_id, user_id, value) VALUES (%L, %L, 1)', p, c), 'c vota +1');
    RETURN NEXT throws_ok(format('INSERT INTO public.publication_votes (publication_id, user_id, value) VALUES (%L, %L, 1)', p, c),
        '23505', NULL, 'un voto por usuario');
    RETURN NEXT throws_ok(format('INSERT INTO public.publication_votes (publication_id, user_id, value) VALUES (%L, %L, 1)', p, a),
        '42501', NULL, 'no vota a nombre de otro');
    RESET ROLE;
    RETURN NEXT is((SELECT votes_count FROM public.publications WHERE id = p), 2, 'votes_count = 2');
    PERFORM tests.tap_as(c);
    RETURN NEXT lives_ok(format('UPDATE public.publication_votes SET value = -1 WHERE publication_id = %L', p), 'c cambia a -1');
    RESET ROLE;
    RETURN NEXT is((SELECT votes_count FROM public.publications WHERE id = p), 0, 'votes_count = 0 tras cambiar el voto');
    PERFORM tests.tap_as(c);
    RETURN NEXT lives_ok(format('DELETE FROM public.publication_votes WHERE publication_id = %L', p), 'c retira su voto (toggle)');
    RESET ROLE;
    RETURN NEXT is((SELECT votes_count FROM public.publications WHERE id = p), 1, 'votes_count = 1 tras retirar el voto');
    RETURN NEXT is((SELECT count(*)::int FROM public.publication_votes WHERE publication_id = p), 1, '...y el voto de b sigue');

    PERFORM tests.tap_as(b);
    RETURN NEXT lives_ok(format('SELECT public.report_sighting(%L, ''Lo vi en el parque'')', p), 'b reporta un avistamiento');
    RETURN NEXT lives_ok(format('SELECT public.report_sighting(%L, ''Otra vez'')', p), 'b lo reporta de nuevo');
    RETURN NEXT throws_ok(format('SELECT public.report_sighting(%L, '' '')', p), '22023', NULL, 'avistamiento vacío rechazado');
    PERFORM tests.tap_as(a);
    RETURN NEXT throws_ok(format('SELECT public.report_sighting(%L, ''yo'')', p), '22023', NULL, 'el autor no se reporta a sí mismo');
    RETURN NEXT is((SELECT count(*)::int FROM public.sightings WHERE publication_id = p), 2, 'el autor ve los avistamientos');
    PERFORM tests.tap_as(c);
    RETURN NEXT is((SELECT count(*)::int FROM public.sightings WHERE publication_id = p), 0, 'un tercero no los ve');
    RESET ROLE;
    RETURN NEXT is(tests.tap_points(b), 7, 'avistamiento suma +5 solo la primera vez');
    RETURN NEXT is(tests.tap_notifs(a, 'SIGHTING'), 2, 'el autor recibe SIGHTING');
END;
$$;

CREATE OR REPLACE FUNCTION tests.test_22_cron() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE
    a UUID := tests.tap_user('{"name":"tap autor"}');
    viejo UUID;
BEGIN
    RETURN NEXT is((SELECT count(*)::int FROM cron.job WHERE jobname = 'huellas_cierre_automatico'), 1, 'job huellas_cierre_automatico');
    RETURN NEXT is((SELECT count(*)::int FROM cron.job WHERE jobname = 'huellas_limpieza_notificaciones'), 1, 'job huellas_limpieza_notificaciones');

    INSERT INTO public.publications (author_id, category, title, species, breed, size, city, description, status, created_at)
    VALUES (a, 'PERDIDOS', 'tap viejo', 'Perro', 'Criollo', 'Mediano', 'Medellín', 'tap', 'APROBADA', NOW() - INTERVAL '61 days')
    RETURNING id INTO viejo;
    INSERT INTO public.notifications (user_id, type, title, text, read, created_at)
    VALUES (a, 'LEVEL_UP', 'tap', 'tap', TRUE, NOW() - INTERVAL '91 days');
    EXECUTE (SELECT command FROM cron.job WHERE jobname = 'huellas_cierre_automatico');
    EXECUTE (SELECT command FROM cron.job WHERE jobname = 'huellas_limpieza_notificaciones');
    RETURN NEXT is((SELECT lifecycle FROM public.publications WHERE id = viejo), 'FINALIZADA', 'el cierre automático finaliza perdidos de más de 60 días');
    RETURN NEXT is((SELECT count(*)::int FROM public.notifications WHERE user_id = a AND title = 'tap'), 0, 'la limpieza borra leídas de más de 90 días');
END;
$$;
