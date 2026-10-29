# Imagen de desarrollo: incluye JDK y Wrapper para compilar sin Java en Windows.
FROM eclipse-temurin:21-jdk-jammy@sha256:4e61d0b3471137e239397789661f6adeaf8db9adcfae45916b22f628c8a32dc0

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 10001 petmatch

WORKDIR /app
RUN chown petmatch:petmatch /app
USER petmatch

COPY --chown=petmatch:petmatch gradlew build.gradle settings.gradle gradle.properties ./
COPY --chown=petmatch:petmatch gradle ./gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew --version

COPY --chown=petmatch:petmatch src ./src
# Compila aplicación y pruebas; las pruebas se ejecutan después con PostgreSQL.
RUN ./gradlew classes testClasses --no-daemon

EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=60s --retries=12 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1
CMD ["./gradlew", "bootRun", "--no-daemon"]
