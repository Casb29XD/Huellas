#!/usr/bin/env bash
# Ejecuta SQL en el proyecto Supabase hosted vía Management API (como rol postgres).
# Uso: scripts/supabase-remote-sql.sh "select now()"      |  scripts/supabase-remote-sql.sh -f archivo.sql
# Requiere SUPABASE_ACCESS_TOKEN y SUPABASE_PROJECT_REF en el entorno (source C:\temp\huellas-audit\.env.remote).
# Imprime el JSON de filas de la última sentencia; sale con código 1 si la API devuelve error.
set -euo pipefail
# Python en Windows lee stdin/argv en cp1252: sin esto el SQL con tildes llega con mojibake ("BogotÃ¡").
export PYTHONUTF8=1
: "${SUPABASE_ACCESS_TOKEN:?define SUPABASE_ACCESS_TOKEN}"
: "${SUPABASE_PROJECT_REF:?define SUPABASE_PROJECT_REF}"
if [ "${1:-}" = "-f" ]; then SQL="$(cat "$2")"; else SQL="${1:?sql}"; fi
BODY="$(python -c 'import json,sys;print(json.dumps({"query": sys.stdin.read()}))' <<<"$SQL")"
OUT="$(curl -s -X POST "https://api.supabase.com/v1/projects/$SUPABASE_PROJECT_REF/database/query" \
  -H "Authorization: Bearer $SUPABASE_ACCESS_TOKEN" -H "Content-Type: application/json" --data-binary "$BODY")"
echo "$OUT"
python -c 'import json,sys
d=json.loads(sys.argv[1]) if sys.argv[1].strip() else []
sys.exit(1 if isinstance(d,dict) and "message" in d and "Failed" in str(d.get("message")) else 0)' "$OUT"
