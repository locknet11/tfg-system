# Contract: Heartbeat carries de-provision signal

Extends the existing heartbeat endpoint (feature 011). Additive only — existing fields unchanged.

## Endpoint

`PUT /api/agent/comm/heartbeat`

- **Auth**: existing agent-communication auth (agentId + api key / preauth). The signal is bound to the authenticated agent; an unauthenticated or mismatched caller cannot obtain `deprovision=true` for another agent.
- **Request**: unchanged (agent identified via existing auth/header as today).

## Response body (extended `HeartbeatResponse`)

```json
{
  "agentId": "418J-H2QC-14SGQ",
  "status": "ACTIVE",
  "lastConnection": "2026-07-13T08:00:00",
  "hasPlan": true,
  "deprovision": false,
  "deprovisionReason": null
}
```

| Field | Type | Meaning |
|-------|------|---------|
| `deprovision` | boolean | `true` when the agent has been deleted/de-provisioned on the platform. Agent MUST trigger teardown (`PLATFORM_DEPROVISION`). |
| `deprovisionReason` | string \| null | Optional human-readable reason (e.g. "deleted by operator"). Diagnostic only. |

## Agent behavior

- On `deprovision == true`: validate the response is for this agent (`agentId` matches configured id), then call `TeardownService.selfDestruct(PLATFORM_DEPROVISION)`.
- On repeated **authenticated rejection** (`401` / `403` / `404 agent-not-found`) for **3 consecutive** heartbeats: call `TeardownService.selfDestruct(AUTH_REVOKED)`.
- Transport failures (timeout, connection refused, `5xx`, DNS) MUST NOT trigger teardown and MUST reset the rejection counter is left unchanged (only successful auth resets it; transport errors are ignored for counting).

## API behavior

- `updateHeartbeat(agentId)` returns `deprovision=true` when the agent's record has `deprovisioned=true` (or `status=KILLED`).
- The agent record is soft-marked de-provisioned by `deleteAgent` (see `agent-deprovision-flag.md`) and hard-reaped only after the teardown report arrives or a grace TTL elapses.
