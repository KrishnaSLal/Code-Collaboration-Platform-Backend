# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY version-service/pom.xml version-service/
RUN mvn -f version-service/pom.xml dependency:go-offline
COPY version-service/src version-service/src
RUN mvn -f version-service/pom.xml clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/version-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
