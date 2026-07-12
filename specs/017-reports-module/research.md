# Phase 0 Research: Reports Module

Resolves the unknowns surfaced while grounding the TRD against the actual codebase.
All decisions favor the smallest correct change and reuse of existing machinery.

## R1 — Resolving CVE severity/CVSS by `cveId`

**Question**: The TRD says "look up `cveId` in `ServiceVulnerabilityRepository`/`CveEntry`",
but `CveEntry` is embedded inside `ServiceVulnerabilityRecord.cves` (a `List<CveEntry>`),
and `ServiceVulnerabilityRepository` has no finder by `cveId`. How do we map a remediation
record's `cveId` → severity/CVSS?

**Decision**: In `ReportServiceImpl.generate()`, build an in-memory `Map<String, CveEntry>`
keyed by `cveId` once per generation by streaming the distinct `cveId`s of the matched
remediation records and resolving them against the service-vulnerability cache, then look
up each `ReportItem` from that map. Prefer a single scoped/collection read over N queries.
When a `cveId` is not present in the cache, set `severity = "UNKNOWN"` and `cvssScore = null`.

**Rationale**: Remediation counts drive the report; CVE metadata is enrichment. A per-generation
map keeps the lookup O(records) and deterministic (testable without network). Avoids adding
a heavyweight aggregation query.

**Alternatives considered**:
- Add a `findByCves_CveId` derived query returning the parent record — workable, but still
  requires unwrapping the matching `CveEntry`, and issues one query per distinct CVE.
- Add a MongoDB aggregation to unwind `cves` — more power than needed at prototype scale;
  rejected as premature complexity (Constitution III).

If a broader lookup is warranted later, a small `CveLookupService` can wrap this map-building
logic; for v1 it lives in the report service.

## R2 — `ScopedEntity` auto-population does not cover `Report`

**Question**: `ScopedEntity.setOrganizationIdValue/setProjectIdValue` use an explicit
`instanceof` chain (Target, Agent, Template, AlertConfiguration, ReplicationRequest,
RemediationRecord). `Report` is not listed, so saving a `Report` would not auto-populate
org/project.

**Decision**: Set `organizationId`/`projectId` **explicitly in `ReportServiceImpl.generate()`**
from `ProjectContext` (which the service already must read to scope the query and to reject
missing context). Do not modify the shared `ScopedEntity` `instanceof` chain.

**Rationale**: Smallest correct change (Constitution III) and keeps the immutable-snapshot
construction fully explicit in one place. Editing the shared marker interface to add `Report`
is broader blast radius for no benefit, since the service already holds the context.

**Alternatives considered**: Add `Report` to the `instanceof` chain — rejected as an
unnecessary edit to shared code; the values are already in hand at construction time.

## R3 — Immutability guarantee

**Question**: How is "cannot be altered after creation" (FR-005) enforced technically?

**Decision**: Enforce by construction and by API surface, not by a DB-level lock:
- No `PUT`/`PATCH`/update endpoint and no `save` of an existing report id in the service.
- `Report` is only ever written once, inside `generate()`, from computed values (the snapshot
  stores resolved severities/names/counts, not references), so later changes to
  `remediation_records`/CVE cache/targets never propagate.
- `BaseEntity.updatedAt` is set once at creation and never touched afterward.

**Rationale**: Matches the existing persistence style; a stored snapshot is inherently
drift-proof. A DB immutability constraint is out of scope for the prototype.

**Verification**: Integration test persists a report, mutates a source `RemediationRecord`,
re-reads the report by id, and asserts identical values (SC-2).

## R4 — MTTR shared helper (and fixing `getStatistics()`)

**Question**: MTTR is hardcoded to `0` in `RemediationServiceImpl.getStatistics()`
(line ~211). The report needs a real MTTR; the TRD asks to extract a shared helper and reuse
it to fix the `0`.

**Decision**: Define MTTR = mean of `Duration.between(startedAt, completedAt).getSeconds()`
over records with `status == SUCCESS` and **both** timestamps non-null; return `0` when the
set is empty. Extract this as a small stateless helper reused by both `ReportServiceImpl.generate()`
and `RemediationServiceImpl.getStatistics()`. Place it where both can reach it without a new
cross-domain dependency (e.g. a package-visible static util in the remediation domain that the
report service imports, or a `RemediationService` method). Final placement decided in tasks;
requirement is single source of truth, no duplication.

**Rationale**: One definition prevents the two screens from disagreeing (Constitution III,
DRY). Seconds-granularity matches the existing `meanTimeToRemediateSeconds` field type (`long`).

**Alternatives considered**: Compute MTTR independently in each place — rejected (duplication,
drift risk, explicitly called out by the TRD).

## R5 — Scheduled generation: which projects, what cadence, default state

**Question**: The `@Scheduled` component must iterate "projects with data". How are projects
enumerated, and how is cadence/enablement configured and kept off in tests?

**Decision**:
- **Enumeration**: derive the set of `(organizationId, projectId)` pairs that have remediation
  activity from `remediation_records` (distinct org/project). For each pair, set `ProjectContext`,
  call `generate(...)` with `generationType = SCHEDULED`, then clear the context in a `finally`.
- **Skip empties**: `generate()` already returns an empty-result signal and persists nothing,
  so projects with no matching data are naturally skipped (P-FR-08 / FR-009).
- **Config**: `reports.scheduler.enabled` (boolean) and `reports.scheduler.cron` (cron expr)
  in `application.yml`; component annotated so it is inert when disabled. **Default disabled in
  the test profile** so unit/integration tests stay deterministic; a sensible default cadence
  (e.g. daily) documented for non-test profiles.
- Enable Spring scheduling via `@EnableScheduling` if not already enabled in the app.

**Rationale**: Reuses the tenancy mechanism the rest of the app already relies on
(`ProjectContext`), avoids a new "projects with data" persistence concept, and keeps tests
free of timers/network (Constitution III + testing rules).

**Alternatives considered**: Maintain a dedicated project registry — unnecessary; the
remediation collection already implies which projects have data.

**Open follow-up (non-blocking)**: exact production cron value is an ops choice recorded in
config, not a code decision.

## R6 — Empty-result signaling on `POST /api/reports`

**Question**: How should the "no data for filters" case be represented so the UI can show a
message without treating it as an error, while nothing is persisted?

**Decision**: On empty match, persist nothing and return **HTTP 422 Unprocessable Entity** with
a small structured body carrying a stable error code (e.g. `REPORT_EMPTY_RESULT`) and a
human-friendly message, surfaced through the existing `@ControllerAdvice`. The UI treats 422 +
that code as the "no data" empty-state/toast rather than a hard failure. Missing project
context returns a distinct code (e.g. `REPORT_NO_PROJECT_CONTEXT`).

**Rationale**: Keeps a clean, testable contract; 422 communicates "request understood, nothing
to produce" without inventing a 2xx "no-op success" the client must special-case ambiguously.
Aligns with the TRD's "422 if empty" note and existing error-advice pattern.

**Alternatives considered**: 200 with `null`/empty body — rejected: harder to distinguish from
a real (but tiny) report and muddies the "stored nothing" contract. 204 No Content — rejected:
no way to carry the explanatory message/code.

## R7 — History ordering field

**Question**: TRD mentions both "`createdAt` desc" and a `generatedAt` field. Which drives
"newest first"?

**Decision**: Sort history by `BaseEntity.createdAt` descending (the field the existing
remediation list already sorts by), and set `generatedAt` equal to creation time. This keeps
ordering consistent with the rest of the app and avoids indexing a second time field.

**Rationale**: Consistency with existing list endpoints; `createdAt` is always present via
`BaseEntity`. `generatedAt`/`generatedBy` remain on the snapshot for display/audit.

## Summary of decisions

| # | Topic | Decision |
|---|-------|----------|
| R1 | CVE severity lookup | Per-generation `Map<cveId, CveEntry>` from the service-vuln cache; UNKNOWN when absent |
| R2 | Tenancy population | Set org/project explicitly in the service from `ProjectContext`; don't edit `ScopedEntity` |
| R3 | Immutability | No update endpoint/path; snapshot stores computed values; verified by mutation test |
| R4 | MTTR | Shared helper (SUCCESS + both timestamps), seconds; also fixes `getStatistics()` `0` |
| R5 | Scheduling | Iterate distinct org/project from remediation data; config-gated; off in tests; empties skipped |
| R6 | Empty result | HTTP 422 + `REPORT_EMPTY_RESULT` code via `@ControllerAdvice`; persist nothing |
| R7 | History order | Sort by `createdAt` desc; `generatedAt` = creation time |

No unresolved NEEDS CLARIFICATION remain.
