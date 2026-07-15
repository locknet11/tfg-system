# Implementation Plan: OCI Image Deployment

**Branch**: `023-oci-image-deployment` | **Date**: 2026-07-14 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/023-oci-image-deployment/spec.md`

## Summary

Package the API backend (Spring Boot 3 / Java 17) and UI frontend (Angular 17) as OCI-compliant container images. Provide a Docker Compose definition that orchestrates both services behind a Caddy reverse proxy with domain-based routing (`tfg-api.locknet.com.ar` for the API, `tfg.locknet.com.ar` for the UI). The API image uses Eclipse Temurin with multi-stage builds to minimize size; the UI image uses nginx to serve production-built static assets; Caddy handles TLS termination and reverse-proxy duties.

## Technical Context

**Language/Version**: Java 17 (API), TypeScript 5.x / Angular 17 (UI)
**Primary Dependencies**: Spring Boot 3.1.3, Angular 17, Maven, npm, nginx, Caddy 2
**Storage**: N/A — MongoDB is external, not bundled in images
**Testing**: Image builds verified via `docker build`; runtime verified via `docker compose up` and HTTP health checks
**Target Platform**: Linux containers (OCI), any OCI-compliant runtime (Docker, Podman, containerd)
**Project Type**: Container image build configuration + orchestration definition
**Performance Goals**: Image build <10 min (clean checkout), container startup <60 s, combined image overhead <500 MB
**Constraints**: Non-root execution, no secrets in image layers, Eclipse Temurin base images, nginx for UI, Caddy reverse proxy, domain-based routing
**Scale/Scope**: 2 Dockerfiles (api/Dockerfile, ui/Dockerfile) + 1 docker-compose.yml + 1 Caddyfile + 1 .dockerignore per module

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- ✅ Repository guidance reviewed: `AGENTS.md` and relevant `.agents/skills/*/SKILL.md`
- ✅ English-only rule satisfied for code, Dockerfiles, Compose file, Caddyfile, and docs
- ✅ Proposed design is the smallest correct change: two Dockerfiles + docker-compose + Caddyfile — no new application code, no refactoring
- ✅ Stack rules captured for affected modules: `api/` uses Maven build (`./mvnw clean package`), `ui/` uses npm build (`npm ci && npm run build`)
- ✅ Verification steps identified: `docker build` for each image, `docker compose up` for integration, HTTP health checks
- ✅ No git actions needed until user explicitly requests commits
- ✅ No unknown or ambiguous requirements — user provided specific tech choices (Eclipse Temurin, nginx, Caddy, domains)

## Project Structure

### Documentation (this feature)

```text
specs/023-oci-image-deployment/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── environment-variables.md
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
api/
├── Dockerfile           # NEW: multi-stage Eclipse Temurin build
├── .dockerignore        # NEW: exclude target/, .git, etc.
├── src/
├── pom.xml
└── mvnw

ui/
├── Dockerfile           # NEW: multi-stage node build + nginx serve
├── .dockerignore        # NEW: exclude node_modules, dist, .git, etc.
├── nginx.conf           # NEW: nginx configuration for SPA + health check
├── src/
├── angular.json
└── package.json

docker-compose.yml       # NEW: api + ui + caddy services
Caddyfile                # NEW: reverse proxy + domain routing
```

**Structure Decision**: Dockerfiles live alongside their respective modules (`api/Dockerfile`, `ui/Dockerfile`) so each image is self-contained with its module's build context. The `docker-compose.yml` and `Caddyfile` live at the repo root as they orchestrate both modules together.

## Complexity Tracking

> No violations — this is a straightforward containerization of two existing modules using standard patterns.
