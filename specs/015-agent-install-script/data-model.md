# Data Model: Agent Self-Installing Shell Script

**Feature**: 015-agent-install-script  
**Date**: 2026-07-08

## Entity Changes

### Agent (existing — fields added)

The `Agent` entity (MongoDB document in `api/`) needs one new field for the install download token.

| Field | Type | Change | Description |
|-------|------|--------|-------------|
| `installToken` | String | **NEW** | One-time token for binary download via install script. Null after use or expiry. |
| `installTokenExpiresAt` | Instant | **NEW** | Expiry timestamp for the install token. Null when token not active. |
| `apiKey` | String | Existing | Now included in install script (previously generated but not sent to agent). |
| `organizationIdentifier` | String | Existing (on script model, not entity) | Already available in template context via ScriptService. |
| `projectIdentifier` | String | Existing (on script model, not entity) | Already available in template context. |
| `targetUniqueId` | String | Existing (on script model, not entity) | Already available in template context. |

### Template Variables (Script Service Model)

New variables added to `ScriptServiceImpl.generateInstallScript()` for the FreeMarker template:

| Variable | Type | Source | Description |
|----------|------|--------|-------------|
| `apiKey` | String | **NEW** | Agent's API key for authentication |
| `agentId` | String | **NEW** | Agent's ID for identification |
| `downloadUrl` | String | **NEW** | Full URL to download agent binary (includes install token) |
| `centralPublicKey` | String | **NEW** | RSA public key for signature verification |

Existing variables retained:

| Variable | Type | Description |
|----------|------|-------------|
| `apiUrl` | String | Central platform base URL |
| `organizationIdentifier` | String | Organization identifier |
| `projectIdentifier` | String | Project identifier |
| `targetUniqueId` | String | Target unique identifier |
| `preauthCode` | String | Preauth code (still passed for traceability) |

### Agent Config Properties (agents/unix/)

Properties file written by install script (`/tmp/agent.properties`):

| Property | Source Variable | Status |
|----------|----------------|--------|
| `agent.central-url` | `apiUrl` | Existing |
| `agent.api-key` | `apiKey` | **NEW** — was missing |
| `agent.agent-id` | `agentId` | **NEW** — was missing |
| `agent.central-public-key` | `centralPublicKey` | **NEW** |
| `agent.organization-identifier` | `organizationIdentifier` | **NEW** (optional, for traceability) |
| `agent.project-identifier` | `projectIdentifier` | **NEW** (optional, for traceability) |
| `agent.target-unique-id` | `targetUniqueId` | **NEW** (optional, for traceability) |

### State Transitions

**Install Token Lifecycle**:

```
[Generated during registerAgent()]
    │
    ▼
ACTIVE (token exists, expiresAt set to now+5min)
    │
    ├── Binary downloaded → CONSUMED (token nulled, expiresAt nulled)
    │
    └── Time expires → EXPIRED (token nulled, expiresAt nulled)
```

**Agent (same entity, no new statuses)**:
- Status remains `ACTIVE` after registration (existing behavior, no change).
- `apiKey` and `installToken` are generated together during `registerAgent()`.

## Relationships (unchanged)

```
Organization ──┐
               ├── Project ──┬── Target
               │             │
               │             └── Agent (apiKey, installToken)
               │
               └── [unchanged]
```

The Agent already has `organizationId` and `projectId` references. The new fields (`installToken`, `installTokenExpiresAt`) are stored directly on the Agent document — no new collection needed.
