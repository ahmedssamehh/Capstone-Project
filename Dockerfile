# =============================================================================
# WorkHub Enterprise SaaS - Production Multi-Stage Dockerfile
# =============================================================================
#
# Architecture:
#   Stage 1 (deps)      - Resolve Maven dependencies only (cached when pom.xml unchanged)
#   Stage 2 (builder)   - Compile and package executable JAR (cached when source unchanged)
#   Stage 3 (extractor) - Explode Spring Boot layers for optimal Docker layer caching
#   Stage 4 (runtime)   - Minimal JRE-only image with non-root execution
#
# Why multi-stage builds:
#   - Smaller attack surface: final image contains no compiler, source, or build tools
#   - Smaller image size: JRE-only runtime vs full JDK (~40-60% reduction typical)
#   - Faster deploys: layer caching means code changes do not invalidate dependency layers
#   - Security: non-root user, minimal OS packages, no Maven or source in production image
#
# Performance:
#   - Dependency layer rebuilds only when pom.xml changes
#   - Application layer rebuilds only when application code changes
#   - Spring Boot layered JAR lets Docker cache dependency bytecode separately from app code
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Dependency resolution (cache-friendly)
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17-alpine AS deps
WORKDIR /build

# Copy only POM first so dependency download is cached across source edits
COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# -----------------------------------------------------------------------------
# Stage 2: Compile and package executable JAR
# -----------------------------------------------------------------------------
FROM deps AS builder

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B

# -----------------------------------------------------------------------------
# Stage 3: Extract Spring Boot layers for Docker layer caching
# -----------------------------------------------------------------------------
FROM builder AS extractor
WORKDIR /extract

ARG JAR_FILE=target/workhub-0.0.1-SNAPSHOT.jar
COPY --from=builder /build/${JAR_FILE} app.jar

RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination /extracted

# -----------------------------------------------------------------------------
# Stage 4: Production runtime (minimal JRE + application layers only)
# -----------------------------------------------------------------------------
# Pin digest in CI/prod for immutable deploys; tag updated on security patches
FROM eclipse-temurin:17-jre-alpine AS runtime

# Labels for image metadata (OCI / registry scanning)
LABEL org.opencontainers.image.title="WorkHub API" \
      org.opencontainers.image.description="Multi-tenant SaaS backend - production runtime" \
      org.opencontainers.image.vendor="WorkHub"

# curl: health checks only; tzdata: consistent UTC timestamps in logs
RUN apk add --no-cache \
        curl \
        tzdata \
    && rm -rf /var/cache/apk/*

ENV TZ=UTC \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

# JVM tuned for containers: cgroup-aware memory, G1 GC, fail fast on OOM
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    -XX:+UseG1GC \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/urandom \
    -Dfile.encoding=UTF-8"

WORKDIR /app

# Non-root user (UID 1000 is conventional for Spring Boot / Kubernetes security contexts)
RUN addgroup -g 1000 -S spring && \
    adduser -u 1000 -S spring -G spring -h /app -D

# Copy exploded layers (dependencies change less often than application code)
COPY --from=extractor --chown=spring:spring /extracted/dependencies/ ./
COPY --from=extractor --chown=spring:spring /extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=spring:spring /extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=spring:spring /extracted/application/ ./

USER spring:spring

EXPOSE 8080

# Profile and secrets are set at deploy time (compose/K8s), never baked into the image
# Actuator liveness - public in SecurityConfig; matches Kubernetes liveness probes
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health/liveness || exit 1

# Spring Boot 3 layered launcher (executable JAR layout without repackaging fat jar)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
