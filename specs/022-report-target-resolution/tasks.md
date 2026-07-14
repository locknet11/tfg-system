# Tasks: Report Target Resolution

**Input**: Design documents from `/specs/022-report-target-resolution/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/reports-api.md, quickstart.md

**Tests**: Unit tests are required (existing `ReportServiceImplTest` pattern; deterministic, Mockito, no
network/filesystem per constitution).

**Organization**: Grouped by user story. The core fix (US1) and name resolution (US2) both live in
`ReportServiceImpl`; US3 is the no-regression guard.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1 / US2 / US3
- Exact file paths included.

## Path Conventions

Web app; this feature edits `api/` only:
- Impl: `api/src/main/java/com/spulido/tfg/domain/report/services/impl/ReportServiceImpl.java`
- Test: `api/src/test/java/com/spulido/tfg/domain/report/services/impl/ReportServiceImplTest.java`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirm baseline and reproduce the defect before changing code.

- [X] T001 Review repository guidance in `CLAUDE.md`/`AGENTS.md` (Java conventions: no wildcard imports, import order, Lombok, deterministic tests) and the feature `plan.md`/`research.md`.
- [X] T002 Establish the current baseline by running `cd api && ./mvnw -q test -Dtest=ReportServiceImplTest` and confirming it passes on `main`/branch start.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Load and index the active project's targets once per generation — the shared mechanism both US1
(filtering) and US2 (name resolution) depend on.

- [X] T003 In `api/src/main/java/com/spulido/tfg/domain/report/services/impl/ReportServiceImpl.java`, add a private helper that loads the active org/project's targets via `TargetRepository.findAllScoped(Pageable)` (single query, generous page size) and builds two in-memory maps: `Map<String,Target>` keyed by `Target.id` and `Map<String,Target>` keyed by `Target.ipOrDomain` (skip null/blank ipOrDomain; first-wins on duplicates). Build these once inside `generate(...)` and pass them to filtering and item mapping.

**Checkpoint**: Target index available to the rest of `generate(...)`; no behavior change yet.

---

## Phase 3: User Story 1 - Generate a report scoped to a specific target (Priority: P1) 🎯 MVP

**Goal**: A report scoped to a target returns records whose stored `targetId` equals the selected Target's id
OR its ipOrDomain.

**Independent Test**: Generate a report scoped to a target whose `ipOrDomain` matches records' host/IP → the
report is created with those records instead of `REPORT_EMPTY_RESULT`.

- [X] T004 [US1] In `ReportServiceImpl.applyInMemoryFilters(...)` (or the target-filter portion of `generate(...)`), change target filtering so that when `filters.targetId` is present, it resolves the selected `Target` from the id index and matches a record if `record.targetId` equals the raw `filters.targetId` OR the selected target's `ipOrDomain`. Preserve existing null/blank handling and the both-bounds/single-sided date logic unchanged. When the filter value resolves to no known target, fall back to the current exact-string match (`filters.targetId.equals(record.targetId)`).
- [X] T005 [P] [US1] In `ReportServiceImplTest`, add a test: target-scoped generation matches records by the selected target's `ipOrDomain` and returns a non-empty report.
- [X] T006 [P] [US1] In `ReportServiceImplTest`, add a test: target-scoped generation still matches records whose `targetId` equals the selected target's id (id path preserved).
- [X] T007 [P] [US1] In `ReportServiceImplTest`, add a test: a target filter that matches no record still throws `ReportException` with `ErrorCode.REPORT_EMPTY_RESULT`.

**Checkpoint**: Per-target reports return data; US1 independently testable and demoable.

---

## Phase 4: User Story 2 - Meaningful target names in report rows (Priority: P2)

**Goal**: Each report row shows the target's `systemName`, resolved by id first then ipOrDomain, else empty.

**Independent Test**: Generate a report containing a record whose `targetId` matches a target's ipOrDomain →
the row's `targetName` is that target's `systemName`.

- [X] T008 [US2] In `ReportServiceImpl.resolveTargetName(...)` (and `toItem(...)`), replace the per-record `targetRepository.findById(targetId)` lookup with resolution against the pre-built indexes from T003: look up by id first, then by ipOrDomain, returning `systemName` or `null`. Remove the now-unused per-item `findById` path.
- [X] T009 [P] [US2] In `ReportServiceImplTest`, add tests for name resolution: resolves by id, resolves by ipOrDomain, and returns `null` when the record's `targetId` matches no target.

**Checkpoint**: Report rows display target names; US1 + US2 complete.

---

## Phase 5: User Story 3 - Preserve all existing report behavior (Priority: P1)

**Goal**: Unscoped generation, severity/status/date filters, summaries, history, and org/project isolation
are unchanged.

**Independent Test**: Unscoped generation returns the same record count as before; filtered reports and
summaries match prior behavior.

- [X] T010 [P] [US3] In `ReportServiceImplTest`, add/confirm a regression test: unscoped generation (no target filter) returns all matching records and unchanged summary counts.
- [X] T011 [P] [US3] In `ReportServiceImplTest`, confirm existing severity/status/date-range filter and tenant-scope tests still pass unchanged (adjust mocks only to supply the new `findAllScoped` stub; assert no cross-tenant target is used for matching or naming).

**Checkpoint**: No regressions; all three stories satisfied.

---

## Phase 6: Polish & Validation

- [X] T012 Run `cd api && ./mvnw -q test -Dtest=ReportServiceImplTest` (and the report package tests) — all green. Verify no wildcard imports were introduced and import order follows `CLAUDE.md`.
- [X] T013 Build the API jar (`cd api && ./mvnw -q -DskipTests package`), redeploy to lab-server (`java -jar api.jar`, port 8080), and run the end-to-end checks in `quickstart.md`: target-scoped POST `/api/reports` returns 201 with items and non-null `targetName`; unscoped POST still returns 201 with the full record set.

---

## Dependencies & Execution Order

- **Setup (T001–T002)** → **Foundational (T003)** → **US1 (T004–T007)** → **US2 (T008–T009)** → **US3 (T010–T011)** → **Polish (T012–T013)**.
- T003 (target index) blocks both T004 (US1 filter) and T008 (US2 naming).
- T004 and T008 edit the same file (`ReportServiceImpl.java`) → **sequential**, not parallel.
- Test-writing tasks marked **[P]** (T005, T006, T007, T009, T010, T011) touch only the test file and can be written in parallel with each other once their target behavior exists, but must run against the completed impl.

## Parallel Opportunities

- Within US1: T005/T006/T007 (all in the test file) can be authored together.
- US2's T009 and US3's T010/T011 can be authored together after T008.

## Implementation Strategy

- **MVP = US1 (T001–T007)**: restores per-target report generation — the exact user-reported failure.
- **Increment 2 = US2 (T008–T009)**: readable target names.
- **Increment 3 = US3 (T010–T011) + Polish (T012–T013)**: regression guard and live validation.
