# Environment Variable Contracts

**Feature**: 023-oci-image-deployment | **Date**: 2026-07-14

## API Container

The API image accepts all configuration through environment variables. Variables follow Spring Boot relaxed binding: uppercase underscore-separated names automatically map to dotted property paths.

### Contract

- **Image**: `tfg-api:latest`
- **Port**: `8080` (configurable via `SERVER_PORT`)
- **Health**: `GET /actuator/health` → `200 OK` with `{"status":"UP"}` when healthy

### Required Variables

| Variable | Validation | Failure Behavior |
|---|---|---|
| `MONGODB_URI` | Must be a valid MongoDB connection string starting with `mongodb://` or `mongodb+srv://` | Service fails to start; error logged to stderr |
| `JWT_SECRET` | Non-empty string; minimum 32 characters recommended | Service starts but authentication fails for all requests |

### Optional Variables

See [data-model.md](../data-model.md) for the full variable table. All optional variables have sensible defaults as documented in `api/src/main/resources/application.properties`.

### Startup Validation

The container entrypoint does not perform pre-flight validation. Spring Boot validates the MongoDB connection on first repository access. Missing `MONGODB_URI` results in a connection refused error at startup, which container orchestration detects via health check failure.

## UI Container

### Contract

- **Image**: `tfg-ui:latest`
- **Port**: `80`
- **Health**: `GET /health` → `200 OK` with body `healthy`
- **SPA Routing**: All paths that don't match a static file fall back to `/index.html`

### Required Variables

None. The container starts serving the dashboard regardless of configuration. If `API_BASE_URL` is omitted, the compiled placeholder (`__API_BASE_URL__`) remains in the JS bundle and API calls will fail with invalid URLs.

### Runtime Configuration Mechanism

1. At image build time, `environment.ts` is compiled with `baseUrl: '__API_BASE_URL__'`
2. At container startup, `docker-entrypoint.sh` reads the `API_BASE_URL` environment variable
3. The script runs `find /usr/share/nginx/html -type f -name '*.js' -exec sed -i "s|__API_BASE_URL__|${API_BASE_URL}|g" {} +`
4. nginx starts and serves the updated files

This ensures **SC-004**: the same image works across environments by changing only the `API_BASE_URL` variable.

### Variable

| Variable | Default | Validation |
|---|---|---|
| `API_BASE_URL` | `https://tfg-api.locknet.com.ar` | Should be a valid absolute URL; no validation at container level — invalid URLs cause client-side request failures |
