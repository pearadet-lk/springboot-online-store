Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

Write-Host "Checking kubectl context..." -ForegroundColor Cyan
$currentContext = kubectl config current-context
if ([string]::IsNullOrWhiteSpace($currentContext)) {
    throw "kubectl has no current context. Enable Kubernetes in Docker Desktop and retry."
}

if ($currentContext -notlike "docker-desktop*") {
    Write-Host "Warning: current context is '$currentContext' (not docker-desktop)." -ForegroundColor Yellow
    Write-Host "If needed, switch with: kubectl config use-context docker-desktop" -ForegroundColor Yellow
}

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

Write-Host "Starting Docker Desktop Kubernetes port-forwards..." -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "port-forward-dockerdesktop-k8s.ps1")

$gatewayUrl = "http://localhost:5152"
Write-Host "Docker Desktop Kubernetes deployment completed." -ForegroundColor Green
Write-Host "Gateway URL: $gatewayUrl" -ForegroundColor Green
Write-Host "PostgreSQL (DBeaver / SQL clients): localhost:55432 (db/user/pass: onlinestore) — see README." -ForegroundColor Green
Write-Host ""
Write-Host "Note: k8s/minikube-all-in-one.yaml does not deploy the React/Angular/Vue apps." -ForegroundColor Yellow
Write-Host "  - Start a UI locally (see README Frontend apps)." -ForegroundColor Yellow
Write-Host "  - Port-forwards are started automatically for gateway, services, postgres, and redis." -ForegroundColor Yellow
Write-Host "  - Frontend dev proxies can use http://localhost:5152" -ForegroundColor Yellow
Write-Host "  - Kafka and email-service are deployed (port-forward kafka:9092, email:5164)." -ForegroundColor Yellow
Write-Host "  - Tunnel-only refresh: make restart-port-forward-dockerdesktop-k8s (if localhost:5152 refuses)." -ForegroundColor Yellow
