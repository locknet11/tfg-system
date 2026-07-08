---

description: "Task list for feature implementation"
---

# Tasks: Assign Plan Modal — Correct Assignment & Visibility

**Input**: Design documents from `/specs/014-assign-plan-modal/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/agent-plan-api.md, quickstart.md

**Tests**: Not explicitly requested in the spec; manual verification via `quickstart.md` is used instead, per Constitution Principle III (a verification step is required for every non-trivial change).

**Scope**: UI-only fix in `ui/src/app/pages/agents/`. No backend changes — both REST endpoints (`GET`/`PUT /api/agent/{id}/plan`) already exist and are reused unmodified.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)

---

## Phase 1: Foundational (Blocking Prerequisites)

**Purpose**: Data-access plumbing needed by every user story below

- [X] T001 [P] Add an `AgentPlanInfo` type (`notes`, `allowTemplating`, `steps`) to `ui/src/app/pages/agents/data-access/agents.model.ts`, mirroring the backend `PlanInfo` response shape
- [X] T002 Add `getPlan(agentId: string)` to `ui/src/app/pages/agents/data-access/agents.service.ts`, calling `GET /api/agent/{id}/plan` and mapping an HTTP 204 response to `null` (depends on T001 for the return type)

**Checkpoint**: Data-access layer can fetch an agent's current plan — user story work can begin

---

## Phase 2: User Story 1 - Assign a plan to an agent reliably (Priority: P1) 🎯 MVP

**Goal**: A confirmed assignment persists correctly; a failed assignment shows a clear error without corrupting any state

**Independent Test**: Assign a plan to an agent, confirm the success toast, then confirm persistence via `GET /api/agent/{id}/plan` (network tab or a manual request). Force a failed assignment and confirm the error is shown and nothing about the agent's persisted plan changes.

- [X] T003 [US1] In `ui/src/app/pages/agents/feature/assign-plan-modal/assign-plan-modal.component.ts`, update `submitForm()`'s error handler so a failed assignment shows the existing error toast without mutating any loaded current-assignment state
- [ ] T004 [US1] Verify assign / replace / error flows per `quickstart.md` steps 5–6

**Checkpoint**: User Story 1 is functional and testable independently

---

## Phase 3: User Story 2 - See the currently assigned plan (Priority: P2)

**Goal**: Opening the dialog shows the agent's actual current assignment (or a clear empty state), scoped to the correct agent

**Independent Test**: Assign a plan to an agent, reopen the dialog, confirm the assigned plan is displayed. Open the dialog for an agent with no plan and confirm the empty state.

- [X] T005 [US2] In `ui/src/app/pages/agents/feature/assign-plan-modal/assign-plan-modal.component.ts`, add `currentPlan`, `loadingPlan`, and `planLoadError` state plus a `loadCurrentPlan(agentId)` method that calls `agentsService.getPlan()`, guarding on the requested `agentId` so a late response for a previously opened agent is discarded (depends on T002)
- [X] T006 [US2] In `ui/src/app/pages/agents/feature/assign-plan-modal/assign-plan-modal.component.html`, render a "Currently Assigned Plan" section above the assign form: loading indicator, plan summary (notes + steps) when present, empty-state message when absent, and an error message on load failure
- [ ] T007 [US2] Verify current-assignment display flows per `quickstart.md` steps 1–2

**Checkpoint**: User Stories 1 AND 2 both work independently

---

## Phase 4: User Story 3 - Fresh dialog state on every open (Priority: P3)

**Goal**: Every dialog open starts clean — no leftover selections, errors, or another agent's data

**Independent Test**: Open the dialog, change the selection without saving, close it, reopen it — confirm no abandoned edits remain. Open the dialog for a different agent and confirm no data from the previous agent's session appears.

- [X] T008 [US3] In `ui/src/app/pages/agents/feature/assign-plan-modal/assign-plan-modal.component.ts`, implement `ngOnChanges` to detect `showModal` becoming `true` (or `agentId` changing while open), fully reset form/`useTemplate`/`selectedTemplateId`/error state, then call `loadCurrentPlan(agentId)` (depends on T005)
- [X] T009 [US3] In `ui/src/app/pages/agents/feature/assign-plan-modal/assign-plan-modal.component.ts`, extend `closeModal()` to also reset `currentPlan`/`loadingPlan`/`planLoadError` alongside the existing form reset
- [ ] T010 [US3] Verify fresh-state flows (abandoned edits, agent switching) per `quickstart.md` steps 3–4

**Checkpoint**: All three user stories are independently functional

---

## Phase 5: Polish & Cross-Cutting Concerns

- [X] T011 [P] Confirm all new UI text (loading/empty/error labels) uses `i18n`/`$localize` per Constitution Principle II
- [ ] T012 Run the full `quickstart.md` flow (all 6 steps) end-to-end in the browser

---

## Dependencies & Execution Order

- **Foundational (Phase 1)**: No dependencies — start immediately; blocks all user stories
- **User Story 1 (Phase 2)**: Depends on Phase 1 (T002)
- **User Story 2 (Phase 3)**: Depends on Phase 1 (T002); independent of US1's task but shares the same component file, so implement sequentially within the file
- **User Story 3 (Phase 4)**: Depends on Phase 3 (T005, since it wraps `loadCurrentPlan`)
- **Polish (Phase 5)**: Depends on all three user stories being complete

### Parallel Opportunities

- T001 can run in parallel with nothing else in Phase 1 (T002 depends on it) — mark [P] since it's a pure type addition
- T004, T007, T010 are manual verification tasks and can be batched together at the end if preferred, run against the same running instance
- T011 can run in parallel with T012

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Foundational
2. Complete Phase 2: User Story 1
3. **STOP and VALIDATE**: confirm assignment persists and errors don't corrupt state
4. Demo if ready — note that seeing the confirmation still requires checking via the network tab/API until US2 lands

### Incremental Delivery

1. Foundational → Phase 2 (US1) → validate → Phase 3 (US2) → validate → Phase 4 (US3) → validate → Polish
2. Each story adds value without breaking the previous one; all three touch the same single component file (`assign-plan-modal.component.ts`), so implement them in order rather than in parallel across people

---

## Notes

- All work is confined to `ui/src/app/pages/agents/feature/assign-plan-modal/` and `ui/src/app/pages/agents/data-access/`; no backend files change
- Do not run git commands unless the user explicitly approves them
- Stop at any checkpoint to validate a story independently before moving on
