# Implementation Plan: Assign Plan Modal — Correct Assignment & Visibility

**Branch**: `014-assign-plan-modal` | **Date**: 2026-07-08 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/014-assign-plan-modal/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Users need the Assign Plan dialog to (a) reliably persist a plan assignment to an
agent and (b) show the agent's currently assigned plan every time the dialog opens,
starting from a clean state each time. The backend already stores one `Plan` per
`Agent` and exposes `GET /api/agent/{id}/plan` (204 when unassigned), but the
`AssignPlanModalComponent` never calls it — it always opens as if no plan exists and
never carries agent identity into its load, so it can also render a previous agent's
data if requests resolve out of order. The fix is UI-only: fetch the current
assignment on every open (keyed to the agent the dialog was opened for, ignoring
stale responses), fully reset component state on each open/close, and render the
existing assignment (template vs. custom, with steps) before allowing edits.

## Technical Context

**Language/Version**: TypeScript 5 (Angular 17, standalone components); Java 21 (Spring Boot 3) for the existing API — no backend changes required for this feature
**Primary Dependencies**: Angular Reactive Forms, PrimeNG (`p-dialog`, `p-dropdown`, `p-checkbox`, `MessageService`), RxJS
**Storage**: MongoDB via existing `AgentRepository` (`Agent.plan` field) — read-only for this feature, no schema changes
**Testing**: Jasmine/Karma (`ng test`) for the Angular component/service; existing JUnit suite for `AgentController`/`AgentPlanServiceImpl` only if a regression test is added for the already-existing `GET /{id}/plan` endpoint
**Target Platform**: Web (Angular SPA) served to browser clients of the existing TFG system
**Project Type**: Web application (`ui/` Angular frontend + `api/` Spring Boot backend, per repository structure)
**Performance Goals**: Dialog open-to-render of current assignment in line with existing modal load times (single lightweight GET, no added round trips beyond current template list load)
**Constraints**: No new backend endpoints or schema changes — reuse existing `GET /api/agent/{id}/plan` and `PUT /api/agent/{id}/plan`; must not regress the existing "use template" vs. "custom plan" toggle
**Scale/Scope**: Single component (`AssignPlanModalComponent`) plus its service call; scope limited to `ui/src/app/pages/agents/feature/assign-plan-modal/` and `ui/src/app/pages/agents/data-access/agents.service.ts`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Repository guidance reviewed: no `AGENTS.md` exists at the repo root; `.agents/skills/` was checked and contains no rules affecting this Angular-only change
- English-only rule satisfied: all new UI copy will use `i18n`/`$localize` per existing modal conventions (see current `assign-plan-modal.component.html` usage)
- Proposed design is the smallest correct change: reuse the existing `GET /{id}/plan` endpoint and existing form/dropdown structure; no new abstractions, services, or backend surface introduced
- Stack rules captured: Angular strict TS, standalone component, `readonly`/signals where natural, kebab-case files (unchanged), existing centralized `MessageService`/HTTP error handling reused
- Verification steps identified: manual UI verification (open/assign/reopen/switch-agent flows) via the `run` skill in the browser; `ng test` for the modal component if unit tests are added
- Git actions identified: none required by the plan itself; any commits/branch actions require explicit user approval per Principle V
- Unknown or ambiguous requirements: none blocking — the only open item (behavior while a plan is mid-execution) is a non-blocking note carried from the spec and does not change this plan's design (existing PUT endpoint's current behavior is preserved as-is)

## Project Structure

### Documentation (this feature)

```text
specs/014-assign-plan-modal/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
ui/src/app/pages/agents/
├── feature/
│   ├── assign-plan-modal/
│   │   ├── assign-plan-modal.component.ts      # MODIFIED: load current assignment on open, reset on close
│   │   ├── assign-plan-modal.component.html    # MODIFIED: render current-assignment / loading / empty states
│   │   └── assign-plan-modal.component.scss    # MODIFIED (minor): styles for current-assignment display
│   └── agents-list/
│       └── agents-list.component.ts            # UNCHANGED: already passes agentId + showModal correctly
└── data-access/
    ├── agents.model.ts                         # MODIFIED: add PlanInfo/current-assignment response shape if needed
    └── agents.service.ts                       # MODIFIED: add getPlan(agentId) call to existing GET /{id}/plan

api/src/main/java/com/spulido/tfg/domain/
├── agent/controller/AgentController.java        # UNCHANGED: GET /api/agent/{id}/plan already implemented
├── agent/services/impl/plan/AgentPlanServiceImpl.java  # UNCHANGED: getAgentPlan already implemented
└── plan/model/dto/PlanInfo.java                  # UNCHANGED: existing response DTO reused as-is

ui/src/app/pages/agents/feature/assign-plan-modal/
└── (component test file, if added)               # NEW/MODIFIED: unit tests for load-on-open and reset-on-close
```

**Structure Decision**: This is a UI-only fix within the existing `ui/` Angular
frontend of the web application (`ui/` + `api/`) structure. No backend files change —
the required read endpoint (`GET /api/agent/{id}/plan`) and write endpoint
(`PUT /api/agent/{id}/plan`) already exist and are reused unmodified. All work is
scoped to `AssignPlanModalComponent`, its template, and the `AgentsService` data-access
layer that talks to those existing endpoints.

## Complexity Tracking

*No constitution violations identified. This section is intentionally empty — the
design reuses existing endpoints, existing form structure, and existing PrimeNG
components with no new abstractions.*
