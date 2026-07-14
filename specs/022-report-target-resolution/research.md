# Phase 0 Research: Report Target Resolution

## Decision 1 — Where the fix lives

**Decision**: Edit only `ReportServiceImpl` (methods `applyInMemoryFilters` and `resolveTargetName`, plus a
small helper to load/index the active project's targets). No repository, model, UI, or agent changes.

**Rationale**: Investigation on the live system confirmed the backend query and persistence are correct; the
defect is purely in how the report read path joins records to targets. Keeping the change in one class is the
smallest correct fix (Constitution III) and avoids touching the report contract.

**Alternatives considered**:
- Normalize `RemediationRecord.targetId` to the Target id at write time + migrate existing records — rejected
  by the chosen fix direction (touches the agent path and requires a data migration).
- New Mongo query joining records to targets — rejected; MongoDB has no server-side join here and the data
  volume per project is small enough to index targets in memory.

## Decision 2 — How to load targets for matching

**Decision**: Load the active org/project's targets via `TargetRepository.findAllScoped(Pageable)` with a
generous page size, once per `generate(...)` call, and build two maps: id → Target and ipOrDomain → Target.

**Rationale**: `findAllScoped` already applies org/project isolation through `ProjectContext` (Constitution:
tenant isolation, FR-006). One query instead of per-record `findById` keeps round-trips bounded (plan
Constraints). Building both indexes supports id-first, ipOrDomain-second resolution.

**Alternatives considered**:
- Per-record `targetRepository.findById(...)` (current name-resolution approach) — rejected; it is the source
  of the null-name bug and would still not resolve host/IP keys.
- `findByIpOrDomainAndOrganizationIdAndProjectId` per record — rejected; more round-trips than a single scoped
  load and does not cover the id case in one place.

## Decision 3 — Target-scoped filtering semantics (FR-001)

**Decision**: When `filters.targetId` is present, resolve the selected Target by id within scope. Then a
record matches the target filter if `record.targetId` equals the selected Target's `id` OR its `ipOrDomain`.
If the selected target id does not resolve to a Target in scope, fall back to matching `record.targetId` equal
to the raw filter value (preserves today's exact-string behavior and avoids surfacing cross-tenant data).

**Rationale**: Directly satisfies FR-001 while keeping the current behavior for the degenerate case where the
filter value is not a known target. Records with null/blank `targetId` never match a target filter (edge
case), consistent with today.

**Alternatives considered**:
- Match only by ipOrDomain — rejected; some records may legitimately store the Target id (e.g.
  `AgentServiceImpl` sets `targetId` to `target.getId()`), so id must also match (Edge Case: id precedence).

## Decision 4 — Name resolution precedence (FR-002)

**Decision**: Resolve a record's display name by looking up `record.targetId` in the id index first, then the
ipOrDomain index; return `systemName` of the first hit, else `null`.

**Rationale**: Matches the spec's stated precedence (id beats ipOrDomain) and preserves the graceful `null`
fallback for unknown identifiers.

## Decision 5 — Testing strategy

**Decision**: Extend `ReportServiceImplTest` (JUnit 5 + Mockito) with cases: (a) target-scoped match by
ipOrDomain returns records; (b) target-scoped match by id returns records; (c) target filter with no matching
records yields `REPORT_EMPTY_RESULT`; (d) name resolves by id, by ipOrDomain, and is null when unknown; (e)
unscoped generation unchanged. Mock `TargetRepository.findAllScoped` and the remediation/vulnerability
repositories; no real Mongo or network (Constitution: deterministic tests). End-to-end validation is run
against the live lab API per quickstart.

**Rationale**: Deterministic unit coverage for every acceptance scenario plus a real-system smoke test.
