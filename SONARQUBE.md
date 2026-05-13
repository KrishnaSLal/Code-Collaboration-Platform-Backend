# SonarQube Backend Scan

This backend uses the SonarScanner for Maven through the root aggregator `pom.xml`. The aggregator includes all backend microservices and keeps the individual service POM files unchanged.

## Prerequisites

- Maven on PATH.
- A SonarQube Server project with key `CodeSync`, or permission for the scanner to create it.
- A SonarQube token exposed as `SONAR_TOKEN`.
- Optional: `SONAR_HOST_URL`; defaults to `http://localhost:9000` in the helper script.

## Run Locally

From `E:\CodeSync-Backend`:

```powershell
$env:SONAR_TOKEN = "your-token"
$env:SONAR_HOST_URL = "http://localhost:9000"
.\scripts\sonar-scan.ps1
```

To skip tests while still compiling and packaging the services:

```powershell
.\scripts\sonar-scan.ps1 -SkipTests
```

To wait for the SonarQube quality gate result:

```powershell
.\scripts\sonar-scan.ps1 -WaitForQualityGate
```

## Direct Maven Commands

```powershell
mvn -B clean install
mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar -Dsonar.host.url=http://localhost:9000
```

The scanner reads authentication from `SONAR_TOKEN` automatically.

## Coverage

The root POM is prepared to import JaCoCo XML reports from `**/target/site/jacoco/jacoco.xml`. Add JaCoCo test reporting to the services when tests are introduced or when coverage gates are enabled in SonarQube.
