---

description: "Task list for Reports Module implementation"
---

# Tasks: Reports Module

**Input**: Design documents from `/specs/017-reports-module/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/reports-api.md, quickstart.md

**Tests**: Included — the TRD/plan explicitly request API unit tests, an API integration
round-trip, UI TestBed specs, and an E2E smoke. Test tasks appear per user story.

**Organization**: Grouped by user story (P1 → P2 → P3) so each is independently
implementable and testable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: US1, US2, US3 (Setup/Foundational/Polish have no story label)
- Absolute-from-repo-root file paths included per task

## Path Conventions

- API base: `api/src/main/java/com/spulido/tfg/domain/report/`
- API tests: `api/src/test/java/com/spulido/tfg/domain/report/`
- UI base: `ui/src/app/pages/reports/`
- i18n: `ui/src/i18n/messages.json`, `ui/src/i18n/messages.es.json`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm conventions and create the new domain/feature folders.

- [x] T001 Review repository guidance in `AGENTS.md`/`CLAUDE.md` and the constitution `.specify/memory/constitution.md`; confirm the `remediation` domain and `remediations` UI feature as the patterns to mirror.
- [x] T002 [P] Create the API domain folder skeleton `api/src/main/java/com/spulido/tfg/domain/report/` with subpackages `controller/`, `db/`, `model/`, `model/dto/`, `services/`, `services/impl/`.
- [x] T003 [P] Create the UI feature folder skeleton `ui/src/app/pages/reports/` with `data-access/` and `feature/reports-list/` + `feature/report-detail/` subfolders.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The `Report` domain model, enums, repository, and shared MTTR helper that ALL
user stories depend on. No user story can begin until this phase is complete.

**⚠️ CRITICAL**: Blocks Phases 3–5.

- [x] T004 [P] Create `GenerationType` enum (`ON_DEMAND`, `SCHEDULED`) in `api/src/main/java/com/spulido/tfg/domain/report/model/GenerationType.java`.
- [x] T005 [P] Create embedded `ReportFilters` (`targetId?`, `from?`, `to?`, `severities: List<String>`, `statuses: List<RemediationStatus>`) in `api/src/main/java/com/spulido/tfg/domain/report/model/ReportFilters.java` per data-model.md.
- [x] T006 [P] Create embedded `ReportSummary` (`vulnerabilitiesBySeverity`, `remediationsByStatus`, `meanTimeToRemediateSeconds`, `targetsCovered`, `totalVulnerabilities`, `totalRemediations`) in `api/src/main/java/com/spulido/tfg/domain/report/model/ReportSummary.java`.
- [x] T007 [P] Create embedded `ReportItem` (`cveId`, `severity?`, `cvssScore?`, `targetId`, `targetName`, `remediationStatus`, `startedAt?`, `completedAt?`) in `api/src/main/java/com/spulido/tfg/domain/report/model/ReportItem.java`.
- [x] T008 Create `Report` document (`@Document(collection = "reports")`, `extends BaseEntity implements ScopedEntity`, `@CompoundIndex` on `{organizationId, projectId}`, fields per data-model.md incl. `organizationId`/`projectId` with getters for `ScopedEntity`) in `api/src/main/java/com/spulido/tfg/domain/report/model/Report.java` (depends on T004–T007).
- [x] T009 Create `ReportRepository extends MongoRepository<Report, String>` with `findByOrganizationIdAndProjectId(org, proj, Pageable)` and `findByIdAndOrganizationIdAndProjectId(id, org, proj)` in `api/src/main/java/com/spulido/tfg/domain/report/db/ReportRepository.java`.
- [x] T010 Extract a shared MTTR helper (mean of `completedAt - startedAt` in seconds over `SUCCESS` records with both timestamps; `0` when empty) reusable by both the report and remediation services, per research.md R4. Place it so both services reach it without a new cross-domain dependency (e.g. a static util in the remediation domain or a `RemediationService` method).
- [x] T011 Replace the hardcoded `meanTimeToRemediateSeconds(0)` in `api/src/main/java/com/spulido/tfg/domain/remediation/services/impl/RemediationServiceImpl.java` (`getStatistics()`, ~line 211) with a call to the T010 helper; populate `RecentActivity.targetName` while there via `TargetRepository`.
- [x] T012 [P] Add report error codes/handling (`REPORT_EMPTY_RESULT`, `REPORT_NO_PROJECT_CONTEXT`) to the existing `@ControllerAdvice` so `POST` empty-result and missing-context map to HTTP 422 with `{code, message}` per contracts/reports-api.md (locate the current advice class first).
- [x] T013 [P] Scaffold the UI data-access layer: `ReportsService` (HttpClient to `{baseUrl}/api/reports` — `generate`, `list`, `get`, Observables suffixed `$`) in `ui/src/app/pages/reports/data-access/reports.service.ts`, and TS models/enums (`Report`, `ReportSummary`, `ReportItem`, `ReportFilters`, `GenerationType`, severity/status) in `ui/src/app/pages/reports/data-access/reports.model.ts` (strict types, no `any`).
- [x] T014 Add the lazy-loaded `reports` child route under the layout shell in `ui/src/app/app-routing.module.ts` pointing at `ui/src/app/pages/reports/reports.routes.ts` (create the routes file); the menu link already targets `['reports']`.

**Checkpoint**: `Report` persistence, tenancy fields, shared MTTR, error mapping, and UI data-access exist — user stories can begin.

---

## Phase 3: User Story 1 — Generate and view a report (Priority: P1) 🎯 MVP

**Goal**: An administrator generates a report (optionally filtered) for the selected
org/project and views it on screen with charts, KPI cards, and a detail table; the report is
persisted immutably. Empty-filter selections show a "no data" message and store nothing.

**Independent Test**: With remediation data present, open Reports → Generate (optionally set
filters) → the detail view renders severity/status charts, MTTR/totals/targets KPIs, and the
detail table. Filters matching nothing show "no data" and add no history row.

### Tests for User Story 1 ⚠️ (write first, ensure they fail)

- [x] T015 [P] [US1] Unit test `ReportServiceImplTest` covering: severity/status grouping, MTTR computation, targets-covered/totals, empty-result → persists nothing, and tenancy (org/project set from `ProjectContext`) in `api/src/test/java/com/spulido/tfg/domain/report/ReportServiceImplTest.java`.
- [x] T016 [P] [US1] Integration test `ReportControllerIntegrationTest`: `POST /api/reports` with data → 201 snapshot; `POST` with no-match filters → 422 `REPORT_EMPTY_RESULT` and no stored report; `GET /api/reports/{id}` returns the snapshot; snapshot stays identical after mutating a source `RemediationRecord` in `api/src/test/java/com/spulido/tfg/domain/report/ReportControllerIntegrationTest.java`.
- [x] T017 [P] [US1] UI spec for `ReportsService.generate$` and the generate→detail flow using TestBed + HttpClientTestingModule spies in `ui/src/app/pages/reports/data-access/reports.service.spec.ts`.

### Implementation for User Story 1

- [x] T018 [P] [US1] Create `ReportGenerateRequest` DTO (`targetId?`, `from?`, `to?`, `severities`, `statuses`) with `jakarta.validation` enforcing `from ≤ to` in `api/src/main/java/com/spulido/tfg/domain/report/model/dto/ReportGenerateRequest.java`.
- [x] T019 [P] [US1] Create `ReportService` interface (`generate`, `findById`) in `api/src/main/java/com/spulido/tfg/domain/report/services/ReportService.java`.
- [x] T020 [US1] Implement `ReportServiceImpl.generate(...)` per research.md + data-model.md: read/validate `ProjectContext` (422 code if absent); load remediation records via `RemediationRecordRepository` for scope+filters (date range via `findByOrganizationIdAndProjectIdAndCompletedAtBetween`, target/status applied in query or in-memory); build a per-generation `Map<cveId, CveEntry>` from `ServiceVulnerabilityRepository` for severity/CVSS (UNKNOWN when absent); resolve `targetName` via `TargetRepository`; build `summary` (using the T010 MTTR helper) + `items`; empty → return empty-result signal and persist nothing; else set org/project explicitly and persist the immutable `Report` (in `api/src/main/java/com/spulido/tfg/domain/report/services/impl/ReportServiceImpl.java`) (depends on T008–T012, T018, T019).
- [x] T021 [US1] Implement `ReportServiceImpl.findById(id)` using `findByIdAndOrganizationIdAndProjectId` (tenant-safe; not-found/other-tenant → 404) in the same impl file.
- [x] T022 [US1] Create `ReportController` with `POST /api/reports` (201 snapshot / 422 empty / 422 no-context) and `GET /api/reports/{id}` (200 / 404), both `@PreAuthorize("isAuthenticated()")`, in `api/src/main/java/com/spulido/tfg/domain/report/controller/ReportController.java` (depends on T020, T021).
- [x] T023 [P] [US1] Build the `report-detail` component (KPI `p-card`s for MTTR/totals/targets; severity chart bar/doughnut + status pie via the theme-aware `agents-metrics` CSS-var approach; detail `p-table` of items) in `ui/src/app/pages/reports/feature/report-detail/`.
- [x] T024 [US1] Build the generate flow in `reports-list` feature: filter form (target dropdown from `TargetsService`, date range, severity multiselect, status multiselect) + "Generate report" action → `ReportsService.generate$` → navigate to `report-detail`; on 422 `REPORT_EMPTY_RESULT` show the empty-state/toast, in `ui/src/app/pages/reports/feature/reports-list/` (depends on T013, T023).
- [x] T025 [P] [US1] Add new English i18n labels (filters, KPI titles, chart titles, "Generate report", "no data" message) via `i18n`/`$localize` to `ui/src/i18n/messages.json` and Spanish translations to `ui/src/i18n/messages.es.json` (reuse the existing "Reports" label).
- [x] T026 [US1] Verify: run `cd api && ./mvnw test` (T015/T016 green) and `cd ui && npm run build` + `npx prettier --check .` (T017 green); walk the quickstart US-1/SC-1/SC-4/SC-5 steps manually.

**Checkpoint**: MVP — generate, view, immutably store a report; empty result handled; MTTR real (dashboard `0` also fixed).

---

## Phase 4: User Story 2 — Consult report history (Priority: P2)

**Goal**: Every generated report is browsable as history (newest first) for the current
project and reopenable exactly as generated; strictly tenant-scoped; immutable.

**Independent Test**: After reports exist, open Reports history → listed newest first for the
current project only → open one → identical values even after source records change.

### Tests for User Story 2 ⚠️

- [x] T027 [P] [US2] Extend `ReportControllerIntegrationTest` (or add `ReportHistoryIntegrationTest`) for `GET /api/reports` paging + `createdAt` desc ordering, and tenant isolation (another org/project's reports never appear; cross-tenant `GET /{id}` → 404) in `api/src/test/java/com/spulido/tfg/domain/report/`.
- [x] T028 [P] [US2] UI spec for the history table load + row navigation to `report-detail` (TestBed + spies) in `ui/src/app/pages/reports/feature/reports-list/reports-list.component.spec.ts`.

### Implementation for User Story 2

- [x] T029 [P] [US2] Create `ReportInfo` history-row DTO (`id`, `title`, `generationType`, `generatedAt`, `generatedBy`, `totalVulnerabilities`, `totalRemediations`, `targetsCovered`) in `api/src/main/java/com/spulido/tfg/domain/report/model/dto/ReportInfo.java`.
- [x] T030 [US2] Add `findHistory(Pageable)` to `ReportService`/`ReportServiceImpl` using `findByOrganizationIdAndProjectId` scoped to `ProjectContext`, sorted `createdAt` desc, mapping to `ReportInfo`.
- [x] T031 [US2] Add `GET /api/reports` (paged, `size` capped at 100, `Page<ReportInfo>`) to `ReportController` per contracts/reports-api.md (depends on T029, T030).
- [x] T032 [US2] Implement the history `p-table` in `reports-list` (paginated, newest first, row click → `report-detail`) wired to `ReportsService.list$`, in `ui/src/app/pages/reports/feature/reports-list/` (depends on T024).
- [x] T033 [P] [US2] Add any new history-table i18n labels (columns, generation-type badges) to `ui/src/i18n/messages.json` + `ui/src/i18n/messages.es.json`.
- [x] T034 [US2] Verify: `cd api && ./mvnw test` (T027) and `cd ui && npm run build` (T028) green; walk quickstart US-2/SC-2/SC-6 (history order, reopen-immutability, tenancy).

**Checkpoint**: History browsing + immutable reopen + tenant isolation proven, on top of the MVP.

---

## Phase 5: User Story 3 — Automatic reports (Priority: P3)

**Goal**: The system periodically generates a `SCHEDULED` report per project that has data,
skipping empty projects, without manual action; configurable and off in tests.

**Independent Test**: With the scheduler enabled and data present, after the interval a new
`SCHEDULED` report appears in each data-bearing project's history; empty projects get none.

### Tests for User Story 3 ⚠️

- [x] T035 [P] [US3] Unit test the scheduled generator: iterates distinct org/project pairs, sets/clears `ProjectContext`, calls `generate(...)` with `SCHEDULED`, and skips projects with no data (mock `generate`) in `api/src/test/java/com/spulido/tfg/domain/report/ScheduledReportGeneratorTest.java`.

### Implementation for User Story 3

- [x] T036 [US3] Create `ScheduledReportGenerator` (`@Component`, `@Scheduled` gated by `reports.scheduler.enabled`) that derives distinct `(organizationId, projectId)` from `remediation_records`, sets `ProjectContext` per pair, calls `ReportService.generate(...)` with `generationType = SCHEDULED`, clears context in `finally`; empties are skipped via the existing empty-result signal, in `api/src/main/java/com/spulido/tfg/domain/report/scheduler/ScheduledReportGenerator.java` (depends on T020).
- [x] T037 [US3] Enable scheduling (`@EnableScheduling` if not already) and add `reports.scheduler.enabled` (default false; false in the test profile) + `reports.scheduler.cron` (sensible default, e.g. daily) to `api/src/main/resources/application.yml` (and the test profile config).
- [x] T038 [P] [US3] Ensure `generationType` is surfaced in the history UI so `SCHEDULED` vs `ON_DEMAND` is distinguishable (badge/column), in `ui/src/app/pages/reports/feature/reports-list/` + i18n labels.
- [x] T039 [US3] Verify: `cd api && ./mvnw test` (T035 green); with a non-test profile + short cron, confirm a `SCHEDULED` report appears for data-bearing projects and none for empty ones (quickstart US-3/FR-009).

**Checkpoint**: All three stories independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [x] T040 [P] Run `npx prettier --write .` in `ui/` and confirm Java import ordering/Lombok conventions in the new `report` domain match the `remediation` domain.
- [x] T041 [P] Confirm all new user-facing text is English-authored with Spanish translations present in both message catalogs; no `@@`-style references.
- [x] T042 Re-run the full quickstart.md verification end-to-end (E2E smoke: seed data → menu Reports → generate → view → appears in history), and confirm ≤3 clicks / ~3s (SC-1).
- [x] T043 Confirm no update/delete endpoint exists for reports and comments are limited to non-obvious logic (immutability + constitution).

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: after Setup — **blocks all user stories**.
- **US1 (Phase 3)**: after Foundational. MVP.
- **US2 (Phase 4)**: after Foundational; builds on US1 UI (`reports-list`, `report-detail`) but the history endpoint/table are independently testable.
- **US3 (Phase 5)**: after Foundational; depends on `ReportService.generate` (T020) from US1.
- **Polish (Phase 6)**: after the desired stories are complete.

### Within Each User Story

- Tests written first and failing before implementation.
- Models → services → endpoints → UI wiring → verify.

### Parallel Opportunities

- Setup: T002, T003 in parallel.
- Foundational: T004–T007 (embedded models) in parallel; T012, T013 in parallel with the model work.
- US1 tests T015–T017 in parallel; then T018/T019/T023/T025 are parallelizable before T020/T022/T024 sequence.
- US2: T027/T028 in parallel; T029 parallel to test writing.
- Across teams: once Foundational is done, US1/US2/US3 can be split (US3 needs T020 from US1).

---

## Parallel Example: User Story 1

```bash
# Tests first (parallel):
Task: "Unit test ReportServiceImplTest in api/.../report/ReportServiceImplTest.java"
Task: "Integration test ReportControllerIntegrationTest in api/.../report/ReportControllerIntegrationTest.java"
Task: "UI spec reports.service.spec.ts in ui/.../reports/data-access/"

# Then parallel implementation pieces:
Task: "ReportGenerateRequest DTO in api/.../report/model/dto/"
Task: "report-detail component in ui/.../reports/feature/report-detail/"
Task: "i18n labels in ui/src/i18n/messages.json + messages.es.json"
```

---

## Implementation Strategy

### MVP First (User Story 1)

1. Phase 1 Setup → 2. Phase 2 Foundational → 3. Phase 3 US1 → **STOP & validate** (generate,
   view, immutable store, empty-result, MTTR fix) → demo.

### Incremental Delivery

- Foundation ready → US1 (MVP) → US2 (history) → US3 (scheduled), each independently testable
  and non-breaking.

---

## Notes

- [P] = different files, no incomplete dependencies.
- Reuse existing repos/patterns; smallest correct change (Constitution III).
- Do not run git commands unless the user explicitly approves them (commit hooks are optional).
- Stop at any checkpoint to validate a story independently.
