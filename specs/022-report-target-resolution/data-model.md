# Phase 1 Data Model: Report Target Resolution

No schema changes. This feature only changes how existing documents are joined at read time.

## Entities (existing, unchanged)

### RemediationRecord (`remediation_records`)
| Field | Type | Role in this feature |
|-------|------|----------------------|
| `id` | String (ObjectId) | — |
| `targetId` | String | **Join key from the record side.** Currently holds a host/IP (e.g. `127.0.0.1`); may in some paths hold a Target id. Not modified. |
| `cveId` | String | Feeds severity/CVSS lookup (unchanged) |
| `status` | RemediationStatus | Status filter (unchanged) |
| `startedAt` / `completedAt` | Instant | Date-range filter (unchanged) |
| `organizationId` / `projectId` | String | Tenant scope (unchanged) |

### Target (`targets`)
| Field | Type | Role in this feature |
|-------|------|----------------------|
| `id` | String (ObjectId) | **Join key #1** — what the UI filter and `findById` use |
| `ipOrDomain` | String | **Join key #2** — matches the host/IP stored in `RemediationRecord.targetId` |
| `systemName` | String | Human-friendly name shown in report rows |
| `organizationId` / `projectId` | String | Tenant scope for `findAllScoped` |

## Derived in-memory structures (per report generation, not persisted)

- `targetsById: Map<String, Target>` — from the active project's targets, keyed by `id`.
- `targetsByIpOrDomain: Map<String, Target>` — same targets, keyed by `ipOrDomain` (skip null/blank; on
  duplicate ipOrDomain, first wins deterministically).

## Host canonicalization

Host/IP values are compared through a `canonicalHost(value)` normalization so loopback spellings join:

- lowercase + trim; blank → `null`.
- `localhost`, `127.0.0.0/8` (`startsWith("127.")`), `::1`, and `0:0:0:0:0:0:0:1` all canonicalize to `127.0.0.1`.
- any other value canonicalizes to its lowercased/trimmed form (so exact host/IP matches still work).

The host index is keyed by `canonicalHost(ipOrDomain)`; record lookups use `canonicalHost(record.targetId)`.

## Matching rules

- **Target filter (FR-001)**: given selected `targetId`, let `T = targetsById.get(targetId)`. A record matches
  if `record.targetId` equals `targetId`, or (when `T` exists) `canonicalHost(record.targetId)` equals
  `canonicalHost(T.ipOrDomain)`.
- **Name resolution (FR-002)**: for a record's `targetId`, return `targetsById.get(targetId).systemName` if
  present, else `targetsByHost.get(canonicalHost(targetId)).systemName` if present, else `null`.

## Invariants

- No writes to `remediation_records` or `targets`.
- All target lookups are confined to the active organization/project (FR-006).
- Records with null/blank `targetId` never match a target filter and resolve to a null name.
