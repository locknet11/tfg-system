# Phase 1 Data Model: Assign Plan Modal — Correct Assignment & Visibility

No persisted data model changes are introduced by this feature. It reuses the
existing backend entities and DTOs as-is. This document describes the entities
relevant to the feature and the new/changed **frontend-only** view-state shape
that governs correct modal behavior.

## Existing Entities (unchanged)

### Agent (backend: `com.spulido.tfg.domain.agent.model.Agent`)

- `id: String`
- `plan: Plan | null` — the agent's single currently assigned plan; replaced
  wholesale on reassignment (`AgentPlanServiceImpl.assignPlanFromTemplate` pushes
  the previous value onto `planHistory` before overwriting).
- `planHistory: List<Plan>` — out of scope for this feature (not surfaced in the
  dialog per spec Assumptions: only the current assignment is shown).

### Plan (backend: `com.spulido.tfg.domain.plan.model.Plan`; DTO: `PlanInfo`)

- `notes: String?`
- `allowTemplating: boolean` (domain model only — not present on `PlanInfo`/
  `PlanRequest`, pre-existing asymmetry, out of scope for this feature)
- `steps: List<Step>` (domain) / `List<PlanStepInfo>` (DTO: `action`, `status`,
  `logs`)

A `Plan` is a value object embedded on `Agent` — it has no independent identity
and no back-reference to a source `Template`, which is why it remains fully
displayable even if the source template is later deleted or edited (see
research.md).

### Template (backend: `com.spulido.tfg.domain.template.model.Template`)

Unchanged; used only to populate the "assign from template" dropdown and, at
assignment time, to seed a new `Plan` via `TemplateServiceMapper.templateToPlan`.

## Frontend View State (new/changed)

`AssignPlanModalComponent` gains explicit, agent-scoped view state to replace its
current implicit "always starts empty" behavior:

```ts
type AssignPlanModalState =
  | { status: 'idle' }                              // dialog not open
  | { status: 'loading'; forAgentId: string }        // fetching current assignment
  | { status: 'loaded'; forAgentId: string; currentPlan: PlanInfo | null } // fetch done
  | { status: 'error'; forAgentId: string; message: string };             // fetch failed
```

- **`forAgentId`** is the discriminator used to drop stale HTTP responses: when the
  `GET /api/agent/{id}/plan` response arrives, it is applied only if
  `forAgentId === agentId` still matches the component's current `@Input agentId`
  and the dialog is still open for that agent (satisfies FR-005 / the
  rapid-open-close edge case).
- **State transitions**:
  - `showModal` input becomes `true` (or `agentId` changes while open) →
    reset form/selection state → `loading` → fetch `GET /{agentId}/plan`.
  - Fetch resolves with a plan → `loaded` with `currentPlan` populated; form/
    template selection pre-filled from it for display (read-only presentation of
    the current assignment; editing starts a new selection, per spec — the dialog
    does not require "edit in place" semantics beyond choosing a new plan/template).
  - Fetch resolves with `204 No Content` (no `Location`/body) → `loaded` with
    `currentPlan: null` → empty-state UI.
  - Fetch fails → `error` with message → error UI, no silent fallback to "no plan".
  - `showModal` becomes `false` (cancel or after successful assign) → state reset
    to `idle`; all form controls, `useTemplate`, `selectedTemplateId`, and any
    error/loading flags cleared, per FR-004 and FR-007.
  - Successful `PUT /{agentId}/plan` → `planAssigned` emitted, dialog closes
    (existing behavior), state reset to `idle` (no need to re-fetch before close;
    next open re-fetches fresh).

## Key Entities (from spec, mapped to implementation)

- **Agent** → `Agent` (backend) / `Agent` (frontend model, `agents.model.ts`).
- **Plan** → `Plan` (backend) / `PlanInfo` (backend DTO, read) / `PlanRequest`
  (backend DTO, write) / `Plan` (existing frontend model, used for custom-plan
  authoring).
- **Plan Assignment** → represented at runtime by `Agent.plan`; not a separate
  persisted entity. On the frontend, represented by the new `currentPlan` view
  state described above.
- **Template** → `Template` (backend) / existing `templates` dropdown data in the
  modal (unchanged).
