#!/usr/bin/env bash
# Ejecuta Gradle dentro de Docker. Uso: scripts/gradle-docker.sh :app:testDebugUnitTest
# El cache de Gradle vive en el volumen huellas-gradle; la app alcanza Supabase local en host.docker.internal:54321.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
IMAGE="${HUELLAS_ANDROID_IMAGE:-huellas-android}"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) ROOT="$(cygpath -w "$ROOT")"; export MSYS_NO_PATHCONV=1;; esac
exec docker run --rm \
  -v "$ROOT":/workspace \
  -v huellas-gradle:/root/.gradle \
  -e SUPABASE_URL="${SUPABASE_URL:-http://host.docker.internal:54321}" \
  -e SUPABASE_PUBLISHABLE_KEY="${SUPABASE_PUBLISHABLE_KEY:-}" \
  -e SUPABASE_SERVICE_ROLE_KEY="${SUPABASE_SERVICE_ROLE_KEY:-}" \
  -e GRADLE_USER_HOME=/root/.gradle \
  -e GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx2g -Dkotlin.compiler.execution.strategy=in-process -Dorg.gradle.workers.max=2" \
  --add-host=host.docker.internal:host-gateway \
  "$IMAGE" "$@"
