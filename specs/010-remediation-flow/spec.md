# Feature Specification: Autonomous Remediation Flow

**Feature Branch**: `010-remediation-flow`  
**Created**: 2026-06-26  
**Status**: Draft  
**Input**: User description: "Implementar remediation feature flow (flujo de remediación autónomo)"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Agent Autonomously Remediates Its Own Target (Priority: P1)

As a security operator, I want each agent deployed on a managed host to detect vulnerabilities and automatically fix them without human intervention, following the remediation steps configured in its execution plan. This closes the loop from "vulnerability detected" to "vulnerability fixed and verified," transforming the system from purely diagnostic to therapeutic.

The agent executes remediation as a natural step within its assigned workflow: after scanning the system, identifying running services, and looking up known vulnerabilities, the agent proceeds to fix what it can fix on its own target. No human clicks a button to start remediation — the agent does it as part of its assigned plan.

**Why this priority**: This is the core value proposition of the feature. Without autonomous remediation, the system remains a monitoring tool. With it, the system becomes an active defense mechanism that continuously reduces the attack surface across the entire managed infrastructure.

**Independent Test**: Can be fully tested by deploying an agent on a target with a known vulnerable service, assigning it a plan that includes a remediation step, and verifying that the agent fixes the vulnerability and reports success — delivering a measurable reduction in open vulnerabilities.

**Acceptance Scenarios**:

1. **Given** an agent is executing a plan that includes a remediation step after vulnerability discovery, **When** the agent identifies a service-level vulnerability (e.g., an outdated SSH server with a known CVE), **Then** the agent applies the appropriate fix, restarts the affected service, verifies the fix was effective, and reports the remediation as successful with complete execution logs.

2. **Given** an agent encounters a vulnerability that requires a full system reboot to take effect, **When** the agent applies the fix, **Then** the agent reports that a reboot is pending, acknowledges it will not survive the reboot, and the central platform records the pending reboot state and notifies the administrator.

3. **Given** an agent encounters a kernel-level vulnerability that cannot be safely fixed automatically, **When** the remediation step processes this CVE, **Then** the agent does NOT attempt any fix, reports the finding with the recommended safe kernel version, and marks the vulnerability as requiring manual action.

---

### User Story 2 - Remediation as a Configurable Plan Step (Priority: P1)

As a security operator, I want to include a remediation step in agent execution plan templates, positioned after vulnerability discovery steps, so that I can compose autonomous scan-detect-remediate workflows without custom code or manual intervention.

**Why this priority**: This enables the composition of autonomous workflows. Without a configurable plan step, remediation would require custom agent logic for each deployment. With it, operators can create reusable templates that define the full security lifecycle.

**Independent Test**: Can be fully tested by creating a plan template that includes a remediation step, assigning it to an agent, and verifying the agent executes all steps including remediation — delivering a configurable autonomous workflow.

**Acceptance Scenarios**:

1. **Given** a plan template is created with steps in sequence: system scan, service scan, vulnerability lookup, remediation, and report, **When** the template is assigned to an agent, **Then** the agent executes all steps in order, including the remediation step that processes discovered vulnerabilities.

2. **Given** a plan template omits the remediation step, **When** the template is assigned to an agent, **Then** the agent scans and reports vulnerabilities but does not attempt to fix them — remediation is opt-in, not mandatory.

---

### User Story 3 - Remediation After Auto-Replication (Priority: P1)

As a security operator, I want a newly replicated agent — spawned via auto-replication onto a previously unmanaged host — to automatically begin remediating vulnerabilities on that host, so that every host brought into the managed mesh gets secured without additional human configuration.

**Why this priority**: This ensures the system scales securely. When the auto-replication mechanism brings a new host under management, that host should immediately benefit from the same autonomous remediation capabilities as existing managed hosts.

**Independent Test**: Can be fully tested by triggering auto-replication to a new target, verifying the new agent receives a plan with remediation, and confirming it scans and fixes vulnerabilities on its own target independently — delivering automatic security for newly managed hosts.

**Acceptance Scenarios**:

1. **Given** a parent agent exploits a target and replicates itself to that target, **When** the new agent registers with the central platform, **Then** the central platform auto-assigns a plan that includes remediation, the new agent scans its own system, finds vulnerabilities, and begins fixing them autonomously.

2. **Given** a newly replicated agent is remediating its target, **When** it reports results, **Then** the remediation records are scoped to the new target and reported independently from the parent agent's results.

---

### User Story 4 - View Remediation History (Priority: P2)

As a security operator, I want to see a complete, filterable history of all remediation actions across all agents and targets, so that I can audit what was fixed, when, by which agent, and with what outcome.

**Why this priority**: Auditability and traceability are essential for compliance and troubleshooting. While autonomous remediation delivers the core value, operators need visibility into what happened to maintain trust and control.

**Independent Test**: Can be fully tested by navigating to the remediation history page after agents have performed remediations, applying filters, and verifying the displayed records match the actual remediation actions — delivering full audit visibility.

**Acceptance Scenarios**:

1. **Given** agents have performed remediation actions across multiple targets, **When** a security operator navigates to the remediation history page, **Then** the page displays all remediation records with columns showing: target, CVE identifier, remediation type, status, agent, start time, and completion time.

2. **Given** the remediation history page is displayed, **When** the operator applies filters by status (success, failed, pending reboot, skipped), target, date range, or severity, **Then** the displayed records are narrowed to match the filter criteria.

3. **Given** the remediation history page shows a list of records, **When** the operator clicks on a specific record, **Then** the operator is taken to the remediation detail view with full execution logs.

---

### User Story 5 - View Remediation Detail and Logs (Priority: P2)

As a security operator, I want to drill into a specific remediation record and see the full execution context, so that I can understand what happened during the fix attempt and troubleshoot failures.

**Why this priority**: Detailed logs are essential for troubleshooting failed remediations and verifying successful ones. This supports the auditability requirement and builds operator confidence in the autonomous system.

**Independent Test**: Can be fully tested by clicking on a remediation record and verifying the detail view shows complete execution context including pre-check, execution, and post-verification logs — delivering full transparency into remediation actions.

**Acceptance Scenarios**:

1. **Given** a security operator is viewing a remediation record detail, **When** the page loads, **Then** the view displays: target information, CVE details, remediation type, the exact action performed, and timestamps for each phase of execution.

2. **Given** a remediation record includes execution logs, **When** the operator expands the log sections, **Then** the operator can view pre-check output, execution output, and post-verification output in full without truncation.

---

### User Story 6 - Receive Remediation Alerts (Priority: P2)

As a security operator, I want to receive notifications when remediation succeeds or fails, so that I am immediately aware of outcomes without checking the dashboard.

**Why this priority**: Proactive notification ensures operators are aware of critical outcomes without manual monitoring. This is especially important for failed remediations that may require manual intervention.

**Independent Test**: Can be fully tested by configuring alert conditions for remediation success and failure, triggering a remediation, and verifying that configured recipients receive notifications — delivering proactive awareness of remediation outcomes.

**Acceptance Scenarios**:

1. **Given** an alert configuration includes a condition for remediation success, **When** an agent successfully remediates a vulnerability, **Then** the system triggers an alert and delivers a notification to configured recipients with details about the CVE, target, and outcome.

2. **Given** an alert configuration includes a condition for remediation failure, **When** an agent fails to remediate a vulnerability, **Then** the system triggers an alert with the failure reason and delivers a notification to configured recipients.

---

### User Story 7 - Dashboard Remediation Overview (Priority: P3)

As a security operator, I want a dashboard widget showing remediation statistics at a glance, so that I can assess the overall security posture of managed targets and the effectiveness of the autonomous remediation system.

**Why this priority**: Dashboard visibility provides executive-level insights into remediation effectiveness. While not essential for core functionality, it helps operators understand the value delivered by the autonomous system.

**Independent Test**: Can be fully tested by viewing the dashboard after remediations have occurred and verifying the widget displays accurate counts and metrics — delivering at-a-glance security posture visibility.

**Acceptance Scenarios**:

1. **Given** agents have performed remediation actions, **When** a security operator views the project dashboard, **Then** a remediation widget displays counts of remediations by status: successful, failed, pending reboot, and skipped.

2. **Given** the remediation widget is displayed, **When** the operator views it, **Then** the widget shows the mean time to remediate across all targets and a recent activity feed showing the last 5 remediation actions.

---

### Edge Cases

- What happens when a remediation command fails or causes a service to crash? The agent marks the remediation as failed, includes error details in the execution logs, and the vulnerability remains open for manual intervention.

- What happens when the agent loses connectivity to the central platform during remediation? The agent completes the remediation locally and retries reporting when connectivity is restored. If the agent dies, the central platform times out the in-progress state and marks the remediation as failed.

- What happens when no remediation strategy exists for a discovered CVE? The agent receives a "no strategy available" response from the central platform and reports the CVE as unfixable, leaving it visible for manual action.

- What happens when post-verification shows the CVE is still present after applying a fix? The remediation is marked as failed, not successful. The CVE remains open and the vulnerability is still reported.

- What happens when multiple agents attempt to remediate the same target? This cannot happen by design — each agent is responsible only for its own assigned target. The principle is: one agent, one target.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST persist a complete remediation record for every remediation attempt, including: target identifier, agent identifier, CVE identifier, remediation type, action description, status, execution logs (pre-check, execution, post-verification), timestamps, and error details when applicable.

- **FR-002**: System MUST provide a remediation step action that can be included in agent execution plan templates, positioned after vulnerability discovery steps.

- **FR-003**: When an agent encounters a remediation step, the agent MUST retrieve vulnerability findings from prior steps, determine the appropriate remediation type for each CVE, and handle each according to the remediation type catalog.

- **FR-004**: System MUST support three remediation types: (A) service-level remediation that updates or reconfigures a service and restarts it, (B) remediation that requires a target reboot, and (C) kernel updates that are reported but not automatically applied.

- **FR-005**: For service-level remediation (Type A), the agent MUST apply the fix, restart the affected service, verify the service is running with the expected version, re-scan to confirm the CVE is no longer reported, and report the outcome.

- **FR-006**: For reboot-required remediation (Type B), the agent MUST apply the fix, report that a reboot is pending, acknowledge it will not survive the reboot, and the central platform MUST record the pending reboot state and notify the administrator.

- **FR-007**: For kernel updates (Type C), the agent MUST NOT attempt any fix, MUST report the vulnerability with the recommended safe kernel version, and MUST mark it as requiring manual action.

- **FR-008**: System MUST provide a remediation strategy knowledge base that maps CVEs and target operating systems to specific remediation actions, including pre-check commands, fix commands, and post-verification commands.

- **FR-009**: When no remediation strategy exists for a CVE, the system MUST return a "no strategy available" response, and the agent MUST report the vulnerability as unfixable.

- **FR-010**: After applying a fix (Type A or B), the agent MUST verify effectiveness by re-scanning the affected service, querying for CVEs against the new version, and confirming the target CVE is no longer present. If verification fails, the remediation MUST be marked as failed.

- **FR-011**: System MUST expose capabilities for querying remediation records with pagination and filtering by target, status, remediation type, CVE, and date range.

- **FR-012**: System MUST provide a capability for agents to report remediation progress and results to the central platform.

- **FR-013**: When a remediation completes, the system MUST create an alert event and trigger the existing alert evaluation engine to deliver notifications to configured recipients.

- **FR-014**: System MUST provide a remediation history page displaying a sortable, paginated table of remediation records with status displayed as colored indicators and filters for status, target, date range, and remediation type.

- **FR-015**: System MUST provide a remediation detail page showing target information, CVE details, remediation type, action description, status timeline with timestamps, and expandable sections for execution logs.

- **FR-016**: System MUST provide a dashboard widget showing remediation counts by status, mean time to remediate, and recent remediation activity.

- **FR-017**: Remediation records MUST be immutable after creation to ensure audit integrity.

- **FR-018**: Remediation records MUST be scoped to organization and project boundaries; cross-project data leakage is not permitted.

- **FR-019**: The central platform MUST NOT execute remediation commands on targets directly; all remediation execution happens through agents deployed on those targets.

### Cross-Cutting Requirements

- **Internationalization**: All user-facing text must be in English, following existing localization conventions. All status labels, error messages, and UI text must be localizable.

- **Accessibility**: The remediation history page and detail page must follow existing accessibility standards, including keyboard navigation, proper ARIA labels for status indicators, and screen reader support for log sections.

- **Validation and Error Handling**: All remediation requests must validate that the target exists, the agent is authorized for that target, and the CVE is valid. Failed remediations must include detailed error information. The system must handle agent disconnection gracefully by timing out in-progress remediations.

- **Security Constraints**: Remediation execution must only occur through authorized agents on their assigned targets. All remediation actions must be logged for audit purposes. The remediation strategy knowledge base must be protected from unauthorized modification.

### Key Entities

- **Remediation Record**: Represents a single remediation attempt for a specific CVE on a specific target. Key attributes include: unique identifier, target reference, agent reference, CVE identifier, remediation type, action description, status (pending, in-progress, success, failed, pending-reboot, skipped), execution logs (pre-check, execution, post-verification), timestamps, error details, and organizational scope.

- **Remediation Strategy**: Represents a mapping from a CVE and operating system to a specific remediation action. Key attributes include: remediation type, action commands, pre-check commands, post-verification commands, and applicable operating system.

- **Plan Step (Remediation)**: Represents a remediation step within an agent execution plan. Key attributes include: step action type, position in the plan sequence, and execution state.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Agents can successfully remediate service-level vulnerabilities (Type A) with a success rate of at least 80% for CVEs that have known remediation strategies in the knowledge base.

- **SC-002**: The time from vulnerability detection to remediation completion is reduced by at least 90% compared to manual remediation workflows (measured in controlled test scenarios).

- **SC-003**: 100% of remediation attempts are recorded with complete audit trails, including execution logs, timestamps, and outcome status.

- **SC-004**: Security operators can view the complete remediation history and filter records by status, target, and date range within 3 seconds for up to 1000 records.

- **SC-005**: Newly replicated agents begin autonomous remediation on their assigned targets within 5 minutes of registration with the central platform.

- **SC-006**: Kernel-level vulnerabilities (Type C) are correctly identified and reported with safe version recommendations in 100% of cases, with no automated fix attempts.

- **SC-007**: Alert notifications are delivered to configured recipients within 1 minute of remediation completion for both success and failure outcomes.

- **SC-008**: The dashboard widget accurately reflects remediation statistics and displays the most recent 5 remediation actions with correct status counts and mean time to remediate.

## Assumptions

- Each agent is responsible for remediating vulnerabilities only on its own assigned target. Cross-target remediation is not supported.

- The remediation strategy knowledge base will initially cover common vulnerabilities for widely-used services on supported operating systems. Coverage will expand over time.

- Type B remediation (reboot required) acknowledges that agents do not yet have persistence across reboots. This capability will be addressed in a separate feature.

- Automatic rollback is not supported in this feature. If a remediation causes issues, manual intervention is required.

- Remediation executes opportunistically as part of agent plan execution, not on a fixed schedule or batch basis.

- The existing agent execution engine, SSH remote execution capability, and alert system will be reused for remediation workflows.

- The remediation strategy knowledge base will be maintained by security operators and updated as new CVEs and fixes become available.

- Agents have sufficient permissions on their assigned targets to install updates, modify configurations, and restart services as needed for remediation.

## Constitution Notes

- The feature spans three modules: ui/ (Angular frontend), api/ (backend platform), and agents/unix/ (autonomous agents). Each module has its own conventions documented in AGENTS.md.

- The agent execution engine already supports step-based workflows with context propagation between steps. The remediation step will integrate with this existing pattern.

- The alert system already has pre-configured conditions for remediation success and failure. This feature will activate those conditions by firing the appropriate events.

- All user-facing text must be in English and follow the existing i18n conventions defined in AGENTS.md.

- The remediation strategy knowledge base is a new capability that requires careful design to support multiple operating systems and package managers. Initial implementation should focus on a single OS family to validate the approach.

- Open question: Should remediation require human approval before execution, or should it execute autonomously as part of the agent plan? The PRD assumes autonomous execution, but this may need validation with stakeholders concerned about production safety.
