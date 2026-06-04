Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "Checking Minikube..." -ForegroundColor Cyan
minikube status | Out-Null

Write-Host "Switching Docker CLI to Minikube Docker daemon..." -ForegroundColor Cyan
& minikube -p minikube docker-env --shell powershell | Invoke-Expression

$services = @("gateway", "product-service", "cart-service", "order-service", "payment-service", "user-service", "inventory-service", "shipping-service", "history-service", "email-service")

foreach ($service in $services) {
    $image = "java-online-store/${service}:local"
    Write-Host "Building $image..." -ForegroundColor Yellow
    docker build -f docker/Dockerfile.service --build-arg SERVICE=$service -t $image .
}

Write-Host "Applying Kubernetes manifests..." -ForegroundColor Cyan
kubectl apply -f k8s/minikube-all-in-one.yaml
kubectl apply -f k8s/minikube-monitoring.yaml

$deployments = @(
    "postgres",
    "redis",
    "kafka",
    "email-service",
    "gateway",
    "product-service",
    "cart-service",
    "order-service",
    "payment-service",
    "user-service",
    "inventory-service",
    "shipping-service",
    "history-service",
    "jaeger",
    "zipkin",
    "prometheus",
    "grafana",
    "elasticsearch",
    "kibana"
)

foreach ($deployment in $deployments) {
    $rolloutTimeout = switch ($deployment) {
        "elasticsearch" { "420s" }
        "kibana" { "420s" }
        "kafka" { "300s" }
        Default { "240s" }
    }
    Write-Host "Waiting for deployment/$deployment (timeout $rolloutTimeout)..." -ForegroundColor Yellow
    kubectl rollout status deployment/$deployment -n online-store --timeout=$rolloutTimeout
}

Write-Host "Starting all Minikube port-forwards..." -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "port-forward-minikube.ps1")

Write-Host "Minikube deployment completed." -ForegroundColor Green
Write-Host "Primary API URL: http://localhost:5152 (gateway)" -ForegroundColor Green
Write-Host "PostgreSQL (DBeaver / SQL clients): localhost:55432 (db/user/pass: onlinestore) — see README." -ForegroundColor Green
Write-Host "(Final localhost URLs also printed above by port-forward-minikube.ps1.)" -ForegroundColor DarkGray
Write-Host ""
Write-Host "Note: k8s/minikube-all-in-one.yaml does not deploy the React/Angular/Vue apps." -ForegroundColor Yellow
Write-Host "  - Start a UI locally (see README Frontend apps)." -ForegroundColor Yellow
Write-Host "  - Port-forwards are started automatically for gateway, services, postgres, and redis." -ForegroundColor Yellow
Write-Host "  - Frontend dev proxies can use http://localhost:5152" -ForegroundColor Yellow
Write-Host "  - Kafka and email-service are deployed in-cluster (port-forward kafka:9092, email:5164)." -ForegroundColor Yellow
Write-Host "  - Run checkout simulator: .\scripts\run-checkout-simulator-minikube.ps1 (HTTP + Jaeger + optional Kafka publish)." -ForegroundColor Yellow
Write-Host "  - Tunnel-only refresh: make restart-port-forward-minikube (if localhost:5152 refuses)." -ForegroundColor Yellow
