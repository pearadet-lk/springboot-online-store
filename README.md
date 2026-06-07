# Java Online Store (Spring Boot microservices)

Java/Spring Boot port of [online-store](https://github.com/pearadet-lk/online-store) with the same microservice boundaries, Kafka email events, deploy scripts, and React/Angular/Vue frontends.

## Services

- `gateway` — Spring Cloud Gateway (proxy + JWT; no checkout orchestration)
- `user-service`, `product-service`, `cart-service`, `order-service`, `payment-service`
- `inventory-service` (REST + **gRPC** on port `5213`), `shipping-service`, `history-service`
- `email-service` (Kafka consumer)
- `tools/checkout-simulator` — E2E checkout + optional Kafka publish

## Persistence (JPA + Flyway)

| Service | PostgreSQL profile | Fallback |
|---------|-------------------|----------|
| `user-service` | `postgres` — Flyway `user_service` schema, JPA entities | In-memory |
| `product-service` | `postgres` — Flyway `product_service` schema, catalog seed | In-memory |

Activate PostgreSQL:

```powershell
mvn -pl services/user-service spring-boot:run -Dspring-boot.run.profiles=postgres
```

Docker Compose sets `SPRING_PROFILES_ACTIVE=postgres` and JDBC URLs automatically.

## Local run (Maven)

```powershell
mvn -pl services/user-service spring-boot:run
mvn -pl services/product-service spring-boot:run
# ... start other services, then:
mvn -pl services/gateway spring-boot:run
```

Gateway: `http://localhost:5152`

## Docker Compose

```powershell
docker compose up --build -d
mvn -pl tools/checkout-simulator spring-boot:run
```

Gateway: `http://localhost:8081` | Kafka: `localhost:9092` | Jaeger: `http://localhost:16686`

## Integration tests

Requires Docker (Testcontainers):

```powershell
mvn -pl tests/integration-tests test
```

Covers API versioning, gateway JWT, user-service PostgreSQL login, email Kafka consumer, inventory gRPC reserve/check.

## Checkout simulator (Jaeger + Kafka)

```powershell
docker compose up --build -d
mvn -pl tools/checkout-simulator spring-boot:run
# Minikube profile:
mvn -pl tools/checkout-simulator spring-boot:run -Dspring-boot.run.profiles=minikube
```

## Deploy (Minikube / Docker Desktop Kubernetes)

```powershell
minikube start
.\scripts\deploy-minikube.ps1
# or: make deploy-minikube
```

Includes **gateway**, all **10 microservices**, **PostgreSQL**, **Redis**, **Kafka**, **email-service**, and the **monitoring stack**. Frontends still run on your host (not in-cluster).

| After port-forward | URL |
|--------------------|-----|
| Gateway | `http://localhost:5152` |
| PostgreSQL | `localhost:55432` |
| Kafka | `localhost:9092` |
| Email service (direct) | `http://localhost:5164` |
| Inventory gRPC | `localhost:5213` |
| Jaeger UI | `http://localhost:16686` |

Full checkout + Kafka email (host simulator):

```powershell
make run-checkout-simulator-minikube
```

Docker Desktop Kubernetes: `.\scripts\deploy-dockerdesktop-k8s.ps1` (same manifests).

Images: `java-online-store/*:local`. Spring env vars are in `k8s/minikube-all-in-one.yaml` (`gateway-config`, `java-services-config`).

## Frontends (React, Angular, Vue)

Frontends are **not** included in Docker Compose or Kubernetes — run them on your host while the backend is up.

**Prerequisites:** Node.js 20+ and npm. Start the gateway first:

| Backend | Gateway URL |
|---------|-------------|
| Maven (`mvn -pl services/gateway spring-boot:run`) | `http://localhost:5152` |
| Docker Compose (`docker compose up -d`) | `http://localhost:8081` |
| Minikube (after port-forward) | `http://localhost:5152` |

Dev servers proxy `/api` and `/health` to the gateway so the UI can call the API without CORS issues.

### React (`src/frontend/react`)

```powershell
cd src/frontend/react
npm install
npm run dev
```

Open **`https://localhost:5173`** (Vite uses a local HTTPS cert).

Point at Docker gateway:

```powershell
$env:VITE_GATEWAY_TARGET = "http://localhost:8081"
npm run dev
```

### Angular (`src/frontend/angular`)

```powershell
cd src/frontend/angular
npm install
npm start
```

Open **`http://localhost:4200`**. `npm start` runs `ng serve` with `proxy.conf.json` (default target: `http://localhost:5152`).

For Docker, change both `target` values in `src/frontend/angular/proxy.conf.json` to `http://localhost:8081`, then restart `npm start`.

### Vue (`src/frontend/vue`)

```powershell
cd src/frontend/vue
npm install
npm run dev
```

Open **`http://localhost:5173`** (Vite picks the next free port if React is already using `5173`).

Point at Docker gateway:

```powershell
$env:VITE_GATEWAY_TARGET = "http://localhost:8081"
npm run dev
```

### Run all three at once

Use separate terminals (one per app). Default dev URLs:

| App | URL | Gateway config |
|-----|-----|----------------|
| React | `https://localhost:5173` | `VITE_GATEWAY_TARGET` env var |
| Angular | `http://localhost:4200` | `proxy.conf.json` |
| Vue | `http://localhost:5173` or `5174` | `VITE_GATEWAY_TARGET` env var |

**Note:** `POST /api/checkout` is not implemented on the gateway (same as the .NET reference). Checkout from the UI returns 404; use `tools/checkout-simulator` for an end-to-end checkout flow.

## Kafka email flow

- Topic: `email-notifications`
- Producer: **checkout-simulator** (not gateway)
- Consumer: `email-service`
- Status: `GET /api/email/status/{orderId}`

## Build all

```powershell
mvn verify
```

---

## Architecture parity (.NET reference vs Java)

Comparison with [online-store](your-local-folder-path\online-store) (.NET) and this repo.

| Concern | .NET (`online-store`) | Java (`java-online-store`) | Match |
|---------|----------------------|----------------------------|-------|
| 10 microservices + gateway | Yes | Yes | Yes |
| Proxy-only gateway (no `POST /api/checkout`) | YARP | Spring Cloud Gateway | Yes |
| API versioning `/api/v1/*` + header | Yes | Yes | Yes |
| JWT at gateway + claim headers | Yes | Yes | Yes |
| User/Product PostgreSQL | Npgsql + Flyway-like SQL bootstrap | **JPA + Flyway** (`postgres` profile) | Yes |
| In-memory fallback (no PG) | Yes | Yes | Yes |
| Cart/Order/Payment/etc. in-memory | Yes | Yes | Yes |
| Inventory **gRPC** | `Grpc.AspNetCore` + proto | **grpc-spring** + proto in `contracts` | Yes |
| Inventory REST | Yes | Yes | Yes |
| Kafka email (`email-notifications`) | Confluent.Kafka | spring-kafka | Yes |
| Email producer placement | CheckoutSimulator | CheckoutSimulator | Yes |
| Stripe payments (mock without key) | Stripe.net | stripe-java | Yes |
| Payment idempotency header | Yes | Yes | Yes |
| Demo user / 100 products seed | Yes | Yes | Yes |
| Docker Compose full stack | Yes | Yes | Yes |
| Minikube (Kafka + email-service pods) | No (reference gap) | **Yes** (updated manifests) | Java ahead |
| React / Angular / Vue frontends | Yes | Copied | Yes |
| Integration tests | xUnit + Testcontainers pattern | **Testcontainers IT module** | Partial |
| Gateway rate limiting | Yes | Not yet | Gap |
| Serilog → Elasticsearch | Yes | Not wired | Gap |
| Full OTel on every service | Yes | Micrometer/Actuator baseline | Partial |
| Gateway checkout orchestrator | Not active | Not active | Yes |
| `POST /api/checkout` (frontends) | Fails (404) | Fails (404) | Yes |

### Technology mapping (summary)

| Layer | .NET | Java |
|-------|------|------|
| Runtime | .NET 10 | Java 21 |
| Web | ASP.NET Core minimal APIs | Spring Boot 3.4 Web |
| Gateway | YARP | Spring Cloud Gateway |
| ORM / migrations | Raw Npgsql + SQL scripts | JPA + Flyway |
| Messaging | Confluent.Kafka | spring-kafka |
| RPC | gRPC (inventory) | gRPC (inventory, port 5213) |
| Metrics | prometheus-net | Micrometer Prometheus |
| Tests | xUnit | JUnit 5 + Testcontainers |
