# Stage 1: Build Java 21 Spring Boot Application using Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and package application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Lightweight JRE 21 Runtime Environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built JAR artifact from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot default port 8080
EXPOSE 8080

# Environment variables
ENV PORT=8080

# Run Spring Boot Application
ENTRYPOINT ["java", "-jar", "app.jar"]
