#!/usr/bin/env bash
# Aplica al proyecto hosted las migraciones de supabase/migrations que aún no estén registradas en
# supabase_migrations.schema_migrations (mismo formato que usa `supabase db push`), en orden y append-only.
# Uso: scripts/apply-migrations-remote.sh            (aplica pendientes)
#      scripts/apply-migrations-remote.sh --mark 20261101000000 20261103000000   (marca como aplicadas sin ejecutar)
# Requiere SUPABASE_ACCESS_TOKEN y SUPABASE_PROJECT_REF (source C:\temp\huellas-audit\.env.remote).
set -euo pipefail
# Python en Windows lee stdin/argv en cp1252: sin esto el SQL con tildes llega con mojibake ("BogotÃ¡").
export PYTHONUTF8=1
cd "$(dirname "$0")/.."
SQL=scripts/supabase-remote-sql.sh
esc() { python -c 'import sys;print(sys.stdin.read().replace("\x27","\x27\x27"))' <<<"$1"; }

$SQL "create schema if not exists supabase_migrations;
create table if not exists supabase_migrations.schema_migrations(version text primary key, statements text[], name text);" >/dev/null

if [ "${1:-}" = "--mark" ]; then
  shift
  for v in "$@"; do
    f="$(ls supabase/migrations/${v}_*.sql | head -1)"; n="$(basename "$f" .sql)"; n="${n#*_}"
    $SQL "insert into supabase_migrations.schema_migrations(version,name) values ('$v','$(esc "$n")') on conflict do nothing" >/dev/null
    echo "marcada $v ($n)"
  done
  exit 0
fi

APPLIED="$($SQL "select version from supabase_migrations.schema_migrations" | python -c 'import json,sys;print(" ".join(r["version"] for r in json.load(sys.stdin)))')"
status=0
for f in $(ls supabase/migrations/*.sql | sort); do
  base="$(basename "$f" .sql)"; v="${base%%_*}"; n="${base#*_}"
  case " $APPLIED " in *" $v "*) continue;; esac
  echo "==> aplicando $base"
  if OUT="$($SQL -f "$f")"; then
    $SQL "insert into supabase_migrations.schema_migrations(version,name) values ('$v','$(esc "$n")') on conflict do nothing" >/dev/null
    echo "    OK"
  else
    echo "    ERROR: $OUT"; status=1; break
  fi
done
exit $status
