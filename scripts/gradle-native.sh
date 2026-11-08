#!/usr/bin/env bash
# Ejecuta Gradle en el host (sin Docker). Uso: scripts/gradle-native.sh :app:testDebugUnitTest
# Usa JAVA_HOME/ANDROID_HOME si ya están definidos; si no, el JDK 21 y el SDK portables de C:\temp\huellas-audit\tools.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLS="${HUELLAS_TOOLS:-/c/temp/huellas-audit/tools}"
if [ -z "${JAVA_HOME:-}" ] || ! "${JAVA_HOME}/bin/java" -version 2>&1 | grep -q 'version "\(1[7-9]\|2[0-9]\)'; then
  JDK_DIR="$(ls -d "$TOOLS"/jdk/jdk-* 2>/dev/null | head -1)"
  [ -n "$JDK_DIR" ] || { echo "No hay JDK 17+ (define JAVA_HOME)"; exit 1; }
  export JAVA_HOME="$JDK_DIR"
fi
export ANDROID_HOME="${ANDROID_HOME:-$TOOLS/android-sdk}"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) export JAVA_HOME="$(cygpath -w "$JAVA_HOME")"; export ANDROID_HOME="$(cygpath -w "$ANDROID_HOME")";; esac
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export GRADLE_OPTS="${GRADLE_OPTS:--Dorg.gradle.jvmargs=-Xmx3g -Dkotlin.compiler.execution.strategy=in-process}"
cd "$ROOT"
exec bash ./gradlew "$@"
