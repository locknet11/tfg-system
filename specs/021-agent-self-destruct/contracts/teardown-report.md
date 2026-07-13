# Contract: Teardown outcome report

New endpoint. The agent reports its per-artifact teardown outcome for central audit **before** removing its binary and exiting.

## Endpoint

`POST /api/agent/comm/teardown`

- **Auth**: existing agent-communication auth (same as other `/api/agent/comm/*` calls).
- **When**: called once per agent, immediately after Java-side artifact removal completes and before the detached binary-removal script is spawned. Best-effort — if it fails (host offline), the agent proceeds with teardown anyway.

## Request body (`TeardownReportRequest`)

```json
{
  "agentId": "418J-H2QC-14SGQ",
  "trigger": "PLAN_COMPLETION",
  "timestamp": "2026-07-13T08:15:00Z",
  "results": [
    { "type": "AGENT_CONFIG",     "path": "/tmp/agent.properties", "status": "REMOVED",     "detail": "" },
    { "type": "AGENT_LOG",        "path": "/tmp/agent.log",        "status": "REMOVED",     "detail": "" },
    { "type": "DOWNLOADED_TOOLS", "path": "/tmp/agent-tools-xyz",  "status": "REMOVED",     "detail": "" },
    { "type": "OS_REGISTRATION",  "path": null,                    "status": "NOT_PRESENT", "detail": "" },
    { "type": "INSTALL_SCRIPT",   "path": null,                    "status": "NOT_PRESENT", "detail": "" }
  ],
  "binaryRemoval": "PENDING_DETACHED"
}
```

**Validation** (`jakarta.validation`): `agentId` not blank; `trigger` ∈ {`PLAN_COMPLETION`,`PLATFORM_DEPROVISION`,`AUTH_REVOKED`,`SELF_DESTRUCT_STEP`}; `results` non-null; each result `type` and `status` (`REMOVED`|`FAILED`|`NOT_PRESENT`) not blank.

## Response

`200 OK` with an acknowledgement body:

```json
{ "received": true, "recordId": "665f0c3e1a2b3c4d5e6f7a8b" }
```

## API behavior

- Persist an `AgentTeardownRecord` (MongoDB) with the request fields + server `reportedAt` + denormalized org/project/target from the agent record.
- Mark the agent fully torn down and reap the live agent record (or leave a tombstone per existing agent lifecycle conventions).
- Idempotent: a duplicate report for an already-recorded agent teardown returns `200` and does not create a second record (dedupe on `agentId` + `trigger` + first-seen).
