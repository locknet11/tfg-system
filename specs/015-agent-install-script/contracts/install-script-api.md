# API Contract: Agent Install Script Endpoints

**Feature**: 015-agent-install-script  
**Date**: 2026-07-08

---

## 1. POST /api/agent/{organizationIdentifier}/{projectIdentifier}/{targetUniqueId}

**Purpose**: Register a new agent and return the self-installing shell script.

**Status**: MODIFIED (contract extended)

### Request

```
POST /api/agent/{organizationIdentifier}/{projectIdentifier}/{targetUniqueId}?preauthCode={preauthCode}
Content-Type: (none — no body required)
```

| Parameter | Location | Required | Description |
|-----------|----------|----------|-------------|
| `organizationIdentifier` | Path | Yes | Organization identifier (e.g., "418J") |
| `projectIdentifier` | Path | Yes | Project identifier (e.g., "H2QC") |
| `targetUniqueId` | Path | Yes | Target unique identifier (e.g., "14SGQ") |
| `preauthCode` | Query | Yes | Preauth code matching the target's stored code |

**Security**: Public endpoint (`.permitAll()`). Authorized by preauth code matching.

### Response — Success (201 Created)

```
HTTP/1.1 201 Created
Content-Type: text/plain

#!/bin/sh
#
# Agent Installation Script
# Generated for target: {targetUniqueId}
#
# [Full install script — downloads, verifies, configures, and launches agent]
```

**Response body**: A POSIX-compatible shell script that:
1. Echoes identification information (API URL, organization, project, target)
2. Downloads the agent binary from `$DOWNLOAD_URL` (curl or wget)
3. Extracts and verifies the Blake3 hash from the embedded manifest
4. Verifies the RSA signature using the embedded public key
5. Writes `/tmp/agent.properties` with agent configuration
6. Launches the agent via `nohup /tmp/agent > /tmp/agent.log 2>&1 &`
7. Checks PID and reports success/failure
8. Prints `INSTALL_OK` on success

### Response — Error (400 Bad Request)

```
HTTP/1.1 400 Bad Request
Content-Type: text/plain

#!/bin/sh

# Error script when setting up the agent
echo "Error: Couldn't install security agent for target: {targetUniqueId}"
echo "Error reason: {errorMessage}"

exit 1
```

### Response — Error (404 Not Found)

If organization, project, or target not found:

```
HTTP/1.1 404 Not Found
Content-Type: application/json
{"error": "agent.error.organizationNotFound"}
```

---

## 2. GET /api/agent/binary/download/{installToken}

**Purpose**: Serve the agent binary for one-time install script download.

**Status**: NEW

### Request

```
GET /api/agent/binary/download/{installToken}
```

| Parameter | Location | Required | Description |
|-----------|----------|----------|-------------|
| `installToken` | Path | Yes | One-time token from Agent.installToken |

**Security**: Public endpoint (`.permitAll()`). Authorized by one-time install token. Token expires after 5 minutes or first use.

### Response — Success (200 OK)

```
HTTP/1.1 200 OK
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="agent"
X-Blake3-Manifest: {"blake3Hash":"abc...","signature":"def...","algorithm":"SHA256withRSA"}

{binary bytes}
{"blake3Hash":"abc123...","signature":"def456...","algorithm":"SHA256withRSA"}
```

**Response body**: The agent binary bytes followed by a newline (`\n`) and the JSON manifest. The manifest contains:
- `blake3Hash`: Expected Blake3 hash of the binary (hex-encoded)
- `signature`: RSA signature of the hash (base64-encoded)
- `algorithm`: Signature algorithm (e.g., "SHA256withRSA")

### Response — Error (404 Not Found)

```
HTTP/1.1 404 Not Found
{"error": "Invalid or expired install token"}
```

### Response — Error (410 Gone)

```
HTTP/1.1 410 Gone
{"error": "Install token already used"}
```

---

## 3. Agent API Key Header Contract (Existing — now enforced)

**Purpose**: All requests from agent to platform (`/api/agent/comm/**`) must include auth headers.

**Status**: EXISTING (contract unchanged, but agent-side implementation was missing — fixed in this feature)

### Required Headers

| Header | Value | Description |
|--------|-------|-------------|
| `X-Agent-Api-Key` | Agent's API key (from `agent.api-key`) | Authenticates the agent |
| `X-Agent-Id` | Agent's ID (from `agent.agent-id`) | Identifies the agent |

**Enforcement**: `AgentApiKeyFilter` reads these headers and authenticates the agent. Without them, requests to `/api/agent/comm/**` receive 401/403.

**Agent-side fix**: `WorkerPoolConfig.restTemplate()` now adds a `ClientHttpRequestInterceptor` that injects both headers from `AgentConfig`.

---

## 4. Install Script Output Contract

**Status**: NEW

The install script communicates results to the caller via:

### Success

```
INSTALL_OK
```
Printed as the last line on successful completion. Exit code 0.

### Failure

Any of:
- `FATAL: Neither curl nor wget available` → exit 1
- `FATAL: Downloaded agent binary is empty` → exit 1
- `FATAL: Could not parse manifest` → exit 1
- `FATAL: Blake3 hash MISMATCH` → exit 1
- `FATAL: RSA signature verification FAILED` → exit 1
- `FATAL: Cannot reach central platform` → exit 1
- `WARNING: Agent may not have started — check /tmp/agent.log` → exit 0 (non-fatal, agent may still start)

All output is plain text to stdout. Script uses `set -e` so any unhandled error exits immediately.
