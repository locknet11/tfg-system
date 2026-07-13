# Contract: Operator de-provision of an agent

Reuses the existing delete endpoint; changes its semantics to a soft-mark so the running agent can observe the signal on its next heartbeat.

## Endpoint

`DELETE /api/agent/{id}` (existing — `AgentController.deleteAgent`)

- **Auth**: existing operator/admin auth (unchanged).
- **Behavior change**: instead of immediately hard-deleting the agent document, the service **soft-marks** it de-provisioned:
  - set `deprovisioned = true` (and optionally `status = KILLED`, `deprovisionReason`)
  - the agent record remains readable by `updateHeartbeat` so the next heartbeat returns `deprovision = true`
- Hard deletion / reaping occurs when:
  1. the agent posts its teardown report (`POST /api/agent/comm/teardown`), or
  2. a grace TTL elapses with no report (agent already gone / offline) — reconciled via the existing heartbeat-offline monitor (feature 011).

## Response

Unchanged from today (`204 No Content` / existing contract). Operators see the agent transition to a de-provisioning/killed state in the existing agent list until reaped.

## Notes

- Agents deleted by any path that hard-deletes immediately are still handled: the agent's next heartbeat gets an authenticated `404 agent-not-found`, and after 3 consecutive such rejections the agent self-destructs via the `AUTH_REVOKED` trigger (see `heartbeat-deprovision.md`).
- No new operator-facing UI is required by this feature. If the de-provisioning state is surfaced, it reuses the existing agent-list views and i18n patterns.
