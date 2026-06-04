Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "Checkout simulator (Docker Compose defaults)" -ForegroundColor Cyan
Write-Host "  Uses appsettings.json: Gateway http://localhost:8081/, OTLP http://localhost:4317, Kafka publish on unless Simulator__PublishKafka overrides." -ForegroundColor Yellow
Write-Host "  Start stack first: make deploy-docker-local (or docker compose up --build -d from repo root)." -ForegroundColor Yellow
Write-Host ""

if (-not $env:SIMULATOR_PUBLISH_KAFKA) {
    $env:SIMULATOR_PUBLISH_KAFKA = "true"
}
if (-not $env:GATEWAY_BASE_URL) {
    $env:GATEWAY_BASE_URL = "http://localhost:8081/"
}

$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if ($mvn) {
    & mvn -pl tools/checkout-simulator spring-boot:run @args
} else {
    Write-Host "Maven (mvn) not found on PATH. Install Maven or run: docker compose up --build -d" -ForegroundColor Red
    exit 1
}
