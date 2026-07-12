# Implementation Plan: Reports Module

**Branch**: `017-reports-module` | **Date**: 2026-07-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/017-reports-module/spec.md`

## Summary

Add a Reports capability that lets an authenticated administrator generate an on-screen
security report for their currently selected organization/project, summarizing detected
vulnerabilities (by severity) and applied remediations (by status), the mean time to
remediate (MTTR), and the targets covered, backed by a detail table. Every report is
persisted as an **immutable snapshot** in a new `reports` MongoDB collection so history
never drifts when the underlying records change. Reports can be filtered (target, date
range, severity, status), browsed as history (newest first), reopened exactly as
generated, and generated automatically on a schedule.

Technical approach: introduce a new `report` domain in `api/` mirroring the existing
`remediation` domain layout, and a new lazy-loaded `pages/reports/` feature in `ui/`
following the `remediations` data-access/feature convention. **Reuse** existing tenancy
(`ProjectContext`, `BaseEntity`, `ScopedEntity`), remediation queries
(`RemediationRecordRepository`), CVE metadata (`ServiceVulnerabilityRecord`/`CveEntry`),
target names (`TargetRepository`), and the PrimeNG `p-chart`/`p-table` patterns. Fix the
hardcoded `meanTimeToRemediateSeconds(0)` in `RemediationServiceImpl.getStatistics()` by
extracting a shared MTTR helper and reusing it.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 3, `api/`); TypeScript 5 strict (Angular 17, `ui/`)
**Primary Dependencies**: Spring Boot 3 (Web, Data MongoDB, Security, Validation, Scheduling), Lombok; Angular 17, PrimeNG, chart.js 4, RxJS
**Storage**: MongoDB — new `reports` collection; reuses `remediation_records`, `targets`, and the service-vulnerability/CVE cache collections
**Testing**: JUnit 5 + Mockito (API unit, deterministic, no network/FS); Spring Boot integration test for POST→GET round-trip; Angular TestBed + spies (UI)
**Target Platform**: Linux server (API), modern browsers (UI)
**Project Type**: Web application (Angular frontend + Spring Boot backend); `agents/unix/` is untouched
**Performance Goals**: Report generated and rendered in ≈3 s or less for a typical dataset (SC-1); reachable in ≤3 clicks
**Constraints**: Strict organization/project tenancy on every read and write; immutable snapshots (no update path); authenticated access only; English-authored i18n with Spanish translations
**Scale/Scope**: Prototype scale — single administrator role, per-project reports; three endpoints; two UI screens (history+generate, detail)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Repository guidance reviewed: `AGENTS.md`/`CLAUDE.md` and constitution `.specify/memory/constitution.md`
- [x] English-only rule satisfied for code, UI text, docs, and comments; UI text via `i18n`/`$localize`, translations in the message catalogs
- [x] Proposed design is the smallest correct change and avoids unnecessary abstraction — reuses existing repos, tenancy, and chart/table patterns; adds one domain + one UI feature; no new framework
- [x] Stack rules captured for affected modules: `api/` (Spring/Lombok conventions, import ordering, `@PreAuthorize`, `jakarta.validation`, `@ControllerAdvice`), `ui/` (strict TS, no `any`, `readonly`/`const`, Observables `$`, grouped imports, centralized HTTP error handling). `agents/unix/` not affected.
- [x] Verification steps identified for every affected module: `./mvnw test` (api), `npm run build` + unit specs (ui), manual E2E smoke
- [x] Git actions identified; explicit user approval required before any git command runs (branch already created by the specify hook; commits are optional hooks)
- [x] Unknown or ambiguous requirements resolved or called out — see research.md (CVE-severity resolution, ScopedEntity auto-population gap, scheduled iteration source, empty-result signaling)

**No constitution violations.** Complexity Tracking table below is intentionally empty.

## Project Structure

### Documentation (this feature)

```text
specs/017-reports-module/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── reports-api.md   # POST /api/reports, GET /api/reports, GET /api/reports/{id}
├── checklists/
│   └── requirements.md  # Spec quality checklist (from /speckit.specify)
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```text
api/src/main/java/com/spulido/tfg/domain/report/          # NEW domain (mirrors remediation/)
├── controller/
│   └── ReportController.java            # POST /api/reports, GET /api/reports, GET /api/reports/{id}
├── db/
│   └── ReportRepository.java            # MongoRepository<Report,String> + tenant-scoped finders
├── model/
│   ├── Report.java                      # @Document("reports"), BaseEntity + ScopedEntity
│   ├── GenerationType.java              # enum { ON_DEMAND, SCHEDULED }
│   ├── ReportFilters.java               # embedded
│   ├── ReportSummary.java               # embedded
│   └── ReportItem.java                  # embedded
│   └── dto/
│       ├── ReportGenerateRequest.java   # validated request body
│       ├── ReportInfo.java              # history-row / list projection
│       └── ReportDetail.java            # full snapshot response (or reuse Report)
├── services/
│   └── ReportService.java
└── services/impl/
    └── ReportServiceImpl.java           # generate(), findHistory(), findById() (tenant-scoped)

api/src/main/java/com/spulido/tfg/domain/remediation/...   # MODIFIED
├── services/impl/RemediationServiceImpl.java  # use shared MTTR helper (replace meanTimeToRemediateSeconds(0))
└── (shared MTTR helper location — see research.md; e.g. a small util reused by both services)

api/src/main/java/com/spulido/tfg/domain/report/scheduler/  # NEW (P3)
└── ScheduledReportGenerator.java        # @Scheduled; cadence+enable flag from application.yml (off in tests)

api/src/main/resources/application.yml     # MODIFIED: reports.scheduler.enabled / .cron (default off in tests)

api/src/test/java/com/spulido/tfg/domain/report/...        # NEW tests
├── ReportServiceImplTest.java           # grouping, MTTR, empty-result, tenancy, immutability
└── ReportControllerIntegrationTest.java # POST→GET/{id} round-trip; snapshot stable after source mutation

ui/src/app/pages/reports/                  # NEW feature (mirrors pages/remediations/)
├── reports.routes.ts
├── data-access/
│   ├── reports.service.ts               # HttpClient → {baseUrl}/api/reports (list, get, generate)
│   └── reports.model.ts                 # Report, ReportSummary, ReportItem, ReportFilters, enums
└── feature/
    ├── reports-list/                    # history p-table + "Generate report" action + filter form
    └── report-detail/                   # KPI p-cards + severity/status p-charts + detail p-table

ui/src/app/app-routing.module.ts           # MODIFIED: add lazy 'reports' child route under layout shell
ui/src/i18n/messages.json, messages.es.json # MODIFIED: add new labels (English + Spanish); "Reports" label already exists
```

**Structure Decision**: Web application. The API gains a self-contained `report` domain
that copies the proven `remediation` domain shape (controller/db/model/model.dto/services/services.impl),
plus a small scheduler package for P3 and a one-line-behavior fix in the remediation
service. The UI gains a lazy-loaded `pages/reports/` feature matching the `remediations`
data-access/feature split. No changes to `agents/unix/`, the dashboard, or agent-metrics.

## Complexity Tracking

> No constitution violations; no entries required.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| —         | —          | —                                    |
