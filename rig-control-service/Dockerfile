# Multi-stage build for Rig Control Service with Hamlib
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage with Hamlib
FROM eclipse-temurin:17-jre

# Install Hamlib
RUN apt-get update && \
    apt-get install -y hamlib-utils && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose service port
EXPOSE 8081

# Expose rigctld port
EXPOSE 4532

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
