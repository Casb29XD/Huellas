# Imagen para compilar y testear la app Android sin SDK en el host.
ARG BASE=ghcr.io/cirruslabs/android-sdk:36
FROM ${BASE}
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.jvmargs=-Xmx2g -Dkotlin.compiler.execution.strategy=in-process"
RUN yes | sdkmanager --licenses >/dev/null 2>&1 || true
# compileSdk/targetSdk 37: AGP lo resuelve como "platforms;android-37.0"; se intentan ambos ids.
RUN (sdkmanager "platforms;android-37.0" || sdkmanager "platforms;android-37" || true) >/dev/null 2>&1; \
    (sdkmanager "build-tools;37.0.0" || true) >/dev/null 2>&1
WORKDIR /workspace
ENTRYPOINT ["bash", "./gradlew"]
