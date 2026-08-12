# syntax=docker/dockerfile:1

# ---------------------------------------------------------------- build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Resolve dependencies first so this layer is cached until pom.xml itself changes.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
# Tests run in CI against a real migration-backed schema; skipping here keeps image
# builds reproducible and fast.
RUN mvn clean package -B -DskipTests \
    && mv target/*.jar target/app.jar

# -------------------------------------------------------------- runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# curl is needed by the healthcheck below.
RUN apk add --no-cache curl \
    && addgroup -S spring \
    && adduser -S spring -G spring

# Copy as root, then hand ownership to the unprivileged user. The original Dockerfile
# switched user *before* COPY, which left the jar owned by root.
COPY --from=build --chown=spring:spring /app/target/app.jar app.jar

USER spring:spring

EXPOSE 8080
ENV PORT=8080

# Respect the container's memory limit instead of the JVM's default 1/4-of-host heuristic,
# and prefer a container-aware GC. Override JAVA_OPTS per environment as needed.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+UseContainerSupport -XX:+ExitOnOutOfMemoryError"

# Fail fast on an unresponsive process so the orchestrator can replace it. Uses the
# liveness probe, which reports the process itself rather than downstream dependencies.
HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=3 \
    CMD curl -fsS "http://localhost:${PORT}/actuator/health/liveness" || exit 1

# Shell form so JAVA_OPTS expands; exec ensures the JVM is PID 1 and receives SIGTERM
# directly, allowing Spring's graceful shutdown to run.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
