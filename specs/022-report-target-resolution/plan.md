# Implementation Plan: Report Target Resolution

**Branch**: `022-report-target-resolution` | **Date**: 2026-07-14 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/022-report-target-resolution/spec.md`

## Summary

Reports scoped to a target return "No data" because `RemediationRecord.targetId` stores the target host/IP
(e.g. `127.0.0.1`) while the UI filters by the Target's Mongo id, and the two never match. The same mismatch
makes report rows show empty target names. The fix is confined to `ReportServiceImpl` in `api/`: load the
active org/project's targets once, build id- and ipOrDomain-keyed indexes, match the selected target's id
**or** ipOrDomain when filtering, and resolve row names by id first then ipOrDomain. No UI change, no agent
change, no data migration, no contract change.

## Technical Context

**Language/Version**: Java 21 (Spring Boot 3), matching `api/`
**Primary Dependencies**: Spring Boot 3, Spring Data MongoDB, Lombok; JUnit 5 + Mockito for tests
**Storage**: MongoDB (`tfg-system` db) — collections `remediation_records`, `targets`, `reports`
**Testing**: `./mvnw test` (JUnit 5 + Mockito, no real Mongo/network); existing `ReportServiceImplTest`
**Target Platform**: Linux server (API runs as `java -jar api.jar` on lab-server port 8080)
**Project Type**: Web service (Angular UI + Spring Boot API); this change is API-only
**Performance Goals**: One additional scoped target query per report generation (index targets in memory);
no per-record repository round-trips
**Constraints**: Deterministic, side-effect-free read-path change; unchanged report request/response contract
and `ErrorCode` set; preserve `REPORT_EMPTY_RESULT` behavior; org/project isolation preserved
**Scale/Scope**: Single service class edited (`ReportServiceImpl`) plus its unit test; tens of targets and
tens–hundreds of remediation records per project

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Repository guidance reviewed: `CLAUDE.md`/`AGENTS.md` (Java conventions, no wildcard imports, Lombok,
  deterministic unit tests) — **PASS**
- English-only rule satisfied for code, UI text, docs, and comments — **PASS** (no new user-facing text;
  no new comments beyond non-obvious logic)
- Proposed design is the smallest correct change and avoids unnecessary abstraction — **PASS** (edits within
  `ReportServiceImpl`; reuses existing `TargetRepository.findAllScoped`; no new classes/interfaces required)
- Stack rules captured for affected modules — **PASS** (`api/` only)
- Verification steps identified for every affected module — **PASS** (unit tests + live lab end-to-end)
- Git actions identified; explicit user approval required before any git command runs — **PASS** (no commits
  or pushes will run without the user asking; branch already created for this feature)
- Unknown or ambiguous requirements resolved or called out — **PASS** (id-vs-ipOrDomain precedence resolved
  in spec: id wins)

**Result: PASS — no violations, Complexity Tracking not required.**

## Project Structure

### Documentation (this feature)

```text
specs/022-report-target-resolution/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/
│   └── reports-api.md   # Phase 1 output (unchanged external contract, documented)
├── checklists/
│   └── requirements.md  # From /speckit.specify
└── tasks.md             # Phase 2 output (/speckit.tasks — not created here)
```

### Source Code (repository root)

```text
api/
├── src/main/java/com/spulido/tfg/domain/
│   ├── report/services/impl/ReportServiceImpl.java   # EDIT: filter + name resolution
│   ├── target/db/TargetRepository.java               # REUSE: findAllScoped(Pageable)
│   ├── target/model/Target.java                      # REFERENCE: id, systemName, ipOrDomain
│   └── remediation/model/RemediationRecord.java      # REFERENCE: targetId (host/IP)
└── src/test/java/com/spulido/tfg/domain/report/services/impl/
    └── ReportServiceImplTest.java                    # EDIT: add target-resolution cases
```

**Structure Decision**: Existing multi-module web app (`ui/`, `api/`, `agents/unix/`). This feature edits
only `api/` — one service class and its unit test. No new modules or packages.

## Complexity Tracking

> No Constitution Check violations — section intentionally empty.
