# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /build

# Copy Maven wrapper and POM first for dependency caching
COPY mvnw .
COPY .mvn/ .mvn/
COPY pom.xml .

# Cache Maven dependencies in a separate layer
RUN ./mvnw dependency:go-offline -B

# Copy source code and build the application (skip tests — they may need MongoDB)
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine

# Create non-root user and install wget for health check
RUN apk add --no-cache wget && \
    addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /build/target/app-*.jar /app/app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
