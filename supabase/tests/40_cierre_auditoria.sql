-- Cierre de los ítems que el verificador dejó parciales (21, 23, 51, 52) y guarda de codificación.
-- Usa helpers de 10_security.sql; runtests() revierte cada test_*.

CREATE OR REPLACE FUNCTION tests.test_40_longitudes_perfil() RETURNS SETOF TEXT
LANGUAGE plpgsql AS $$
DECLARE a UUID := tests.tap_user('{"name":"tap A"}');
BEGIN
    PERFORM tests.tap_as(a);
    RETURN NEXT throws_ok(format('UPDATE public.profiles SET name = %L WHERE id = auth.uid()', repeat('N', 81)),
        '23514', NULL, 'name de 81 caracteres se rechaza');
    RETURN NEXT throws_ok('UPDATE public.profiles SET name = ''   '' WHERE id = auth.uid()', '23514', NULL, 'name en blanco se rechaza');
    RETURN NEXT throws_ok(format('UPDATE public.profiles SET city = %L WHERE id = auth.uid()', repeat('c', 81)),
        '23514', NULL, 'city de 81 caracteres se rechaza');
    RETURN NEXT throws_ok(format('UPDATE public.profiles SET phone = %L WHERE id = auth.uid()', repeat('3', 31)),
        '23514', NULL, 'phone de 31 caracteres se rechaza');
    RETURN NEXT throws_ok(format('UPDATE public.profiles SET bio = %L WHERE id = auth.uid()', repeat('b', 501)),
        '23514', NULL, 'bio de 501 caracteres se rechaza');
    RETURN NEXT lives_ok(format('UPDATE public.profiles SET name = %L, city = %L, phone = %L, bio = %L WHERE id = auth.uid()',
        repeat('N', 80), repeat('c', 80), repeat('3', 30), repeat('b', 500)), 'en el límite exacto se acepta');
    RESET ROLE;
    -- El alta recorta igual que los CHECK: un nombre larguísimo no rompe el registro.
    RETURN NEXT lives_ok(format('SELECT tests.tap_user(%L)', json_build_object('name', repeat('x', 5000))::text),
        'alta con nombre de 5000 caracteres no falla');
END;
$$;
