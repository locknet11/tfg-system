# PRD — Reports Module

**Product:** Autonomous Cloud Security Platform (TFG)
**Feature:** Reports Module
**Source requirements:** RF-09, RF-10, RF-11
**Date:** 2026-07-12 · **Status:** Draft

## 1. Summary

The platform's agents already detect vulnerabilities and apply remediations on target systems, but there is no way for an administrator to consolidate that activity into a report. This feature adds a **Reports module** where an administrator, within their selected organization and project, generates a security report summarizing the vulnerabilities detected and the remediations applied, views it on screen, and keeps every generated report stored for later audit and follow-up.

This is the last major capability declared in the prototype's scope. Note: a "Reports" menu entry already exists in the UI but currently leads nowhere — this feature makes it real.

## 2. Problem & Motivation

- Vulnerability and remediation activity is scattered across raw records; there is no consolidated, human-readable view for analysis or audit.
- Auditing and follow-up require a point-in-time record of "what was detected and what was fixed" that does not change afterwards.
- Manual reporting is slow and error-prone; the platform should surface this automatically.

## 3. Goals

- Give administrators an on-screen security report of detected vulnerabilities and applied remediations for their project.
- Let them narrow a report by target, date range, severity, and remediation status.
- Preserve every generated report as an immutable historical record for audit.
- Generate reports automatically on a schedule so a baseline history accrues without manual effort.

## 4. Non-Goals (v1)

- **No file download** (PDF/CSV/Excel). Reports are viewed online only.
- No cross-organization or cross-project aggregation. Reports cover the currently selected project only.
- No user/role management or role-based access beyond "authenticated user" (out of the prototype's scope).
- No changes to the existing (mocked) dashboard or agent-metrics screens.

## 5. Users

- **Administrator** — the single authenticated role that operates the platform, selects an organization/project, and consults reports for analysis and audit.

## 6. User Stories

**US-1 (P1) — Generate and view a report.**
As an administrator, I generate a report for my selected project that summarizes vulnerabilities detected (by severity), remediations applied (by status), the mean time to remediate, and the targets covered, and I view it on screen with charts and a detail table.

**US-2 (P2) — Consult report history.**
As an administrator, every report I generate is stored, so I can later browse the list of past reports for my project and reopen any of them exactly as it was generated, for audit and follow-up.

**US-3 (P3) — Automatic reports.**
As an administrator, the system periodically generates a report for my project on its own, so a historical baseline builds up without manual action.

## 7. Functional Requirements (product-level)

| ID | Requirement | Traces to |
|----|-------------|-----------|
| P-FR-01 | The administrator can open a Reports section from the main menu. | RF-09 |
| P-FR-02 | The administrator can generate a report for the selected project, optionally filtered by target, date range, severity, and remediation status. | RF-09, RF-10 |
| P-FR-03 | A report shows: vulnerabilities detected grouped by severity, remediations applied grouped by status, mean time to remediate (MTTR), and the targets covered. | RF-09, RF-10 |
| P-FR-04 | A report includes a detail list backing the summary (per vulnerability: CVE, severity, target, remediation status, timestamps). | RF-09 |
| P-FR-05 | Every generated report is stored and cannot be altered afterwards. | RF-11 |
| P-FR-06 | The administrator can browse a history of stored reports for the project, newest first. | RF-11 |
| P-FR-07 | The administrator can reopen any stored report and see it exactly as generated. | RF-09, RF-11 |
| P-FR-08 | If no data matches the chosen filters, the system says so and does not store an empty report. | RF-09 (alt. course) |
| P-FR-09 | The system can generate reports automatically on a schedule. | RF-10 |
| P-FR-10 | A report only ever contains and shows data from its own organization and project. | Security |

## 8. Non-Functional Requirements

- **Usability:** report reachable in ≤ 3 clicks; readable summary with charts; consistent with existing screens; works on modern browsers.
- **Internationalization:** all text in English source + Spanish, following the existing i18n approach.
- **Accessibility:** keyboard-navigable filters and tables; charts backed by a readable data summary.
- **Security & privacy:** authenticated access only; strict organization/project isolation.
- **Traceability/audit:** stored reports are immutable point-in-time snapshots.

## 9. Success Criteria

- **SC-1:** An administrator generates and views a project report in ≤ 3 clicks and under ~3 seconds for a typical dataset.
- **SC-2:** 100% of generated reports appear in history and reopen with values identical to generation time.
- **SC-3:** Summary counts match the underlying activity for the same filters.
- **SC-4:** MTTR is shown and is non-zero whenever successful remediations with start/end times exist.
- **SC-5:** Empty-filter selections show a "no data" message and store nothing.
- **SC-6:** No report ever exposes another organization's or project's data.

## 10. Assumptions & Dependencies

- Organization and project come from the existing project selector; the report is implicitly scoped to that selection plus an optional in-page target filter.
- Depends on remediation and vulnerability data already produced by the agents.
- "General state of the targets" is represented by the covered targets and their current status.
- Download/export can be added later without reworking the stored-report model.

## 11. Open Questions

- Default cadence and on/off switch for automatic report generation (US-3).
- Whether very large reports need a cap on the number of detail rows shown/stored.

---

# TRD — Reports Module

**Feature branch (proposed):** `017-reports-module`
**Companion PRD:** Reports Module (above)
**Date:** 2026-07-12 · **Status:** Draft

## 1. Scope & Approach

Add a new `report` domain in the API and a new `reports` feature in the UI. **Reuse existing aggregation and tenancy machinery** rather than building new querying. A report is generated by aggregating existing remediation + vulnerability data into an **immutable snapshot document**, persisted in a new `reports` collection, and rendered online from that stored snapshot. No file export in v1.

## 2. Current-State Findings (what exists / what's missing)

Reuse:
- `api/.../remediation/controller/RemediationController.java` → `/api/remediations` (paged list, filters by `status`/`targetId`), `/{id}` (detail + logs), `/statistics`.
- `RemediationServiceImpl.getStatistics()` already computes `byStatus` counts + recent activity.
- `RemediationRecordRepository` already has `findByOrganizationIdAndProjectIdAndCompletedAtBetween` (date range) and `...AndCveId` (unused today).
- `vulnerability/.../ServiceVulnerabilityRecord` + `CveEntry` — global CVE cache with `severity`/`cvssScore`, joined by `cveId`.
- Tenancy: thread-local `common/context/ProjectContext` (org+project); base classes `BaseEntity`, `ScopedEntity`.
- UI: chart patterns in `pages/dashboard`, `pages/agents/.../agents-metrics`, `pages/remediations/.../remediation-widget` (PrimeNG `p-chart`, chart.js 4 — bar/line/pie); tables via `p-table`.

Gaps to close:
- **The "Reports" menu link is dead:** `ui/.../layout/feature/menu/menu.component.ts` sets `routerLink: ['reports']`, but there is **no `reports` route and no `pages/reports/` folder**. The `"Reports"` i18n string already exists in `messages.json` + `messages.es.json`.
- **MTTR is hardcoded to `0`** in `RemediationServiceImpl.getStatistics()` (`meanTimeToRemediateSeconds(0)`); `RecentActivity.targetName` is never populated.
- No persisted "report" concept exists.

## 3. Data Model

New collection `reports` (document `Report extends BaseEntity implements ScopedEntity`), compound index `{ organizationId, projectId }`, following the `RemediationRecord` pattern:

```
Report
  id                    String
  organizationId        String        (scoped)
  projectId             String        (scoped)
  title                 String
  generationType        enum { ON_DEMAND, SCHEDULED }
  generatedAt           Instant
  generatedBy           String        (user id/email)
  filters               ReportFilters
  summary               ReportSummary
  items                 List<ReportItem>

ReportFilters
  targetId              String?       // optional
  from                  Instant?
  to                    Instant?
  severities            List<String>
  statuses              List<RemediationStatus>

ReportSummary
  vulnerabilitiesBySeverity   Map<String, Long>   // CRITICAL/HIGH/MEDIUM/LOW/UNKNOWN
  remediationsByStatus        Map<String, Long>   // reuse RemediationStatus names
  meanTimeToRemediateSeconds  long
  targetsCovered              int
  totalVulnerabilities        long
  totalRemediations           long

ReportItem
  cveId                 String
  severity              String?       // from CveEntry; UNKNOWN if not cached
  cvssScore             Double?
  targetId              String
  targetName            String
  remediationStatus     String
  startedAt             Instant?
  completedAt           Instant?
```

Immutability: writes only on creation; no update endpoint. Snapshot stores computed values so history never drifts when underlying records change.

## 4. Backend Design (`api/.../domain/report/`)

Mirror the existing domain layout (`controller` / `model` / `db` / `services` / `services/impl` / `model/dto`).

**Endpoints** (all `@PreAuthorize("isAuthenticated()")`, scoped via `ProjectContext`):

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/reports` | Generate + persist a report from filter body; returns the created report (or 200 "no data" signal / 422 if empty). |
| `GET`  | `/api/reports` | Paged history for the active org+project, `createdAt` desc. |
| `GET`  | `/api/reports/{id}` | Full stored snapshot (KPIs + items). |

**`ReportRepository extends MongoRepository<Report, String>`**
- `Page<Report> findByOrganizationIdAndProjectId(org, proj, pageable)`
- `Optional<Report> findByIdAndOrganizationIdAndProjectId(id, org, proj)` (tenant-safe fetch)

**`ReportService.generate(ReportGenerateRequest)`** algorithm:
1. Read `orgId`/`projectId` from `ProjectContext`; reject if absent.
2. Load remediation records for the scope + filters, reusing `RemediationRecordRepository` (use `findByOrganizationIdAndProjectIdAndCompletedAtBetween` for date range; apply target/status in query or in-memory).
3. For each record, resolve severity/CVSS by looking up `cveId` in `ServiceVulnerabilityRepository`/`CveEntry` (UNKNOWN when absent); resolve `targetName` via `TargetRepository`.
4. Build `summary` (group counts, targets covered, totals) and `items`.
5. **Compute MTTR** = mean of `completedAt - startedAt` over `SUCCESS` records with both timestamps. Extract this into a shared helper and **reuse it to fix the `0` in `getStatistics()`**.
6. If no items match → return an empty-result signal, persist nothing (P-FR-08).
7. Otherwise persist the `Report` snapshot and return it.

**Scheduled generation (P3):** a `@Scheduled` component iterates projects with data and calls `generate(...)` with `generationType = SCHEDULED`; cadence + enable flag in `application.yml` (default off in tests). Skip projects with no data.

**Validation/errors:** `jakarta.validation` on the request (`from ≤ to`); errors via existing `@ControllerAdvice`; new error codes for empty-result / missing-context.

## 5. Frontend Design (`ui/src/app/pages/reports/`)

Follow the `data-access / feature` convention used by `remediations`:

```
pages/reports/
  reports.routes.ts
  data-access/
    reports.service.ts     // HttpClient → {baseUrl}/api/reports (list, get, generate)
    reports.model.ts       // Report, ReportSummary, ReportItem, ReportFilters, enums
  feature/
    reports-list/          // history table (p-table) + "Generate report" action + filter form
    report-detail/         // online view: KPI p-cards + severity/status p-charts + detail p-table
```

- **Routing:** add a `reports` child route under the layout shell in `app-routing.module.ts` (lazy-loaded). The menu link already targets `['reports']`.
- **Generate flow:** filter form (target dropdown from `TargetsService`, date range, severity multiselect, status multiselect) → `POST /api/reports` → navigate to `report-detail`; on empty result show the toast/empty-state ("no data for selected filters").
- **Charts:** reuse the theme-aware chart approach from `agents-metrics` (reads CSS vars); severity → bar/doughnut, status → pie; KPI cards for MTTR / totals / targets covered.
- **History:** `p-table`, paginated, newest first, row → `report-detail`.
- **i18n:** all new labels in English via `$localize` / `i18n`, added to `messages.json` + `messages.es.json`.

## 6. Security & Tenancy

- All endpoints authenticated; every query filtered by `ProjectContext` org+project; `{id}` fetch is tenant-scoped (`findByIdAndOrganizationIdAndProjectId`) to prevent cross-tenant reads.
- Snapshots contain only data the caller was already authorized to see. No secrets introduced.

## 7. Reuse Summary (do-not-rebuild)

| Need | Reuse |
|------|-------|
| Remediation data + date range | `RemediationRecordRepository` (existing queries) |
| CVE severity/CVSS | `ServiceVulnerabilityRepository` + `CveEntry` |
| Target names/state | `TargetRepository` / `Target.status` |
| Tenancy | `ProjectContext`, `ScopedEntity`, `BaseEntity` |
| MTTR | new shared helper, also fixes `getStatistics()` |
| UI charts/tables | dashboard / agents-metrics / remediation-widget patterns; PrimeNG `p-chart`, `p-table` |

## 8. Testing / Verification

- **API unit:** aggregation grouping, MTTR computation, empty-result (no persist), tenant isolation, immutability (no update path). Deterministic, no network/FS.
- **API integration:** `POST` then `GET`/`GET {id}` round-trip; verify snapshot renders identically after mutating source records.
- **UI:** TestBed + spies for `ReportsService`; filter form → generate → detail; empty-result message; history navigation.
- **E2E smoke:** seed remediation data → menu "Reports" → generate → view → appears in history.

## 9. Constitution / Conventions

- Any multi-line/structured text (e.g., a future export template) lives in resource templates (FreeMarker in `api/`), never inline-built.
- TS strict, no `any`, Observables suffixed `$`; Java follows existing Spring/Lombok conventions and import ordering; smallest reasonable change; English-authored i18n.

## 10. Rollout Order

1. **US-1** (P1): `Report` model + repo + `POST /api/reports` + MTTR fix + reports-list generate + report-detail view.
2. **US-2** (P2): history endpoints + list UI + immutability guarantees.
3. **US-3** (P3): scheduled generation.
