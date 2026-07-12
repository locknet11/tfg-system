# API Contract: Reports Module

Base path: `/api/reports`. All endpoints are `@PreAuthorize("isAuthenticated()")` and are
**scoped to the active organization/project** via `ProjectContext`. Requests without an active
project context are rejected (see error `REPORT_NO_PROJECT_CONTEXT`). No endpoint ever returns
data outside the caller's org/project.

Timestamps are ISO-8601 instants (UTC). Enum string values:
- Severity: `CRITICAL | HIGH | MEDIUM | LOW | UNKNOWN`
- `RemediationStatus`: `PENDING | IN_PROGRESS | SUCCESS | FAILED | PENDING_REBOOT | SKIPPED`
- `generationType`: `ON_DEMAND | SCHEDULED`

---

## POST `/api/reports` — Generate and persist a report

Generates an immutable snapshot from the current org/project's remediation data, optionally
narrowed by the request filters, and stores it.

### Request body — `ReportGenerateRequest`

```json
{
  "targetId": "t-123",              // optional
  "from": "2026-06-01T00:00:00Z",   // optional
  "to":   "2026-07-01T00:00:00Z",   // optional
  "severities": ["CRITICAL", "HIGH"], // optional; empty/absent = all
  "statuses": ["SUCCESS", "FAILED"]   // optional; empty/absent = all
}
```

Validation: if both `from` and `to` are present, `from` MUST be ≤ `to` (else 400).

### Responses

- **201 Created** — the stored report (full snapshot):

```json
{
  "id": "r-9f2",
  "organizationId": "org-1",
  "projectId": "proj-1",
  "title": "Security report — proj-1 — 2026-07-12",
  "generationType": "ON_DEMAND",
  "generatedAt": "2026-07-12T10:15:30Z",
  "generatedBy": "admin@example.com",
  "createdAt": "2026-07-12T10:15:30",
  "filters": {
    "targetId": null,
    "from": null,
    "to": null,
    "severities": [],
    "statuses": []
  },
  "summary": {
    "vulnerabilitiesBySeverity": { "CRITICAL": 2, "HIGH": 5, "MEDIUM": 3, "LOW": 1, "UNKNOWN": 0 },
    "remediationsByStatus": { "SUCCESS": 8, "FAILED": 2, "SKIPPED": 1 },
    "meanTimeToRemediateSeconds": 4200,
    "targetsCovered": 4,
    "totalVulnerabilities": 11,
    "totalRemediations": 11
  },
  "items": [
    {
      "cveId": "CVE-2025-12345",
      "severity": "CRITICAL",
      "cvssScore": 9.8,
      "targetId": "t-123",
      "targetName": "web-01",
      "remediationStatus": "SUCCESS",
      "startedAt": "2026-06-10T08:00:00Z",
      "completedAt": "2026-06-10T09:10:00Z"
    }
  ]
}
```

- **422 Unprocessable Entity** — no data matched the filters; **nothing was stored** (FR-008):

```json
{ "code": "REPORT_EMPTY_RESULT", "message": "No data matches the selected filters." }
```

- **422 Unprocessable Entity** — no active project context:

```json
{ "code": "REPORT_NO_PROJECT_CONTEXT", "message": "No organization/project selected." }
```

- **400 Bad Request** — validation failure (e.g. `from` after `to`), via existing `@ControllerAdvice`.
- **401 Unauthorized** — not authenticated.

---

## GET `/api/reports` — History (paged)

Returns stored reports for the active org/project, newest first (`createdAt` desc).

### Query params

| Param | Type | Default | Notes |
|-------|------|---------|-------|
| `page` | int | `0` | zero-based |
| `size` | int | `20` | capped at `100` (matches remediation controller) |

### Response — **200 OK**, Spring `Page<ReportInfo>`

```json
{
  "content": [
    {
      "id": "r-9f2",
      "title": "Security report — proj-1 — 2026-07-12",
      "generationType": "ON_DEMAND",
      "generatedAt": "2026-07-12T10:15:30Z",
      "generatedBy": "admin@example.com",
      "totalVulnerabilities": 11,
      "totalRemediations": 11,
      "targetsCovered": 4
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 20
}
```

Only reports whose `organizationId`/`projectId` match the active context are ever returned.

---

## GET `/api/reports/{id}` — Full stored snapshot

Returns the complete stored report exactly as generated. Fetched tenant-safely via
`findByIdAndOrganizationIdAndProjectId`.

### Responses

- **200 OK** — same full body shape as the `POST` 201 response above.
- **404 Not Found** — no report with that id **in the caller's org/project** (also the response
  for a report that exists under a different tenant — no cross-tenant disclosure).
- **401 Unauthorized** — not authenticated.

---

## Repository contract (`ReportRepository extends MongoRepository<Report, String>`)

```java
Page<Report> findByOrganizationIdAndProjectId(String organizationId, String projectId, Pageable pageable);
Optional<Report> findByIdAndOrganizationIdAndProjectId(String id, String organizationId, String projectId);
```

## Tenancy & immutability invariants (must hold in tests)

- Every response contains only the active tenant's data (SC-6).
- `GET /{id}` for another tenant's report → 404, never the document.
- A stored report re-read after its source `RemediationRecord`s change is byte-for-byte identical
  in its summary/items (SC-2).
- There is **no** update/delete endpoint for reports in v1.
