# API Contracts: Agent Autoreplication

**Feature**: 004-agent-autoreplication | **Date**: 2026-06-01

All endpoints follow existing conventions: JSON request/response bodies, `@Valid` validation, standard HTTP status codes, `@ControllerAdvice` error handling.

---

## Agent-Facing Endpoints

Auth: `@PreAuthorize("hasRole('AGENT')")` + `X-Agent-Api-Key` + `X-Agent-Id` headers.

### POST /api/agent/comm/replication-request

Agent submits a replication request after identifying an exploitable vulnerability.

**Request Body** (`CreateReplicationRequest`):
```json
{
  "targetIp": "172.31.128.4",
  "targetPort": 22,
  "exploitId": "exploit-ssh-default-key",
  "cveId": "CVE-2025-12345",
  "serviceName": "openssh",
  "serviceVersion": "8.9p1",
  "severity": "HIGH"
}
```

Validation:
- `targetIp`: `@NotBlank`, valid IP format
- `targetPort`: `@NotNull`, `@Min(1)`, `@Max(65535)`
- `exploitId`: `@NotBlank`
- `cveId`: `@NotBlank`
- `serviceName`: `@NotBlank`
- `serviceVersion`: `@NotBlank`
- `severity`: `@NotBlank`, one of CRITICAL, HIGH, MEDIUM, LOW

**Response** (201 Created):
```json
{
  "id": "rep-abc123",
  "status": "APPROVED",
  "replicationToken": "550e8400-e29b-41d4-a716-446655440000",
  "downloadUrl": "https://central.example.com/api/agent/binary/550e8400-e29b-41d4-a716-446655440000",
  "preauthCode": "preauth-xyz789",
  "centralUrl": "https://central.example.com"
}
```

**Status values**:
- `APPROVED` — policy auto-approved; token and URLs included
- `PENDING` — manual approval required; agent should poll
- `DENIED` — policy denied or duplicate rejected
- `DUPLICATE` — same target+exploit already has an active request

**Error responses**:
- 400: Validation failure (standard `GenericErrorResponse`)
- 409: Duplicate request for same targetIp + exploitId with PENDING/APPROVED status

---

### GET /api/agent/comm/replication-request/{id}/status

Agent polls for the status of a PENDING replication request.

**Path parameter**: `id` — replication request ID

**Response** (200 OK):
```json
{
  "status": "APPROVED",
  "replicationToken": "550e8400-e29b-41d4-a716-446655440000",
  "downloadUrl": "https://central.example.com/api/agent/binary/550e8400-e29b-41d4-a716-446655440000",
  "preauthCode": "preauth-xyz789",
  "centralUrl": "https://central.example.com"
}
```

When status is `PENDING`, `replicationToken`, `downloadUrl`, `preauthCode`, and `centralUrl` are null.

**Error responses**:
- 404: Request not found or not owned by this agent

---

### GET /api/agent/binary/{replicationToken}

Download the agent binary and its signed Blake3 hash manifest.

Auth: **None** (permitAll). The `replicationToken` itself is the authentication.

**Path parameter**: `replicationToken` — UUID token from an APPROVED replication request

**Response** (200 OK, `multipart/mixed`):

Part 1 — `application/octet-stream` (the binary):
```
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="agent"

<binary bytes>
```

Part 2 — `application/json` (the signed manifest):
```json
{
  "blake3Hash": "a1b2c3d4e5f6...hex...",
  "signature": "base64-encoded-signature...",
  "algorithm": "Ed25519"
}
```

**Error responses**:
- 404: Token not found
- 410 Gone: Token expired (past TTL)
- 403 Forbidden: Token already consumed (single-use)

---

## Admin-Facing Endpoints

Auth: `@PreAuthorize("isAuthenticated()")` + JWT + `X-Organization-Id` + `X-Project-Id` headers.

### GET /api/replication-requests

List replication requests with pagination and optional filters.

**Query parameters**:
- `page` (int, default 0)
- `size` (int, default 20)
- `status` (optional, filter by ReplicationRequestStatus)
- `severity` (optional, filter by severity string)

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "rep-abc123",
      "parentAgentId": "agent-001",
      "parentAgentName": "Agent-target-001",
      "targetIp": "172.31.128.4",
      "targetPort": 22,
      "exploitId": "exploit-ssh-default-key",
      "cveId": "CVE-2025-12345",
      "serviceName": "openssh",
      "serviceVersion": "8.9p1",
      "severity": "HIGH",
      "status": "PENDING",
      "policy": "MANUAL_APPROVE",
      "approvedBy": null,
      "createdAt": "2026-06-01T10:30:00",
      "expiresAt": "2026-06-01T11:30:00",
      "resolvedAt": null
    }
  ],
  "totalPages": 1,
  "totalElements": 1,
  "page": 0,
  "size": 20
}
```

---

### PUT /api/replication-requests/{id}/approve

Administrator approves a PENDING replication request.

**Path parameter**: `id` — replication request ID

**Response** (200 OK):
```json
{
  "id": "rep-abc123",
  "status": "APPROVED",
  "replicationToken": "550e8400-e29b-41d4-a716-446655440000",
  "downloadUrl": "https://central.example.com/api/agent/binary/550e8400-e29b-41d4-a716-446655440000",
  "approvedBy": "user-admin-001",
  "resolvedAt": "2026-06-01T10:35:00"
}
```

**Error responses**:
- 404: Request not found
- 409: Request is not in PENDING status

---

### PUT /api/replication-requests/{id}/deny

Administrator denies a PENDING replication request.

**Path parameter**: `id` — replication request ID

**Response** (200 OK):
```json
{
  "id": "rep-abc123",
  "status": "DENIED",
  "approvedBy": "user-admin-001",
  "resolvedAt": "2026-06-01T10:35:00"
}
```

**Error responses**:
- 404: Request not found
- 409: Request is not in PENDING status

---

## Project Settings Extension

### PUT /api/projects/{id}/replication-policy

Update the replication policy for a project.

**Request Body** (`UpdateReplicationPolicyRequest`):
```json
{
  "mode": "AUTO_APPROVE",
  "minSeverity": "HIGH",
  "notifyAdmin": true
}
```

Validation:
- `mode`: `@NotNull`
- `minSeverity`: nullable, one of CRITICAL, HIGH, MEDIUM, LOW (null means all severities)
- `notifyAdmin`: `@NotNull`

**Response** (200 OK):
```json
{
  "id": "proj-001",
  "name": "Production",
  "projectIdentifier": "production",
  "description": "Production environment",
  "organizationId": "org-001",
  "status": "ACTIVE",
  "replicationPolicy": {
    "mode": "AUTO_APPROVE",
    "minSeverity": "HIGH",
    "notifyAdmin": true
  },
  "createdAt": "2026-01-01T00:00:00",
  "updatedAt": "2026-06-01T10:00:00"
}
```

---

## Security Configuration Changes

### WebSecurity.java

Add permitAll rule for binary download endpoint:
```java
.requestMatchers(HttpMethod.GET, "/api/agent/binary/**").permitAll()
```

This endpoint uses the replication token as its own authentication mechanism.

---

## Error Codes (extended ErrorCode enum)

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `REPLICATION_REQUEST_NOT_FOUND` | 404 | Replication request ID not found |
| `REPLICATION_REQUEST_NOT_PENDING` | 409 | Request is not in PENDING status |
| `REPLICATION_DUPLICATE_REQUEST` | 409 | Same target+exploit already has active request |
| `REPLICATION_TOKEN_NOT_FOUND` | 404 | Binary download token not found |
| `REPLICATION_TOKEN_EXPIRED` | 410 | Binary download token has expired |
| `REPLICATION_TOKEN_CONSUMED` | 403 | Binary download token already used |
| `REPLICATION_POLICY_NOT_FOUND` | 404 | Project has no replication policy configured |
| `BINARY_INTEGRITY_CHECK_FAILED` | 500 | Binary hash/signature computation failed |
