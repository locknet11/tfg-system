# Phase 0 Research: Assign Plan Modal — Correct Assignment & Visibility

No `NEEDS CLARIFICATION` markers remain in the Technical Context — the codebase
already contains everything needed to resolve the unknowns below by inspection
rather than external research.

## Decision: Root cause of "no current assignment shown"

- **Decision**: The dialog must call `GET /api/agent/{id}/plan` when it opens and
  render the result before allowing further edits, instead of always initializing
  as if no plan is assigned.
- **Rationale**: `AssignPlanModalComponent.ngOnInit` only calls `loadTemplates()`.
  It never reads the agent's current plan. The backend already exposes
  `GET /api/agent/{id}/plan` (`AgentController.getPlan`, returns `PlanInfo` or
  `204 No Content` when unassigned via `AgentPlanServiceImpl.getAgentPlan`), and the
  frontend `Agent` model even has an optional `plan` field — but nothing in the
  agents list (`AgentInfo` DTO) or the modal wires that data through. The endpoint
  is dead code from the frontend's perspective today.
- **Alternatives considered**:
  - Add `plan` to the `AgentInfo` DTO returned by `GET /api/agent` and pass it into
    the modal as an `@Input`. Rejected for this change: it would require a backend
    DTO/mapper change and a new `@Input` wiring in `agents-list.component.html`,
    which is a larger surface than necessary when a dedicated single-agent endpoint
    already exists and is cheap to call once per dialog open.
  - Have the modal reuse `AgentsService.list()` response instead of a dedicated
    call. Rejected: the list endpoint is paginated/searched and not a reliable
    single-source-of-truth for one agent's live state at dialog-open time.

## Decision: Preventing stale/cross-agent data in the modal

- **Decision**: Reset all modal state (selected template, form value, loading/error
  flags, loaded current-assignment) at the start of every open, and guard the
  current-assignment HTTP response with the agent id it was requested for so a
  late-arriving response for a previously opened agent is discarded if the dialog
  has since been opened for a different agent.
- **Rationale**: `AssignPlanModalComponent` is a single long-lived instance reused
  across all agents in `agents-list.component.html` (`[agentId]="selectedAgentId"`,
  `[showModal]="showAssignPlanModal"`). Because Angular reuses the component
  instance, any state not explicitly reset persists across opens today —
  `closeModal()` does reset form/useTemplate/selectedTemplateId, but only on close,
  not on open, and only for the fields that already existed before this feature.
  `ngOnInit` runs once for the component's lifetime (it is not destroyed/recreated
  per open), so the new "load current assignment" call must be triggered by the
  `showModal` input transitioning to `true`, not by `ngOnInit`.
- **Alternatives considered**:
  - Use `*ngIf` in the parent to destroy/recreate the modal component on every open
    (forces a fresh instance via Angular lifecycle). Rejected: larger change to
    `agents-list.component.html`/`.ts` and changes the modal's animation/mask
    behavof PrimeNG `p-dialog`; the in-component reset approach is smaller and
    keeps the existing parent/child contract (`agentId`, `showModal`,
    `modalClosed`, `planAssigned`) unchanged.
  - Rely on `ngOnChanges` for `agentId`/`showModal` inputs. Selected as the
    mechanism (see Data Model / component design) — this is the standard Angular
    pattern for reacting to `@Input` changes on a reused component instance and
    requires no parent changes.

## Decision: Rendering the current assignment (template vs. custom, deleted template)

- **Decision**: Render the loaded `PlanInfo` (notes + steps, read from the agent's
  stored `Plan`) directly, independent of whether it originated from a template.
  Do not attempt to re-resolve or re-link it to a template id.
- **Rationale**: `AgentPlanServiceImpl.assignPlanFromTemplate` copies the template's
  steps into a new `Plan` embedded on the `Agent` document at assignment time
  (`templateMapper.templateToPlan(template)`); the assigned plan does not retain a
  live reference to the source template. This means the currently assigned plan is
  always self-contained and safe to display even if the originating template is
  later deleted or modified — satisfying the "must still show what is actually
  assigned" edge case from the spec without any extra handling.
- **Alternatives considered**: Storing/showing the source `templateId` on the
  assignment for display. Rejected: `Plan`/`PlanInfo` do not carry a `templateId`
  today, and adding one is a backend data-model change outside this feature's
  UI-only scope; the spec only requires showing the assigned plan's identifying
  content (name/steps), not its provenance.

## Decision: Testing approach

- **Decision**: Cover the new load-on-open and reset-on-close/switch-agent behavior
  with Jasmine/Karma component tests (`ng test`) against `AssignPlanModalComponent`,
  mocking `AgentsService`. Verify manually in the browser via the `run` skill for
  the end-to-end flow (open → see current assignment → reassign → reopen → see new
  assignment; switch agents; cancel without saving).
- **Rationale**: Matches the repository's existing Angular testing stack
  (Jasmine/Karma) and Constitution Principle III (verification step required for
  every non-trivial change). No backend logic changes, so no new JUnit tests are
  required unless a regression test for the pre-existing `GET /{id}/plan` behavior
  is judged valuable during implementation.
- **Alternatives considered**: E2E-only verification. Rejected as insufficient on
  its own — the stale-state and race-condition requirements (FR-004, FR-005) are
  best pinned down with deterministic unit tests around observable ordering.
