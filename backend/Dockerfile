# syntax=docker/dockerfile:1

# --- Stage 1: build the Spring Boot fat JAR -----------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the POM first and pre-fetch dependencies so this layer is cached as long
# as the POM does not change (fast rebuilds when only source changes).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- Stage 2: slim runtime image ---------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the repackaged (executable) JAR only - *.jar excludes the *.jar.original.
COPY --from=build /app/target/votevox-backend-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
