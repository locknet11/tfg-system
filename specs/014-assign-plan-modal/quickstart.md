# Quickstart: Verifying the Assign Plan Modal Fix

## Prerequisites

- Local stack running: `ui/` (Angular dev server) and `api/` (Spring Boot) against
  a MongoDB instance with at least two agents and at least one template.
- Authenticated session with access to the Agents page.

## Manual verification flow

1. **Empty state on first assignment**
   - Open the Agents list, pick an agent with no plan assigned.
   - Click "Assign Plan". Confirm the dialog opens showing a clear "no plan
     currently assigned" indication (not a blank/broken form).
   - Assign a plan (either via template or custom steps) and confirm. Expect a
     success toast and the dialog to close.

2. **Current assignment is shown on reopen**
   - Reopen "Assign Plan" for the same agent.
   - Confirm the dialog now shows the plan just assigned (name/steps identifiable),
     not an empty state.

3. **Fresh state across agents**
   - Open "Assign Plan" for a second agent (different from step 1–2's agent, with a
     different or no plan assigned).
   - Confirm the dialog shows that second agent's own assignment (or empty state),
     with no leftover selection, steps, or error text from the first agent's
     session.

4. **Cancel does not persist**
   - Open the dialog for an agent, change the selection (template or steps)
     without confirming, and close via Cancel or the mask/Escape.
   - Reopen the dialog for the same agent. Confirm it shows the agent's actual
     persisted assignment (unchanged by the abandoned edit), not the abandoned
     selection.

5. **Replacement**
   - For an agent with an existing assignment, assign a different plan and
     confirm.
   - Reopen the dialog. Confirm it now shows the new plan, not the old one.

6. **Error handling**
   - Simulate a failed assignment (e.g., temporarily disconnect the API or submit
     an invalid state if reachable) and confirm a clear error message appears and
     the dialog's displayed current-assignment state is not corrupted.

## Automated checks

- `cd ui && ng test --include='**/assign-plan-modal.component.spec.ts'`
  (or the project's equivalent test-runner invocation) once component tests are
  added per `research.md`'s testing decision.
- No backend tests are required unless a regression test is added for the
  pre-existing `GET /api/agent/{id}/plan` endpoint.
