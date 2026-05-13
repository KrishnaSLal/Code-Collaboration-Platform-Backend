# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY comment-service/pom.xml comment-service/
RUN mvn -f comment-service/pom.xml dependency:go-offline
COPY comment-service/src comment-service/src
RUN mvn -f comment-service/pom.xml clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/comment-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
