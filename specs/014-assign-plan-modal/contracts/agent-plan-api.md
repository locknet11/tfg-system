# Contract: Agent Plan Assignment API

This feature consumes two **existing, unmodified** REST endpoints exposed by
`AgentController` (`api/src/main/java/com/spulido/tfg/domain/agent/controller/AgentController.java`).
No backend contract changes are introduced. This document exists to pin the
contract the UI is now required to honor correctly.

## GET /api/agent/{id}/plan

Fetches the agent's currently assigned plan. **Newly consumed by the frontend as
part of this feature** — previously called by nothing in the UI.

- **Auth**: `@PreAuthorize("isAuthenticated()")`
- **Path params**: `id` — agent id (scoped to the caller's organization/project via
  `AgentRepository.findByIdScoped`)
- **Success response**: `200 OK` with body `PlanInfo`:
  ```json
  {
    "notes": "string | null",
    "allowTemplating": false,
    "targetId": "string | null",
    "targetIp": "string | null",
    "steps": [
      { "action": "SYSTEM_SCAN", "status": "PENDING", "logs": [] }
    ]
  }
  ```
- **No assignment**: `204 No Content`, empty body. The UI MUST treat this as the
  "no plan currently assigned" state (FR-003), not as an error.
- **Not found / not authorized**: existing `AgentException` handling applies
  (unchanged); the UI MUST surface this as an error state (FR-008), not as an
  empty-plan state.
- **UI contract obligations** (new, enforced by this feature):
  - Called once per dialog open, scoped to the `agentId` the dialog was opened for.
  - Response applied to the view only if the dialog is still open for the same
    `agentId` when the response arrives (guards FR-005).
  - Drives the dialog's initial rendering: `loaded.currentPlan` populated → show
    current assignment; `null` → show empty state; request failure → show error
    state, no assignment inferred.

## PUT /api/agent/{id}/plan

Assigns (replaces) the agent's plan. **Existing endpoint, behavior unchanged** —
already consumed by `AssignPlanModalComponent.submitForm()` via
`AgentsService.assignPlan`.

- **Auth**: `@PreAuthorize("isAuthenticated()")`
- **Path params**: `id` — agent id
- **Request body**: `AssignPlanRequest`
  ```json
  {
    "useTemplate": true,
    "templateId": "string | null (required if useTemplate=true)",
    "plan": {
      "notes": "string | null",
      "steps": [{ "action": "SYSTEM_SCAN" }]
    }
  }
  ```
  (`plan` required if `useTemplate=false`; validated server-side per existing
  `AgentServiceImpl`/`AgentPlanServiceImpl` logic — unchanged by this feature.)
- **Success response**: `200 OK` with body `AgentInfo` (does not include the
  assigned plan's content — the UI relies on `GET /{id}/plan` for that, per the
  research decision to keep this feature UI-only).
- **Failure response**: existing `AgentException` mapping; the UI MUST show a
  clear error and MUST NOT alter its displayed "current assignment" state on
  failure (FR-008), since the persisted assignment did not change.
- **UI contract obligation** (new, enforced by this feature): on success, the
  dialog's in-memory `currentPlan` view state is cleared to `idle` along with the
  rest of the modal state (the dialog closes); it is not required to optimistically
  update `currentPlan` in place, since the next open will re-fetch it fresh via
  `GET /{id}/plan`.

## Out of scope for this feature

- `POST /api/agent/{id}/plan/from-template/{templateId}` — an alternate
  template-assignment endpoint that exists in `AgentController` but is not called
  by `AssignPlanModalComponent` (which uses `PUT /{id}/plan` with
  `useTemplate: true` instead). No change proposed; noted here only to avoid
  confusion during implementation.
