# Quickstart: Expand Remediation Strategies Knowledge Base

**Feature**: 019-expand-remediation-strategies  
**Date**: 2026-07-12

## Prerequisites

- Java 17+ and Maven for api/
- Node.js 18+ and npm for ui/
- Docker Engine and Docker Compose for lab/
- MongoDB running (local or Docker) for api/
- Git branch: `019-expand-remediation-strategies`

## Development Workflow

### 1. API: Expand strategies.json

Edit `api/src/main/resources/remediation/strategies.json` and add new strategy entries.

**Strategy entry template**:

```json
{
  "cveId": "CVE-YYYY-NNNNN",
  "operatingSystem": "ubuntu-22.04",
  "packageName": "package-name",
  "remediationType": "SERVICE_UPDATE",
  "action": "APT_UPGRADE",
  "targetVersion": "X.Y.Z-version",
  "preCheckCommands": ["dpkg -l package-name"],
  "fixCommands": ["apt-get update", "apt-get install -y package-name"],
  "postCheckCommands": ["dpkg -l package-name", "service --version"],
  "serviceName": "service-name",
  "requiresReboot": false,
  "notes": "Brief description of vulnerability and fix."
}
```

**Validation**: Build the API and check logs during startup for any seeding errors.

```bash
cd api && ./mvnw clean package -DskipTests
# Check logs for "Successfully seeded N remediation strategies"
```

### 2. API: Modify seed loader for incremental seeding

`RemediationStrategyLoader.java` currently skips seeding if collection is non-empty. Change to iterate and insert individually, skipping duplicates.

**Test**:

```bash
cd api && ./mvnw test -Dtest=RemediationStrategyLoaderTest
```

### 3. API: Add strategy listing endpoint

Create `RemediationStrategyController` with:
- `GET /api/remediation-strategies` — paginated list with filters
- `GET /api/remediation-strategies/{id}` — single strategy
- `GET /api/remediation-strategies/count` — aggregate counts

### 4. Lab: Add new vulnerable containers

Edit `lab/docker-compose.yml` and add new service definitions. Create Dockerfiles under `lab/targets/<service>/`.

**Dockerfile template** (for APT-based vulnerable services):

```dockerfile
FROM ubuntu:22.04

RUN apt-get update && \
    apt-get install -y --no-install-recommends <package>=<vulnerable-version> && \
    apt-get clean

EXPOSE <port>
CMD ["<service-binary>", "<args>"]
```

**Start the lab**:

```bash
cd lab && docker compose down && docker compose up -d
docker compose ps  # verify all services running
```

**Verify new targets**:

```bash
docker compose ps --format "table {{.Name}}\t{{.Ports}}\t{{.Status}}"
```

### 5. UI: Add strategy catalog view

Create `StrategiesListComponent` under `ui/src/app/pages/remediations/feature/strategies-list/`.

Update `remediations.model.ts` with strategy interfaces, `remediations.service.ts` with strategy API calls, and `remediations.routes.ts` with the new route.

**Start UI dev server**:

```bash
cd ui && npm ci && npx ng serve
```

Navigate to the strategies catalog view in the dashboard.

### 6. End-to-end verification

1. Start MongoDB
2. Start API: `cd api && ./mvnw spring-boot:run`
3. Start UI: `cd ui && npx ng serve`
4. Start lab: `cd lab && docker compose up -d`
5. Log into dashboard → verify strategy catalog shows 30+ entries
6. Deploy agent to lab network → verify agent detects vulnerabilities on new containers
7. Trigger remediation → verify new strategies are resolved and applied

## File Change Summary

| File | Action | Description |
|------|--------|-------------|
| `api/src/main/resources/remediation/strategies.json` | Expand | 24+ new strategy entries |
| `api/.../config/RemediationStrategyLoader.java` | Modify | Incremental seeding logic |
| `api/.../controller/RemediationStrategyController.java` | New | Strategy listing API |
| `api/.../model/dto/RemediationStrategyResponse.java` | New | Strategy DTO |
| `api/.../services/impl/RemediationStrategyServiceImpl.java` | Modify | Add list/findAll methods |
| `api/.../db/RemediationStrategyRepository.java` | Modify | Add query methods |
| `lab/docker-compose.yml` | Expand | 5+ new services |
| `lab/targets/{postgres,mysql,bind9,postfix,php-fpm,nodejs}/Dockerfile` | New | Custom lab containers |
| `ui/.../remediations/feature/strategies-list/*` | New | Strategy catalog UI |
| `ui/.../data-access/remediations.model.ts` | Modify | Add strategy types |
| `ui/.../data-access/remediations.service.ts` | Modify | Add strategy API calls |
| `ui/.../remediations.routes.ts` | Modify | Add strategies route |
