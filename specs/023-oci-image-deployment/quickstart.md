# Quickstart: OCI Image Deployment

**Feature**: 023-oci-image-deployment | **Date**: 2026-07-14

## Prerequisites

- Docker 24+ (or any OCI-compliant runtime with Compose support)
- Git (to clone the repository)
- No local Java, Node.js, or Maven installation needed — everything runs inside containers

## Quick Start (Local Development)

### 1. Clone and enter the repository

```bash
git clone <repo-url> tfg-system
cd tfg-system
```

### 2. Start all services

```bash
docker compose up --build
```

This builds both images and starts three containers: `api`, `ui`, and `caddy`.

### 3. Verify

- **API health**: `curl -H "Host: tfg-api.locknet.com.ar" http://localhost/actuator/health`
- **UI**: Open `http://localhost` in a browser (Caddy routes to the UI by default for unknown hosts, or set `Host: tfg.locknet.com.ar` header)

### 4. Stop

```bash
docker compose down
```

## Building Individual Images

### API only

```bash
docker build -t tfg-api:latest ./api
```

### UI only

```bash
docker build -t tfg-ui:latest ./ui
```

## Configuration

### API Environment

Edit the `api` service's `environment:` block in `docker-compose.yml` to set:
- `MONGODB_URI` — your MongoDB connection string (required)
- `JWT_SECRET` — a secure random string for JWT signing (required for auth)
- Other optional variables as needed (see [data-model.md](./data-model.md))

### UI Backend URL

Set `API_BASE_URL` in the `ui` service's `environment:` block in `docker-compose.yml`.

## Production Deployment Notes

### Domain Configuration

For production, point DNS A/AAAA records to the Caddy host:
- `tfg.locknet.com.ar` → Caddy server IP
- `tfg-api.locknet.com.ar` → Caddy server IP

Caddy automatically obtains TLS certificates from Let's Encrypt when it detects valid public DNS.

### Without Public DNS (Local / Staging)

For environments without public DNS, either:
- Add entries to `/etc/hosts` on the host machine mapping the domains to `127.0.0.1`
- Or use Caddy's `tls internal` directive for self-signed certificates

### Image Registry Push

```bash
# Tag for your registry
docker tag tfg-api:latest registry.example.com/tfg-api:0.0.1
docker tag tfg-ui:latest registry.example.com/tfg-ui:0.0.1

# Push
docker push registry.example.com/tfg-api:0.0.1
docker push registry.example.com/tfg-ui:0.0.1
```

## Troubleshooting

| Symptom | Check |
|---|---|
| API fails to start | Verify `MONGODB_URI` points to a reachable MongoDB instance |
| UI shows blank page | Check browser console for CORS errors; verify `ALLOWED_ORIGINS` includes the UI domain |
| Caddy returns 502 | Ensure `api` and `ui` services are healthy: `docker compose ps` |
| API calls fail from UI | Verify `API_BASE_URL` in the UI container matches the API's public URL |
