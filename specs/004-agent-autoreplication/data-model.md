# Data Model: Agent Autoreplication

**Feature**: 004-agent-autoreplication | **Date**: 2026-06-01

## New Collections

### replication_requests

MongoDB collection storing replication request documents.

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `_id` | String | `@Id`, auto-generated | Document identifier |
| `parentAgentId` | String | `@NotBlank`, indexed | ID of the agent requesting replication |
| `targetIp` | String | `@NotBlank` | IP address of the target host |
| `targetPort` | Integer | `@NotNull`, 1-65535 | Port of the vulnerable service |
| `exploitId` | String | `@NotBlank` | Identifier of the exploit to use |
| `cveId` | String | `@NotBlank` | CVE identifier for the vulnerability |
| `serviceName` | String | `@NotBlank` | Name of the vulnerable service |
| `serviceVersion` | String | `@NotBlank` | Version of the vulnerable service |
| `severity` | String | `@NotBlank` | CVSS severity: CRITICAL, HIGH, MEDIUM, LOW |
| `status` | ReplicationRequestStatus | `@NotNull`, indexed | PENDING, APPROVED, DENIED, EXPIRED |
| `replicationToken` | String | nullable, indexed (unique) | UUID v4 for binary download (set on approval) |
| `downloadUrl` | String | nullable | Full URL for binary download (set on approval) |
| `policy` | ReplicationApprovalMode | `@NotNull` | Policy mode that was evaluated |
| `approvedBy` | String | nullable | User ID if manually approved |
| `preauthCode` | String | nullable | Pre-authorization code for new agent registration (set on approval) |
| `organizationId` | String | `@NotBlank`, indexed | Organization scope |
| `projectId` | String | `@NotBlank`, indexed | Project scope |
| `createdAt` | LocalDateTime | auto | Request creation timestamp |
| `updatedAt` | LocalDateTime | auto | Last update timestamp |
| `expiresAt` | LocalDateTime | nullable | TTL for PENDING requests (default: 1 hour) |
| `resolvedAt` | LocalDateTime | nullable | Timestamp when request was approved/denied/expired |

**Indexes**:
- Compound: `{ organizationId, projectId }` (scoped queries)
- Single: `{ status }` (filter by status)
- Single: `{ replicationToken }` (unique, for binary download lookup)
- Compound: `{ targetIp, exploitId, status }` (duplicate detection)

**Implements**: `ScopedEntity` (auto-populated org/project via `ProjectScopeMongoEventListener`)

## Modified Collections

### agents (extended fields)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `replicatedFrom` | String | nullable | ID of the parent agent that replicated this agent |
| `replicatedAt` | LocalDateTime | nullable | Timestamp when this agent was created via replication |
| `replicationExploit` | ReplicationExploitInfo | nullable | Embedded: `{ cveId, exploitId }` of the exploit used |

**ReplicationExploitInfo** (embedded value object):
| Field | Type | Description |
|-------|------|-------------|
| `cveId` | String | CVE identifier |
| `exploitId` | String | Exploit identifier |

### projects (extended fields)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `replicationPolicy` | ReplicationPolicy | nullable (defaults to MANUAL_APPROVE) | Embedded replication policy |

**ReplicationPolicy** (embedded value object):
| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `mode` | ReplicationApprovalMode | MANUAL_APPROVE | AUTO_APPROVE or MANUAL_APPROVE |
| `minSeverity` | String | null | Minimum severity for auto-approval (CRITICAL, HIGH, MEDIUM, LOW). Null means all severities auto-approved. |
| `notifyAdmin` | Boolean | true | Whether to notify admin on replication events |

## New Enums

### ReplicationRequestStatus

```java
public enum ReplicationRequestStatus {
    PENDING,
    APPROVED,
    DENIED,
    EXPIRED
}
```

### ReplicationApprovalMode

```java
public enum ReplicationApprovalMode {
    AUTO_APPROVE,
    MANUAL_APPROVE
}
```

### StepAction (extended)

New values added to existing enum:

```java
REQUEST_REPLICATION,
EXECUTE_EXPLOIT,
TRANSFER_AGENT,
REPLICATE
```

### WhenCondition (extended)

New values added to existing enum:

```java
ON_REPLICATION_REQUESTED,
ON_AGENT_REPLICATED
```

## New Value Objects

### BinaryManifest

Not persisted — computed at runtime when serving the binary.

| Field | Type | Description |
|-------|------|-------------|
| `blake3Hash` | String | Hex-encoded Blake3 hash of the binary |
| `signature` | String | Base64-encoded PKI signature of the hash |
| `algorithm` | String | Signature algorithm (e.g., "Ed25519" or "SHA256withRSA") |

## Entity Relationships

```
Organization 1──* Project 1──* ReplicationRequest
Project 1──1 ReplicationPolicy (embedded)
Agent 1──* ReplicationRequest (parentAgentId)
ReplicationRequest 1──1 Agent (created via replication, linked by replicatedFrom)
ReplicationRequest *──1 Target (targetIp matches target.ipOrDomain)
AlertConfiguration *──* WhenCondition (includes replication events)
```

## State Transitions

### ReplicationRequest Status

```
                    ┌──────────┐
                    │  PENDING  │
                    └────┬─────┘
                         │
            ┌────────────┼────────────┐
            │            │            │
            ▼            ▼            ▼
     ┌──────────┐  ┌──────────┐  ┌──────────┐
     │ APPROVED  │  │  DENIED  │  │ EXPIRED  │
     └──────────┘  └──────────┘  └──────────┘
```

- **PENDING → APPROVED**: Admin approves or policy auto-approves. Token and downloadUrl are set.
- **PENDING → DENIED**: Admin denies. Decision is logged.
- **PENDING → EXPIRED**: TTL exceeded without approval. System marks as expired.
- Terminal states (APPROVED, DENIED, EXPIRED) are immutable.

### Severity Ordering (for policy threshold)

```
CRITICAL > HIGH > MEDIUM > LOW
```

When `minSeverity` is set to "HIGH", only HIGH and CRITICAL requests are auto-approved.

## Agent-Side Models (agents/unix/)

### New DTOs

**ReplicationRequestBody** (sent to Central):
| Field | Type | Description |
|-------|------|-------------|
| `targetIp` | String | Target host IP |
| `targetPort` | int | Target service port |
| `exploitId` | String | Exploit identifier |
| `cveId` | String | CVE identifier |
| `serviceName` | String | Vulnerable service name |
| `serviceVersion` | String | Vulnerable service version |
| `severity` | String | CVSS severity level |

**ReplicationRequestResponse** (received from Central):
| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Request ID |
| `status` | String | APPROVED, PENDING, DENIED, DUPLICATE |
| `replicationToken` | String | Token for binary download (if APPROVED) |
| `downloadUrl` | String | Binary download URL (if APPROVED) |
| `preauthCode` | String | Pre-auth code for new agent (if APPROVED) |
| `centralUrl` | String | Central URL for new agent config (if APPROVED) |

**ReplicationStatusResponse** (polling response):
| Field | Type | Description |
|-------|------|-------------|
| `status` | String | Current status |
| `replicationToken` | String | Token (if APPROVED) |
| `downloadUrl` | String | URL (if APPROVED) |
| `preauthCode` | String | Pre-auth code (if APPROVED) |
| `centralUrl` | String | Central URL (if APPROVED) |

### StepResult Extensions

The existing `StepResult` carries `services`, `scripts`, and `logs`. For replication steps, the context map will additionally carry:

- **REQUEST_REPLICATION** result: `replicationToken`, `downloadUrl`, `preauthCode`, `centralUrl` (stored in logs with prefix markers, same pattern as `targetId:` in SERVICE_SCAN)
- **EXECUTE_EXPLOIT** result: `reverseShellSessionId`, `targetIp` (stored in logs)
- **TRANSFER_AGENT** result: `newAgentId`, `installationStatus` (stored in logs)
