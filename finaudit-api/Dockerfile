# Stage 1: Build multi-module Maven project
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Copy parent and module POMs
COPY pom.xml ./
COPY finaudit-core/pom.xml ./finaudit-core/
COPY finaudit-api/pom.xml ./finaudit-api/

# Copy source trees
COPY finaudit-core/src ./finaudit-core/src
COPY finaudit-api/src ./finaudit-api/src

# Package application jar (unit/integration tests executed during CI pipeline)
RUN mvn clean install -DskipTests -B -pl finaudit-api -am

# Stage 2: Minimal hardened JRE runtime
FROM eclipse-temurin:21-jre-jammy

# Install curl for container health check
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root system group and user
RUN groupadd -r appgroup && useradd -r -g appgroup -d /app -s /sbin/nologin appuser

WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder /workspace/finaudit-api/target/finaudit-api-0.0.1-SNAPSHOT.jar /app/app.jar

# Enforce secure ownership
RUN chown -R appuser:appgroup /app

# Switch to unprivileged user
USER appuser

ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

HEALTHCHECK --interval=20s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
