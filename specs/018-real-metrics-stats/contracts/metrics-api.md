# API Contract: Metrics & Statistics Endpoints

**Feature**: `018-real-metrics-stats`  
**Base paths**: `/api/dashboard`, `/api/agent/metrics`, `/api/vulnerabilities/statistics`

All endpoints are `@PreAuthorize("isAuthenticated()")` and scoped to the active
organization/project via `ProjectContext`. Requests without an active project context
return 422.

Timestamps are ISO-8601 instants (UTC). Enum values:
- Severity: `CRITICAL | HIGH | MEDIUM | LOW | UNKNOWN`
- `RemediationStatus`: `PENDING | IN_PROGRESS | SUCCESS | FAILED | PENDING_REBOOT | SKIPPED`
- `AgentStatus`: `IN_CREATION | CREATED | ACTIVE | UNRESPONSIVE | KILLED`

---

## GET `/api/dashboard/kpis` — Dashboard KPIs

Returns the three headline KPI numbers for the Dashboard overview cards.

### Response — **200 OK**

```json
{
  "targetsCount": 12,
  "activeAgentsCount": 5,
  "fixedVulnerabilitiesCount": 23
}
```

### Data Sources

- `targetsCount` — count of documents in `targets` collection scoped to org/project
- `activeAgentsCount` — count of `agents` where `status = ACTIVE` in scope
- `fixedVulnerabilitiesCount` — count of `remediation_records` where `status = SUCCESS` in scope

### Errors

- **401 Unauthorized** — not authenticated
- **422 Unprocessable Entity** — no active project context

---

## GET `/api/dashboard/critical-vulnerabilities` — Critical Vulnerability List

Returns the 10 most recent Critical-severity vulnerabilities for the Dashboard table.

### Response — **200 OK**

```json
[
  {
    "serviceKey": "ssh:22",
    "cveId": "CVE-2025-12345",
    "description": "Buffer overflow in SSH daemon",
    "serviceName": "openssh-server",
    "cvssScore": 9.8,
    "reportedDate": "2026-07-10T08:00:00Z"
  }
]
```

### Logic

- Query `service_vulnerabilities` where any `cves[].severity = "CRITICAL"`
- Sort by `fetchedAt` descending
- Limit to 10 results
- Flatten: one entry per Critical CVE (not per service record)

### Errors

- **401** — not authenticated
- **422** — no active project context

---

## GET `/api/dashboard/vulnerability-trend` — Monthly Trend Chart Data

Returns monthly vulnerability detection counts for the bar chart.

### Query Parameters

| Param | Type | Default | Notes |
|-------|------|---------|-------|
| `months` | int | `6` | Number of months to include (1–24) |

### Response — **200 OK**

```json
[
  { "period": "2026-02", "count": 12 },
  { "period": "2026-03", "count": 19 },
  { "period": "2026-04", "count": 3 },
  { "period": "2026-05", "count": 5 },
  { "period": "2026-06", "count": 2 },
  { "period": "2026-07", "count": 8 }
]
```

### Logic

- MongoDB aggregation: `$match` by `fetchedAt` within the last N months (from now), `$group` by `{ $dateToString: { format: "%Y-%m", date: "$fetchedAt" } }`, `$count`
- Months with zero vulnerabilities are included (count = 0) for chart continuity
- Sorted chronologically (oldest first)

### Errors

- **400** — `months` outside 1–24 range
- **401** — not authenticated
- **422** — no active project context

---

## GET `/api/agent/metrics` — Agent KPIs & Trend

Returns aggregated agent metrics for the Key Metrics section.

### Response — **200 OK**

```json
{
  "activeAgents": 8,
  "totalAgents": 10,
  "detectedVulnerabilities": 142,
  "appliedRemediations": 87,
  "uptimePercentage": 80.0,
  "vulnerabilityTrend": [
    { "period": "2026-W27", "count": 15 },
    { "period": "2026-W28", "count": 22 },
    { "period": "2026-W29", "count": 18 },
    { "period": "2026-W30", "count": 25 }
  ]
}
```

### Data Sources

- All counts scoped to org/project
- `activeAgents` — count of `agents` where `status = ACTIVE`
- `totalAgents` — count of all `agents` in scope
- `detectedVulnerabilities` — count of `service_vulnerabilities` in scope
- `appliedRemediations` — count of `remediation_records` where `status = SUCCESS`
- `uptimePercentage` — `(activeAgents / totalAgents) * 100.0`, 0 if no agents
- `vulnerabilityTrend` — weekly aggregation over last 4 weeks (ISO week, `fetchedAt`)

### Errors

- **401** — not authenticated
- **422** — no active project context

---

## GET `/api/vulnerabilities/statistics` — Severity Distribution

Returns counts by severity, optionally filtered by search query or severity.

### Query Parameters

| Param | Type | Default | Notes |
|-------|------|---------|-------|
| `query` | string | — | Service name search (matches `serviceName` via regex) |
| `severity` | string | — | Filter to specific severity level |

### Response — **200 OK**

```json
{
  "totalCount": 27,
  "bySeverity": {
    "CRITICAL": 3,
    "HIGH": 8,
    "MEDIUM": 10,
    "LOW": 5,
    "UNKNOWN": 1
  }
}
```

### Logic

- When filters are present, counts reflect the filtered subset only
- Matches the same filtering logic as `GET /api/vulnerabilities` (list endpoint)
- Severity keys always present (zero counts included for missing severities)

### Errors

- **401** — not authenticated
- **422** — no active project context

---

## Existing Endpoint (Contract Preserved)

### GET `/api/remediations/statistics`

**No change to this endpoint.** The contract is already correct. See `specs/017-reports-module/contracts/reports-api.md` for the referenced `RemediationStatistics` shape.

The fix is **frontend-only**: align `ui/src/app/pages/remediations/data-access/remediations.model.ts` with the actual backend response.

Backend response shape (unchanged):
```json
{
  "totalCount": 34,
  "byStatus": {
    "SUCCESS": 24,
    "FAILED": 3,
    "PENDING": 2,
    "IN_PROGRESS": 1,
    "PENDING_REBOOT": 2,
    "SKIPPED": 2
  },
  "meanTimeToRemediateSeconds": 4200,
  "recentActivity": [
    {
      "id": "rec-001",
      "cveId": "CVE-2025-12345",
      "targetName": "web-01",
      "status": "SUCCESS",
      "completedAt": "2026-07-10T08:00:00Z"
    }
  ]
}
```
