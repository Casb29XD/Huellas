#!/usr/bin/env bash
# Corre toda la batería: reinicia Supabase local, pgTAP y las pruebas JVM (unitarias, Robolectric y e2e) en Docker.
# Uso: scripts/test-all.sh   (requiere `supabase start` y la imagen huellas-android; Git Bash en Windows o Linux)
set -uo pipefail
cd "$(dirname "$0")/.."

step() { echo; echo "==> $1"; }
declare -A RESULT
run() { local name=$1; shift; step "$name"; if "$@"; then RESULT[$name]=OK; else RESULT[$name]="FALLO ($?)"; fi; }

run "supabase db reset" supabase db reset

step "claves de supabase status"
# Las pruebas corren en Docker: 127.0.0.1 del host se alcanza como host.docker.internal.
eval "$(supabase status -o env 2>/dev/null | grep -E '^[A-Z_]+=')"
export SUPABASE_URL="${API_URL/127.0.0.1/host.docker.internal}"
export SUPABASE_PUBLISHABLE_KEY="${PUBLISHABLE_KEY:-$ANON_KEY}"
export SUPABASE_SERVICE_ROLE_KEY="$SERVICE_ROLE_KEY"
echo "SUPABASE_URL=$SUPABASE_URL"

run "supabase test db" supabase test db
run "gradle testDebugUnitTest" scripts/gradle-docker.sh --continue :app:testDebugUnitTest

step "resumen"
fail=0
for name in "supabase db reset" "supabase test db" "gradle testDebugUnitTest"; do
  printf '  %-28s %s\n' "$name" "${RESULT[$name]}"
  [ "${RESULT[$name]}" = OK ] || fail=1
done
# Conteo por clase desde los XML de JUnit (app/build/test-results).
for f in app/build/test-results/testDebugUnitTest/TEST-*.xml; do
  [ -f "$f" ] || continue
  head -n 2 "$f" | grep -o 'testsuite name="[^"]*" tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' \
    | sed -E 's/testsuite name="[^"]*\.([^".]*)"/  \1/; s/"//g'
done
exit $fail
