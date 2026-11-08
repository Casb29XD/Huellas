#!/usr/bin/env bash
# Corre toda la batería: pgTAP y las pruebas JVM (unitarias, Robolectric y e2e). Ver docs/PRUEBAS.md.
# Uso: scripts/test-all.sh           modo hosted (por defecto): Supabase remoto + Gradle nativo, sin Docker.
#      scripts/test-all.sh --local   Supabase local (supabase db reset) + Gradle en Docker.
# Hosted: carga $HUELLAS_ENV (por defecto /c/temp/huellas-audit/.env.remote) si existe; si no, exige las variables
# SUPABASE_ACCESS_TOKEN, SUPABASE_PROJECT_REF, SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY y SUPABASE_SERVICE_ROLE_KEY.
set -uo pipefail
cd "$(dirname "$0")/.."

step() { echo; echo "==> $1"; }
STEPS=()
declare -A RESULT
run() { local name=$1; shift; STEPS+=("$name"); step "$name"; if "$@"; then RESULT[$name]=OK; else RESULT[$name]="FALLO ($?)"; fi; }

pgtap_local() {
  psql "$DB_URL" -q -c "create extension if not exists pgtap with schema extensions; create schema if not exists tests;" || return 1
  for f in supabase/tests/*.sql; do psql "$DB_URL" -q -v ON_ERROR_STOP=1 -f "$f" || return 1; done
  local out; out="$(psql "$DB_URL" -At -c "set search_path = tests, public, extensions; select * from runtests('tests'::name, '^test_')")" || return 1
  echo "$out"; ! grep -q '^not ok' <<<"$out"
}

if [ "${1:-}" = "--local" ]; then
  run "supabase db reset" supabase db reset
  step "claves de supabase status"
  # Las pruebas corren en Docker: 127.0.0.1 del host se alcanza como host.docker.internal.
  eval "$(supabase status -o env 2>/dev/null | grep -E '^[A-Z_]+=')"
  export SUPABASE_URL="${API_URL/127.0.0.1/host.docker.internal}"
  export SUPABASE_PUBLISHABLE_KEY="${PUBLISHABLE_KEY:-$ANON_KEY}"
  export SUPABASE_SERVICE_ROLE_KEY="$SERVICE_ROLE_KEY"
  echo "SUPABASE_URL=$SUPABASE_URL"
  run "pgTAP local" pgtap_local
  run "gradle testDebugUnitTest" scripts/gradle-docker.sh --continue :app:testDebugUnitTest
else
  ENV_FILE="${HUELLAS_ENV:-/c/temp/huellas-audit/.env.remote}"
  # shellcheck disable=SC1090
  if [ -f "$ENV_FILE" ]; then set -a; source "$ENV_FILE"; set +a; fi
  export SUPABASE_PUBLISHABLE_KEY="${SUPABASE_PUBLISHABLE_KEY:-${SUPABASE_ANON_KEY:-}}"
  missing=""
  for v in SUPABASE_ACCESS_TOKEN SUPABASE_PROJECT_REF SUPABASE_URL SUPABASE_PUBLISHABLE_KEY SUPABASE_SERVICE_ROLE_KEY; do
    [ -n "${!v:-}" ] || missing="$missing $v"
  done
  [ -z "$missing" ] || { echo "Faltan variables:$missing (define HUELLAS_ENV o expórtalas; ver docs/PRUEBAS.md)"; exit 2; }
  echo "SUPABASE_URL=$SUPABASE_URL"
  run "pgTAP remoto" scripts/run-pgtap-remote.sh
  run "gradle testDebugUnitTest" scripts/gradle-native.sh --continue :app:testDebugUnitTest
fi

step "resumen"
fail=0
for name in "${STEPS[@]}"; do
  printf '  %-28s %s\n' "$name" "${RESULT[$name]}"
  [ "${RESULT[$name]}" = OK ] || fail=1
done
# Conteo por clase desde los XML de JUnit (app/build/test-results).
echo
printf '  %-36s %6s %6s %6s %6s\n' clase tests skip fail error
for f in app/build/test-results/testDebugUnitTest/TEST-*.xml; do
  [ -f "$f" ] || continue
  head -n 2 "$f" | grep -o 'testsuite name="[^"]*" tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' \
    | sed -E 's/testsuite name="([^"]*\.)?([^".]*)" tests="([0-9]*)" skipped="([0-9]*)" failures="([0-9]*)" errors="([0-9]*)"/\2 \3 \4 \5 \6/' \
    | xargs printf '  %-36s %6s %6s %6s %6s\n'
done
exit $fail
