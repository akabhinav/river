# Multi-stage build for River Platform
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy parent POM
COPY pom.xml .

# Copy all module POMs
COPY river-core/pom.xml river-core/
COPY river-engine/pom.xml river-engine/
COPY river-connectors-api/pom.xml river-connectors-api/
COPY river-connectors-source/pom.xml river-connectors-source/
COPY river-connectors-dest/pom.xml river-connectors-dest/
COPY river-api/pom.xml river-api/
COPY river-runtime/pom.xml river-runtime/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY river-core/src river-core/src
COPY river-engine/src river-engine/src
COPY river-connectors-api/src river-connectors-api/src
COPY river-connectors-source/src river-connectors-source/src
COPY river-connectors-dest/src river-connectors-dest/src
COPY river-api/src river-api/src
COPY river-runtime/src river-runtime/src

# Build the application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built JAR
COPY --from=build /app/river-runtime/target/river-runtime-*.jar app.jar

# Create non-root user
RUN addgroup -S river && adduser -S river -G river
USER river

# Expose ports
EXPOSE 8080 9090

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
