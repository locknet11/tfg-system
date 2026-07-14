# Contract: Reports API (unchanged surface)

This feature does **not** change the HTTP contract. It only changes which records a request returns and how
target names are populated. Documented here to make the invariant explicit.

## POST /api/reports

Generate and persist a report for the active org/project.

**Auth**: authenticated; raw JWT in the `Authorization` header (the filter does not strip `Bearer `).
**Context headers**: `X-Organization-Id`, `X-Project-Id` (both required together).

**Request body** (`ReportGenerateRequest`, unchanged):
```json
{
  "targetId": "string | null",
  "from": "instant | null",
  "to": "instant | null",
  "severities": ["CRITICAL|HIGH|MEDIUM|LOW|UNKNOWN"],
  "statuses": ["SUCCESS|FAILED|PENDING|IN_PROGRESS|SKIPPED|PENDING_REBOOT"]
}
```

**Responses** (unchanged status/shape):
- `201 Created` — the persisted `Report` with `summary` and `items[]`. Each item includes `targetName`.
- `422 Unprocessable Entity`, `errorCode REPORT_EMPTY_RESULT` — no records match after all filters.
- `422`, `REPORT_NO_PROJECT_CONTEXT` — no org/project selected.

**Behavioral change (this feature only)**:
- When `targetId` is a Target id whose `ipOrDomain` matches records' stored `targetId`, those records are now
  **included** (previously produced `REPORT_EMPTY_RESULT`).
- `items[].targetName` is now populated when the record's `targetId` matches a target by id **or** ipOrDomain
  (previously `null` for host/IP-keyed records).

## GET /api/reports and GET /api/reports/{id}

Unchanged in every respect (history listing and stored-snapshot retrieval).
