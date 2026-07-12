# Tasks: Real Metrics and Statistics in Graph Views

**Input**: Design documents from `/specs/018-real-metrics-stats/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/metrics-api.md

**Tests**: Unit tests are requested per the project AGENTS.md ("unit tests deterministic, avoid network/filesystem"). Include unit tests for new services. No E2E tests — manual verification per quickstart.md.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **API**: `api/src/main/java/com/spulido/tfg/domain/`
- **API tests**: `api/src/test/java/com/spulido/tfg/domain/`
- **UI**: `ui/src/app/pages/`
- **i18n**: `ui/src/i18n/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify project state and review existing patterns before implementation

- [x] T001 Review repository guidance in `AGENTS.md`, `.specify/memory/constitution.md`, and relevant skills in `.agents/skills/angular-component/SKILL.md` and `.agents/skills/java-springboot/SKILL.md`
- [x] T002 [P] Verify API builds clean: run `cd api && ./mvnw clean package -DskipTests`
- [x] T003 [P] Verify UI builds clean: run `cd ui && npm ci && npm run build`
- [x] T004 [P] Study existing `RemediationServiceImpl.getStatistics()` in `api/src/main/java/com/spulido/tfg/domain/remediation/services/impl/RemediationServiceImpl.java` as reference pattern for new statistics services
- [x] T005 [P] Study existing `DashboardService` mock data in `ui/src/app/pages/dashboard/data-access/dashboard.service.ts` to understand current mock structure that must be replaced

---

## Phase 2: Foundational (Shared DTO)

**Purpose**: Shared data type used by multiple user stories (US1 Dashboard trend + US2 Agent weekly trend)

**⚠️ CRITICAL**: This shared DTO must exist before US1 trend endpoint and US2 metrics endpoint can be built

- [x] T006 Create `VulnerabilityTrendPoint` DTO in `api/src/main/java/com/spulido/tfg/domain/dashboard/model/dto/VulnerabilityTrendPoint.java` with fields `period: String` and `count: long`, using Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`

**Checkpoint**: Shared DTO ready — user story implementation can now begin

---

## Phase 3: User Story 1 — Dashboard Displays Real-Time KPIs and Charts (Priority: P1) 🎯 MVP

**Goal**: Replace all hardcoded mock data in the Dashboard with live API data — KPI cards, critical vulnerabilities table, and monthly vulnerability bar chart

**Independent Test**: Log in, navigate to Dashboard, verify KPI numbers match Targets/Agents/Remediations pages, critical vuln table shows real CVE data (not CVE-2023-12345), bar chart shows real months/counts

### API — New Dashboard Domain

- [x] T007 [P] [US1] Create `DashboardKpis` DTO in `api/src/main/java/com/spulido/tfg/domain/dashboard/model/dto/DashboardKpis.java` with fields `targetsCount: long`, `activeAgentsCount: long`, `fixedVulnerabilitiesCount: long`, using Lombok
- [x] T008 [P] [US1] Create `CriticalVulnerabilityInfo` DTO in `api/src/main/java/com/spulido/tfg/domain/dashboard/model/dto/CriticalVulnerabilityInfo.java` with fields `serviceKey: String`, `cveId: String`, `description: String`, `serviceName: String`, `cvssScore: Double`, `reportedDate: Instant`, using Lombok
- [x] T009 [US1] Create `DashboardService` interface in `api/src/main/java/com/spulido/tfg/domain/dashboard/services/DashboardService.java` with methods `getKpis()`, `getCriticalVulnerabilities()`, `getVulnerabilityTrend(int months)`
- [x] T010 [US1] Implement `DashboardServiceImpl` in `api/src/main/java/com/spulido/tfg/domain/dashboard/services/impl/DashboardServiceImpl.java` — inject `AgentRepository`, `TargetRepository`, `RemediationRecordRepository`, `ServiceVulnerabilityRepository`; implement `getKpis()` (count targets, active agents, successful remediations scoped to org/project via `ProjectContext`); implement `getCriticalVulnerabilities()` (query `service_vulnerabilities` for records with CRITICAL severity cves, flatten to one entry per CVE, sort by `fetchedAt` desc, limit 10); implement `getVulnerabilityTrend(int months)` (MongoDB aggregation pipeline: `$match` by `fetchedAt` within N months, `$group` by year-month string, `$count`, fill missing months with zero)
- [x] T011 [US1] Create `DashboardController` in `api/src/main/java/com/spulido/tfg/domain/dashboard/controller/DashboardController.java` — `@RestController @RequestMapping("/api/dashboard")`, `@PreAuthorize("isAuthenticated()")`, inject `DashboardService`; add `GET /api/dashboard/kpis`, `GET /api/dashboard/critical-vulnerabilities`, `GET /api/dashboard/vulnerability-trend?months=6` with `@RequestParam(defaultValue = "6")`; validate `months` 1–24, return 422 on missing project context
- [x] T012 [US1] Add `@PreAuthorize("isAuthenticated()")` check on `DashboardController` and validate organization/project context is set before queries; return `ResponseEntity` with appropriate status codes per contracts/metrics-api.md
- [x] T013 [US1] Write unit test for `DashboardServiceImpl` in `api/src/test/java/com/spulido/tfg/domain/dashboard/DashboardServiceImplTest.java` — mock all 4 repositories, verify KPI counts, verify critical vulns sorted/limited, verify trend aggregation with mocked dates

### UI — Dashboard

- [x] T014 [US1] Update `DashboardKPIs` interface in `ui/src/app/pages/dashboard/data-access/dashboard.model.ts` — align with backend `DashboardKpis` DTO: `targetsCount: number`, `activeAgentsCount: number`, `fixedVulnerabilitiesCount: number`; remove unused fields
- [x] T015 [P] [US1] Add `CriticalVulnerability` interface in `ui/src/app/pages/dashboard/data-access/dashboard.model.ts` — fields: `serviceKey: string`, `cveId: string`, `description: string`, `serviceName: string`, `cvssScore: number | null`, `reportedDate: string`
- [x] T016 [P] [US1] Add `VulnerabilityTrendPoint` interface in `ui/src/app/pages/dashboard/data-access/dashboard.model.ts` — fields: `period: string`, `count: number`
- [x] T017 [US1] Rewrite `DashboardService` in `ui/src/app/pages/dashboard/data-access/dashboard.service.ts` — remove all `of(mockData)` returns; replace `getKPIs()` with `this.http.get<DashboardKPIs>(...)`; replace `getCriticalVulnerabilities()` with `this.http.get<CriticalVulnerability[]>(...)`; replace `getVulnerabilitiesChartData()` with `this.http.get<VulnerabilityTrendPoint[]>(...)` and map to chart.js format; use `environment.baseUrl + '/api/dashboard'` as base; keep `@Injectable()` with `providedIn: 'root'` and inject `HttpClient`
- [x] T018 [US1] Update `DashboardComponent` in `ui/src/app/pages/dashboard/dashboard.component.ts` — add loading/error/empty state signals; subscribe to all 3 service methods in `ngOnInit`; remove direct `TargetsService` injection (target count now comes from KPIs endpoint); handle errors with inline retry; use `DestroyRef` for cleanup
- [x] T019 [US1] Update `DashboardComponent` template in `ui/src/app/pages/dashboard/dashboard.component.html` — add `*ngIf` loading state with `p-skeleton` placeholders for KPI cards and chart area; add error state with `p-message` and retry button; update chart data binding to use async data; ensure empty-state shows "0" when counts are zero
- [x] T020 [US1] Verify dashboard feature end-to-end: API tests pass (5/5), API builds clean, UI builds clean: run `cd api && ./mvnw test` for API unit tests, `cd ui && npm run build` for UI build, then manual smoke per quickstart.md verification checklist items 1–9

**Checkpoint**: Dashboard shows real data — KPIs match list views, critical vuln table is real, bar chart reflects actual monthly trends

---

## Phase 4: User Story 3 — Remediation Widget Displays Accurate Statistics (Priority: P1)

**Goal**: Fix the frontend/backend data-model mismatch so the remediation statistics widget shows correct pie chart, status counts, success rate, and MTTR from the already-working backend endpoint

**Independent Test**: Navigate to Dashboard (widget at bottom), verify pie chart segments are proportional, status cards match pie, success rate is correct, MTTR shows human-readable format

### UI — Remediation Widget (Frontend-Only Fix)

- [x] T021 [US3] Update `RemediationStatistics` interface in `ui/src/app/pages/remediations/data-access/remediations.model.ts` — align with backend DTO: replace flat fields (`successCount`, `failedCount`, etc.) with `totalCount: number`, `byStatus: Record<string, number>`, `meanTimeToRemediateSeconds: number`, `recentActivity: RecentActivity[]`; add `RecentActivity` sub-interface with `id`, `cveId`, `targetName`, `status`, `completedAt`
- [x] T022 [US3] Update `RemediationWidgetComponent` in `ui/src/app/pages/remediations/feature/remediation-widget/remediation-widget.component.ts` — extract status counts from `stats.byStatus['SUCCESS']`, `stats.byStatus['FAILED']`, etc. with `?? 0` fallbacks; update `buildChart()` to use extracted values; update `successRate` getter to use `stats.totalCount` and extracted SUCCESS count; add `mttr` computed property that formats `meanTimeToRemediateSeconds` into human-readable string (hours and minutes)
- [x] T023 [US3] Update `RemediationWidgetComponent` template in `ui/src/app/pages/remediations/feature/remediation-widget/remediation-widget.component.html` — replace `statistics.successCount` references with extracted local variables; add MTTR display row; add loading state with `p-skeleton` while statistics are undefined; add error state with retry
- [x] T024 [US3] Verify remediation widget: UI builds clean: run `cd ui && npm run build`, then manual smoke — check pie chart segments match status cards on same widget, success rate is accurate, MTTR displays correctly

**Checkpoint**: Remediation widget pie chart, status counts, success rate, and MTTR all display correct real data

---

## Phase 5: User Story 2 — Agent Metrics Show Live Data (Priority: P2)

**Goal**: Replace hardcoded values in the Agents page Key Metrics section and line chart with live API data

**Independent Test**: Navigate to Agents → Key Metrics tab, verify all 4 metric cards show real values matching agent/remediation/vulnerability counts, line chart uses real weekly data

### API — Agent Metrics Endpoint

- [x] T025 [US2] Create `AgentMetricsResponse` DTO in `api/src/main/java/com/spulido/tfg/domain/agent/model/dto/AgentMetricsResponse.java` — fields: `activeAgents: long`, `totalAgents: long`, `detectedVulnerabilities: long`, `appliedRemediations: long`, `uptimePercentage: double`, `vulnerabilityTrend: List<VulnerabilityTrendPoint>`, using Lombok
- [x] T026 [US2] Add `getMetrics()` method to `AgentService` interface in `api/src/main/java/com/spulido/tfg/domain/agent/services/AgentService.java`
- [x] T027 [US2] Implement `getMetrics()` in `AgentServiceImpl` in `api/src/main/java/com/spulido/tfg/domain/agent/services/impl/AgentServiceImpl.java` — inject `RemediationRecordRepository` and `ServiceVulnerabilityRepository` (in addition to existing `AgentRepository`); query agents scoped via `ProjectContext`; compute `activeAgents` (status=ACTIVE), `totalAgents` (all), `uptimePercentage` (active/total*100), `detectedVulnerabilities` (count of service_vulnerabilities in scope), `appliedRemediations` (count of remediation_records with SUCCESS), `vulnerabilityTrend` (weekly aggregation using same MongoDB pattern as US1 but grouped by ISO week, last 4 weeks)
- [x] T028 [US2] Add `GET /api/agent/metrics` endpoint in `AgentController` in `api/src/main/java/com/spulido/tfg/domain/agent/controller/AgentController.java` — `@GetMapping("/metrics")`, `@PreAuthorize("isAuthenticated()")`, inject and call `AgentService.getMetrics()`, return `ResponseEntity<AgentMetricsResponse>`
- [x] T029 [US2] Write unit test for agent metrics in `api/src/test/java/com/spulido/tfg/domain/agent/AgentMetricsTest.java` — mock repositories, verify `AgentMetricsResponse` fields are populated correctly, verify uptime calculation, verify trend aggregation

### UI — Agent Metrics

- [x] T030 [US2] Update `AgentMetrics` interface in `ui/src/app/pages/agents/data-access/agents.model.ts` — align with backend `AgentMetricsResponse`: `activeAgents: number`, `totalAgents: number`, `detectedVulnerabilities: number`, `appliedRemediations: number`, `uptimePercentage: number`, `vulnerabilityTrend: VulnerabilityTrendPoint[]`
- [x] T031 [US2] Add `getMetrics()` method to `AgentsService` in `ui/src/app/pages/agents/data-access/agents.service.ts` — `this.http.get<AgentMetrics>(...)` calling `/api/agent/metrics`
- [x] T032 [US2] Rewrite `AgentsMetricsComponent` in `ui/src/app/pages/agents/feature/agents-metrics/agents-metrics.component.ts` — replace hardcoded `metrics` field literal with `inject(AgentsService).getMetrics()` call in `ngOnInit`; use signal-based state for loading/error/data; update chart initialization to use real trend data from API response; remove hardcoded sample `vulnerabilitiesOverTime` data
- [x] T033 [US2] Update `AgentsMetricsComponent` template in `ui/src/app/pages/agents/feature/agents-metrics/agents-metrics.component.html` — add loading state with `p-skeleton` for metric cards and chart; add error state with retry; ensure zero values display as "0" not hidden
- [x] T034 [US2] Verify agent metrics feature: run `cd api && ./mvnw test`, `cd ui && npm run build`, then manual smoke per quickstart.md verification checklist items for agent metrics

**Checkpoint**: Agent metrics cards and chart all display real data from the live system

---

## Phase 6: User Story 4 — Vulnerability Views Support Aggregated Statistics (Priority: P2)

**Goal**: Add severity distribution statistics to the Vulnerabilities page with optional filtering

**Independent Test**: Navigate to Vulnerabilities page, verify severity counts match filtered list, apply filter — counts update accordingly

### API — Vulnerability Statistics Endpoint

- [x] T035 [US4] Create `VulnerabilityStatistics` DTO in `api/src/main/java/com/spulido/tfg/domain/vulnerability/model/dto/VulnerabilityStatistics.java` — fields: `totalCount: long`, `bySeverity: Map<String, Long>`, using Lombok
- [x] T036 [US4] Add `getStatistics(String query, String severity)` method to `VulnerabilityController` in `api/src/main/java/com/spulido/tfg/domain/vulnerability/controller/VulnerabilityController.java` — `@GetMapping("/statistics")`, `@PreAuthorize("isAuthenticated()")`, accept optional `@RequestParam query` and `@RequestParam severity`; query `ServiceVulnerabilityRepository` using same filter logic as existing `list()` method; aggregate counts by `cves[].severity` in Java (fetch filtered records, stream over all CVEs, group by severity); always include all 5 severity keys (CRITICAL, HIGH, MEDIUM, LOW, UNKNOWN) with zero for missing
- [x] T037 [US4] Write unit test for vulnerability statistics in `api/src/test/java/com/spulido/tfg/domain/vulnerability/VulnerabilityStatisticsTest.java` — mock repository with records of various severities, verify `bySeverity` map has correct counts, verify filtering by query and severity

### UI — Vulnerability Statistics

- [x] T038 [US4] Add `VulnerabilityStatistics` interface in `ui/src/app/pages/vulnerabilities/data-access/vulnerabilities.model.ts` — fields: `totalCount: number`, `bySeverity: Record<string, number>`
- [x] T039 [US4] Add `getStatistics(query?: string, severity?: string)` method to `VulnerabilitiesService` in `ui/src/app/pages/vulnerabilities/data-access/vulnerabilities.service.ts` — `this.http.get<VulnerabilityStatistics>(...)` with query params
- [x] T040 [US4] Update `VulnerabilitiesComponent` in `ui/src/app/pages/vulnerabilities/feature/vulnerabilities.component.ts` — add `statistics` signal; call `vulnService.getStatistics()` alongside existing `loadRecords()` whenever filters change; extract severity counts for display
- [x] T041 [US4] Update `VulnerabilitiesComponent` template in `ui/src/app/pages/vulnerabilities/feature/vulnerabilities.component.html` — add a severity distribution summary row above the table showing count badges for each severity level (Critical, High, Medium, Low) with color-coded `p-tag` components; add loading skeleton for summary while statistics load; ensure summary updates when search query or severity filter changes
- [x] T042 [US4] Verify vulnerability statistics feature: run `cd api && ./mvnw test`, `cd ui && npm run build`, then manual smoke — check severity counts match filtered table rows, apply filter — counts correct, zero-vuln project shows all zeros

**Checkpoint**: Vulnerabilities page shows severity distribution that updates with filters

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: i18n, accessibility, error handling, and final verification across all stories

- [x] T043 [P] Add new i18n labels to `ui/src/i18n/messages.json` — loading messages, error messages ("Failed to load metrics", "Retry"), empty-state messages ("No targets configured", "No active agents", "No vulnerabilities detected"), MTTR label, success rate label, severity summary labels; all authored in English
- [x] T044 [P] Add Spanish translations for all new labels in `ui/src/i18n/messages.es.json`
- [x] T045 [P] Add `aria-label` attributes to chart canvas elements in `ui/src/app/pages/dashboard/dashboard.component.html`, `ui/src/app/pages/agents/feature/agents-metrics/agents-metrics.component.html`, and `ui/src/app/pages/remediations/feature/remediation-widget/remediation-widget.component.html` for screen reader accessibility
- [x] T046 [P] Ensure severity color-coded tags in all views include text labels (not color-only) for accessibility compliance
- [x] T047 Verify all components handle `ProjectContext` scope changes — ensure metrics refresh when user switches organization/project (FR-011); test by switching between two projects with different data
- [x] T048 Run full API build with tests: `cd api && ./mvnw clean package`
- [x] T049 Run UI lint and build: `cd ui && npx prettier --check . && npm run build`
- [x] T050 Run quickstart.md manual verification checklist — all items for all 4 user stories
- [x] T051 Confirm all user-facing strings are English-authored, no `console.log` in production code, no `any` types in new TypeScript code, no wildcard imports in Java code

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — T006 creates shared DTO used by US1 and US2 trend endpoints
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) — uses `VulnerabilityTrendPoint`
- **User Story 3 (Phase 4)**: No API dependencies — frontend-only fix for existing endpoint; can run in parallel with US1
- **User Story 2 (Phase 5)**: Depends on Foundational (Phase 2) — uses `VulnerabilityTrendPoint`; independent of US1/US3/US4
- **User Story 4 (Phase 6)**: Independent of all other stories — extends VulnerabilityController; no shared DTOs needed
- **Polish (Phase 7)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (Dashboard, P1)**: Can start after Phase 2. No dependency on US2, US3, or US4.
- **US3 (Remediation Widget, P1)**: Can start immediately after Setup — no backend changes. Independent of all other stories.
- **US2 (Agent Metrics, P2)**: Can start after Phase 2. No dependency on US1, US3, or US4.
- **US4 (Vulnerability Stats, P2)**: Can start after Setup. No dependency on any other story.

All four user stories are **independent** — they touch different API controllers, different UI pages, and different data-access services. No cross-story file conflicts.

### Within Each User Story

- API DTOs before service, service before controller
- Models (interfaces) before UI service methods, service methods before component wiring
- Component logic before template updates
- Unit tests alongside or immediately after implementation

### Parallel Opportunities

- **Phase 1**: T002, T003, T004, T005 all run in parallel
- **Phase 3 (US1)**: T007 + T008 run in parallel (different DTO files); T014 + T015 + T016 run in parallel (different types in same model file but can be done together)
- **Phase 4 (US3)**: Entire phase is UI-only — can run in parallel with Phases 3, 5, 6
- **Phase 5 (US2)**: Can run in parallel with Phase 6 (US4) — different controllers, different UI pages
- **Phase 7**: T043, T044, T045, T046 all run in parallel

---

## Parallel Example: US1 API DTOs

```bash
# These two DTOs are independent — different files, no shared dependencies:
Task: "Create DashboardKpis DTO in api/src/.../dashboard/model/dto/DashboardKpis.java"
Task: "Create CriticalVulnerabilityInfo DTO in api/src/.../dashboard/model/dto/CriticalVulnerabilityInfo.java"
```

## Parallel Example: US1 + US3 + US4 (All Independent)

```bash
# Once Setup + Foundational (Phase 1-2) complete, launch in parallel:
# Developer A: US1 Dashboard (Phase 3) — new dashboard domain + dashboard UI
# Developer B: US3 Remediation Widget (Phase 4) — frontend-only model fix
# Developer C: US4 Vulnerability Stats (Phase 6) — extend vuln controller + UI
# US2 Agent Metrics can follow next
```

---

## Implementation Strategy

### MVP First (US1 + US3 — Both P1)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (shared `VulnerabilityTrendPoint` DTO)
3. Complete Phase 3: US1 — Dashboard (real KPIs, critical vulns, monthly chart)
4. Complete Phase 4: US3 — Remediation Widget (fix pie chart + MTTR)
5. **STOP and VALIDATE**: Dashboard and remediation widget both show real data
6. Deploy/demo — this covers the two P1 stories and fixes the most visible broken views

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 (Dashboard) + US3 (Remediation Widget) → Test independently → Demo (MVP! 🎯)
3. Add US2 (Agent Metrics) → Test independently → Demo
4. Add US4 (Vulnerability Statistics) → Test independently → Demo
5. Polish (i18n, a11y, verification) → Final release
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With 2 developers:
1. Both complete Setup + Foundational together
2. Once Foundational is done:
   - Developer A: US1 API (T007-T013) + US2 API (T025-T029)
   - Developer B: US3 UI (T021-T024) + US4 API+UI (T035-T042)
3. Then split remaining UI tasks:
   - Developer A: US1 UI (T014-T020)
   - Developer B: US2 UI (T030-T034)
4. Both converge on Polish (Phase 7)

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable per spec acceptance scenarios
- No new MongoDB collections — all data read from existing `agents`, `targets`, `remediation_records`, `service_vulnerabilities`
- Frontend `RemediationStatistics` fix (US3) is the only story with zero backend changes
- `VulnerabilityTrendPoint` (T006) is the only cross-story dependency — used by US1 monthly trend and US2 weekly trend
- Cross-cutting concerns (i18n, a11y, error handling) consolidated in Phase 7 Polish
- Do not run git commands unless the user explicitly approves them
- Refer to quickstart.md for manual verification checklist after each phase
