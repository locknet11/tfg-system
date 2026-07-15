# Data Model: OCI Image Deployment

**Feature**: 023-oci-image-deployment | **Date**: 2026-07-14

## Entities

### API Container Image

An OCI-compliant image for the Spring Boot backend, built with Eclipse Temurin.

| Attribute | Type | Description |
|---|---|---|
| `image_name` | string | `tfg-api` |
| `version_tag` | string | `latest` or semver (e.g., `0.0.1`) |
| `base_image` | string | `eclipse-temurin:17-jre-alpine` |
| `exposed_port` | integer | `8080` |
| `run_user` | string | `appuser` (uid 1001, non-root) |
| `health_endpoint` | string | `GET /actuator/health` |

### UI Container Image

An OCI-compliant image for the Angular frontend, served by nginx.

| Attribute | Type | Description |
|---|---|---|
| `image_name` | string | `tfg-ui` |
| `version_tag` | string | `latest` or semver (e.g., `0.0.1`) |
| `base_image` | string | `nginx:stable-alpine` |
| `exposed_port` | integer | `80` |
| `run_user` | string | `nginx` (built-in, non-root) |
| `health_endpoint` | string | `GET /health` |

### Docker Compose Configuration

Defines the three-service orchestration.

| Service | Build Context | Internal Port | Host Port | Depends On |
|---|---|---|---|---|
| `api` | `./api` | 8080 | none | (none) |
| `ui` | `./ui` | 80 | none | (none) |
| `caddy` | (image) | 80, 443 | 80, 443 | api, ui |

### Caddy Routes

| Domain | Target Service | Target Port |
|---|---|---|
| `tfg.locknet.com.ar` | `ui` | 80 |
| `tfg-api.locknet.com.ar` | `api` | 8080 |

## Environment Variable Contracts

### API Service Variables

All variables map to Spring Boot properties via relaxed binding. Variables without defaults in the Docker Compose file use empty strings as placeholders that the operator must replace.

| Variable | Required | Default (Compose) | Maps To |
|---|---|---|---|
| `SERVER_PORT` | No | `8080` | `server.port` |
| `MONGODB_URI` | **Yes** | `mongodb://localhost:27017` | `mongodb.connection.uri` |
| `MONGODB_DATABASE_NAME` | No | `tfg-system` | `mongodb.database.name` |
| `MONGODB_AUTO_INDEX_CREATION` | No | `true` | `spring.data.mongodb.auto-index-creation` |
| `JWT_SECRET` | **Yes** | (placeholder) | `jwt.secret` |
| `LOG_RESOLVED_EXCEPTION` | No | `false` | `spring.mvc.log-resolved-exception` |
| `ALLOWED_ORIGINS` | No | `https://tfg.locknet.com.ar` | `allowedOrigins` |
| `APPLICATION_DOMAIN` | No | `https://tfg.locknet.com.ar` | `applicationDomain` |
| `API_BASE_URL` | No | `https://tfg-api.locknet.com.ar` | `api.base-url` |
| `RESEND_API_KEY` | No | (empty — email disabled) | `resend.api-key` |
| `RESEND_FROM_ADDRESS` | No | (empty) | `resend.from-address` |
| `NVD_API_KEY` | No | (empty) | `nvd.api.key` |
| `NVD_API_BASE_URL` | No | `https://services.nvd.nist.gov/rest/json` | `nvd.api.base-url` |
| `NVD_RATE_LIMIT_REQUESTS` | No | `5` | `nvd.api.rate-limit.requests-per-window` |
| `NVD_RATE_LIMIT_WINDOW` | No | `30` | `nvd.api.rate-limit.window-seconds` |
| `VULN_CACHE_STALENESS_DAYS` | No | `30` | `vulnerability.cache.staleness-days` |
| `HEARTBEAT_TIMEOUT_SECONDS` | No | `120` | `heartbeat.timeout.seconds` |
| `HEARTBEAT_SCHEDULER_DELAY_MS` | No | `30000` | `heartbeat.scheduler.delay-ms` |
| `REPORTS_SCHEDULER_ENABLED` | No | `false` | `reports.scheduler.enabled` |
| `REPORTS_SCHEDULER_CRON` | No | `0 0 2 * * *` | `reports.scheduler.cron` |
| `AGENT_BINARY_RESOURCE_PATH` | No | `agents` | `agent.binary.resource-path` |
| `CENTRAL_PUBLIC_KEY` | No | (embedded default) | `central.public-key` |
| `REPLICATION_PRIVATE_KEY` | No | (embedded default) | `replication.private-key` |

### UI Service Variables

| Variable | Required | Default (Compose) | Purpose |
|---|---|---|---|
| `API_BASE_URL` | No | `https://tfg-api.locknet.com.ar` | Injected into compiled JS at container startup; sets the backend API endpoint the dashboard calls |

## State Transitions

### Container Lifecycle

```
Build → Image stored → Container created → Starting → Healthy → Stopping → Stopped
                                                      ↘ Unhealthy (restart)
```

### Image Build Lifecycle

```
Source checkout → Dependency resolution → Compilation → Image layer assembly → Image tag → Push to registry (optional)
```
