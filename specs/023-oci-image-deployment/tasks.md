# Tasks: OCI Image Deployment

**Input**: Design documents from `/specs/023-oci-image-deployment/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/environment-variables.md, quickstart.md

**Tests**: No automated test framework for Dockerfiles — verification uses `docker build` and `docker compose up` with HTTP health checks as described in each story's checkpoint.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **API module**: `api/` (Spring Boot 3 / Java 17)
- **UI module**: `ui/` (Angular 17)
- **Orchestration root**: `docker-compose.yml`, `Caddyfile` at repo root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Review existing project configuration and prepare for containerization

- [x] T001 Review repository guidance in `AGENTS.md` and `.agents/skills/java-springboot/SKILL.md` for API build conventions
- [x] T002 [P] Review `api/pom.xml` to confirm Java version (17), artifact name (`app`), and main class (`com.spulido.tfg.WsApplication`)
- [x] T003 [P] Review `ui/angular.json` to confirm build output path (`dist/admin-site`) and production configuration
- [x] T004 [P] Review `ui/src/environments/environment.ts` to confirm the `baseUrl` property shape for runtime replacement

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create the Docker Compose skeleton and ignore files that both images depend on

**⚠️ CRITICAL**: No image-specific work can begin until these shared files exist

- [x] T005 Create `api/.dockerignore` excluding `target/`, `.git/`, `*.md`, `node_modules/`, IDE files, and `Dockerfile*`
- [x] T006 [P] Create `ui/.dockerignore` excluding `node_modules/`, `dist/`, `.git/`, `*.md`, IDE files, `Dockerfile*`, and `.angular/`
- [x] T007 Create `docker-compose.yml` skeleton at repo root with three service stubs (`api`, `ui`, `caddy`) sharing a custom network, with `api` and `ui` having `build:` contexts pointing to `./api` and `./ui` respectively, and `caddy` using the `caddy:2-alpine` image

**Checkpoint**: Foundation ready — both module context directories are clean for Docker builds, docker-compose skeleton exists

---

## Phase 3: User Story 1 - Build API Container Image (Priority: P1) 🎯 MVP

**Goal**: A working `api/Dockerfile` that builds the Spring Boot backend into an OCI image using Eclipse Temurin multi-stage builds

**Independent Test**: `docker build -t tfg-api ./api && docker run --rm -e MONGODB_URI=mongodb://host.docker.internal:27017 -e JWT_SECRET=test -p 8080:8080 tfg-api` — API starts and responds at `http://localhost:8080/actuator/health`

### Implementation for User Story 1

- [x] T008 [US1] Create `api/Dockerfile` with two-stage build:
  - Stage 1 (build): `eclipse-temurin:17-jdk-alpine` — copy `pom.xml` and `mvnw`, run `./mvnw dependency:go-offline` to cache dependencies, then copy `src/` and run `./mvnw clean package -DskipTests`
  - Stage 2 (runtime): `eclipse-temurin:17-jre-alpine` — create non-root user `appuser` (uid 1001), copy JAR from stage 1 as `app.jar`, expose port 8080, set `ENTRYPOINT ["java", "-jar", "/app/app.jar"]`
- [x] T009 [US1] Verify API image builds successfully: run `docker build -t tfg-api:latest ./api` from repo root and confirm no errors
- [x] T010 [US1] Verify API container starts: run `docker run --rm -e MONGODB_URI=mongodb://host.docker.internal:27017 -e JWT_SECRET=test -p 8080:8080 tfg-api:latest` and confirm the container starts without immediate crash (may fail health check if MongoDB unreachable, which is expected)

**Checkpoint**: API Docker image builds and the container process starts. The API image is independently usable.

---

## Phase 4: User Story 2 - Build UI Container Image (Priority: P2)

**Goal**: A working `ui/Dockerfile` that builds the Angular frontend with `--configuration production` and serves it via nginx

**Independent Test**: `docker build -t tfg-ui ./ui && docker run --rm -p 8081:80 tfg-ui` — nginx serves the dashboard at `http://localhost:8081`

### Implementation for User Story 2

- [x] T011 [US2] Create `ui/nginx.conf` with:
  - Server listening on port 80
  - Root pointing to `/usr/share/nginx/html`
  - `location /` with `try_files $uri $uri/ /index.html` (SPA fallback)
  - `location /health` returning `200 "healthy"` with `text/plain` content type
  - Gzip compression enabled for JS/CSS/HTML/JSON/SVG
  - Cache headers: immutably-named assets cached for 1 year, `index.html` not cached
- [x] T012 [US2] Create `ui/Dockerfile` with two-stage build:
  - Stage 1 (build): `node:20-alpine` — copy `package.json` and `package-lock.json`, run `npm ci`, copy `src/` and `angular.json` and `tsconfig*.json`, run `npm run build -- --configuration production`
  - Stage 2 (runtime): `nginx:stable-alpine` — copy built output from stage 1 (`dist/admin-site/`) to `/usr/share/nginx/html/`, copy `nginx.conf` to `/etc/nginx/conf.d/default.conf`, expose port 80, run as built-in `nginx` user
- [x] T013 [US2] Verify UI image builds successfully: run `docker build -t tfg-ui:latest ./ui` from repo root and confirm no errors
- [x] T014 [US2] Verify UI container serves content: run `docker run --rm -p 8081:80 tfg-ui:latest` and confirm `curl http://localhost:8081/health` returns `healthy` and `curl http://localhost:8081/` returns HTML

**Checkpoint**: UI Docker image builds, nginx serves the dashboard and health endpoint. The UI image is independently usable.

---

## Phase 5: User Story 3 - External Configuration via Environment (Priority: P2)

**Goal**: Both images accept runtime configuration through environment variables. The API Compose service declares all env vars. The UI image supports runtime `API_BASE_URL` injection.

**Independent Test**: Start the API with custom `SERVER_PORT` and verify it listens on the new port. Start the UI with `API_BASE_URL=https://example.com` and verify the compiled JS contains the injected URL.

### Implementation for User Story 3

- [x] T015 [US3] Update `api/Dockerfile` runtime stage to use `exec` form ENTRYPOINT that passes JVM options via `JAVA_OPTS` environment variable: `ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]`
- [x] T016 [US3] Populate the `api` service in `docker-compose.yml` with the `environment:` block containing all variables from `api/src/main/resources/application.properties` with their documented defaults (see contracts/environment-variables.md). Set `SERVER_PORT=8080`. Do NOT expose ports in the `api` service.
- [x] T017 [US3] Create `ui/docker-entrypoint.sh` that:
  - Reads `API_BASE_URL` env var (default `https://tfg-api.locknet.com.ar`)
  - Runs `find /usr/share/nginx/html -type f -name '*.js' -exec sed -i "s|__API_BASE_URL__|${API_BASE_URL}|g" {} +`
  - Execs nginx: `exec nginx -g 'daemon off;'`
  - Make the script executable (`chmod +x`) in the Dockerfile
- [x] T018 [US3] Update `ui/src/environments/environment.ts` to use placeholder: `baseUrl: '__API_BASE_URL__'` so it gets replaced at container startup
- [x] T019 [US3] Update `ui/Dockerfile` to copy `docker-entrypoint.sh` into the runtime stage, make it executable, and set it as `ENTRYPOINT` instead of directly running nginx
- [x] T020 [US3] Populate the `ui` service in `docker-compose.yml` with `environment:` block containing `API_BASE_URL=https://tfg-api.locknet.com.ar`. Do NOT expose ports in the `ui` service.
- [x] T021 [US3] Verify runtime configuration: build both images, run UI with `docker run --rm -e API_BASE_URL=https://example.com -p 8081:80 tfg-ui`, then `docker exec <container> grep -r "example.com" /usr/share/nginx/html/ | head -1` to confirm the URL was injected

**Checkpoint**: Both images accept runtime configuration. The same image can be promoted across environments.

---

## Phase 6: User Story 4 - Health Checks and Observability (Priority: P3)

**Goal**: Both containers expose health endpoints and handle SIGTERM gracefully. Spring Boot actuator health is confirmed available.

**Independent Test**: `curl http://<container>/actuator/health` returns `{"status":"UP"}` for API; `curl http://<container>/health` returns `healthy` for UI. `docker stop --time 30 <container>` completes without forced kill.

### Implementation for User Story 4

- [x] T022 [US4] Verify Spring Boot Actuator is present in `api/pom.xml`. If missing, add `spring-boot-starter-actuator` dependency; add `HEALTHCHECK` instruction in `api/Dockerfile`: `HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:8080/actuator/health || exit 1`
- [x] T023 [US4] Verify `ui/nginx.conf` health endpoint from T011 returns 200. Add `HEALTHCHECK` instruction in `ui/Dockerfile`: `HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:80/health || exit 1`
- [x] T024 [US4] Add `stop_grace_period: 30s` to both `api` and `ui` services in `docker-compose.yml` to allow graceful shutdown
- [x] T025 [US4] Verify graceful shutdown: start API via compose, send `docker compose stop api`, confirm container exits with code 0 within 30 seconds (Spring Boot handles SIGTERM natively)

**Checkpoint**: Both images support health checks and graceful shutdown. Orchestration platforms can manage container lifecycle.

---

## Phase 7: User Story 5 - Caddy Reverse Proxy & Integration (Priority: P3)

**Goal**: Caddy reverse proxy routes traffic to the correct backend based on domain name. Full stack works end-to-end via `docker compose up`.

**Independent Test**: `docker compose up --build` — all three services start. `curl -H "Host: tfg-api.locknet.com.ar" http://localhost/actuator/health` reaches the API. `curl -H "Host: tfg.locknet.com.ar" http://localhost/` returns the UI dashboard HTML.

### Implementation for User Story 5

- [x] T026 [US5] Create `Caddyfile` at repo root:
  ```
  tfg.locknet.com.ar {
      reverse_proxy ui:80
  }

  tfg-api.locknet.com.ar {
      reverse_proxy api:8080
  }
  ```
- [x] T027 [US5] Configure the `caddy` service in `docker-compose.yml`:
  - Image: `caddy:2-alpine`
  - Ports: `80:80`, `443:443`
  - Volumes: `./Caddyfile:/etc/caddy/Caddyfile:ro`, `caddy_data:/data`
  - Depends on: `api`, `ui`
- [x] T028 [US5] Define the `caddy_data` named volume in `docker-compose.yml` for TLS certificate storage
- [x] T029 [US5] Verify full integration: run `docker compose up --build -d`, wait for all services, then:
  - `curl -s -H "Host: tfg-api.locknet.com.ar" http://localhost:80/actuator/health` returns 200
  - `curl -s -H "Host: tfg.locknet.com.ar" http://localhost:80/` returns HTML with Angular app shell
- [x] T030 [US5] Verify `docker compose down` cleans up all containers and the named volume can be removed with `docker compose down -v`

**Checkpoint**: Full stack operational. Both domains route correctly through Caddy. Feature is complete.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, cleanup, and final validation

- [x] T031 [P] Validate all instructions in `specs/023-oci-image-deployment/quickstart.md` work end-to-end from a clean checkout
- [x] T032 [P] Run `docker compose config` to validate `docker-compose.yml` syntax
- [x] T033 Verify no secrets, credentials, or hardcoded internal IPs exist in Dockerfiles, nginx.conf, Caddyfile, or docker-compose.yml
- [x] T034 Confirm all user-facing container log output, nginx error pages, and Caddyfile comments use English only per constitution principle II
- [x] T035 Verify `ui/Dockerfile` production build stage includes i18n assets: confirm `src/i18n/` directory is copied in build stage and included in the output

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup completion (T005-T006 need T003-T004 context)
- **US1 - API Image (Phase 3)**: Depends on Foundational (needs T005 api/.dockerignore)
- **US2 - UI Image (Phase 4)**: Depends on Foundational (needs T006 ui/.dockerignore). Independent of US1.
- **US3 - Environment Config (Phase 5)**: Depends on US1 (T015-T016 modify api/Dockerfile) and US2 (T017-T019 modify ui/Dockerfile, T018 modifies environment.ts)
- **US4 - Health Checks (Phase 6)**: Depends on US1 and US2 (adds HEALTHCHECK to existing Dockerfiles)
- **US5 - Caddy Integration (Phase 7)**: Depends on US1, US2, US3 (needs working api + ui images with env config)
- **Polish (Phase 8)**: Depends on all previous phases

### User Story Dependencies

```
Phase 1 (Setup)
    ↓
Phase 2 (Foundational)
    ↓
    ├── Phase 3: US1 (API Image) ──┐
    └── Phase 4: US2 (UI Image) ───┤
                                    ↓
                              Phase 5: US3 (Env Config)
                                    ↓
                              Phase 6: US4 (Health Checks)
                                    ↓
                              Phase 7: US5 (Caddy + Integration)
                                    ↓
                              Phase 8 (Polish)
```

- **US1 and US2** are parallel — different files, no dependencies between them
- **US3** depends on both US1 and US2 (modifies files created in those phases)
- **US4** depends on US1 and US2 (adds HEALTHCHECK to existing Dockerfiles)
- **US5** depends on US3 (needs docker-compose env blocks filled) and US4 (needs health checks)

### Within Each Phase

- Review tasks before creation tasks
- Creation before verification
- Verification only after build succeeds
- Ask instead of guessing when requirements or implementation details are unclear

### Parallel Opportunities

- **Phase 1**: T002, T003, T004 can run in parallel (different files)
- **Phase 2**: T005 and T006 can run in parallel (different directories)
- **Phase 3 & Phase 4**: US1 and US2 can be implemented in parallel by different team members
- **Phase 8**: T031, T032, T035 can run in parallel (independent verification tasks)

---

## Parallel Example: US1 + US2 Concurrent Implementation

```bash
# Developer A: API Dockerfile
Task: T008 [US1] Create api/Dockerfile with multi-stage Eclipse Temurin build
Task: T009 [US1] Verify API image builds

# Developer B: UI Dockerfile + nginx.conf (parallel with Developer A)
Task: T011 [US2] Create ui/nginx.conf with SPA fallback + health endpoint
Task: T012 [US2] Create ui/Dockerfile with node build + nginx serve
Task: T013 [US2] Verify UI image builds
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T004)
2. Complete Phase 2: Foundational (T005-T007)
3. Complete Phase 3: US1 — API Dockerfile (T008-T010)
4. **STOP and VALIDATE**: `docker build -t tfg-api ./api` succeeds, container starts
5. The API image is already deployable — MVP delivered

### Incremental Delivery

1. Setup + Foundational → shared infrastructure ready
2. Add US1 (API image) → **MVP!** Backend containerized
3. Add US2 (UI image) → frontend containerized, both images independently usable
4. Add US3 (env config) → same images work across environments
5. Add US4 (health checks) → production-ready container lifecycle
6. Add US5 (Caddy integration) → full stack with reverse proxy, one-command deploy
7. Each phase adds value without breaking previous phases

### Single Developer Strategy

Follow phases sequentially (1 → 2 → 3 → 4 → 5 → 6 → 7 → 8). Phases 3 and 4 can still benefit from [P]-marked parallel tasks within each phase.

---

## Notes

- [P] tasks = different files, no dependencies on incomplete [P] tasks
- [Story] label maps task to specific user story for traceability
- Each phase has a checkpoint to validate independently before proceeding
- `docker build` verification is the test mechanism (no separate test framework for Dockerfiles)
- Do not run git commands unless the user explicitly approves them
- The `ui/src/environments/environment.ts` change (T018) is the ONLY application code modification — all other files are net-new
- Stop at any checkpoint to validate the phase independently
- Avoid: vague tasks, same file conflicts, cross-phase dependencies that break phase independence
