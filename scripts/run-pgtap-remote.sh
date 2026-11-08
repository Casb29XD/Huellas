#!/usr/bin/env bash
# Corre los tests pgTAP contra el proyecto hosted sin Docker ni psql.
# Convención: cada archivo supabase/tests/*.sql define funciones `tests.test_*()` RETURNS SETOF TEXT
# (CREATE OR REPLACE, idempotentes) que usan pgTAP (ok, is, throws_ok, lives_ok, results_eq, ...).
# Este script (1) crea el schema tests y la extensión pgtap, (2) carga todos los archivos,
# (3) ejecuta runtests('tests', '^test_') y muestra el TAP; falla si hay "not ok".
# Requiere SUPABASE_ACCESS_TOKEN y SUPABASE_PROJECT_REF (source C:\temp\huellas-audit\.env.remote).
set -euo pipefail
cd "$(dirname "$0")/.."
SQL=scripts/supabase-remote-sql.sh
$SQL "create extension if not exists pgtap with schema extensions; create schema if not exists tests;" >/dev/null
for f in $(ls supabase/tests/*.sql 2>/dev/null | sort); do
  echo "==> cargando $f"
  $SQL -f "$f" >/dev/null || { echo "ERROR cargando $f"; $SQL -f "$f"; exit 1; }
done
OUT="$($SQL "set search_path = tests, public, extensions; select * from runtests('tests'::name, '^test_')")"
python - "$OUT" <<'EOF'
import json, sys
rows = json.loads(sys.argv[1])
if isinstance(rows, dict):
    print(rows); sys.exit(1)
lines = [list(r.values())[0] for r in rows]
bad = [l for l in lines if l.startswith("not ok")]
print("\n".join(lines))
print(f"\npgTAP: {sum(1 for l in lines if l.startswith('ok'))} ok, {len(bad)} not ok")
sys.exit(1 if bad else 0)
EOF
