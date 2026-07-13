# Feature Specification: Unix Agent Self-Destruction & Self-Cleanup

**Feature Branch**: `021-agent-self-destruct`  
**Created**: 2026-07-13  
**Status**: Draft  
**Input**: User description: "The unix agent must have self-destruction capabilities. Self-destruction is triggered either when a plan is finished (all steps completed), or when the agent is deleted from the platform. It must remove all install scripts, traces, ENVs and auxiliary data used on the target system."

## User Scenarios & Testing *(mandatory)*

The unix agent is deployed by the central platform onto managed, authorized hosts to run scan and remediation plans. This feature gives the agent a **teardown lifecycle**: once the agent is no longer needed, it fully removes itself and every operational artifact it placed on the host. This is authorized de-provisioning and operational hygiene for agents the platform owns and controls — it prevents credentials, scripts, downloaded tools, and working data from lingering on hosts after the job is done.

### User Story 1 - Self-Destruct on Plan Completion (Priority: P1)

An agent has been assigned a plan and works through its steps. When every step of the plan reaches a completed state, the agent reports the final plan status and results to the central platform, and then tears itself down: it removes its installation script, working data, environment configuration, OS-level registration, and finally its own binary, and exits.

**Why this priority**: This is the primary trigger and the core value of the feature — an agent that finishes its authorized work should not remain resident on the host. Without it, every completed job leaves a running agent and its artifacts behind, accumulating risk and clutter across the fleet.

**Independent Test**: Assign a plan with all steps that complete successfully to an agent on a test host, let it finish, and verify (a) the platform records the plan as completed with final results, (b) the agent process is no longer running, (c) the install script, working directory, config/env files, OS registration, and binary are gone from the host.

**Acceptance Scenarios**:

1. **Given** an agent is executing a plan and the last remaining step transitions to completed, **When** the plan is detected as fully complete, **Then** the agent first sends the final plan status/results to the platform and only then begins teardown.
2. **Given** the agent has reported final plan results, **When** teardown runs, **Then** the install script, working/temp data, downloaded tool binaries, config and env files, and OS-level registration are removed, the agent process exits, and the agent binary is removed from the host.
3. **Given** a plan completes but the final status report to the platform fails (host offline), **When** teardown proceeds anyway, **Then** the agent still removes all local artifacts and exits, and the platform later reflects the agent as gone via its existing offline/heartbeat detection.

---

### User Story 2 - Self-Destruct on Platform-Initiated Deletion (Priority: P1)

An administrator deletes / de-provisions an agent from the central platform. The next time the agent contacts the platform (e.g. on its regular heartbeat), the platform signals that the agent has been removed. The agent acknowledges the signal and performs the same full teardown, exiting cleanly so it does not restart.

**Why this priority**: Operators need a reliable way to remotely retire an agent and have it clean up after itself. This is equal in importance to plan-completion teardown because de-provisioning is the other half of the agent lifecycle. It also covers agents whose plans never finish (cancelled, abandoned, or long-running).

**Independent Test**: Register an agent on a test host, delete the agent from the platform, and verify that on its next heartbeat the agent receives the deletion signal, tears down all artifacts, and the process exits and does not come back.

**Acceptance Scenarios**:

1. **Given** an agent is registered and sending heartbeats, **When** the agent is deleted from the platform and the agent's next heartbeat is answered with a deletion/de-provision signal, **Then** the agent performs full teardown and exits.
2. **Given** an agent can no longer authenticate to the platform (its credentials/registration have been revoked), **When** the agent repeatedly fails to authenticate over a sustained period, **Then** the agent treats itself as removed and performs full teardown and exits.
3. **Given** an agent receives a deletion signal while it is mid-way through executing a plan, **When** the signal is processed, **Then** the agent stops taking new work, attempts a best-effort final status report, and proceeds with teardown.

---

### User Story 3 - Complete, Best-Effort, Verifiable Artifact Removal (Priority: P2)

Whichever trigger fires, teardown removes the full set of artifacts the agent placed on the host and does so defensively: each removal step is attempted independently, a failure in one step does not abort the others, and the agent records which steps succeeded or failed (reporting that to the platform when it still can) before the process exits. Running teardown a second time on an already-cleaned host is harmless.

**Why this priority**: Correctness and auditability of the cleanup. The triggers (P1) deliver the behavior; this story guarantees the cleanup is thorough, resilient to partial failures, idempotent, and traceable. It is P2 because a basic teardown is usable before the full resilience/reporting guarantees are in place.

**Independent Test**: Trigger teardown on a host, inject a failure into one removal step (e.g. make one path non-removable), and verify the remaining artifacts are still removed, the agent reports per-step success/failure, the process still exits, and re-running teardown produces no errors.

**Acceptance Scenarios**:

1. **Given** teardown is running, **When** one removal step fails, **Then** every other removal step is still attempted and the failure is recorded rather than aborting teardown.
2. **Given** teardown has already fully cleaned the host, **When** teardown runs again, **Then** it completes without error and makes no further changes (idempotent).
3. **Given** teardown completes while the host is still online, **When** the agent reports teardown outcome, **Then** the platform receives a record of which artifacts were removed and which (if any) could not be removed, with a timestamp.
4. **Given** teardown has removed the OS-level registration, **When** the host or its service manager attempts to restart the agent, **Then** the agent does not come back because no residual registration remains.

---

### Edge Cases

- **Self-removal of a running binary**: the agent must be able to remove its own executable after it is confirmed loaded and running, so that no binary remains on disk once the process exits.
- **No OS registration present**: if the agent was launched ad-hoc (no systemd unit / launchd plist / cron entry), the registration-removal step finds nothing and succeeds without error.
- **Concurrent triggers**: if plan completion and a platform deletion signal both occur close together, teardown runs exactly once (subsequent trigger is a no-op).
- **Offline at teardown time**: if the host cannot reach the platform, local artifact removal still completes; the outcome report is skipped and the agent's disappearance is reconciled by existing heartbeat/offline detection.
- **Partial prior teardown**: if a previous teardown was interrupted (power loss, kill), a later teardown removes whatever remains without failing on already-missing artifacts.
- **Shared vs. agent-owned paths**: teardown only removes artifacts the agent itself created/installed; it must not delete unrelated host data that happens to sit nearby.
- **In-flight remediation at deletion**: a deletion signal arriving mid-remediation stops new work but should avoid leaving a target in a broken half-remediated state where the specification allows finishing the current atomic step first.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The agent MUST detect when an assigned plan is fully complete (all steps in a completed/terminal state) and treat that as a self-destruction trigger.
- **FR-002**: On plan-completion trigger, the agent MUST report the final plan status and results to the central platform **before** removing local artifacts, so completion and teardown are recorded centrally for traceability.
- **FR-003**: The agent MUST detect a platform-initiated deletion/de-provision signal delivered through its regular communication with the platform (e.g. the heartbeat response) and treat it as a self-destruction trigger.
- **FR-004**: The agent MUST treat sustained inability to authenticate to the platform (registration/credentials revoked) as an implicit indication it has been removed, and self-destruct after a bounded number of failed attempts / bounded time window.
- **FR-005**: Teardown MUST remove the agent's installation script(s) placed on the host.
- **FR-006**: Teardown MUST remove the agent's environment configuration — in-process environment and any values persisted to profile files, service/unit definitions, or env files on disk.
- **FR-007**: Teardown MUST remove auxiliary/working data: local temporary logs, cached files, downloaded tool binaries, config/manifest files, and working directories created by the agent.
- **FR-008**: Teardown MUST remove any OS-level registration used to install or run the agent (e.g. systemd unit, launchd plist, cron entry, or equivalent service definition) so the agent is not restarted after teardown.
- **FR-009**: Teardown MUST remove the agent's own binary/executable from the host, including handling the case where the binary is the currently running program.
- **FR-010**: After teardown, the agent process MUST exit and MUST NOT be restarted by any residual OS registration or supervisor.
- **FR-011**: Teardown MUST be best-effort: each removal step is attempted independently and a failure in one step MUST NOT prevent the remaining steps from running.
- **FR-012**: Teardown MUST be idempotent: running it against an already-cleaned (or partially-cleaned) host completes without error and makes no unintended changes.
- **FR-013**: The agent MUST record the per-step outcome of teardown (which artifacts were removed vs. failed) and MUST report this outcome to the platform when the host is still able to communicate, before the process exits.
- **FR-014**: Self-destruction MUST run at most once per agent instance; a second trigger arriving after teardown has started MUST be a no-op.
- **FR-015**: On a platform-initiated deletion trigger, the agent MUST stop accepting new work, make a best-effort final report, and then tear down — even if no plan is assigned or the current plan is incomplete.
- **FR-016**: Teardown MUST only remove artifacts the agent created or installed; it MUST NOT remove unrelated host data.

### Cross-Cutting Requirements

- **Internationalization**: Any operator-facing text surfaced by the platform about teardown/de-provisioning status must be authored in English and follow the project i18n approach. The agent's own local log lines are diagnostic (English) and are themselves removed during teardown.
- **Accessibility**: No new end-user UI is mandated by this feature; if teardown/agent-removal status is surfaced in the existing dashboard, it must meet the project's existing accessibility expectations (keyboard, ARIA, focus).
- **Validation and Error Handling**: The deletion signal and plan-completion detection must be validated before triggering the irreversible teardown (avoid destroying an agent on a malformed or spoofed signal). Each teardown step must capture and record failures rather than crashing.
- **Security Constraints**: The deletion/de-provision signal MUST be authenticated as coming from the legitimate platform for this agent (an unauthenticated party MUST NOT be able to trigger teardown). Credentials, preauth codes, and config removed during teardown MUST be handled so they are not left readable on disk afterward. This capability is authorized cleanup of platform-owned agents on authorized hosts; it is operational hygiene, not a means to evade the host's owner/administrator.

### Key Entities *(include if feature involves data)*

- **Agent Instance**: A deployed agent on a specific host, associated with an organization/project/target and a registration identity. Gains a teardown lifecycle state (e.g. active → tearing-down → gone).
- **Teardown Outcome Record**: A report of a self-destruction event — trigger type (plan-completion vs. platform-deletion vs. auth-revoked), timestamp, and per-artifact removal results (removed / failed / not-present). Stored centrally for audit.
- **Artifact Set**: The catalog of things the agent placed on the host that teardown must remove — install script(s), binary, env/config, working data, downloaded tool binaries, OS registration.
- **Deletion Signal**: The authenticated instruction from the platform (delivered via the agent's regular channel) that the agent has been de-provisioned and must self-destruct.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After a plan completes with all steps successful, 100% of the agent's artifacts (install script, binary, env/config, working data, downloaded tools, OS registration) are removed from the host, verifiable by inspection.
- **SC-002**: The platform records the final plan results for every plan-completion teardown before the agent's artifacts are removed (final report precedes local removal in 100% of online cases).
- **SC-003**: After an agent is deleted from the platform, the agent self-destructs and its process is no longer running on the host within one heartbeat interval of the deletion being observed by the agent.
- **SC-004**: After teardown, the agent does not restart: over a sustained observation window (e.g. 10 minutes) following teardown, no agent process reappears and no residual OS registration remains.
- **SC-005**: When a single teardown step is forced to fail, all other teardown steps still complete and the agent still exits (best-effort verified), and the per-step outcome is reported when the host is online.
- **SC-006**: Running teardown twice on the same host produces no errors and no additional changes on the second run (idempotency verified).
- **SC-007**: An unauthenticated or spoofed deletion signal does not trigger teardown (0 false self-destructions in adversarial signal tests).

## Assumptions

- The agent already registers with the platform, sends periodic heartbeats (~30s), and executes plans as sequential steps (per features 004, 011, and 001); this feature adds a teardown lifecycle on top of those, and reuses the heartbeat response as the delivery channel for the deletion signal.
- "Plan finished" means all steps of the assigned plan have reached a terminal completed state as already tracked by the platform/agent; teardown does not redefine step execution.
- The set of artifacts to remove is exactly what the agent's own installation/bootstrap and runtime create (install script, binary, env/config, working dir, downloaded tool binaries, OS registration); host software and data the agent did not place are out of scope for removal.
- Scope is the unix agent only (`agents/unix`); Windows agents are out of scope.
- The host is one the platform is authorized to manage; teardown is de-provisioning/hygiene, not anti-forensic evasion of the host owner.
- "Sustained inability to authenticate" uses a bounded threshold (number of consecutive failures and/or a time window) to avoid self-destructing on a transient network/platform outage — exact threshold to be set in planning with a conservative default.
- Reporting teardown outcome depends on the host still having connectivity; when offline, local removal still completes and central reconciliation happens via existing offline detection.

## Constitution Notes

- Applies repository guidance from `CLAUDE.md`/`AGENTS.md`: this is `agents/unix` (Spring Boot + GraalVM native) work. Per the script/template boundary rule, any shell used for teardown (self-removal, OS-registration removal) must live in resource template files (`src/main/resources/scripts/*.sh.tmpl` / `*.ftl`) and be materialized via `ClassPathResource` + `String.replace()` (GraalVM-native safe), never built inline with `String.format`/`StringBuilder`/concatenation.
- Confirm whether `.agents/skills/` introduces additional stack-specific constraints for the unix agent before implementation.
- Open questions for planning (not to be guessed silently): the exact failed-auth threshold before implicit self-destruct; whether a mid-plan deletion signal finishes the current atomic remediation step before tearing down; how the binary self-removal is performed on the supported unix targets given GraalVM-native packaging.
