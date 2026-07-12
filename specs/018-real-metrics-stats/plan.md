# Implementation Plan: Real Metrics and Statistics in Graph Views

**Branch**: `018-real-metrics-stats` | **Date**: 2026-07-12 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/018-real-metrics-stats/spec.md`

## Summary

Replace all hardcoded mock data in the UI's graph and metric views (Dashboard, Agent
Metrics, Remediation Widget, Vulnerabilities) with live data from real backend API
endpoints. Add new aggregation endpoints where none exist, and fix the frontend/backend
data-model mismatch in the remediation statistics widget.

Technical approach: add a lightweight `dashboard` domain in `api/` that provides
aggregated KPIs, critical-vulnerability listings, and monthly vulnerability trends; add
a `statistics` sub-resource to the existing `vulnerability` controller; add an agent
metrics endpoint to the existing `agent` controller; fix the `RemediationStatistics`
frontend model to match the existing backend DTO. On the UI side, replace mock-service
return values with real HTTP calls, using the same `HttpClient` + `environment.baseUrl`
pattern already established in the codebase.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 3, `api/`); TypeScript 5 strict (Angular 17, `ui/`)
**Primary Dependencies**: Spring Boot 3 (Web, Data MongoDB, Security, Validation), Lombok; Angular 17, PrimeNG, chart.js 4, RxJS
**Storage**: MongoDB — reads `agents`, `targets`, `remediation_records`, `service_vulnerabilities` collections; no new collections
**Testing**: JUnit 5 + Mockito (API unit, deterministic); Spring Boot integration test for dashboard endpoint; Angular TestBed + spies (UI)
**Target Platform**: Linux server (API), modern browsers (UI)
**Project Type**: Web application (Angular frontend + Spring Boot backend); `agents/unix/` untouched
**Performance Goals**: Dashboard KPI + chart data loaded and rendered within 2 s; statistics endpoints return in ≈500 ms for typical datasets
**Constraints**: Strict organization/project tenancy via `ProjectContext` on every read; authenticated access only; English-authored i18n with Spanish translations; no new MongoDB collections
**Scale/Scope**: Prototype scale — single administrator role, per-project scoping; ~5 new endpoints, ~6 modified UI components

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] Repository guidance reviewed: `AGENTS.md` and `.agents/skills/angular-component/SKILL.md`, `.agents/skills/java-springboot/SKILL.md`
- [x] English-only rule satisfied for code, UI text, docs, and comments; UI text via `i18n`/`$localize`, translations in the message catalogs
- [x] Proposed design is the smallest correct change — adds a max 3-class dashboard domain, extends 2 existing controllers by one method each, fixes 1 frontend model, and switches 4 mock services to real HTTP calls; no new framework or pattern
- [x] Stack rules captured for affected modules: `api/` (Spring/Lombok conventions, import ordering, `@PreAuthorize`, `jakarta.validation`, `@ControllerAdvice`), `ui/` (strict TS, no `any`, `readonly`/`const`, Observables `$`, grouped imports, centralized HTTP error handling). `agents/unix/` not affected.
- [x] Verification steps identified for every affected module: `./mvnw test` (api), `npm run build` + unit specs (ui), manual E2E smoke
- [x] Git actions identified; explicit user approval required before any git command runs (branch already created)
- [x] Unknown or ambiguous requirements resolved or called out — see research.md

**No constitution violations.** Complexity Tracking table below is intentionally empty.

## Project Structure

### Documentation (this feature)

```text
specs/018-real-metrics-stats/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── metrics-api.md   # New endpoints: dashboard, agent metrics, vulnerability statistics
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```text
api/src/main/java/com/spulido/tfg/domain/dashboard/       # NEW domain
├── controller/
│   └── DashboardController.java       # GET /api/dashboard/kpis, /critical-vulnerabilities, /vulnerability-trend
├── model/dto/
│   ├── DashboardKpis.java            # targetsCount, activeAgentsCount, fixedVulnerabilitiesCount
│   ├── CriticalVulnerabilityInfo.java # CVE summary for the dashboard table
│   └── VulnerabilityTrendPoint.java   # month label + count for the bar chart
└── services/
    ├── DashboardService.java          # interface
    └── impl/
        └── DashboardServiceImpl.java  # queries agents, targets, remediations, vulnerabilities repos

api/src/main/java/com/spulido/tfg/domain/vulnerability/controller/
└── VulnerabilityController.java       # MODIFIED: add GET /api/vulnerabilities/statistics

api/src/main/java/com/spulido/tfg/domain/vulnerability/model/dto/
└── VulnerabilityStatistics.java       # NEW: counts by severity + optional filter params

api/src/main/java/com/spulido/tfg/domain/agent/controller/
└── AgentController.java              # MODIFIED: add GET /api/agent/metrics

api/src/main/java/com/spulido/tfg/domain/agent/model/dto/
└── AgentMetricsResponse.java         # NEW: activeAgents, detectedVulnerabilities, appliedRemediations, avgUptime, vulnTrend

ui/src/app/pages/dashboard/
├── dashboard.component.ts             # MODIFIED: subscribe to real data, handle loading/error/empty
├── dashboard.component.html           # MODIFIED: add loading/error/empty UI, MTTR display
└── data-access/
    ├── dashboard.service.ts           # MODIFIED: replace mock returns with HttpClient calls
    └── dashboard.model.ts             # MODIFIED: update types for real API responses

ui/src/app/pages/remediations/
├── feature/remediation-widget/
│   ├── remediation-widget.component.ts  # MODIFIED: extract from byStatus map, add MTTR
│   └── remediation-widget.component.html # MODIFIED: add MTTR display, loading/error states
└── data-access/
    ├── remediations.service.ts         # UNMODIFIED (already calls real endpoint)
    └── remediations.model.ts           # MODIFIED: align with backend RemediationStatistics DTO

ui/src/app/pages/agents/
├── feature/agents-metrics/
│   ├── agents-metrics.component.ts     # MODIFIED: call metrics API, handle loading/error/empty
│   └── agents-metrics.component.html   # MODIFIED: add loading/error/empty states
└── data-access/
    ├── agents.service.ts               # MODIFIED: add getMetrics() method
    └── agents.model.ts                 # MODIFIED: update AgentMetrics for real API response

ui/src/app/pages/vulnerabilities/
├── feature/
│   ├── vulnerabilities.component.ts    # MODIFIED: load statistics alongside records
│   └── vulnerabilities.component.html  # MODIFIED: add severity summary row
└── data-access/
    ├── vulnerabilities.service.ts      # MODIFIED: add getStatistics() method
    └── vulnerabilities.model.ts        # MODIFIED: add VulnerabilityStatistics interface

ui/src/i18n/messages.json              # MODIFIED: new labels
ui/src/i18n/messages.es.json           # MODIFIED: Spanish translations
```

**Structure Decision**: Web application with separate API and UI modules. A new `dashboard`
domain is the smallest self-contained addition that centralizes three related read-only
endpoints without coupling to any existing domain. The agent statistics and vulnerability
statistics endpoints extend their respective existing controllers to keep ownership
clear. All UI changes follow the established `data-access/` + `feature/` convention.

## Complexity Tracking

> No constitution violations; no entries required.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| —         | —          | —                                    |
