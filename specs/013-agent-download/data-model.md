# Data Model: Agent Download Portal

**Feature**: 013-agent-download
**Date**: 2026-07-07

## Entities

### AgentDownloadRecord (NEW)

Represents a single agent binary download event by an authenticated administrator.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | String (UUID) | Yes | Auto-generated unique identifier |
| `userId` | String | Yes | ID of the user who performed the download |
| `userEmail` | String | Yes | Email of the downloading user (denormalized for audit readability) |
| `organizationId` | String | Yes | Organization scope (indexed) |
| `projectId` | String | No | Project scope (indexed, nullable — downloads may happen at org level) |
| `platform` | String | Yes | Target platform identifier (e.g., `linux-x86_64`, `macos-aarch64`) |
| `agentVersion` | String | No | Agent version string (e.g., `0.0.1-SNAPSHOT`) — nullable for future version tracking |
| `fileSizeBytes` | Long | Yes | Size of the downloaded binary in bytes |
| `blake3Hash` | String | Yes | Blake3 hash of the served binary (for audit trail) |
| `clientIp` | String | Yes | IP address of the downloading client |
| `userAgent` | String | No | User-Agent header from the download request |
| `downloadedAt` | LocalDateTime | Yes | Timestamp of the download |

**MongoDB Collection**: `agent_download_records`
**Indexes**:
- `{ organizationId: 1, downloadedAt: -1 }` — for org-scoped listing
- `{ userId: 1, downloadedAt: -1 }` — for user-specific audit
- `{ platform: 1 }` — for platform popularity metrics

### AgentPlatformInfo (DTO, not persisted)

Represents an available agent binary platform for the UI.

| Field | Type | Description |
|-------|------|-------------|
| `platform` | String | Platform identifier |
| `label` | String | Human-readable platform name |
| `agentVersion` | String | Agent version available for this platform |
| `fileSizeBytes` | Long | Size of the binary |
| `blake3Hash` | String | Blake3 hash for integrity verification |
| `lastBuilt` | LocalDateTime | When the binary was built |

### AgentDownloadInfo (DTO, not persisted)

Response DTO for the download endpoint.

| Field | Type | Description |
|-------|------|-------------|
| `platform` | String | Platform identifier |
| `agentVersion` | String | Agent version |
| `fileSizeBytes` | Long | Binary size |
| `blake3Hash` | String | Hash for verification |
| `downloadUrl` | String | URL path that was used |

### Modifications to Existing Entities

**AgentBinaryServiceImpl** (modified, not a new entity):
- `binaryBytes` changes from `byte[]` to `Map<String, byte[]>` keyed by platform
- `blake3Hash` changes to `Map<String, String>` keyed by platform
- `signedManifest` changes to `Map<String, String>` keyed by platform
- New method `getAvailablePlatforms(): List<AgentPlatformInfo>`
- New method `getBinaryForPlatform(String platform): byte[]`
- New config property `agent.binary.resource-path` (default: `agents`)

**AgentBinaryService** (interface — new methods):
- `List<AgentPlatformInfo> getAvailablePlatforms()`
- `byte[] getBinaryForPlatform(String platform)`
- `String getSignedManifestForPlatform(String platform)`

## State Transitions

No complex state machine. Download records are write-once (immutable audit log). No update or delete operations on download records.

Agent binaries are loaded at startup (`@PostConstruct`) and remain static until restart. Binary replacement requires a server restart.

## Relationships

```text
AgentDownloadRecord
  └─ belongs to: User (by userId) — not enforced as FK, just reference
  └─ scoped to: Organization (by organizationId) — enforced via ProjectScopeMongoEventListener
  └─ references: Agent Binary (by blake3Hash) — not enforced, informational only
```

## Validation Rules

- `platform` must match pattern `^[a-z]+-[a-z0-9_]+$` (lowercase letters, hyphen, architecture)
- `agentVersion` if present must follow semantic versioning `^\d+\.\d+\.\d+(-.*)?$`
- `fileSizeBytes` must be positive
- `blake3Hash` must be 64 hex characters
- `userId` and `organizationId` must be non-blank
