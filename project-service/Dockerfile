# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY project-service/pom.xml project-service/
RUN mvn -f project-service/pom.xml dependency:go-offline
COPY project-service/src project-service/src
RUN mvn -f project-service/pom.xml clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/project-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
