# ============================================================
# BookMyJuice Backend - Production Dockerfile
# ============================================================
# Multi-stage build for optimized production image
# ============================================================

# ============================================================
# Stage 1: Build
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: Production Runtime
# ============================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

# JVM optimizations for containerized environments
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:+UseStringDeduplication \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# ============================================================
# Build Instructions:
# ============================================================
# Build Docker image:
#   docker build -t bookmyjuice-backend:latest .
#
# Run container:
#   docker run -d -p 8080:8080 \
#     -e DB_HOSTNAME=mysql \
#     -e DB_NAME=bmj_db \
#     -e DB_USERNAME=bmj \
#     -e DB_PASSWORD=your_password \
#     -e CHARGEBEE_API_KEY=your_key \
#     -e JWT_SECRET=your_jwt_secret \
#     --name bmj-backend \
#     bookmyjuice-backend:latest
#
# View logs:
#   docker logs -f bmj-backend
#
# Access Swagger UI:
#   http://localhost:8080/swagger-ui.html
# ============================================================
