# Feature Specification: Assign Plan Modal — Correct Assignment & Visibility

**Feature Branch**: `014-assign-plan-modal`
**Created**: 2026-07-08
**Status**: Draft
**Input**: User description: "As a user, I want to be able to correctly assign a plan to the agent and see what plan I have assigned to it. Each time I click on assign plan, it show a fresh modal. If a plan was previously assigned it must show it."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Assign a plan to an agent reliably (Priority: P1)

As a user managing agents, I select an agent, open the "Assign Plan" dialog, choose a plan (from a template or a custom plan), and confirm. The assignment is saved and the agent now has that plan associated with it.

**Why this priority**: This is the core action of the feature — if assignment does not work correctly, nothing else matters. Users currently cannot trust that the plan they picked was actually assigned.

**Independent Test**: Can be fully tested by assigning a plan to a single agent, then verifying (via the agent's details or by reopening the dialog) that the assignment was recorded — delivers the core value on its own.

**Acceptance Scenarios**:

1. **Given** an agent with no plan assigned, **When** the user opens the Assign Plan dialog, selects a plan, and confirms, **Then** the plan is saved as assigned to that agent and the user receives confirmation of success.
2. **Given** an agent with a plan already assigned, **When** the user assigns a different plan and confirms, **Then** the new plan replaces the previous one as the agent's assigned plan.
3. **Given** the user selects a plan but the save fails, **When** the failure occurs, **Then** the user sees a clear error message and the agent's previous assignment (or lack of one) remains unchanged.

---

### User Story 2 - See the currently assigned plan (Priority: P2)

As a user, when I open the Assign Plan dialog for an agent that already has a plan assigned, the dialog shows me which plan is currently assigned so I know the agent's current state before making changes.

**Why this priority**: Visibility of the current assignment is what makes the assignment trustworthy and prevents accidental re-assignment or confusion about an agent's state. It depends on Story 1 (an assignment must exist to be shown).

**Independent Test**: Assign a plan to an agent (via any means), close the dialog, reopen it, and verify the previously assigned plan is displayed as the current assignment.

**Acceptance Scenarios**:

1. **Given** an agent with a plan previously assigned, **When** the user opens the Assign Plan dialog for that agent, **Then** the dialog displays the currently assigned plan (identifiable by its name and its steps/content).
2. **Given** an agent with no plan assigned, **When** the user opens the Assign Plan dialog, **Then** the dialog clearly indicates no plan is currently assigned and presents an empty selection state.
3. **Given** two different agents with different assigned plans, **When** the user opens the dialog for each agent in turn, **Then** each dialog shows the plan belonging to that specific agent, never the other agent's plan.

---

### User Story 3 - Fresh dialog state on every open (Priority: P3)

As a user, every time I click "Assign Plan" the dialog opens in a clean, freshly initialized state: no leftover selections, in-progress edits, error messages, or data from a previous open (whether for the same agent or a different one). The only pre-populated content is the agent's actual current assignment.

**Why this priority**: Stale dialog state is the root cause of incorrect assignments and misleading displays. It is prioritized after Stories 1–2 because it is a correctness safeguard on top of the core assign-and-view flows.

**Independent Test**: Open the dialog, make selections without confirming, close it, and reopen it (for the same and for a different agent) — verify the dialog reflects only persisted state, with no residue of the abandoned edits.

**Acceptance Scenarios**:

1. **Given** the user opened the dialog, changed the plan selection, and closed it without confirming, **When** they reopen the dialog, **Then** the abandoned changes are gone and the dialog shows the agent's actual persisted assignment (or empty state).
2. **Given** the user previously opened the dialog for agent A, **When** they open the dialog for agent B, **Then** no data from agent A's dialog session (selection, plan content, errors, loading states) appears.
3. **Given** a previous dialog session ended with an error message shown, **When** the dialog is reopened, **Then** the error message is not displayed.

---

### Edge Cases

- The plan assigned to an agent originates from a template that has since been deleted or modified: the dialog must still show what is actually assigned to the agent (the assignment as saved), not a broken or empty view.
- The user opens the dialog while the agent's assignment data is still loading: the dialog shows a loading indicator rather than a premature "no plan assigned" state.
- The user rapidly opens and closes the dialog, or opens it for several agents in quick succession: the displayed assignment always matches the agent the dialog was opened for (late-arriving data for a previous agent must not overwrite the current view).
- The agent's plan is currently being executed when the user opens the dialog: the current assignment is still displayed; behavior for re-assigning during execution follows existing product rules for plan execution.
- No plans or templates exist yet in the system: the dialog opens in a valid empty state and guides the user rather than failing.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to assign a plan to a specific agent from the Assign Plan dialog, and the assignment MUST be persisted so it survives closing the dialog and reloading the application.
- **FR-002**: When the Assign Plan dialog opens for an agent that has a plan assigned, the dialog MUST display that plan as the current assignment, including enough detail to identify it (at minimum its name; its steps/content where applicable).
- **FR-003**: When the Assign Plan dialog opens for an agent with no assigned plan, the dialog MUST present a clearly empty state indicating no plan is assigned.
- **FR-004**: Each opening of the Assign Plan dialog MUST initialize from the agent's persisted state only — no selections, edits, error messages, loading states, or data from any previous dialog session may carry over.
- **FR-005**: The assignment shown in the dialog MUST always correspond to the agent for which the dialog was opened, including when the user switches between agents or reopens the dialog quickly.
- **FR-006**: Assigning a new plan to an agent that already has one MUST replace the previous assignment, and the dialog MUST reflect the new assignment on subsequent opens.
- **FR-007**: Closing or cancelling the dialog without confirming MUST NOT change the agent's persisted assignment.
- **FR-008**: After a successful assignment, the user MUST receive visible confirmation; after a failed assignment, the user MUST see a clear error message and the persisted state MUST remain unchanged.

### Cross-Cutting Requirements

- **Internationalization**: All user-facing dialog text (labels, empty states, confirmations, error messages) is authored in English, consistent with the rest of the application.
- **Accessibility**: The dialog must be keyboard-operable (open, navigate selections, confirm, dismiss via Escape), move focus into the dialog on open, and return focus to the triggering control on close. Status messages (success/error) must be announced to assistive technologies.
- **Validation and Error Handling**: The confirm action must be disabled or rejected with clear feedback when no valid plan is selected. Failures to load the current assignment or to save a new one must produce user-friendly error messages without leaving the dialog in a misleading state.
- **Security Constraints**: Plan assignment uses the application's existing authenticated session; users may only view and assign plans for agents they are authorized to manage.

### Key Entities

- **Agent**: A managed endpoint/worker registered in the system. Holds at most one currently assigned plan.
- **Plan**: An ordered set of steps/actions to be carried out by an agent. May be created from a template or defined as a custom plan.
- **Plan Assignment**: The association between one agent and its currently assigned plan, including where the plan came from (template or custom). This is what the dialog reads on open and writes on confirm.
- **Template**: A reusable plan definition from which an assignment can be created; the assignment must remain viewable even if the source template later changes or is removed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of confirmed plan assignments are persisted and visible as the current assignment when the dialog is reopened, including after a full application reload.
- **SC-002**: In testing, 0 occurrences of the dialog showing stale data (a different agent's plan, abandoned edits, or leftover errors) across repeated open/close cycles and agent switches.
- **SC-003**: A user can open the dialog, identify the currently assigned plan (or the absence of one), and complete a new assignment in under 30 seconds.
- **SC-004**: Support/bug reports related to "assigned plan not shown" or "wrong plan assigned" drop to zero for this flow after release.

## Assumptions

- An agent has at most one plan assigned at a time; assigning a new plan replaces the previous one rather than queueing multiple plans.
- Closing the dialog without confirming discards in-progress choices silently (no "unsaved changes" warning is required for this feature).
- "Showing" the previously assigned plan means displaying it inside the Assign Plan dialog when it opens; a separate assigned-plan indicator elsewhere (e.g., in the agents list) is out of scope unless it already exists.
- Existing product rules govern whether a plan can be re-assigned while the current plan is executing; this feature does not change those rules.
- The existing plan/template catalog and agent management capabilities are reused; this feature does not introduce new plan-authoring functionality.

## Constitution Notes

- No `AGENTS.md` constitution file is present in the repository root; the project's `CLAUDE.md` defers to the current plan for stack context.
- No `.agents/skills/` stack-specific constraints were identified for this work.
- Open question to resolve during planning (not blocking the spec): confirm the intended behavior when the dialog is opened while the agent's plan is mid-execution.
