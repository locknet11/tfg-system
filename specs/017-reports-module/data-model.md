# Phase 1 Data Model: Reports Module

New MongoDB collection **`reports`**. Document `Report extends BaseEntity implements
ScopedEntity`, following the `RemediationRecord` pattern (Lombok `@Getter/@Setter/@Builder/
@NoArgsConstructor/@AllArgsConstructor`, `@Field` annotations, compound index on
`{ organizationId, projectId }`). All embedded types are stored inline on the document.

`BaseEntity` supplies `id`, `createdAt`, `updatedAt` (`LocalDateTime`). Tenancy fields
`organizationId`/`projectId` are set explicitly by the service from `ProjectContext`
(see research R2).

## Entity: `Report` (`@Document(collection = "reports")`)

| Field | Type | Notes |
|-------|------|-------|
| `id` | `String` | from `BaseEntity`; Mongo `_id` |
| `createdAt` | `LocalDateTime` | from `BaseEntity`; history sort key (desc) |
| `updatedAt` | `LocalDateTime` | from `BaseEntity`; set once, never modified (immutability) |
| `organizationId` | `String` | scoped; set from `ProjectContext`; part of compound index |
| `projectId` | `String` | scoped; set from `ProjectContext`; part of compound index |
| `title` | `String` | human label, e.g. "Security report — <project> — <date>" (English-authored) |
| `generationType` | `GenerationType` | `ON_DEMAND` or `SCHEDULED` |
| `generatedAt` | `Instant` | equals creation time (research R7) |
| `generatedBy` | `String` | authenticated user id/email; `"system"` for scheduled runs |
| `filters` | `ReportFilters` | embedded; the criteria used to build this snapshot |
| `summary` | `ReportSummary` | embedded; computed roll-up |
| `items` | `List<ReportItem>` | embedded; backing detail rows |

Index: `@CompoundIndex(name = "org_proj_idx", def = "{ 'organizationId': 1, 'projectId': 1 }")`.

**Immutability**: written only in `ReportService.generate()`. No update endpoint or resave path.

## Enum: `GenerationType`

```
ON_DEMAND    // administrator triggered via UI
SCHEDULED    // produced by the scheduled generator
```

## Embedded: `ReportFilters`

| Field | Type | Notes |
|-------|------|-------|
| `targetId` | `String?` | optional single-target filter |
| `from` | `Instant?` | inclusive lower bound on remediation completion time |
| `to` | `Instant?` | inclusive upper bound on remediation completion time |
| `severities` | `List<String>` | subset of `CRITICAL/HIGH/MEDIUM/LOW/UNKNOWN`; empty = all |
| `statuses` | `List<RemediationStatus>` | reuse existing enum; empty = all |

Validation (on the **request DTO**, not the stored value): if both `from` and `to` are
present, `from` ≤ `to`.

## Embedded: `ReportSummary`

| Field | Type | Notes |
|-------|------|-------|
| `vulnerabilitiesBySeverity` | `Map<String, Long>` | keys `CRITICAL/HIGH/MEDIUM/LOW/UNKNOWN` |
| `remediationsByStatus` | `Map<String, Long>` | keys = `RemediationStatus` names |
| `meanTimeToRemediateSeconds` | `long` | shared MTTR helper (research R4); `0` if none |
| `targetsCovered` | `int` | distinct `targetId` count across items |
| `totalVulnerabilities` | `long` | distinct CVEs (or item count — fixed in tasks; keep consistent with SC-3) |
| `totalRemediations` | `long` | number of remediation records in scope |

## Embedded: `ReportItem`

| Field | Type | Notes |
|-------|------|-------|
| `cveId` | `String` | from the remediation record |
| `severity` | `String?` | resolved from CVE cache; `"UNKNOWN"` when not cached |
| `cvssScore` | `Double?` | resolved from CVE cache; `null` when not cached |
| `targetId` | `String` | from the remediation record |
| `targetName` | `String` | resolved via `TargetRepository` (`Target.systemName`) |
| `remediationStatus` | `String` | `RemediationStatus` name |
| `startedAt` | `Instant?` | from the remediation record |
| `completedAt` | `Instant?` | from the remediation record |

## Relationships & sourcing

```
Report (reports)
  └─ items[] ── each derived from one RemediationRecord (remediation_records)
                   ├─ cveId ─────────► CveEntry (embedded in ServiceVulnerabilityRecord.cves) → severity, cvssScore
                   └─ targetId ──────► Target (targets) → systemName (targetName), status
  └─ summary ── aggregated over the same RemediationRecord set
  └─ filters ── snapshot of the ReportGenerateRequest criteria
```

Source collections are **read-only** for this feature. Only `reports` is written. The
remediation service is modified only to reuse the shared MTTR helper (behavior fix, no schema
change).

## Request/response DTOs

- **`ReportGenerateRequest`** (POST body): `targetId?`, `from?`, `to?`, `severities: List<String>`,
  `statuses: List<RemediationStatus>`. `jakarta.validation`: `from ≤ to` (class-level constraint
  or manual check). All fields optional; empty/absent = no narrowing.
- **`ReportInfo`** (history-row projection): `id`, `title`, `generationType`, `generatedAt`,
  `generatedBy`, and headline counts (`totalVulnerabilities`, `totalRemediations`,
  `targetsCovered`) for the list table. Avoids shipping full `items` in the list.
- **Full detail response**: the stored `Report` (summary + items) returned by `GET /api/reports/{id}`.

## State & lifecycle

A `Report` has no mutable lifecycle: it is **created once** and thereafter read-only. There are
no state transitions. `generationType` distinguishes origin (manual vs scheduled) but does not
change after creation.
