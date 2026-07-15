# Research: OCI Image Deployment

**Feature**: 023-oci-image-deployment | **Date**: 2026-07-14

## 1. API Dockerfile — Eclipse Temurin Multi-Stage

### Decision
Use a two-stage Dockerfile:
- **Stage 1 (build)**: `eclipse-temurin:17-jdk-alpine` — compiles the Spring Boot fat JAR with Maven wrapper
- **Stage 2 (runtime)**: `eclipse-temurin:17-jre-alpine` — runs only the JRE, copies the built JAR

### Rationale
- Alpine variants minimize image size (critical for the <500 MB overhead goal)
- Multi-stage keeps build toolchain (JDK + Maven cache) out of the final image
- Java 17 matches the project's `pom.xml` `<java.version>17</java.version>`
- Eclipse Temurin is the recommended OpenJDK distribution for containers; no licensing concerns

### Alternatives Considered
- **Single-stage with JDK**: Simpler but produces a ~600 MB image with unnecessary build tools
- **Distroless Java**: Even smaller but harder to debug (no shell); Alpine provides `sh` for health checks and entrypoint scripts
- **Amazon Corretto**: Valid alternative but Temurin is more widely used in OCI ecosystems

### Key Implementation Details
- Copy `pom.xml` and `mvnw` first, then run `./mvnw dependency:go-offline` to cache dependencies in a Docker layer
- Copy `src/` after dependency caching so code changes don't invalidate the Maven cache layer
- Build with `./mvnw clean package -DskipTests` (tests may require MongoDB connectivity)
- Final JAR: `api/target/app-0.0.1-SNAPSHOT.jar`
- Run as non-root user `appuser` (uid 1001)
- Expose port 8080
- ENTRYPOINT: `java -jar app.jar`

## 2. UI Dockerfile — Angular + nginx

### Decision
Use a two-stage Dockerfile:
- **Stage 1 (build)**: `node:20-alpine` — runs `npm ci` and `npm run build -- --configuration production`
- **Stage 2 (runtime)**: `nginx:stable-alpine` — serves compiled static assets from `/usr/share/nginx/html`

### Rationale
- Node Alpine is the standard for frontend container builds
- nginx stable-alpine is the lightest production-grade static file server (~10 MB base)
- `--configuration production` enables Angular optimizations (minification, tree-shaking, AOT)
- Multi-stage keeps Node.js and all `node_modules` out of the final image

### Alternatives Considered
- **Single-stage Node + serve**: Simpler but the serve package is not production-grade; nginx is battle-tested
- **caddy for static files**: Caddy could serve both reverse-proxy and static files but violates separation of concerns; dedicated nginx for UI is more standard
- **httpd (Apache)**: Bulkier than nginx-alpine

### Key Implementation Details
- Build output goes to `ui/dist/admin-site` (per `angular.json` `outputPath`)
- Custom `nginx.conf` mounts at `/etc/nginx/conf.d/default.conf`
- nginx runs as non-root `nginx` user (built into the base image)
- Health check: `location /health { return 200 'healthy'; add_header Content-Type text/plain; }`
- SPA fallback: `try_files $uri $uri/ /index.html` so Angular routing works on page reload

### Runtime Configuration Challenge
Angular compiles `environment.ts` (including `baseUrl`) into the JS bundle at build time. To change the API URL at runtime:
- **Decision**: Use a startup script (`docker-entrypoint.sh`) that replaces a placeholder (`__API_BASE_URL__`) in the compiled JS files with the `API_BASE_URL` environment variable
- The production `environment.ts` will use the placeholder: `baseUrl: '__API_BASE_URL__'`
- The entrypoint script runs `sed` on the main JS bundle before starting nginx
- This keeps the image identical across environments (SC-004)

### Environment Variables (UI Container)
| Variable | Purpose | Default |
|---|---|---|
| `API_BASE_URL` | Backend API URL | `http://localhost:8080` |

## 3. Caddy Reverse Proxy

### Decision
Use Caddy 2 with a `Caddyfile` for domain-based reverse proxying:
- `tfg.locknet.com.ar` → UI service (port 80)
- `tfg-api.locknet.com.ar` → API service (port 8080)

### Rationale
- Caddy provides automatic TLS via Let's Encrypt (production-ready HTTPS)
- Simple declarative config via Caddyfile
- Single binary, minimal overhead
- User explicitly requested Caddy as the reverse proxy

### Alternatives Considered
- **Traefik**: More features but heavier; Caddy is simpler for a 2-service setup
- **nginx as reverse proxy**: Would need separate cert management (Certbot); Caddy handles TLS natively
- **HAProxy**: Overkill for this scale

### Key Implementation Details
- Caddy listens on ports 80 and 443
- Routes by `Host` header to the correct backend
- API service is NOT exposed to host — only reachable through Caddy
- UI service is NOT exposed to host — only reachable through Caddy
- For local development (no public DNS), Caddy can issue internal/self-signed certs

## 4. Docker Compose Orchestration

### Decision
Single `docker-compose.yml` at repo root with three services:
1. `api` — Spring Boot backend (no ports exposed)
2. `ui` — nginx serving Angular (no ports exposed)
3. `caddy` — reverse proxy (exposes 80, 443)

### Rationale
- Internal Docker network connects all three
- API and UI are isolated (only Caddy faces the internet)
- All API environment variables declared in the `environment:` block (no `.env` file secrets committed)
- Placeholder values for secrets; real values injected at deploy time

### Network Architecture
```
Internet → Caddy (:80/:443) → UI (internal:80)
                            → API (internal:8080)
```

### API Environment Variables (from application.properties)
All Spring Boot properties are overridable via environment variables (Spring relaxed binding). The `docker-compose.yml` declares all known variables with documented placeholder/defaults:

| Variable | Purpose |
|---|---|
| `MONGODB_URI` | MongoDB connection string |
| `MONGODB_DATABASE_NAME` | MongoDB database name |
| `MONGODB_AUTO_INDEX_CREATION` | Auto-create indexes |
| `JWT_SECRET` | JWT signing secret |
| `LOG_RESOLVED_EXCEPTION` | Log resolved exceptions |
| `ALLOWED_ORIGINS` | CORS allowed origins |
| `APPLICATION_DOMAIN` | Frontend application domain |
| `API_BASE_URL` | Public API URL (for agent scripts) |
| `RESEND_API_KEY` | Resend email API key |
| `RESEND_FROM_ADDRESS` | Resend sender address |
| `NVD_API_KEY` | NIST NVD API key |
| `NVD_API_BASE_URL` | NVD API base URL |
| `HEARTBEAT_TIMEOUT_SECONDS` | Agent heartbeat timeout |
| `SERVER_PORT` | API listen port (default 8080) |
