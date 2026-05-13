# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY file-service/pom.xml file-service/
RUN mvn -f file-service/pom.xml dependency:go-offline
COPY file-service/src file-service/src
RUN mvn -f file-service/pom.xml clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/file-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
