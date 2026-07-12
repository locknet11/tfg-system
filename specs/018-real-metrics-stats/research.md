# Research: Real Metrics and Statistics in Graph Views

**Feature**: `018-real-metrics-stats`  
**Date**: 2026-07-12

## Research Questions

### RQ1: What mock data needs replacing, and where?

**Decision**: Four distinct sources of mock/hardcoded data were identified:

1. **`DashboardService`** (`ui/src/app/pages/dashboard/data-access/dashboard.service.ts`) — All three methods (`getKPIs()`, `getCriticalVulnerabilities()`, `getVulnerabilitiesChartData()`) return `of(mockData)` with no HTTP calls.
2. **`AgentsMetricsComponent`** (`ui/src/app/pages/agents/feature/agents-metrics/agents-metrics.component.ts`) — The `metrics` field is initialized with hardcoded literals (`activeAgents: 125`, `detectedVulnerabilities: 348`, etc.). No service call is made.
3. **`RemediationStatistics` frontend model** (`ui/src/app/pages/remediations/data-access/remediations.model.ts`) — The TS interface has flat fields (`successCount`, `failedCount`, etc.) but the backend returns `{ totalCount, byStatus: { SUCCESS: n, ... }, meanTimeToRemediateSeconds, recentActivity: [...] }`. The widget tries to read `stats.successCount` which is always undefined.
4. **Vulnerabilities page** — No statistics/aggregation exists at all; only a flat p-table.

**Rationale**: These were identified by code inspection. The dashboard mock data was likely a placeholder during initial UI development that was never connected.

**Alternatives considered**: Auto-generating dashboard data from mock repositories. Rejected because the spec explicitly requires real data.

---

### RQ2: Does the backend have the raw data for these metrics?

**Decision**: Yes. All four collections already hold the needed data:

| Metric | Source Collection | Key Fields |
|--------|------------------|------------|
| Target count | `targets` (via `TargetRepository`) | `organizationId`, `projectId` |
| Active agent count | `agents` (via `AgentRepository`) | `status`, `organizationId`, `projectId` |
| Fixed vulnerability count | `remediation_records` (via `RemediationRecordRepository`) | `status`, `organizationId`, `projectId` |
| Critical vulnerability list | `service_vulnerabilities` (via `ServiceVulnerabilityRepository`) | `cves.severity`, `fetchedAt` |
| Vulnerability trend (monthly) | `service_vulnerabilities` | `fetchedAt` |
| Remediation status distribution | `remediation_records` | `status` — already computed by `RemediationServiceImpl.getStatistics()` |
| MTTR | `remediation_records` | `startedAt`, `completedAt`, `status` — already computed by `RemediationMetrics.meanTimeToRemediateSeconds()` |
| Agent uptime | `agents` | `status`, `lastConnection` — tracked by `AgentHeartbeatMonitorService` |
| Severity distribution | `service_vulnerabilities` / `cves.severity` | Needs aggregation query |

**Rationale**: Code inspection of all repositories and the `RemediationServiceImpl.getStatistics()` method confirms raw data availability.

**Alternatives considered**: Building a separate metrics collection with pre-aggregated data. Rejected as over-engineering for prototype scale; MongoDB aggregation on existing collections is sufficient.

---

### RQ3: What scoping mechanism do existing endpoints use?

**Decision**: Two patterns exist:

1. **SpEL-based queries in repository interfaces** — Used by `AgentRepository` and `TargetRepository`. Example:
   ```java
   @Query("{ 'organizationId': ?#{T(ProjectContext).getOrganizationId()}, 'projectId': ?#{T(ProjectContext).getProjectId()} }")
   Page<Agent> findAllScoped(Pageable pageable);
   ```
2. **Explicit org/project params passed from service** — Used by `RemediationRecordRepository`. The service calls `ProjectContext.getOrganizationId()`/`getProjectId()` and passes them to:
   ```java
   Page<RemediationRecord> findByOrganizationIdAndProjectId(String orgId, String projId, Pageable pageable);
   ```

**Decision for new endpoints**: Use Pattern 2 (explicit params from service) for all new dashboard and statistics queries. This avoids tight coupling to `ProjectContext` in repository interfaces and makes repositories testable without thread-local state. The existing `RemediationServiceImpl` is the reference implementation.

**Rationale**: Pattern 2 is cleaner for unit testing and is already battle-tested in the remediation domain.

**Alternatives considered**: SpEL-based pattern (Pattern 1). Rejected because SpEL in `@Query` annotations is harder to test and debug.

---

### RQ4: What new API endpoints are needed?

**Decision**: Five new endpoints, plus one frontend-only model fix:

| Endpoint | Method | Purpose | Domain |
|----------|--------|---------|--------|
| `/api/dashboard/kpis` | GET | Return target count, active agent count, fixed vuln count | New `dashboard` domain |
| `/api/dashboard/critical-vulnerabilities` | GET | Return 10 most recent Critical-severity CVE records | New `dashboard` domain |
| `/api/dashboard/vulnerability-trend` | GET | Return monthly vulnerability counts for last N months | New `dashboard` domain |
| `/api/agent/metrics` | GET | Return agent KPIs + weekly vulnerability trend | Extends `agent` controller |
| `/api/vulnerabilities/statistics` | GET | Return severity distribution, optionally filtered | Extends `vulnerability` controller |
| `/api/remediations/statistics` | (exists) | Fix frontend model only — backend is correct | No backend change |

**Rationale**: The dashboard endpoints are grouped under a new `/api/dashboard` prefix to keep them together. Agent metrics and vulnerability statistics extend their respective existing controllers because they are resource-specific aggregations. The remediation statistics backend is already correct — only the frontend model needs alignment.

**Alternatives considered**: One monolithic `/api/statistics` endpoint. Rejected because it would couple unrelated domains and violate the single-responsibility pattern used throughout the codebase.

---

### RQ5: How should vulnerability trend aggregation work?

**Decision**: Group `ServiceVulnerabilityRecord` documents by `fetchedAt` month using a MongoDB aggregation pipeline in the repository. The dashboard endpoint queries the last 6 months by default, and the agent metrics endpoint queries the last 4 weeks.

Monthly aggregation:
```java
// MongoDB aggregation: group by year-month of fetchedAt, count documents
// Return List<VulnerabilityTrendPoint> where each point has { month: "2026-07", count: 42 }
```

Weekly aggregation (agent metrics):
```java
// Same approach, grouping by ISO week of fetchedAt
```

The aggregation uses `fetchedAt` (the timestamp when vulnerability data was fetched from NVD) rather than `createdAt` because `fetchedAt` represents when the vulnerability was actually discovered in the system.

**Rationale**: MongoDB's aggregation framework handles this efficiently without pulling all documents into application memory. `fetchedAt` is already indexed on the collection.

**Alternatives considered**: Application-side grouping (fetch all records, group in Java). Rejected due to memory concerns with large datasets and because MongoDB aggregation is purpose-built for this.

---

### RQ6: How should agent average uptime be computed?

**Decision**: Compute uptime as the percentage of agents currently in ACTIVE status within the current scope. This is a point-in-time metric rather than a historical rolling average, which is simpler and more useful for an operations dashboard.

Formula: `(count of agents with status = ACTIVE) / (total count of agents) * 100`

This aligns with the existing agent status lifecycle (`IN_CREATION → CREATED → ACTIVE → UNRESPONSIVE → KILLED`) managed by `AgentHeartbeatMonitorService`.

**Rationale**: The spec's assumption of a "30-day rolling window" requires historical status snapshots that don't exist in the current data model. A point-in-time active ratio is immediately computable from the existing `AgentRepository` and provides actionable information.

**Alternatives considered**: 
- Historical rolling average from `lastConnection` timestamps. Rejected because we lack historical status-change records, making it impossible to compute true uptime over a window without new data.
- Heartbeat success rate. Rejected because heartbeat data is not persisted as individual records.
- Ratio of `(ACTIVE + CREATED) / total`. Rejected because `CREATED` agents are not yet operational.

---

### RQ7: How to fix the RemediationStatistics frontend/backend mismatch?

**Decision**: Update the frontend `RemediationStatistics` interface to match the backend DTO shape. Existing callers extract individual status counts from the `byStatus` map. No backend change.

New frontend model:
```typescript
export interface RemediationStatistics {
  readonly totalCount: number;
  readonly byStatus: Record<string, number>;
  readonly meanTimeToRemediateSeconds: number;
  readonly recentActivity: RecentActivity[];
}
```

The widget component extracts individual counts via:
```typescript
const successCount = stats.byStatus['SUCCESS'] ?? 0;
const failedCount = stats.byStatus['FAILED'] ?? 0;
// etc.
```

**Rationale**: The backend is correct and already deployed. Changing the backend would break any other consumers. Frontend-only fix is the minimal change.

**Alternatives considered**: Create a separate "flattened" DTO on the backend. Rejected as adding unnecessary indirection.

---

### RQ8: What loading, error, and empty states are needed?

**Decision**: Each metric view needs three non-data states:

1. **Loading**: Show PrimeNG skeleton components (`p-skeleton`) or a spinner while the HTTP request is in flight.
2. **Error**: Show an inline error message with a "Retry" button that re-triggers the HTTP request.
3. **Empty**: When the scope has zero records (e.g., no agents registered yet), show a descriptive empty state message rather than displaying zeros silently or showing stale mock data.

For chart components, loading = skeleton placeholder, error = error message overlaid on chart area, empty = chart with all-zero data and an "No data available" annotation.

**Rationale**: The spec explicitly requires these states (FR-012, FR-013, FR-014) and the existing PrimeNG component library provides the necessary primitives.

**Alternatives considered**: Silent fallback to zeros. Rejected per spec requirement FR-014.

---

## Summary of Technical Decisions

| # | Decision | Rationale |
|---|----------|-----------|
| 1 | New `dashboard` domain for read-only aggregation endpoints | Smallest self-contained addition; avoids coupling unrelated domains |
| 2 | Extend existing controllers for agent/vulnerability statistics | Resource-ownership clarity |
| 3 | Explicit org/project params (Pattern 2) for scoping | Testable without thread-local state; matches remediation pattern |
| 4 | MongoDB aggregation pipeline for trend data | Efficient server-side grouping; uses existing `fetchedAt` index |
| 5 | Point-in-time active ratio for agent uptime | Computable from existing data without new persistence |
| 6 | Frontend-only fix for remediation statistics model | Backend is correct; changing it adds risk |
| 7 | PrimeNG skeleton + error + empty patterns for all views | Consistent UX; spec requirement |
