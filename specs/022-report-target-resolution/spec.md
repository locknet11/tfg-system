# Feature Specification: Report Target Resolution

**Feature Branch**: `022-report-target-resolution`  
**Created**: 2026-07-14  
**Status**: Draft  
**Input**: User description: "Fix report generation so it works with real remediation data, specifically when a report is scoped by target."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Generate a report scoped to a specific target (Priority: P1)

An operator opens the Reports page, selects a specific target from the target dropdown (for example "vm-ubuntu"), and generates a report. Even though the underlying remediation activity for that target was recorded against the target's host/IP address rather than its catalog identifier, the report includes all remediation records that belong to the selected target.

**Why this priority**: This is the exact failure the user reported. Today, selecting any target yields "No data matches the selected filters" even though remediation data exists, making per-target reporting completely unusable. Restoring it is the core value of this feature.

**Independent Test**: Log in, select an organization/project that has remediation data, choose a target that has remediation records, and generate a report. The report is created and contains those records instead of an empty-result error.

**Acceptance Scenarios**:

1. **Given** remediation records exist whose stored target identifier equals a target's host/IP address, **When** a user generates a report scoped to that target (selected by its catalog identifier), **Then** the report includes those records.
2. **Given** the same records, **When** a user generates a report scoped to a *different* target that has no matching records, **Then** the report returns the existing "no data matches the selected filters" outcome (unchanged behavior).

---

### User Story 2 - See meaningful target names in report rows (Priority: P2)

When an operator views a generated report, each row shows the human-friendly target name (for example "vm-ubuntu") rather than a blank/empty value, so the report is readable and auditable.

**Why this priority**: Even when records are returned (e.g. an unscoped report), every row currently shows an empty target name because the stored identifier does not match a catalog target by primary identifier. Fixing the display materially improves the report's usefulness but is secondary to actually returning data.

**Independent Test**: Generate any report that contains at least one remediation record whose stored target identifier matches a known target's host/IP; confirm the report row displays that target's name.

**Acceptance Scenarios**:

1. **Given** a remediation record whose stored target identifier equals a target's host/IP address, **When** the record appears in a report, **Then** the row shows that target's name.
2. **Given** a remediation record whose stored target identifier matches no known target, **When** the record appears in a report, **Then** the row shows an empty/absent name (graceful fallback, unchanged behavior).

---

### User Story 3 - Preserve all existing report behavior (Priority: P1)

An operator continues to generate unscoped reports and reports filtered by severity, status, and date range exactly as before, with unchanged summaries, persistence, history listing, and organization/project scoping.

**Why this priority**: The fix must not regress any working report behavior. Unscoped generation already works and must keep working; other filters and tenant isolation are security- and correctness-critical.

**Independent Test**: Generate an unscoped report and confirm it returns all matching records; apply severity/status/date filters and confirm results and summary counts match pre-change behavior.

**Acceptance Scenarios**:

1. **Given** remediation data exists, **When** a user generates a report with no target filter, **Then** all matching records are returned as before.
2. **Given** a severity, status, or date-range filter, **When** a user generates a report, **Then** filtering behaves exactly as it did before this change.
3. **Given** a user in one organization/project, **When** they generate or list reports, **Then** they see only their own organization/project data.

---

### Edge Cases

- A remediation record's stored target identifier matches **both** a target's primary identifier and (a different target's) host/IP. Primary-identifier match takes precedence so a record is never attributed to more than one target.
- Two different targets share the same host/IP value. The selected target's own host/IP is used for matching, so records for that address are attributed to the selected target; name resolution prefers a primary-identifier match and otherwise resolves to a target with that host/IP.
- A remediation record has a null/blank stored target identifier. It is treated as unmatched by any target filter and shows no target name (unchanged behavior).
- The selected target has neither a primary-identifier match nor any records for its host/IP. The report returns the existing "no data matches the selected filters" outcome.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: When a report is scoped to a target, the system MUST include remediation records whose stored target identifier equals **either** the selected target's primary identifier **or** the selected target's host/IP address. Host/IP comparison MUST treat equivalent loopback spellings as the same address (IPv4 `127.0.0.0/8`, IPv6 `::1` and its expanded `0:0:0:0:0:0:0:1`, and `localhost`), because agents and target registration may record loopback differently.
- **FR-002**: The system MUST resolve each report row's target name by matching the record's stored target identifier against known targets by primary identifier first, then by host/IP address (using the same loopback-equivalent comparison as FR-001), and MUST leave the name empty when no target matches.
- **FR-003**: Unscoped report generation (no target filter) MUST return the same records it returns today.
- **FR-004**: Severity, status, and date-range filtering, report summaries, persistence, history listing, and organization/project scoping MUST remain unchanged.
- **FR-005**: The system MUST NOT modify existing remediation records, and MUST NOT change how agents report remediation results or which target identifier they send.
- **FR-006**: Target matching for a report MUST stay within the active organization/project scope; records and targets outside that scope MUST NOT be matched or exposed.

### Cross-Cutting Requirements

- **Internationalization**: No new user-facing text is required. The existing "No data matches the selected filters" message and target-name display remain as-is (authored in English).
- **Accessibility**: No UI structure changes; the reports page and report detail rendering are unchanged.
- **Validation and Error Handling**: The empty-result outcome MUST continue to be returned when no records match after all filters, including a target filter that legitimately matches nothing.
- **Security Constraints**: Target resolution MUST honor existing authentication and organization/project isolation; a target filter MUST never surface records or target names from another tenant.

### Key Entities *(include if feature involves data)*

- **Remediation Record**: A recorded remediation action. Relevant attributes: the stored target identifier (currently a host/IP string), the CVE identifier, status, and timestamps. Its target identifier does not necessarily equal a catalog target's primary identifier.
- **Target**: A catalog entry for a system under management. Relevant attributes: primary identifier, human-friendly name, and host/IP address. Reports and the UI reference targets by primary identifier.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Generating a report scoped to a target that has remediation records returns those records in 100% of cases (previously 0% — always empty).
- **SC-002**: Every report row whose record corresponds to a known target displays that target's name instead of a blank value.
- **SC-003**: Unscoped report generation returns the same record count as before the change (no regression).
- **SC-004**: Severity, status, and date-range filtered reports, summaries, history, and tenant isolation produce identical results to before the change.

## Assumptions

- The chosen fix direction is "robust report resolution": reports match remediation records to targets by primary identifier OR host/IP, with no data migration and no agent change.
- A target's host/IP value is the correct join key for records that store an address rather than a catalog identifier.
- Existing remediation records will continue to store the target host/IP; this feature makes reporting tolerant of that, rather than normalizing the stored value.
- The active organization/project already scopes both the remediation records and the targets available for matching.
- Loopback addresses recorded in different notations (IPv4 `127.0.0.1`, IPv6 `::1` / `0:0:0:0:0:0:0:1`, `localhost`) denote the same host and must join together; this is required by the real lab data, where records store `127.0.0.1` while the corresponding target is registered as `0:0:0:0:0:0:0:1`.

## Constitution Notes

- Repository guidance from `CLAUDE.md`/`AGENTS.md` applies: Java/Spring conventions in `api/`, no wildcard imports, Lombok where present, deterministic unit tests with no network/filesystem, and `@ControllerAdvice`-based error mapping (the existing empty-result error handling is reused).
- No shell scripts, templates, or structured text files are introduced, so the script/template boundary rule does not apply here.
- Open question resolved before implementation: precedence when a record matches by both primary identifier and host/IP — primary identifier wins (captured in Edge Cases).
