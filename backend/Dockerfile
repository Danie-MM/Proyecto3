FROM eclipse-temurin:21-jdk-jammy@sha256:4e61d0b3471137e239397789661f6adeaf8db9adcfae45916b22f628c8a32dc0

RUN apt-get update && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 10001 petmatch
WORKDIR /app
RUN chown petmatch:petmatch /app
USER petmatch

COPY --chown=petmatch:petmatch .mvn .mvn
COPY --chown=petmatch:petmatch mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY --chown=petmatch:petmatch src ./src
RUN ./mvnw -B -ntp -DskipTests package

EXPOSE 8080
CMD ["java", "-jar", "target/petmatchcr-backend.jar"]
