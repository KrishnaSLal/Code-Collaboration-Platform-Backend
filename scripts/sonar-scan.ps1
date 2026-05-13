[CmdletBinding()]
param(
    [string]$SonarHostUrl = $(if ($env:SONAR_HOST_URL) { $env:SONAR_HOST_URL } else { "http://localhost:9000" }),
    [switch]$SkipTests,
    [switch]$WaitForQualityGate
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    throw "Maven is required to run the backend SonarQube scan. Install Maven or add mvn to PATH."
}

if (-not $env:SONAR_TOKEN) {
    throw "SONAR_TOKEN is required. Create a SonarQube token and set it before running this script."
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$scannerGoal = "org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar"
$jacocoGoalPrefix = "org.jacoco:jacoco-maven-plugin:0.8.12"

Push-Location $repoRoot
try {
    $buildArgs = @("-B", "clean", "install")
    if ($SkipTests) {
        $buildArgs += "-DskipTests"
    }
    else {
        $buildArgs = @(
            "-B",
            "clean",
            "${jacocoGoalPrefix}:prepare-agent",
            "install",
            "${jacocoGoalPrefix}:report"
        )
    }

    & mvn @buildArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    if ($SkipTests) {
        Write-Warning "Tests were skipped, so no JaCoCo coverage report will be generated for SonarQube."
    }
    else {
        $coverageReports = Get-ChildItem -Path $repoRoot -Recurse -Filter "jacoco.xml" -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -like "*\target\site\jacoco\jacoco.xml" }

        if (-not $coverageReports) {
            Write-Warning "No JaCoCo XML coverage reports were generated. SonarQube will show 0.0% coverage."
        }
        else {
            Write-Host "Generated $($coverageReports.Count) JaCoCo XML coverage report(s) for SonarQube."
        }
    }

    $sonarArgs = @(
        "-B",
        $scannerGoal,
        "-Dsonar.host.url=$SonarHostUrl"
    )

    if ($WaitForQualityGate) {
        $sonarArgs += "-Dsonar.qualitygate.wait=true"
    }

    & mvn @sonarArgs
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}
