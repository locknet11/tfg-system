# Phase 1 Data Model: Unix Agent Self-Destruction & Self-Cleanup

Entities introduced or extended by this feature. Agent-side types are plain Java objects (no persistence on the host); api-side types persist in MongoDB.

## TeardownTrigger (agent enum)

The cause that initiated self-destruction.

| Value | Meaning |
|-------|---------|
| `PLAN_COMPLETION` | All steps of the assigned plan reached a terminal completed state. |
| `PLATFORM_DEPROVISION` | Heartbeat response carried an authenticated `deprovision=true`. |
| `AUTH_REVOKED` | 3 consecutive authenticated-rejection heartbeats (registration revoked). |
| `SELF_DESTRUCT_STEP` | A `SELF_DESTRUCT` plan step was executed explicitly. |

## ArtifactType (agent enum)

The category of on-host artifact teardown removes. Drives the `ArtifactSet` and the outcome record.

| Value | Concrete default location (from feature 015 install layout / config) |
|-------|----------------------------------------------------------------------|
| `AGENT_BINARY` | `/tmp/agent` (running executable) — removed by the detached script |
| `AGENT_CONFIG` | `/tmp/agent.properties` (contains preauth code + api key) |
| `AGENT_LOG` | `/tmp/agent.log` and any rotated logs |
| `DOWNLOADED_TOOLS` | `BundledToolProvisioner.getExtractionDirectory()` (extracted tool binaries) |
| `WORKING_DIR` | any agent-created temp working directory |
| `INSTALL_SCRIPT` | staged install-script file if one exists on disk (else `NOT_PRESENT`) |
| `OS_REGISTRATION` | systemd unit / launchd plist / cron entry created for the agent (else `NOT_PRESENT`) |
| `RAW_DOWNLOAD` | `/tmp/agent_raw` leftover if the install was interrupted |

## ArtifactSet (agent)

Resolves the concrete host paths for each `ArtifactType` at teardown time.

- **Fields**: `Map<ArtifactType, List<Path>>` resolvedPaths; sourced from `AgentConfig`, the known install layout, and `BundledToolProvisioner.getExtractionDirectory()`.
- **Rules**:
  - Only enumerate paths the agent/installer created — never glob unrelated host directories (FR-016).
  - Missing paths are allowed and resolve to `NOT_PRESENT` at removal (FR-012).
  - `AGENT_BINARY` is resolved from the JVM's own executable path (`ProcessHandle.current().info().command()`), falling back to the configured `/tmp/agent`.

## ArtifactRemovalResult (agent)

Outcome of attempting to remove one artifact.

| Field | Type | Notes |
|-------|------|-------|
| `type` | `ArtifactType` | which artifact |
| `path` | `String` | absolute path attempted (nullable for in-process env) |
| `status` | `RemovalStatus` | `REMOVED` \| `FAILED` \| `NOT_PRESENT` |
| `detail` | `String` | error message when `FAILED`; empty otherwise |

`RemovalStatus` enum: `REMOVED`, `FAILED`, `NOT_PRESENT`.

## TeardownOutcome (agent) → reported to central

Aggregate reported to `POST /api/agent/comm/teardown` before the process exits.

| Field | Type | Notes |
|-------|------|-------|
| `agentId` | `String` | from `AgentConfig.getAgentId()` |
| `trigger` | `TeardownTrigger` | cause |
| `timestamp` | ISO-8601 `String` | when teardown ran |
| `results` | `List<ArtifactRemovalResult>` | per-artifact outcomes (excludes the detached binary unlink, which is reported as `PENDING_DETACHED`) |
| `binaryRemoval` | `String` | `PENDING_DETACHED` (handled by the detached script after exit) |

**State transition (agent lifecycle)**:
`active → tearingDown (single-shot guard set) → reported → exiting → gone`
Once `tearingDown` is set, the worker loop accepts no new steps and starts no new jobs.

## AgentTeardownRecord (api, MongoDB)

Persisted audit record of a teardown event.

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` (ObjectId) | primary key |
| `agentId` | `String` | which agent |
| `organizationId` / `projectId` / `targetId` | `String` | denormalized for filtering/audit (from the agent record) |
| `trigger` | `String` | `PLAN_COMPLETION` \| `PLATFORM_DEPROVISION` \| `AUTH_REVOKED` \| `SELF_DESTRUCT_STEP` |
| `reportedAt` | `Instant` | server receipt time |
| `agentTimestamp` | `String` | agent-reported timestamp |
| `results` | `List<ArtifactResult>` (embedded) | `{ type, path, status, detail }` |
| `binaryRemoval` | `String` | `PENDING_DETACHED` |

**Validation** (`jakarta.validation` on the inbound DTO): `agentId` not blank; `trigger` a known enum; `results` non-null (may be empty); each result's `type`/`status` non-blank.

## Agent (api, existing — extended)

Extend the existing `Agent` MongoDB document minimally:

| New/changed field | Type | Notes |
|-------------------|------|-------|
| `deprovisioned` | `boolean` | set true by `deleteAgent` (soft-mark) before hard reap |
| `deprovisionReason` | `String` (nullable) | optional human-readable reason surfaced in the heartbeat signal |
| `status` | `AgentStatus` (existing) | may transition to `KILLED` on de-provision |

`AgentStatus` (existing enum: `IN_CREATION, CREATED, ACTIVE, UNRESPONSIVE, KILLED`) is reused; `KILLED` denotes a de-provisioned/torn-down agent. No new status value is strictly required.

## DTO changes

- **`HeartbeatResponse`** (agent DTO `com.spulido.agent.worker.http.dto` **and** api DTO `com.spulido.tfg.domain.agent.model.dto`): add `boolean deprovision` and `String deprovisionReason`. Existing fields (`agentId`, `status`, `lastConnection`, `hasPlan`) unchanged.
- **`TeardownReportRequest`** (new, both sides): mirrors `TeardownOutcome` fields for the `POST /api/agent/comm/teardown` body.
