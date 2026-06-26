# Implementation Plan: Autonomous Remediation Flow

**Branch**: `010-remediation-flow` | **Date**: 2026-06-26 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/010-remediation-flow/spec.md`

---

## Summary

This feature implements an autonomous remediation system that closes the loop from vulnerability detection to vulnerability remediation. Agents deployed on managed targets will automatically detect vulnerabilities, request remediation strategies from the central platform, execute fixes (package updates, service restarts, configuration changes), verify success, and report results. The system supports three remediation types: service-level updates (Type A), reboot-required fixes (Type B), and kernel updates (Type C, report-only). All remediation actions are fully auditable with complete execution logs and multi-tenancy support.

**Technical Approach**: Hybrid knowledge base (local JSON + API fallback), multi-layer verification (package version + service status + CVE re-scan), no automatic rollback for MVP (manual recovery via logged rollback hints).

---

## Technical Context

**Language/Version**: 
- Backend (API): Java 17, Spring Boot 3.2.x
- Backend (Agent): Java 17, Spring Boot 3.2.x (GraalVM native-image compatible)
- Frontend (UI): TypeScript 5.2+, Angular 17.x

**Primary Dependencies**:
- API: Spring Data MongoDB, Spring Security (JWT), Jakarta Validation, Lombok, Jackson
- Agent: Spring Web (RestTemplate), JSch (SSH), FreeMarker (script templates)
- UI: PrimeNG 17.x, Angular Router, RxJS

**Storage**: MongoDB 6.x (document database)

**Testing**:
- Backend: JUnit 5, Mockito, Spring Boot Test, Testcontainers (optional for integration tests)
- Frontend: Jasmine, Karma, Angular TestBed, HttpClientTestingModule

**Target Platform**:
- API: Linux server (Docker container or VM)
- Agent: Linux targets (Ubuntu 20.04+, Debian 11+) with SSH access
- UI: Modern browsers (Chrome 90+, Firefox 88+, Safari 14+)

**Project Type**: Web application (Angular SPA + Spring Boot REST API + distributed agents)

**Performance Goals**:
- Remediation strategy lookup: <50ms (in-memory cache)
- Remediation execution: <5 minutes per CVE (including verification)
- Remediation history query: <3 seconds for 1000 records
- Dashboard statistics: <2 seconds load time

**Constraints**:
- No automatic rollback (MVP scope)
- Agent persistence across reboots not supported (separate feature)
- Kernel updates (Type C) are report-only, no automated execution
- Remediation requires SSH access to target with sudo privileges

**Scale/Scope**:
- Initial knowledge base: ~50 common CVEs for Ubuntu 22.04
- Expected remediation volume: ~100-500 per day across all targets
- Multi-tenancy: organization + project scoping

---

## Constitution Check

✅ **Repository guidance reviewed**: AGENTS.md mandates Spring Boot best practices (constructor injection, DTO boundaries, jakarta.validation, centralized exception handling), Angular strict mode, and FreeMarker for scripts. All followed in this plan.

✅ **English-only rule satisfied**: All code identifiers, comments, UI text, and documentation in English. UI text uses i18n patterns (i18n attribute for templates, $localize for components).

✅ **Proposed design is minimal and correct**: 
- Reuses existing `StepHandler` pattern (no new abstractions)
- Extends `BaseEntity` and `ScopedEntity` (consistent with other entities)
- Follows existing API patterns (`AgentCommunicationController`)
- No speculative features (rollback, ML-based strategies deferred)

✅ **Stack rules captured**:
- API: Constructor injection, `@Valid` on DTOs, `@ControllerAdvice` for exceptions, standard import ordering
- Agent: StepHandler pattern, RestTemplate for HTTP, FreeMarker for scripts, GraalVM-safe (no reflection-heavy libs)
- UI: Standalone components, signal inputs/outputs, OnPush change detection, PrimeNG components

✅ **Verification steps identified**:
- API: Unit tests (JUnit 5 + Mockito), integration tests (@SpringBootTest + MockMvc)
- Agent: Unit tests for StepHandler with mocked HTTP client
- UI: Component tests with TestBed + HttpClientTestingModule
- End-to-end: Manual testing with real agent on vulnerable target

✅ **Git actions identified**: Branch `010-remediation-flow` created. Commits required after each implementation step. **User approval required before committing.**

✅ **Unknown requirements resolved**:
- Remediation strategy source: Hybrid (local JSON + API fallback) — see research.md §1
- Type detection: Package metadata + kernel pattern matching — see research.md §2
- Verification approach: Multi-layer (version + service + re-scan) — see research.md §3
- Rollback strategy: None for MVP, log manual recovery commands — see research.md §4

---

## Project Structure

### Documentation (this feature)

```text
specs/010-remediation-flow/
├── spec.md                    # Feature specification (WHAT and WHY)
├── plan.md                    # This file (HOW - technical design)
├── research.md                # Phase 0: Technical decisions and rationale
├── data-model.md              # Phase 1: Entity structures and relationships
├── quickstart.md              # Phase 1: Implementation order and guide
├── contracts/
│   ├── api-contracts.md       # Phase 1: REST API endpoint specifications
│   └── agent-contracts.md     # Phase 1: Agent-API communication protocol
├── checklists/
│   └── requirements.md        # Specification quality checklist
└── tasks.md                   # Phase 2: Actionable implementation tasks (generated by /speckit.tasks)
```

### Source Code (repository root)

```text
# API Module (Spring Boot backend)
api/
├── src/main/java/com/spulido/tfg/domain/remediation/
│   ├── model/
│   │   ├── RemediationType.java              # Enum: SERVICE_UPDATE, REBOOT_REQUIRED, KERNEL_UPDATE, UNKNOWN
│   │   ├── RemediationStatus.java            # Enum: PENDING, IN_PROGRESS, SUCCESS, FAILED, PENDING_REBOOT, SKIPPED
│   │   ├── RemediationAction.java            # Enum: APT_UPGRADE, APT_INSTALL, CONFIG_UPDATE, SYSTEMCTL_RESTART, MANUAL
│   │   ├── RemediationRecord.java            # Entity: extends BaseEntity, implements ScopedEntity
│   │   ├── RemediationStrategy.java          # Entity: knowledge base entry
│   │   └── dto/
│   │       ├── RemediationInfo.java                  # Response DTO for remediation records
│   │       ├── RemediationStatistics.java            # Response DTO for dashboard statistics
│   │       ├── RemediationStrategyRequest.java       # Request DTO for strategy lookup (agent-facing)
│   │       ├── RemediationStrategyResponse.java      # Response DTO for strategy lookup (agent-facing)
│   │       ├── RemediationReportRequest.java         # Request DTO for reporting results (agent-facing)
│   │       └── RemediationReportResponse.java        # Response DTO for reporting results (agent-facing)
│   ├── db/
│   │   ├── RemediationRecordRepository.java          # MongoDB repository for records
│   │   └── RemediationStrategyRepository.java        # MongoDB repository for strategies
│   ├── services/
│   │   ├── RemediationService.java                   # Interface: CRUD operations, statistics
│   │   ├── RemediationStrategyService.java           # Interface: strategy lookup
│   │   ├── RemediationMapper.java                    # Interface: entity ↔ DTO mapping
│   │   └── impl/
│   │       ├── RemediationServiceImpl.java           # Implementation: business logic, alert triggering
│   │       ├── RemediationStrategyServiceImpl.java   # Implementation: strategy resolution
│   │       └── RemediationMapperImpl.java            # Implementation: mapping logic
│   ├── controller/
│   │   └── RemediationController.java                # REST controller: user-facing endpoints
│   ├── config/
│   │   └── RemediationStrategyLoader.java            # Component: loads strategies from JSON on startup
│   └── exception/
│       └── RemediationException.java                 # Custom exception for remediation errors
└── src/main/resources/
    └── remediation/
        └── strategies.json                           # Knowledge base: CVE → remediation strategy mappings

# Agent Module (Spring Boot + GraalVM)
agents/unix/
├── src/main/java/com/spulido/agent/
│   ├── domain/task/
│   │   └── StepAction.java                           # Modified: add REMEDIATE enum value
│   ├── worker/
│   │   ├── WorkerCoordinator.java                    # Modified: register RemediationStepHandler
│   │   ├── step/
│   │   │   └── RemediationStepHandler.java           # New: executes remediation on target
│   │   └── http/
│   │       ├── AgentHttpClient.java                  # Modified: add remediation API methods
│   │       └── dto/
│   │           ├── RemediationStrategyRequest.java   # New: DTO for strategy request
│   │           ├── RemediationStrategyResponse.java  # New: DTO for strategy response
│   │           ├── RemediationReportRequest.java     # New: DTO for reporting results
│   │           └── RemediationReportResponse.java    # New: DTO for report response
└── src/main/resources/
    └── scripts/
        └── remediate.sh.tmpl                         # FreeMarker template for remediation scripts

# UI Module (Angular frontend)
ui/
└── src/app/pages/remediations/
    ├── data-access/
    │   ├── remediations.model.ts                     # Interfaces: RemediationRecord, RemediationType, RemediationStatus
    │   └── remediations.service.ts                   # Service: HTTP calls to API
    ├── feature/
    │   ├── remediations-list/
    │   │   ├── remediations-list.component.ts        # Standalone component: history table
    │   │   ├── remediations-list.component.html      # Template: PrimeNG table with filters
    │   │   └── remediations-list.component.spec.ts   # Component test
    │   ├── remediation-detail/
    │   │   ├── remediation-detail.component.ts       # Standalone component: detail view
    │   │   ├── remediation-detail.component.html     # Template: expandable log sections
    │   │   └── remediation-detail.component.spec.ts  # Component test
    │   └── remediation-widget/
    │       ├── remediation-widget.component.ts       # Standalone component: dashboard widget
    │       ├── remediation-widget.component.html     # Template: statistics cards
    │       └── remediation-widget.component.spec.ts  # Component test
    └── remediations.routes.ts                        # Routing: /remediations, /remediations/:id

# Shared modifications
api/src/main/java/com/spulido/tfg/domain/
├── ScopedEntity.java                                 # Modified: add RemediationRecord instanceof
└── plan/model/
    └── StepAction.java                               # Modified: add REMEDIATE enum value

agents/unix/src/main/java/com/spulido/agent/domain/task/
    └── StepAction.java                               # Modified: add REMEDIATE enum value

api/src/main/java/com/spulido/tfg/domain/agent/
├── controller/
│   └── AgentCommunicationController.java             # Modified: add remediation endpoints
└── services/
    └── AgentCommunicationService.java                # Modified: add remediation methods
```

**Structure Decision**: Follows existing domain-driven structure. Remediation is a new domain module under `api/.../domain/remediation/` with standard subdirectories (model, db, services, controller, config, exception). Agent module extends existing `worker/step/` and `worker/http/` patterns. UI module follows feature-based structure under `pages/remediations/` with data-access and feature subdirectories.

---

## Complexity Tracking

No constitution violations requiring justification. The design:
- Adds one new domain module (remediation) — standard pattern
- Extends two existing enums (StepAction) — minimal change
- Modifies one existing interface (ScopedEntity) — required for multi-tenancy
- Creates three new UI feature components — follows existing pattern
- No additional projects, no speculative abstractions, no backward-compatibility code

---

## Implementation Phases

### Phase 0: Research ✅ (Complete)

**Output**: `research.md`

Resolved all technical unknowns:
- Remediation strategy knowledge base: Hybrid approach (local JSON + API fallback)
- Type detection: Package metadata + kernel pattern matching
- Verification: Multi-layer (package version + service status + CVE re-scan)
- Rollback: None for MVP, log manual recovery commands
- Libraries: Apache Commons Lang, SemVer4j (for version comparison), existing Jackson/JSch
- Null safety: Optional, validation annotations, defensive programming
- Constants: Enums + constants class (no magic strings/numbers)

### Phase 1: Design & Contracts ✅ (Complete)

**Outputs**:
- `data-model.md`: Entity structures (RemediationRecord, RemediationStrategy, enums)
- `contracts/api-contracts.md`: REST API endpoints (user-facing + agent-facing)
- `contracts/agent-contracts.md`: Agent-API protocol (StepHandler, DTOs, script template)
- `quickstart.md`: Implementation order (11 steps with file lists and verification)

### Phase 2: Task Generation (Next)

**Command**: `/speckit.tasks`

**Output**: `tasks.md` with actionable, dependency-ordered tasks

**Expected tasks** (based on quickstart.md):
1. API — Enums and data model (RemediationRecord, RemediationStrategy, repositories)
2. API — Remediation strategy knowledge base (loader, service, strategies.json)
3. API — Remediation service and controller (CRUD, statistics, mapper)
4. API — Agent communication endpoints (strategy lookup, report results)
5. API — Alert integration (fire REMEDIATION_COMPLETED events)
6. Agent — Step action and handler (RemediationStepHandler, DTOs, script template)
7. UI — Models and service (interfaces, HTTP service)
8. UI — Remediation history page (table with filters)
9. UI — Remediation detail page (expandable logs)
10. UI — Dashboard widget (statistics cards)
11. Integration testing (end-to-end scenarios)

---

## Key Design Decisions

### 1. Hybrid Knowledge Base

**Decision**: Store remediation strategies in local JSON file, load into MongoDB on startup, provide API for lookup.

**Rationale**: 
- Fast lookups (in-memory cache)
- Works offline
- Easy to update (edit JSON, restart API)
- Can add external API integration later (Ubuntu Security API, NVD)

**Trade-offs**:
- Manual curation required (security team maintains strategies.json)
- Limited initial coverage (~50 CVEs for Ubuntu 22.04)

### 2. Multi-Layer Verification

**Decision**: Verify remediation success via three checks:
1. Package version updated (dpkg -l)
2. Service running (systemctl is-active)
3. CVE no longer present (re-scan with vulnerability lookup)

**Rationale**:
- Catches partial failures (package updated but service not restarted)
- Provides granular feedback (which check failed)
- Confirms CVE actually resolved

**Trade-offs**:
- Slower than single check (~30 seconds vs ~5 seconds)
- Requires re-scanning (additional API calls)

### 3. No Automatic Rollback

**Decision**: Log rollback commands for manual recovery, no automatic rollback.

**Rationale**:
- Rollback is complex (package downgrades, dependency conflicts, service state)
- Risk of cascading failures
- Out of scope for MVP

**Trade-offs**:
- Requires manual intervention on failure
- Operator must review logs and execute rollback commands

### 4. StepHandler Pattern

**Decision**: Implement remediation as a `StepHandler` in the agent execution engine.

**Rationale**:
- Consistent with existing handlers (ExploitationKnowledgeStepHandler, ExecuteExploitStepHandler)
- Easy to test (mock HTTP client and command executor)
- Access to context from previous steps (vulnerability data from EXPLOITATION_KNOWLEDGE)

**Trade-offs**:
- None significant — this is the natural fit

### 5. Separate Agent-Facing and User-Facing APIs

**Decision**: Agent endpoints under `/api/agent/comm/remediation/*`, user endpoints under `/api/remediations/*`.

**Rationale**:
- Different authentication mechanisms (API key vs JWT)
- Different authorization (AGENT role vs ADMIN/OPERATOR roles)
- Clear separation of concerns

**Trade-offs**:
- Slightly more code (two controllers instead of one)
- Worth it for security and clarity

---

## Java Best Practices Checklist

✅ **No magic strings**: Use enums for status, type, action. Use constants class for log messages and error messages.

✅ **No magic numbers**: Use constants for timeouts (REMEDIATION_TIMEOUT_SECONDS = 300), retry attempts (MAX_RETRY_ATTEMPTS = 3), retry delays (RETRY_DELAY_MS = 5000L).

✅ **Null safety**: 
- Use `Optional<RemediationStrategy>` for nullable returns
- Use `Objects.requireNonNull()` for constructor injection
- Use `StringUtils.isBlank()` for string validation
- Use `@NotBlank`, `@NotNull` on DTO fields

✅ **Prefer libraries**:
- Apache Commons Lang: `StringUtils`, `ObjectUtils`, `NumberUtils`
- SemVer4j: Version comparison (instead of custom parsing)
- Jackson: JSON serialization (already in use)
- Lombok: Reduce boilerplate (already in use)

✅ **Validation**: Use `jakarta.validation` annotations on all request DTOs. Use `@Valid` in controller methods.

✅ **Error handling**: Use custom `RemediationException` with error codes. Centralize handling in `@ControllerAdvice`.

✅ **Logging**: Use SLF4J with parameterized messages. Log at INFO for key events (remediation started, completed, failed). Log at DEBUG for detailed execution logs.

✅ **Immutability**: Use `final` for injected dependencies. Use Lombok `@Value` or `@Getter` without setters where appropriate.

✅ **Testing**: Unit tests for services (mock dependencies). Integration tests for repositories and controllers. Component tests for UI.

---

## Security Considerations

1. **Authentication**: Agent endpoints require API key (X-Agent-Api-Key header). User endpoints require JWT session.

2. **Authorization**: Agent can only remediate its assigned target (validated in `AgentCommunicationService`). Users can only view remediation records in their org/project scope (enforced by `ScopedEntity`).

3. **Input validation**: All request DTOs use `jakarta.validation` annotations. CVE IDs validated with regex pattern `CVE-\d{4}-\d+`.

4. **Command injection prevention**: Remediation commands come from knowledge base (curated by security team), not user input. Script template uses FreeMarker escaping.

5. **Audit trail**: All remediation actions logged with timestamps, agent ID, target ID, execution logs. Records are immutable after creation (no update endpoint for completed remediations).

6. **Secrets management**: SSH credentials stored in agent configuration (environment variables), not in code.

---

## Next Steps

1. Review this plan with stakeholders
2. Run `/speckit.tasks` to generate actionable implementation tasks
3. Run `/speckit.implement` to begin implementation (requires user approval for git commits)

---

**Plan Status**: ✅ Complete  
**Ready for**: Task generation (`/speckit.tasks`)
