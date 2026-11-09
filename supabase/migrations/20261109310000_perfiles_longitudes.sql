-- Ítem 23 (cierre): los campos editables del perfil tienen límite en el servidor (handle_new_user ya recorta
-- en el alta; el PATCH de profiles no). Mismos topes que el alta: name 1..80, city 80, phone 30, bio 500.

UPDATE public.profiles
   SET name  = LEFT(COALESCE(NULLIF(BTRIM(name), ''), 'Usuario Huella'), 80),
       city  = LEFT(city, 80),
       phone = LEFT(phone, 30),
       bio   = LEFT(bio, 500)
 WHERE char_length(BTRIM(name)) NOT BETWEEN 1 AND 80
    OR char_length(city) > 80 OR char_length(phone) > 30 OR char_length(bio) > 500;

ALTER TABLE public.profiles
    DROP CONSTRAINT IF EXISTS profiles_name_len,
    DROP CONSTRAINT IF EXISTS profiles_city_len,
    DROP CONSTRAINT IF EXISTS profiles_phone_len,
    DROP CONSTRAINT IF EXISTS profiles_bio_len,
    ADD CONSTRAINT profiles_name_len  CHECK (char_length(BTRIM(name)) BETWEEN 1 AND 80),
    ADD CONSTRAINT profiles_city_len  CHECK (char_length(city) <= 80),
    ADD CONSTRAINT profiles_phone_len CHECK (char_length(phone) <= 30),
    ADD CONSTRAINT profiles_bio_len   CHECK (char_length(bio) <= 500);
