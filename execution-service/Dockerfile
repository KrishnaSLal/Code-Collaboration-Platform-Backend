# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY execution-service/pom.xml execution-service/
RUN mvn -f execution-service/pom.xml dependency:go-offline
COPY execution-service/src execution-service/src
RUN mvn -f execution-service/pom.xml clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        gcc \
        g++ \
        nodejs \
        npm \
        python3 \
    && npm install -g tsx typescript \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/execution-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
