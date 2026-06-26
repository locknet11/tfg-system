# Product Requirements Document — Remediation Feature Flow

**Feature:** 009-remediation-flow  
**Status:** Draft  
**Date:** 2026-06-26  
**Project:** Autonomous Cybersecurity System for Cloud Infrastructures

---

## 1. Executive Summary

The platform currently provides a complete vulnerability detection pipeline: agents scan their assigned targets, identify running services and versions, look up known CVEs, and report findings to the central dashboard. However, once vulnerabilities are detected, **there is no mechanism to fix them**. The system can see the problem but cannot solve it.

This PRD defines the **Remediation Feature Flow** — an autonomous, agent-driven capability that closes the loop from "vulnerability detected" to "vulnerability fixed and verified," with full auditability and alerting.

---

## 2. Problem Statement

### 2.1 Current State

| Capability | Status |
|---|---|
| Target scanning (OS, services, versions) | ✅ Complete |
| Vulnerability lookup (CVE database queries) | ✅ Complete |
| Autonomous agent self-replication to new targets | ✅ Complete |
| Agent plan/template execution engine | ✅ Complete |
| Alert configurations for remediation events | ✅ Pre-wired (never triggered) |
| **Vulnerability remediation** | ❌ Missing |
| **Remediation tracking and history** | ❌ Missing |
| **Remediation type catalog** | ❌ Missing |

### 2.2 Core Problem

When an agent scans its assigned target and finds vulnerabilities, the findings are reported and displayed on the dashboard — and nothing else happens. There is no path from "CVE found" to "CVE fixed." The system is purely diagnostic; it lacks the therapeutic half of the cybersecurity loop.

---

## 3. Product Vision

### 3.1 Remediation Ownership Model

Each agent is responsible for remediating vulnerabilities **only on its own assigned target**. When the auto-replication mechanism spawns a new agent onto a previously unmanaged host, that new agent inherits full responsibility for remediating its own system.

> **Principle:** One agent, one target. The agent that lives on a target is the agent that fixes that target.

### 3.2 Autonomous Remediation Flow

The remediation step is a configurable step within agent plan templates. A full autonomous workflow looks like:

```
1. SYSTEM_SCAN        → Discover OS and installed packages
2. SERVICE_SCAN       → Identify running services and their versions
3. EXPLOITATION_KNOWLEDGE → Query CVEs for discovered services
4. REMEDIATE          → [NEW] Fix identified vulnerabilities on this target
5. SEND_REPORT        → Report results to the central platform
```

The agent executes this plan autonomously. No human clicks a button to start remediation — the agent does it as part of its assigned plan.

### 3.3 Remediation Type Catalog

Not all vulnerabilities can be fixed the same way. The system must recognize three categories of remediation and handle each appropriately:

#### Type A — Service-Level Remediation (IN SCOPE)

The vulnerability is fixed by updating, reinstalling, or reconfiguring a specific service, then restarting that service. The target does not need to reboot.

**Examples:**
- Upgrade `openssh-server` from 8.9p1 to 9.3p2 to patch CVE-2023-38408
- Change a configuration file to disable weak cipher suites
- Restart a service to apply the change

**Agent behavior:**
1. Apply the fix (update package, modify config, etc.)
2. Restart the affected service
3. Verify the service is running with the expected version
4. Re-scan the service to confirm the CVE is no longer reported
5. Report success/failure to the central platform

#### Type B — Target Restart Remediation (IN SCOPE, with caveat)

The vulnerability is fixed by applying a change, but the fix requires a full system reboot to take effect (e.g., some kernel module updates, certain system library patches).

**Scope limitation:** The system does not yet have a cross-reboot persistence mechanism for agents. That capability will be defined in a separate PRD. For this feature:

- The agent applies the fix
- The agent reports that a reboot is required and schedules it
- The agent acknowledges that it will not survive the reboot (no persistence mechanism yet)
- The central platform records the pending reboot state and notifies the administrator

#### Type C — Kernel Update (OUT OF SCOPE — Report Only)

The vulnerability can only be fixed by updating the operating system kernel, which requires a reboot and carries significant risk of system instability.

**Out of scope for this feature.** When a kernel-level CVE is detected:

- The agent does **not** attempt to fix it
- The agent reports the vulnerability with an explicit note: "This vulnerability requires a kernel update. The safe version is: X.Y.Z"
- The central platform displays the finding with a "Manual action required" indicator
- The administrator handles the kernel update through out-of-band processes

---

## 4. User Stories

### US-01: Agent Autonomously Remediates Its Own Target (P1)

**As a** security operator  
**I want** each agent to detect and fix vulnerabilities on its assigned target without human intervention, following the remediation steps in its plan template  
**So that** vulnerabilities are resolved continuously and automatically across the entire managed infrastructure.

**Acceptance Scenarios:**
1. An agent executes a plan that includes a `REMEDIATE` step after `EXPLOITATION_KNOWLEDGE`. The agent identifies a service-level vulnerability, applies the fix, verifies it, and reports success.
2. An agent encounters a vulnerability that requires a target reboot. The agent applies the fix, reports that a reboot is pending, and marks the remediation as "pending reboot."
3. An agent encounters a kernel-level vulnerability. The agent does not attempt remediation. It reports the finding with the safe kernel version and marks the vulnerability as "manual action required."

### US-02: Remediation as a Configurable Template Step (P1)

**As a** security operator  
**I want** to include a `REMEDIATE` step in plan templates, positioned after vulnerability discovery steps  
**So that** I can compose autonomous scan→detect→remediate workflows without custom code.

**Acceptance Scenarios:**
1. A template is created with steps: SYSTEM_SCAN → SERVICE_SCAN → EXPLOITATION_KNOWLEDGE → REMEDIATE → SEND_REPORT. When assigned to an agent, the agent executes all steps including remediation.
2. A template omits the REMEDIATE step. The agent scans and reports vulnerabilities but does not attempt to fix them.

### US-03: Remediation After Auto-Replication (P1)

**As a** security operator  
**I want** a newly replicated agent — spawned via auto-replication onto a previously unmanaged host — to automatically begin remediating vulnerabilities on that host  
**So that** every host brought into the managed mesh gets secured without additional human configuration.

**Acceptance Scenarios:**
1. Parent agent exploits a target, replicates itself, and the new agent registers with the central platform. The central platform auto-assigns a plan that includes REMEDIATE. The new agent scans its own system, finds vulnerabilities, and begins fixing them.
2. The new agent's remediation results are reported independently from the parent agent's results, scoped to the new target.

### US-04: View Remediation History (P2)

**As a** security operator  
**I want** to see a complete, filterable history of all remediation actions across all agents and targets  
**So that** I can audit what was fixed, when, by which agent, and with what outcome.

**Acceptance Scenarios:**
1. The remediation history page shows all remediation records with columns: target, CVE ID, remediation type, status, agent, start time, completion time.
2. Filters allow narrowing by status (success, failed, pending reboot, skipped), target, date range, and severity.
3. Clicking a row opens the remediation detail view with full logs.

### US-05: View Remediation Detail and Logs (P2)

**As a** security operator  
**I want to** drill into a specific remediation record and see the full execution context  
**So that** I can understand what happened during the fix attempt and troubleshoot failures.

**Acceptance Scenarios:**
1. The detail view shows: target information, CVE details, remediation type, the exact action performed, timestamps for each phase.
2. Expandable log sections show pre-check output, execution output, and post-verification output.

### US-06: Receive Remediation Alerts (P2)

**As a** security operator  
**I want to** receive email notifications when remediation succeeds or fails  
**So that** I am immediately aware of outcomes without checking the dashboard.

**Acceptance Scenarios:**
1. When remediation completes successfully, an alert is triggered and an email is sent to configured recipients.
2. When remediation fails, an alert is triggered with the failure reason and an email is sent.
3. Alert conditions `ON_REMEDIATION_SUCCESS` and `ON_REMEDIATION_FAILURE` that already exist in the alert configuration UI now actually fire.

### US-07: Dashboard Remediation Overview (P3)

**As a** security operator  
**I want** a dashboard widget showing remediation statistics at a glance  
**So that** I can assess the overall security posture of managed targets.

**Acceptance Scenarios:**
1. A widget shows counts of remediations by status: successful, failed, pending reboot, skipped.
2. A widget shows the mean time to remediate (MTTR) across all targets.
3. A recent activity feed shows the last 5 remediation actions.

---

## 5. Functional Requirements

### FR-01: Remediation Record

The system shall persist a remediation record for every remediation attempt, containing:

| Field | Description |
|---|---|
| Unique identifier | Generated by the platform |
| Target identifier | The target being remediated |
| Agent identifier | The agent performing the remediation |
| CVE identifier | The CVE being addressed |
| Vulnerability record reference | Link to the original vulnerability finding |
| Remediation type | SERVICE_UPDATE, SERVICE_CONFIG, TARGET_REBOOT, KERNEL_UPDATE (report-only) |
| Action description | Human-readable description of what was done |
| Status | PENDING, IN_PROGRESS, SUCCESS, FAILED, PENDING_REBOOT, SKIPPED |
| Pre-check output | Logs from commands run before the fix |
| Execution output | Logs from the fix commands |
| Post-verification output | Logs from verification commands |
| Started at | When the agent began execution |
| Completed at | When the agent finished execution |
| Error details | Present when status is FAILED |
| Organization and project scope | Multi-tenancy boundaries |

### FR-02: New Plan Step — REMEDIATE

A new plan step action, `REMEDIATE`, shall be available in plan templates. When an agent encounters this step:

1. The agent retrieves vulnerability findings from prior steps (EXPLOITATION_KNOWLEDGE context)
2. For each CVE found, the agent determines the remediation type (A, B, or C)
3. The agent handles each remediation according to the type catalog (see Section 3.3)
4. The agent reports the result for each CVE to the central platform
5. The step completes when all applicable CVEs have been processed

### FR-03: Remediation Type Resolution

The central platform shall provide a knowledge base that maps CVEs to remediation strategies. For a given CVE and target operating system, the platform returns:

- The remediation type (SERVICE_UPDATE, SERVICE_CONFIG, TARGET_REBOOT, KERNEL_UPDATE)
- The specific action to perform (package name to upgrade, config file to modify, etc.)
- Pre-check commands to validate the current state
- Post-verification commands to confirm the fix

If no mapping exists for a CVE, the platform returns a "no strategy available" response, and the agent reports the vulnerability as unfixable.

### FR-04: Post-Remediation Verification

After applying a fix of Type A or B, the agent shall verify its effectiveness:

1. Re-scan the affected service to get the current version
2. Query the central platform for CVEs against the new version
3. Confirm the target CVE is no longer present in the results
4. Include verification output in the remediation record

If verification shows the CVE is still present, the remediation shall be marked as FAILED, not SUCCESS.

### FR-05: Remediation History API

The central platform shall expose endpoints for querying remediation records:

- List remediation records with pagination, filterable by target, status, remediation type, CVE, date range
- Get a single remediation record with full execution context and logs
- Agent-facing endpoint for reporting remediation progress and results

### FR-06: Alert Integration

When a remediation completes (regardless of outcome), the central platform shall:

1. Create an alert event of type `REMEDIATION_COMPLETED`
2. Include in the event payload: CVE ID, target ID, remediation outcome, agent ID
3. Trigger the existing alert evaluation engine, which already supports `ON_REMEDIATION_SUCCESS` and `ON_REMEDIATION_FAILURE` conditions
4. Deliver email notifications to configured recipients via the existing notification channel

### FR-07: Remediation UI — History Page

A new page shall display the remediation history:
- Sortable, paginated table of remediation records
- Status displayed as colored tags (success = green, failed = red, pending reboot = yellow, skipped = gray)
- Filters: status, target, date range, remediation type
- Click-through to a detail page per record

### FR-08: Remediation UI — Detail Page

A detail page for a single remediation record shall show:
- Target name and identifier
- CVE identifier with link to the CVE detail
- Remediation type and action description
- Status timeline with timestamps
- Expandable sections for pre-check, execution, and post-verification logs
- Error message (if failed)

### FR-09: Remediation UI — Dashboard Widget

The project dashboard shall include a remediation widget showing:
- Remediation counts by status (success, failed, pending reboot, skipped)
- Mean time to remediate (MTTR)
- Most recent 5 remediation actions

### FR-10: Kernel Vulnerability Reporting

When the agent encounters a Type C (kernel update) vulnerability:
- The agent shall NOT attempt any fix
- The agent shall create a remediation record with status SKIPPED
- The record shall include a note: "Kernel update required. Safe version: [version]"
- The central platform shall display these records with a distinct "manual action required" visual indicator
- The original vulnerability report shall remain open

---

## 6. Non-Functional Requirements

| ID | Requirement |
|---|---|
| NFR-01 | All remediation audit records shall be immutable after creation |
| NFR-02 | The central platform must never serve or execute remediation commands on targets directly; all execution happens through agents |
| NFR-03 | Remediation records shall be scoped to organization and project; cross-project data leakage is not permitted |
| NFR-04 | The remediation history page shall load within 3 seconds for up to 1000 records |
| NFR-05 | All user-facing text shall be in English, following existing localization conventions |
| NFR-06 | Remediation execution logs shall be preserved in full; log truncation is not acceptable |

---

## 7. Core Workflow

### 7.1 Agent Remediation Flow

```
                    ┌──────────────────────┐
                    │  Agent plan includes  │
                    │  REMEDIATE step       │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │  Read vulnerabilities │
                    │  from prior step      │
                    │  context              │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │  For each CVE:        │
                    │  Query central for    │
                    │  remediation strategy │
                    └──────────┬───────────┘
                               │
               ┌───────────────┼───────────────┐
               ▼               ▼               ▼
        ┌────────────┐  ┌────────────┐  ┌────────────┐
        │  Type A    │  │  Type B    │  │  Type C    │
        │  Service   │  │  Reboot    │  │  Kernel    │
        │  Update    │  │  Required  │  │  Update    │
        └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
              │               │               │
              ▼               ▼               ▼
        ┌────────────┐  ┌────────────┐  ┌────────────┐
        │ Apply fix  │  │ Apply fix  │  │ Report     │
        │ Restart    │  │ Report     │  │ finding +  │
        │ service    │  │ "pending   │  │ safe       │
        │ Verify     │  │ reboot"    │  │ version    │
        │ Report     │  │            │  │ SKIP       │
        │ SUCCESS/   │  │            │  │            │
        │ FAILED     │  │            │  │            │
        └────────────┘  └────────────┘  └────────────┘
```

### 7.2 Agent ↔ Central Interaction

1. **Agent requests strategy**: "I have CVE-2025-12345 on Ubuntu 22.04, what should I do?"
2. **Central responds**: `{ type: SERVICE_UPDATE, action: "apt upgrade openssh-server to 9.3p2", preCheck: "dpkg -l openssh-server", postCheck: "ssh -V" }`
3. **Agent executes** on its local system
4. **Agent reports result**: "CVE-2025-12345 fixed. openssh-server upgraded 8.9p1 → 9.3p2. Verification: CVE no longer detected."
5. **Central creates** a remediation record and fires alerts

### 7.3 Auto-Replication + Remediation

```
Parent Agent (Target A)              New Agent (Target B, spawned via replication)
───────────────────────              ──────────────────────────────────────────────
1. Scans Target A                    1. Registers with Central
2. Finds vuln on Target B            2. Receives auto-assigned plan:
3. Exploits Target B                     SYSTEM_SCAN → SERVICE_SCAN →
4. Replicates self to Target B           EXPLOITATION_KNOWLEDGE → REMEDIATE
5. Continues operating on Target A   3. Scans its own system (Target B)
                                     4. Finds vulnerabilities on Target B
                                     5. Fixes vulnerabilities on Target B
                                     6. Reports results to Central
```

Each agent owns its target. The parent does not remediate the child's target; the child does.

---

## 8. State Model

```
                  ┌──────────┐
                  │  PENDING  │  Created by agent before execution begins
                  └─────┬─────┘
                        │
                        ▼
                  ┌──────────────┐
                  │ IN_PROGRESS  │  Agent is executing the fix
                  └──┬────────┬──┘
                     │        │
            ┌────────▼──┐  ┌──▼───────┐
            │  SUCCESS   │  │  FAILED  │  Fix completed; CVE confirmed gone (or not)
            └────────────┘  └──────────┘
            
            ┌────────────────┐
            │ PENDING_REBOOT │  Fix applied but requires target restart
            └────────────────┘
            
            ┌──────────┐
            │  SKIPPED  │  Kernel update needed; manual action required
            └──────────┘
```

---

## 9. Success Criteria

1. ✅ An agent executing a plan with a REMEDIATE step successfully fixes a service-level vulnerability (Type A), verifies the fix, and reports SUCCESS
2. ✅ A newly replicated agent auto-remediates vulnerabilities on its own target without human intervention
3. ✅ Kernel-level vulnerabilities (Type C) are reported with safe version information and marked as skipped — no fix is attempted
4. ✅ Remediation records appear in the history page with full logs and are filterable by status, target, and date
5. ✅ Email alerts fire for remediation success and failure events
6. ✅ The dashboard widget shows accurate remediation counts and MTTR
7. ✅ A complete audit trail exists for every remediation action across all targets

---

## 10. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Remediation breaks a production service | Type A remediation only restarts the affected service, not the whole system. Verification step catches failures immediately. |
| No remediation strategy exists for a CVE | Agent receives "no strategy" response and reports the CVE as unfixable. The vulnerability remains visible for manual action. |
| Post-verification shows CVE still present after fix | Remediation is marked as FAILED, not SUCCESS. The CVE remains open. |
| Agent process dies during remediation | The central platform times out the IN_PROGRESS state and marks the remediation as FAILED. The agent will retry when restarted. |
| Type B reboot needed but agent lacks persistence | Agent acknowledges the limitation and reports PENDING_REBOOT. Administrator is notified to handle the reboot manually for now. |

---

## 11. Out of Scope

- **Agent persistence across reboots**: Defined in a separate PRD. Until then, Type B remediations require manual reboot handling.
- **Kernel updates (Type C)**: Report-only. The system identifies the vulnerability and recommends the safe kernel version. No automated action.
- **Rollback**: If a remediation causes issues, there is no automatic rollback mechanism in this feature.
- **Scheduled/batch remediation**: Remediation executes opportunistically as part of agent plan execution, not on a fixed schedule.
- **Cross-target remediation**: An agent only fixes its own target. Agent A never fixes Target B.

---

*Based on codebase review: [codebase-review.md](./codebase-review.md)*
