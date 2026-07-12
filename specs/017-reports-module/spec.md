# Feature Specification: Reports Module

**Feature Branch**: `017-reports-module`  
**Created**: 2026-07-12  
**Status**: Draft  
**Input**: User description: "Reports Module for the Autonomous Cloud Security Platform (TFG). Source requirements: RF-09, RF-10, RF-11. Administrators generate, view and store immutable security reports summarizing detected vulnerabilities and applied remediations for the selected organization and project, with history and scheduled generation."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate and view a report (Priority: P1)

As an administrator, within my currently selected organization and project I generate a security report that summarizes the vulnerabilities detected (grouped by severity), the remediations applied (grouped by status), the mean time to remediate, and the targets covered. The report opens on screen with charts and a detail table so I can analyze the security posture of my project at a glance. Before generating I may narrow the report by target, date range, severity, and remediation status.

**Why this priority**: This is the core value of the feature and the minimum viable product. Without it there is no report at all. It turns the existing (dead) "Reports" menu entry into a working capability and gives administrators the consolidated, human-readable view they lack today. It is independently valuable even if history and scheduling are never added.

**Independent Test**: With remediation and vulnerability data present for a project, an administrator opens Reports, optionally sets filters, triggers generation, and sees a report with severity/status breakdowns, MTTR, targets covered, and a backing detail table. Delivers value on its own.

**Acceptance Scenarios**:

1. **Given** an administrator with a selected organization and project that has remediation activity, **When** they open the Reports section and generate a report without filters, **Then** the system shows vulnerabilities grouped by severity, remediations grouped by status, the mean time to remediate, the targets covered, and a detail table of the underlying items.
2. **Given** the report generation screen, **When** the administrator sets a target, date range, severity, and/or remediation-status filter and generates, **Then** the resulting report reflects only data matching those filters.
3. **Given** a generated report on screen, **When** the administrator reviews the detail list, **Then** each row shows at least the CVE identifier, severity, target, remediation status, and relevant timestamps.
4. **Given** filters that match no data, **When** the administrator generates, **Then** the system displays a clear "no data for the selected filters" message and stores nothing.
5. **Given** successful remediations that have both a start and an end time, **When** a report is generated over them, **Then** the mean time to remediate is shown and is greater than zero.

---

### User Story 2 - Consult report history (Priority: P2)

As an administrator, every report I generate is stored permanently and unchanged, so I can later browse the list of past reports for my project (newest first) and reopen any of them exactly as it was generated, for audit and follow-up.

**Why this priority**: Audit and follow-up are the stated motivation behind the feature, and immutability is required for a trustworthy point-in-time record. It builds directly on P1 (there must be reports before there is a history) but is a distinct, independently testable slice.

**Independent Test**: After one or more reports exist, the administrator opens the Reports history, sees them listed newest first for the current project, opens one, and sees identical values to when it was generated — even if the underlying remediation/vulnerability records have since changed.

**Acceptance Scenarios**:

1. **Given** an administrator has generated one or more reports for a project, **When** they open the Reports history, **Then** the reports for that project are listed newest first.
2. **Given** a stored report, **When** the administrator reopens it, **Then** it displays exactly the values captured at generation time.
3. **Given** a stored report, **When** the underlying remediation or vulnerability records are later modified, **Then** reopening the report still shows the original captured values (immutable snapshot).
4. **Given** an administrator is in Organization A / Project X, **When** they view the history, **Then** only reports belonging to Organization A / Project X are shown.

---

### User Story 3 - Automatic reports (Priority: P3)

As an administrator, the system periodically generates a report for my project on its own, so a historical baseline accrues without any manual action.

**Why this priority**: It improves long-term audit coverage but is not required for an administrator to obtain value from generating and reviewing reports on demand. It layers on top of P1/P2 and can ship last.

**Independent Test**: With scheduled generation enabled and data present for a project, after the scheduled interval elapses a new report tagged as automatically generated appears in that project's history without any user action, and is skipped for projects with no data.

**Acceptance Scenarios**:

1. **Given** scheduled generation is enabled and a project has qualifying data, **When** the schedule fires, **Then** a new report is created for that project and appears in its history, marked as automatically generated.
2. **Given** a project has no qualifying data, **When** the schedule fires, **Then** no empty report is created for that project.
3. **Given** an administrator views the history, **When** an automatic and a manual report are both present, **Then** they can distinguish which reports were generated automatically versus on demand.

---

### Edge Cases

- **No selection**: If no organization/project is selected, report generation and history are unavailable (or clearly prompt the administrator to select a project first).
- **Empty result**: Filters (or an empty project) matching no data produce a "no data" message and persist nothing; they do not create a blank report in history.
- **Invalid date range**: A date range whose start is after its end is rejected with a clear validation message before any report is generated.
- **Missing CVE metadata**: When a vulnerability's severity/score is not available in the catalog, the report still lists the item with severity shown as "unknown" rather than failing.
- **Missing timestamps for MTTR**: Remediations lacking a start or end time are excluded from the mean-time-to-remediate calculation but still counted in status breakdowns.
- **Large datasets**: A report over an unusually large amount of activity still renders within the usability target; if a detail-row cap is applied, the summary counts remain accurate and the cap is disclosed. *(See Assumptions.)*
- **Concurrent modification**: Underlying records changing during or after generation never alter an already-stored report.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The administrator MUST be able to open a Reports section from the main menu. *(P-FR-01, RF-09)*
- **FR-002**: The administrator MUST be able to generate a report for the currently selected organization and project, optionally filtered by target, date range, severity, and remediation status. *(P-FR-02, RF-09, RF-10)*
- **FR-003**: A generated report MUST show vulnerabilities detected grouped by severity, remediations applied grouped by status, the mean time to remediate (MTTR), and the targets covered. *(P-FR-03, RF-09, RF-10)*
- **FR-004**: A generated report MUST include a detail list backing the summary, with at least CVE identifier, severity, target, remediation status, and relevant timestamps per item. *(P-FR-04, RF-09)*
- **FR-005**: Every generated report MUST be stored and MUST NOT be alterable after creation (immutable point-in-time snapshot). *(P-FR-05, RF-11)*
- **FR-006**: The administrator MUST be able to browse a history of stored reports for the current project, ordered newest first. *(P-FR-06, RF-11)*
- **FR-007**: The administrator MUST be able to reopen any stored report and see it exactly as it was generated. *(P-FR-07, RF-09, RF-11)*
- **FR-008**: When no data matches the chosen filters, the system MUST inform the administrator and MUST NOT store an empty report. *(P-FR-08, RF-09 alt. course)*
- **FR-009**: The system MUST be able to generate reports automatically on a schedule, marking them as automatically generated, and MUST skip projects with no qualifying data. *(P-FR-09, RF-10)*
- **FR-010**: A report MUST only ever contain and display data from its own organization and project. *(P-FR-10, Security)*
- **FR-011**: The system MUST compute MTTR as the mean elapsed time of successful remediations that have both a start and an end time, and MUST show a non-zero value whenever such remediations exist. *(SC-4)*
- **FR-012**: The Reports section MUST be reachable, and a report generated and viewed, within 3 clicks from the main screen. *(NFR usability, SC-1)*

### Cross-Cutting Requirements

- **Internationalization**: All new user-facing text MUST be authored in English and provided in Spanish as well, following the existing i18n approach. The "Reports" menu label already exists in the message catalogs and MUST be reused.
- **Accessibility**: Filter controls and tables MUST be keyboard-navigable; charts MUST be backed by a readable data summary (counts/values) so the information is available without relying on the chart visuals alone.
- **Validation and Error Handling**: Report generation input MUST be validated (e.g., start date not after end date); empty-result and missing-project-context conditions MUST return clear, user-friendly messages rather than errors or blank reports.
- **Security Constraints**: All Reports capabilities MUST require an authenticated user. Every read and every generation MUST be strictly scoped to the caller's active organization and project; reopening a specific stored report MUST be tenant-scoped so no administrator can read another organization's or project's report. No secrets are introduced or exposed.

### Key Entities *(include if feature involves data)*

- **Report**: An immutable, point-in-time snapshot generated for one organization/project. Holds a title, how it was generated (on demand vs. automatic), when and by whom it was generated, the filters used, the computed summary, and the backing detail items. Belongs to exactly one organization and one project.
- **Report Filters**: The optional narrowing criteria captured with a report — target, date range (from/to), set of severities, set of remediation statuses.
- **Report Summary**: The computed roll-up stored on a report — vulnerability counts by severity, remediation counts by status, mean time to remediate, number of targets covered, and totals.
- **Report Item**: One backing detail row — CVE identifier, severity, CVSS score (when known), target (id and name), remediation status, and start/end timestamps.
- **Remediation activity** (existing): The source records of applied remediations, including status and timing, from which reports are aggregated.
- **Vulnerability / CVE catalog** (existing): The source of severity and score for a given CVE, joined into report items.
- **Target** (existing): The system a remediation was applied to; supplies the target name and current state used for "targets covered".

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An administrator can generate and view a project report in 3 clicks or fewer, and it renders in about 3 seconds or less for a typical dataset.
- **SC-002**: 100% of generated reports appear in the project's history and, when reopened, show values identical to those at generation time.
- **SC-003**: For any given set of filters, the report's summary counts match the underlying activity for those same filters.
- **SC-004**: The mean time to remediate is shown and is non-zero whenever successful remediations with both start and end times exist.
- **SC-005**: Selecting filters that match no data shows a "no data" message and stores nothing (history count is unchanged).
- **SC-006**: No report ever exposes another organization's or project's data, verified across generation, history listing, and single-report reopen.

## Assumptions

- Organization and project are taken from the existing project selector; a report is implicitly scoped to that selection plus an optional in-page target filter. There is no cross-organization or cross-project aggregation in v1.
- The single authenticated "administrator" role is the only actor; there is no role-based access control beyond "authenticated user" in v1.
- Reports are viewed online only; there is no file download/export (PDF/CSV/Excel) in v1, and adding one later must not require reworking the stored-report model.
- Reports depend on remediation and vulnerability data already produced by the platform's agents; with no such data a project simply yields "no data".
- "General state of the targets" is represented by the covered targets and their current status, not a separate health computation.
- Default cadence and an on/off switch for automatic generation are configurable, and automatic generation defaults to off in test environments. *(Resolves PRD open question on cadence.)*
- If very large reports require a cap on the number of detail rows shown/stored, summary counts remain computed over the full matching set and the cap is disclosed to the administrator. A concrete cap value, if adopted, is a configuration detail deferred to planning. *(Resolves PRD open question on row caps.)*
- The existing (mocked) dashboard and agent-metrics screens are unchanged by this feature.

## Constitution Notes

- Repository guidance from `AGENTS.md`/`CLAUDE.md` applies: UI is Angular 17 with strict TypeScript and Prettier; API is Spring Boot 3; i18n text authored in English with Spanish translations in the message catalogs; no secrets committed; smallest reasonable change.
- The script/template boundary rule applies to any future multi-line/structured text (e.g., a later export template), which must live in resource template files rather than being built inline — noted for the deferred export capability, not required by v1.
- Open questions that were resolved by reasonable defaults (automatic-generation cadence/toggle, detail-row cap) are recorded in Assumptions; none remain blocking for planning.
