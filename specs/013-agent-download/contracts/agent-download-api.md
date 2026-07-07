# API Contracts: Agent Download

**Feature**: 013-agent-download
**Base Path**: `/api/agent/download`

---

## GET /api/agent/download/platforms

List available agent binary platforms for download.

**Authentication**: Required (JWT session)

**Response** `200 OK`:
```json
{
  "platforms": [
    {
      "platform": "linux-x86_64",
      "label": "Linux (x86_64)",
      "agentVersion": "0.0.1-SNAPSHOT",
      "fileSizeBytes": 52428800,
      "blake3Hash": "a1b2c3d4e5f6...",
      "lastBuilt": "2026-07-07T10:00:00"
    },
    {
      "platform": "macos-aarch64",
      "label": "macOS (Apple Silicon)",
      "agentVersion": "0.0.1-SNAPSHOT",
      "fileSizeBytes": 48234496,
      "blake3Hash": "f6e5d4c3b2a1...",
      "lastBuilt": "2026-07-07T10:00:00"
    }
  ]
}
```

**Error Responses**:
- `401 Unauthorized` — No valid session
- `403 Forbidden` — User lacks agent-download permission
- `503 Service Unavailable` — No agent binaries available (empty platforms list)

---

## GET /api/agent/download/{platform}

Download the agent binary for a specific platform.

**Authentication**: Required (JWT session)

**Path Parameters**:
- `platform` (string, required) — Platform identifier, e.g., `linux-x86_64`, `macos-aarch64`

**Response** `200 OK`:
- `Content-Type: application/octet-stream`
- `Content-Disposition: attachment; filename="agent"`
- `Content-Length: <file size>`
- `X-Blake3-Manifest: {"blake3Hash":"...","signature":"...","algorithm":"SHA256withRSA"}`
- `X-Agent-Version: 0.0.1-SNAPSHOT`

Body: Raw agent binary bytes followed by `\n` and the JSON manifest (same format as replication endpoint).

**Error Responses**:
- `400 Bad Request` — Invalid or unsupported platform
- `401 Unauthorized` — No valid session
- `403 Forbidden` — User lacks agent-download permission
- `404 Not Found` — Platform not available (no binary built for this platform)
- `503 Service Unavailable` — Binary not loaded (file missing or corrupted)

**Side Effects**:
- Creates an `AgentDownloadRecord` in MongoDB

---

## GET /api/agent/download/{platform}/manifest

Get the signed manifest for a platform's agent binary (without downloading the binary).

**Authentication**: Required (JWT session or agent API key)

**Path Parameters**:
- `platform` (string, required) — Platform identifier

**Response** `200 OK`:
```json
{
  "platform": "linux-x86_64",
  "blake3Hash": "a1b2c3d4e5f6...",
  "signature": "MEUCIQD...",
  "algorithm": "SHA256withRSA",
  "agentVersion": "0.0.1-SNAPSHOT"
}
```

**Error Responses**:
- `400 Bad Request` — Invalid platform
- `401 Unauthorized` — No valid auth
- `404 Not Found` — Platform not available

**Note**: This endpoint is used by the installation script on target machines to fetch the manifest for integrity verification. It accepts both JWT and agent API key authentication (wider access than the binary download itself).

---

## GET /api/agent/download/records

List download audit records (organization-scoped).

**Authentication**: Required (JWT session)

**Query Parameters**:
- `page` (int, default: 0)
- `size` (int, default: 20)
- `platform` (string, optional) — Filter by platform
- `userId` (string, optional) — Filter by user

**Response** `200 OK`:
```json
{
  "content": [
    {
      "id": "rec-001",
      "userEmail": "admin@example.com",
      "platform": "linux-x86_64",
      "agentVersion": "0.0.1-SNAPSHOT",
      "fileSizeBytes": 52428800,
      "blake3Hash": "a1b2c3...",
      "downloadedAt": "2026-07-07T14:30:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

**Error Responses**:
- `401 Unauthorized`
- `403 Forbidden`

## UI Contract

The agents page (`/agents`) shall include a "Download Agent" button in the header area. Clicking the button opens a PrimeNG Dialog with:

- Platform selector dropdown (populated from `GET /api/agent/download/platforms`)
- Platform details display (version, file size, hash)
- Download button that triggers `GET /api/agent/download/{selectedPlatform}`
- Download progress indicator

The download initiates as a browser file download (not an in-app download). The API sets `Content-Disposition: attachment` so the browser handles the save dialog natively.
