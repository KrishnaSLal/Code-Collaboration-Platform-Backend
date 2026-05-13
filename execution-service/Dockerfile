# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY execution-service/pom.xml execution-service/
RUN mvn -f execution-service/pom.xml dependency:go-offline
COPY execution-service/src execution-service/src
RUN mvn -f execution-service/pom.xml clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/execution-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
