# Contract: Heartbeat Endpoint

**Date**: 2026-06-26  
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md)

## Overview

The heartbeat API contract is an **enhancement** to the existing endpoint. The endpoint URL and request format remain unchanged. The response is enhanced to include target-related status information.

---

## Endpoint

**PUT** `/api/agent/comm/heartbeat`

**Authentication**: Agent API key via `X-Agent-Api-Key` header (validated by `AgentApiKeyFilter`)  
**Scope**: Requires `ROLE_AGENT`  
**Agent ID**: Resolved from authenticated principal

---

## Request

**No request body required.** The agent is identified by its API key in the `X-Agent-Api-Key` header.

### Headers

| Header | Required | Example | Description |
|--------|----------|---------|-------------|
| `X-Agent-Api-Key` | Yes | `abc123def456...` | Agent's unique API key for authentication |

---

## Response

### 200 OK — Heartbeat Accepted

Agent exists and heartbeat was processed successfully.

```json
{
  "agentId": "6653f1a2b3c4d5e6f7a8b9c0",
  "status": "ACTIVE",
  "lastConnection": "2026-06-26T14:30:15",
  "hasPlan": true
}
```

#### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `agentId` | `String` | The agent's unique ID |
| `status` | `String` | Current agent status after heartbeat processing. Possible values: `ACTIVE`, `CREATED`, `UNRESPONSIVE` (if recovered, shows `ACTIVE`) |
| `lastConnection` | `String` (ISO 8601) | The updated last heartbeat timestamp |
| `hasPlan` | `boolean` | Whether the agent currently has an assigned plan |

### 400 Bad Request — Agent Not Found

Agent identified by API key does not exist.

### 401 Unauthorized — Invalid or Missing API Key

---

## Behavior Changes (This Feature)

### On Heartbeat Reception

1. **Update timestamp**: `Agent.lastConnection` is set to current server time.
2. **Status recovery** (NEW — enhanced): If the agent's status was `UNRESPONSIVE`:
   - Set agent status to `ACTIVE`
   - Look up the associated target (via `TargetRepository.findByAssignedAgent(agentId)`)
   - Set target status to `ONLINE` (only if target status is currently `OFFLINE`)
   - Save both agent and target
3. **Normal case**: If agent was already `ACTIVE`, only the timestamp is updated.

### On Registration (Existing, Documented for Completeness)

1. `Agent.lastConnection` is set to `LocalDateTime.now()` as the first heartbeat
2. `Agent.status` is set to `ACTIVE`
3. `Target.status` is set to `ONLINE`

---

## Rate

- Agents SHOULD send heartbeats every **30 seconds** (configurable on the agent side)
- The endpoint is idempotent regarding heartbeats — multiple calls are safe
- The central scheduler runs every **30 seconds** for staleness evaluation
