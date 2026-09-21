# syntax=docker/dockerfile:1

# ============================================================
# Stage 1: Build the Spring Boot application
# ============================================================
FROM maven:3.9.11-eclipse-temurin-21-alpine AS builder

WORKDIR /workspace

# Copy the build descriptor first so dependency layers can be cached.
COPY pom.xml .

# Download dependencies before copying source code.
RUN mvn --batch-mode \
    --no-transfer-progress \
    -DskipTests \
    dependency:go-offline

# Copy application source code.
COPY src ./src

# Tests are executed separately in CI and before release.
# The container build creates the executable Spring Boot JAR.
RUN mvn --batch-mode \
    --no-transfer-progress \
    -DskipTests \
    clean package


# ============================================================
# Stage 2: Minimal Java runtime image
# ============================================================
FROM eclipse-temurin:25-jre-alpine AS runtime

LABEL org.opencontainers.image.title="ExpenseMate Backend"
LABEL org.opencontainers.image.description="Spring Boot backend for ExpenseMate"
LABEL org.opencontainers.image.source="https://github.com/SreenithiRamesh/expensemate"
LABEL org.opencontainers.image.vendor="Sreenithi Ramesh"

# Create a dedicated non-root user.
RUN addgroup --system expensemate \
    && adduser --system \
       --ingroup expensemate \
       --home /app \
       --disabled-password \
       expensemate

WORKDIR /app

# Copy only the final executable JAR from the build stage.
COPY --from=builder \
    --chown=expensemate:expensemate \
    /workspace/target/expensemate-backend-0.0.1-SNAPSHOT.jar \
    /app/expensemate.jar

USER expensemate

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

ENTRYPOINT ["java", "-jar", "/app/expensemate.jar"]