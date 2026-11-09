# Migraciones de Supabase

El esquema vive solo en `supabase/migrations/`. Cada archivo se aplica una vez y queda registrado en
`supabase_migrations.schema_migrations` (mismo registro que usa `supabase db push`).

## Reglas

- **Append-only.** Nunca edites una migración ya aplicada en el hosted: corrige con otra posterior.
- **Nombre:** `AAAAMMDDHHMMSS_descripcion.sql`, con prefijo mayor que el último existente.
- **Idempotente:** `CREATE OR REPLACE FUNCTION`, `DROP POLICY IF EXISTS` + `CREATE POLICY`,
  `ADD COLUMN IF NOT EXISTS`, `ALTER TYPE ... ADD VALUE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`, bloques `DO $$`.
- **Seguridad:** toda tabla nueva con RLS activada, `GRANT` explícitos a `anon`/`authenticated` solo de lo necesario
  y funciones `SECURITY DEFINER` con `SET search_path = public, pg_temp`. Reglas de cliente en
  [docs/SEGURIDAD_BACKEND.md](docs/SEGURIDAD_BACKEND.md).
- **Pruebas:** cada cambio de reglas lleva su test pgTAP en `supabase/tests/*.sql` (funciones `tests.test_*()`
  que limpian lo que crean, porque la BD hosted es compartida).
- **Sin datos:** `supabase/seed.sql` es solo para el stack local; los datos de prueba del hosted los crean y
  borran los tests.

## Aplicar

```bash
# Hosted con la CLI
supabase link --project-ref <project-ref>
supabase db push

# Hosted sin Docker ni link (Management API; requiere SUPABASE_ACCESS_TOKEN y SUPABASE_PROJECT_REF en el entorno)
scripts/apply-migrations-remote.sh
scripts/run-pgtap-remote.sh

# Local (Docker)
supabase start
supabase db reset          # recrea la BD, aplica todas las migraciones y seed.sql
```

`scripts/apply-migrations-remote.sh --mark <versión>...` registra versiones aplicadas a mano sin ejecutarlas.

## Funciones programadas

Los jobs de `pg_cron` se crean con `cron.schedule('<nombre>', ...)` dentro de una migración (reaplicar actualiza
el job, no lo duplica). Hoy: `huellas_cierre_automatico` y `huellas_limpieza_notificaciones`
(`20261109130000_ciclo_de_vida.sql`).
