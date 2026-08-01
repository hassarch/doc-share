# syntax=docker/dockerfile:1

# ---- Build stage ----
# Contains the full JDK + Gradle + build cache - none of this belongs in
# the final image, which is exactly why this is a separate stage.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy only what's needed to resolve dependencies first, so Docker's layer
# cache is reused on every rebuild where only application code changed
# (not build.gradle.kts) - this is the single biggest lever for fast
# repeated builds in CI.
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar --no-daemon -x test
# Tests are deliberately NOT run as part of the image build - they already
# ran in CI (backend-ci.yml) before this build is ever triggered; running
# them again here would need Testcontainers/Docker-in-Docker for no
# additional confidence.

# ---- Runtime stage ----
# Slim JRE (not JDK) - this container only runs a jar, it never compiles
# anything, so the compiler/build tools have no reason to be here.
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Runs as a dedicated non-root user - a compromised app process in this
# container should not have root inside the container, even though
# container root isn't host root, this is still defense in depth (FR-19.x
# spirit: minimize what an attacker gains from any single compromise).
RUN addgroup -S docshare && adduser -S docshare -G docshare
USER docshare

COPY --from=build --chown=docshare:docshare /workspace/build/libs/docshare-backend.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# No profile is hardcoded here - which profile runs (and every JWT_SECRET/
# DB_*/REDIS_*/MINIO_*/KAFKA_*/CORS_ALLOWED_ORIGINS value) is supplied via
# environment variables at `docker run`/compose time, per the Environment
# Variables & Secrets section of this phase - never baked into the image.
ENTRYPOINT ["java", "-jar", "app.jar"]
