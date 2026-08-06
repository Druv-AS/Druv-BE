# Stage 1: Build the Java 21 Spring Boot JAR using Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Package the application skipping tests
RUN mvn clean package -DskipTests

# Stage 2: Lightweight Java Runtime Environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built JAR from stage 1
COPY --from=build /app/target/dhruv-backend-0.0.1-SNAPSHOT.jar app.jar

# Expose server port 8080
EXPOSE 8080

# Run Spring Boot Application
ENTRYPOINT ["java", "-jar", "app.jar"]
