# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY collab-service/pom.xml collab-service/
RUN mvn -f collab-service/pom.xml dependency:go-offline
COPY collab-service/src collab-service/src
RUN mvn -f collab-service/pom.xml clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/collab-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
