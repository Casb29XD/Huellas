# Pruebas

Todo se corre con un solo comando desde Git Bash (Windows) o Linux:

```bash
scripts/test-all.sh            # hosted (por defecto): Supabase remoto + Gradle nativo, sin Docker
scripts/test-all.sh --local    # opcional: Supabase local con Docker
```

Al final imprime el resultado de cada paso y una tabla `clase / tests / skip / fail / error` sacada de
`app/build/test-results/testDebugUnitTest/TEST-*.xml`. Sale con 0 solo si todo pasó (2 si faltan variables).

## Qué corre

| Suite | Dónde | Qué prueba |
|---|---|---|
| pgTAP (`supabase/tests/*.sql`) | BD Supabase | Funciones `tests.test_*()` `RETURNS SETOF TEXT` ejecutadas con `runtests('tests', '^test_')` |
| Unitarias JVM | Gradle | Casos de uso, configuración, endpoints con `MockSupabaseApiService` |
| Robolectric + compose-ui-test | Gradle | Pantallas sin emulador (`ui/LoginScreenRobolectricTest`) |
| E2E (`e2e/*E2eTest`) | Gradle contra Supabase real | `SmokeE2eTest` (camino feliz con `SupabaseRepository`) y `RlsSecurityE2eTest` (exploits de la auditoría) |

`RlsSecurityE2eTest` asserta que cada exploit está **bloqueado**; mientras las migraciones de seguridad no estén
aplicadas sale en rojo a propósito (no marcar `@Ignore`). Sin las variables de Supabase las pruebas E2E se saltan (Assume).

## Modo hosted (por defecto)

Necesita, exportadas o en un archivo que el script carga (`HUELLAS_ENV`, por defecto `/c/temp/huellas-audit/.env.remote`;
**fuera del repo, nunca versionado**):

| Variable | Para qué |
|---|---|
| `SUPABASE_ACCESS_TOKEN` | Management API: SQL remoto y pgTAP (`scripts/supabase-remote-sql.sh`) |
| `SUPABASE_PROJECT_REF` | Proyecto sobre el que corre el SQL remoto |
| `SUPABASE_URL` | Base de la API (`https://<ref>.supabase.co`) para la app y las E2E |
| `SUPABASE_PUBLISHABLE_KEY` (o `SUPABASE_ANON_KEY`) | Llamadas como cliente de la app |
| `SUPABASE_SERVICE_ROLE_KEY` | Fixtures E2E: Admin API y verificación saltando RLS |

Herramientas: `curl` y `python` en el PATH; JDK 17+ y Android SDK (`scripts/gradle-native.sh` usa `JAVA_HOME`/`ANDROID_HOME`
o el toolchain portable de `HUELLAS_TOOLS`, por defecto `/c/temp/huellas-audit/tools`).

Pasos sueltos:

```bash
source /c/temp/huellas-audit/.env.remote
scripts/run-pgtap-remote.sh                                        # solo pgTAP
scripts/gradle-native.sh :app:testDebugUnitTest                    # todas las JVM
scripts/gradle-native.sh :app:testDebugUnitTest --tests "*E2e*"    # solo E2E
```

La BD hosted es compartida y no se puede resetear:

- Las E2E crean sus propios usuarios con la Admin API (`E2eEnv.createUser`, correo `e2e+<nanoTime>@huella.test`,
  confirmado, metadata `name`/`city`), hacen moderador al que lo necesite (`E2eEnv.makeModerator`) y los borran en
  `@AfterClass` (`E2eEnv.cleanup`, `DELETE /auth/v1/admin/users/<id>`; sus filas caen por `ON DELETE CASCADE`).
  Los archivos subidos a Storage (`<uid>/e2e-*.jpg`) se borran en la propia prueba.
- Los tests pgTAP deben limpiar lo que crean (o usar el prefijo `tap_` y borrarlo al final).
- Las migraciones son append-only: `scripts/apply-migrations-remote.sh` aplica solo las no registradas.

## Modo local (opcional, con Docker)

Requiere Docker, Supabase CLI (`supabase start` levantado), `psql` y la imagen `huellas-android` de `scripts/gradle-docker.sh`.
`--local` hace `supabase db reset`, toma las claves de `supabase status -o env`, carga `supabase/tests/*.sql` con `psql`
y corre `runtests`, y ejecuta Gradle en Docker. No necesita variables propias.
