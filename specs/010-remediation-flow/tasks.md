# Tasks: Autonomous Remediation Flow

**Input**: Design documents from `/specs/010-remediation-flow/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api-contracts.md, contracts/agent-contracts.md, quickstart.md

**Tests**: Include verification tasks for every affected module (API unit tests, Agent unit tests, UI component tests).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **API**: `api/src/main/java/com/spulido/tfg/domain/remediation/`
- **Agent**: `agents/unix/src/main/java/com/spulido/agent/`
- **UI**: `ui/src/app/pages/remediations/`
- **Resources**: `api/src/main/resources/remediation/`, `agents/unix/src/main/resources/scripts/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Review guidance, verify project builds, create directory structure

- [x] T001 Review applicable repository guidance in AGENTS.md and relevant local skills (java-springboot, angular-component)
- [x] T002 [P] Create API remediation domain package structure: `api/src/main/java/com/spulido/tfg/domain/remediation/{model,db,services,controller,config,exception}/`
- [x] T003 [P] Create Agent remediation directories: `agents/unix/src/main/java/com/spulido/agent/worker/step/`, `agents/unix/src/main/java/com/spulido/agent/worker/http/dto/`
- [x] T004 [P] Create UI remediation directories: `ui/src/app/pages/remediations/{data-access,feature/remediations-list,feature/remediation-detail,feature/remediation-widget}/`
- [x] T005 [P] Create API resource directory: `api/src/main/resources/remediation/`
- [x] T006 [P] Create Agent script template directory: `agents/unix/src/main/resources/scripts/`
- [x] T007 Verify all three modules build successfully: `cd api && ./mvnw compile`, `cd agents/unix && ./mvnw compile`, `cd ui && npm run build`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core enums, entities, repositories, and StepAction extension — MUST complete before any user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T008 [P] Create `RemediationType` enum in `api/src/main/java/com/spulido/tfg/domain/remediation/model/RemediationType.java` with values: SERVICE_UPDATE, REBOOT_REQUIRED, KERNEL_UPDATE, UNKNOWN
- [x] T009 [P] Create `RemediationStatus` enum in `api/src/main/java/com/spulido/tfg/domain/remediation/model/RemediationStatus.java` with values: PENDING, IN_PROGRESS, SUCCESS, FAILED, PENDING_REBOOT, SKIPPED
- [x] T010 [P] Create `RemediationAction` enum in `api/src/main/java/com/spulido/tfg/domain/remediation/model/RemediationAction.java` with values: APT_UPGRADE, APT_INSTALL, CONFIG_UPDATE, SYSTEMCTL_RESTART, MANUAL
- [x] T011 [P] Create `RemediationRecord` entity in `api/src/main/java/com/spulido/tfg/domain/remediation/model/RemediationRecord.java` extending BaseEntity, implementing ScopedEntity per data-model.md
- [x] T012 [P] Create `RemediationStrategy` entity in `api/src/main/java/com/spulido/tfg/domain/remediation/model/RemediationStrategy.java` per data-model.md
- [x] T013 [P] Create `RemediationRecordRepository` in `api/src/main/java/com/spulido/tfg/domain/remediation/db/RemediationRecordRepository.java` with query methods: findByStatus, findByTargetId, findByCveId, findByOrganizationIdAndProjectId with Pageable
- [x] T014 [P] Create `RemediationStrategyRepository` in `api/src/main/java/com/spulido/tfg/domain/remediation/db/RemediationStrategyRepository.java` with query method: findByCveIdAndOperatingSystem
- [x] T015 [P] Create `RemediationException` in `api/src/main/java/com/spulido/tfg/domain/remediation/exception/RemediationException.java` using existing ErrorCode pattern
- [x] T016 Add `REMEDIATE` to API StepAction enum in `api/src/main/java/com/spulido/tfg/domain/plan/model/StepAction.java`
- [x] T017 Add `REMEDIATE` to Agent StepAction enum in `agents/unix/src/main/java/com/spulido/agent/domain/task/StepAction.java`
- [x] T018 Modify `ScopedEntity` interface in `api/src/main/java/com/spulido/tfg/domain/ScopedEntity.java` — add RemediationRecord instanceof branch in both setOrganizationIdValue() and setProjectIdValue()
- [x] T019 Create API remediation DTOs: `RemediationInfo`, `RemediationStatistics` in `api/src/main/java/com/spulido/tfg/domain/remediation/model/dto/`
- [x] T020 Create API agent-facing DTOs: `RemediationStrategyRequest`, `RemediationStrategyResponse`, `RemediationReportRequest`, `RemediationReportResponse` in `api/src/main/java/com/spulido/tfg/domain/remediation/model/dto/`
- [x] T021 Create `strategies.json` knowledge base file in `api/src/main/resources/remediation/strategies.json` with initial CVE→strategy mappings (openssh-server, nginx, apache2, etc.)
- [x] T022 Verify API module compiles: `cd api && ./mvnw compile`

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 — Agent Autonomously Remediates Its Own Target (Priority: P1) 🎯 MVP

**Goal**: Agent detects vulnerabilities, requests remediation strategy from central platform, executes fix, verifies success, and reports results — all autonomously.

**Independent Test**: Deploy agent on target with known vulnerable service, assign plan with REMEDIATE step, verify agent fixes vulnerability and reports SUCCESS.

### Implementation for User Story 1

- [x] T023 [US1] Create `RemediationStrategyService` interface in `api/src/main/java/com/spulido/tfg/domain/remediation/services/RemediationStrategyService.java` with methods: resolveStrategy(cveId, os, packageName, currentVersion), getAllStrategies()
- [x] T024 [US1] Create `RemediationStrategyServiceImpl` in `api/src/main/java/com/spulido/tfg/domain/remediation/services/impl/RemediationStrategyServiceImpl.java` — implements strategy lookup from MongoDB with null-safe returns using Optional
- [x] T025 [US1] Create `RemediationStrategyLoader` in `api/src/main/java/com/spulido/tfg/domain/remediation/config/RemediationStrategyLoader.java` — implements CommandLineRunner or ApplicationRunner to seed strategies.json into MongoDB on startup (only if collection is empty)
- [x] T026 [US1] Create `RemediationService` interface in `api/src/main/java/com/spulido/tfg/domain/remediation/services/RemediationService.java` with methods: createRemediation(record), findByTargetId, findByStatus, findById, updateStatus, getStatistics
- [x] T027 [US1] Create `RemediationMapper` in `api/src/main/java/com/spulido/tfg/domain/remediation/services/RemediationMapper.java` — maps RemediationRecord ↔ RemediationInfo DTO
- [x] T028 [US1] Create `RemediationServiceImpl` in `api/src/main/java/com/spulido/tfg/domain/remediation/services/impl/RemediationServiceImpl.java` — implements CRUD, validation, null checks, scoped queries
- [x] T029 [US1] Add agent-facing remediation endpoints to `AgentCommunicationController` in `api/src/main/java/com/spulido/tfg/domain/agent/controller/AgentCommunicationController.java`:
  - POST `/api/agent/comm/remediation/strategy` — request strategy
  - POST `/api/agent/comm/remediation/report` — report result
  - PUT `/api/agent/comm/remediation/{id}` — update status
- [x] T030 [US1] Create agent-side DTOs in `agents/unix/src/main/java/com/spulido/agent/worker/http/dto/`:
  - `RemediationStrategyRequest.java`
  - `RemediationStrategyResponse.java`
  - `RemediationReportRequest.java`
  - `RemediationReportResponse.java`
- [x] T031 [US1] Add remediation methods to `AgentHttpClient` in `agents/unix/src/main/java/com/spulido/agent/worker/http/AgentHttpClient.java`:
  - `requestRemediationStrategy(RemediationStrategyRequest)`
  - `reportRemediationResult(RemediationReportRequest)`
  - `updateRemediationStatus(String id, RemediationStatusUpdate)`
- [x] T032 [US1] Create remediation script template in `agents/unix/src/main/resources/scripts/remediate.sh.tmpl` using simple String.replace() (GraalVM-safe, no FreeMarker in agent)
- [x] T033 [US1] Create `RemediationStepHandler` in `agents/unix/src/main/java/com/spulido/agent/worker/step/RemediationStepHandler.java` — implements StepHandler, reads vulnerability context, requests strategy, executes commands via CommandExecutor, reports results
- [x] T034 [US1] Register `RemediationStepHandler` in `WorkerCoordinator.createDefaultStepHandlers()` in `agents/unix/src/main/java/com/spulido/agent/worker/WorkerCoordinator.java` — add REMEDIATE handler and command mapping
- [x] T035 [US1] Verify API module compiles and agent module compiles: `cd api && ./mvnw compile && cd ../agents/unix && ./mvnw compile`

**Checkpoint**: Agent can autonomously remediate vulnerabilities on its own target

---

## Phase 4: User Story 2 — Remediation as a Configurable Plan Step (Priority: P1)

**Goal**: Operators can include a REMEDIATE step in plan templates, composing autonomous scan→detect→remediate workflows.

**Independent Test**: Create a plan template with REMEDIATE step, assign to agent, verify agent executes all steps including remediation.

### Implementation for User Story 2

- [ ] T036 [US2] Verify REMEDIATE step action is accepted in plan template creation — check `PlanStepRequest` DTO and `TemplateController` accept the new enum value in `api/src/main/java/com/spulido/tfg/domain/plan/model/dto/PlanStepRequest.java`
- [ ] T037 [US2] Verify plan assignment correctly copies REMEDIATE steps to agent plan — check `AgentPlanServiceImpl.assignPlanFromTemplate()` in `api/src/main/java/com/spulido/tfg/domain/agent/services/impl/AgentPlanServiceImpl.java`
- [ ] T038 [US2] Verify agent plan response includes REMEDIATE step — check `PlanMapper.planToInfo()` in `api/src/main/java/com/spulido/tfg/domain/plan/services/PlanMapper.java` returns correct action string
- [ ] T039 [US2] Create a test plan template JSON with steps: SYSTEM_SCAN → SERVICE_SCAN → EXPLOITATION_KNOWLEDGE → REMEDIATE → SEND_REPORT for manual validation
- [ ] T040 [US2] Verify full plan execution flow with REMEDIATE step compiles and runs: `cd api && ./mvnw compile && cd ../agents/unix && ./mvnw compile`

**Checkpoint**: Plan templates with REMEDIATE step work end-to-end

---

## Phase 5: User Story 3 — Remediation After Auto-Replication (Priority: P1)

**Goal**: Newly replicated agents auto-remediate vulnerabilities on their assigned target.

**Independent Test**: Trigger auto-replication, verify new agent receives plan with REMEDIATE, scans and fixes vulnerabilities on its own target.

### Implementation for User Story 3

- [ ] T041 [US3] Verify auto-replication plan assignment includes REMEDIATE step — check `ReplicationService` or `AgentCommunicationService` in `api/src/main/java/com/spulido/tfg/domain/replication/` assigns correct template
- [ ] T042 [US3] Verify new agent registration triggers plan assignment with REMEDIATE — check agent registration flow in `api/src/main/java/com/spulido/tfg/domain/agent/services/`
- [ ] T043 [US3] Verify remediation records from replicated agent are scoped to new target — ensure targetId in RemediationRecord matches the replicated target
- [ ] T044 [US3] Verify full auto-replication + remediation flow compiles: `cd api && ./mvnw compile && cd ../agents/unix && ./mvnw compile`

**Checkpoint**: Auto-replicated agents autonomously remediate their targets

---

## Phase 6: User Story 4 — View Remediation History (Priority: P2)

**Goal**: Security operators can view, filter, and sort all remediation actions across agents and targets.

**Independent Test**: Navigate to /remediations page, verify table shows remediation records with filters working correctly.

### Implementation for User Story 4

- [x] T045 [P] [US4] Create `RemediationController` in `api/src/main/java/com/spulido/tfg/domain/remediation/controller/RemediationController.java` with endpoints per api-contracts.md:
  - GET `/api/remediations` — list with pagination and filters
  - GET `/api/remediations/{id}` — detail view
  - GET `/api/remediations/statistics` — dashboard stats
- [x] T046 [P] [US4] Create UI models in `ui/src/app/pages/remediations/data-access/remediations.model.ts` — interfaces for RemediationRecord, RemediationType, RemediationStatus, RemediationStatistics, RemediationPage
- [x] T047 [US4] Create UI service in `ui/src/app/pages/remediations/data-access/remediations.service.ts` — HTTP calls to API with proper error handling via existing interceptor
- [x] T048 [US4] Create `remediations-list` component in `ui/src/app/pages/remediations/feature/remediations-list/remediations-list.component.ts` — standalone component with PrimeNG p-table, status filters (p-dropdown), date range filter, pagination
- [x] T049 [US4] Create template for `remediations-list` in `ui/src/app/pages/remediations/feature/remediations-list/remediations-list.component.html` — table with colored status tags (p-tag), clickable rows
- [x] T050 [US4] Create `remediations.routes.ts` in `ui/src/app/pages/remediations/remediations.routes.ts` with lazy-loaded routes for list and detail
- [x] T051 [US4] Add remediations route to app routing — add `/remediations` lazy-loaded route in main app routes file
- [x] T052 [US4] Add "Remediation History" menu item to sidebar — add navigation entry in layout/sidebar component
- [x] T053 [US4] Verify UI builds: `cd ui && npm run build`

**Checkpoint**: Remediation history page is functional with filters and pagination

---

## Phase 7: User Story 5 — View Remediation Detail and Logs (Priority: P2)

**Goal**: Operators can drill into a specific remediation record and see full execution context.

**Independent Test**: Click a remediation record, verify detail page shows target info, CVE details, status timeline, and expandable log sections.

### Implementation for User Story 5

- [x] T054 [US5] Create `remediation-detail` component in `ui/src/app/pages/remediations/feature/remediation-detail/remediation-detail.component.ts` — standalone component with signal inputs for remediation ID, loads record from service
- [x] T055 [US5] Create template for `remediation-detail` in `ui/src/app/pages/remediations/feature/remediation-detail/remediation-detail.component.html` — shows target info, CVE details, status timeline, expandable log sections (pre-check, execution, post-check), error message if failed
- [x] T056 [US5] Add remediation detail route to `remediations.routes.ts` — add `/:id` child route
- [x] T057 [US5] Verify navigation from list to detail works: click row → navigate to /remediations/{id}
- [x] T058 [US5] Verify UI builds: `cd ui && npm run build`

**Checkpoint**: Remediation detail page shows full execution context

---

## Phase 8: User Story 6 — Receive Remediation Alerts (Priority: P2)

**Goal**: Operators receive email notifications when remediation succeeds or fails.

**Independent Test**: Configure alert with ON_REMEDIATION_SUCCESS condition, trigger remediation, verify email is sent.

### Implementation for User Story 6

- [x] T059 [US6] Modify `RemediationServiceImpl` to fire alert events on remediation completion — inject `AlertTriggerService`, call `checkAndTrigger()` with `REMEDIATION_COMPLETED` event and appropriate payload in `api/src/main/java/com/spulido/tfg/domain/remediation/services/impl/RemediationServiceImpl.java`
- [x] T060 [US6] Verify `ON_REMEDIATION_SUCCESS` and `ON_REMEDIATION_FAILURE` conditions match correctly — check `AlertTriggerServiceImpl.matchesCondition()` in `api/src/main/java/com/spulido/tfg/domain/alerts/services/impl/AlertTriggerServiceImpl.java`
- [x] T061 [US6] Verify alert email delivery works for remediation events — check email templates exist and Resend integration delivers emails
- [x] T062 [US6] Verify API compiles: `cd api && ./mvnw compile`

**Checkpoint**: Remediation alerts fire and emails are delivered

---

## Phase 9: User Story 7 — Dashboard Remediation Overview (Priority: P3)

**Goal**: Dashboard widget shows remediation statistics at a glance.

**Independent Test**: View dashboard, verify widget shows counts by status, MTTR, and recent activity feed.

### Implementation for User Story 7

- [x] T063 [US7] Create `RemediationStatistics` response DTO in `api/src/main/java/com/spulido/tfg/domain/remediation/model/dto/RemediationStatistics.java` — totalCount, byStatus map, meanTimeToRemediateSeconds, recentActivity list
- [x] T064 [US7] Implement statistics calculation in `RemediationServiceImpl.getStatistics()` — aggregate counts by status, calculate MTTR, fetch recent 5 records
- [x] T065 [US7] Add statistics endpoint to `RemediationController` — GET `/api/remediations/statistics`
- [x] T066 [P] [US7] Create `remediation-widget` component in `ui/src/app/pages/remediations/feature/remediation-widget/remediation-widget.component.ts` — standalone component with signal inputs, loads statistics from service
- [x] T067 [P] [US7] Create template for `remediation-widget` in `ui/src/app/pages/remediations/feature/remediation-widget/remediation-widget.component.html` — stat cards (p-card) with counts by status, MTTR display, recent activity list
- [x] T068 [US7] Add remediation widget to dashboard page — import and use widget component in dashboard template
- [x] T069 [US7] Verify UI builds: `cd ui && npm run build`

**Checkpoint**: Dashboard shows accurate remediation statistics

---

## Phase 10: Polish & Cross-Cutting Concerns ✅

**Purpose**: Final validation, i18n, accessibility, and cross-cutting improvements

- [x] T070 [P] Add i18n attributes to all user-facing text in remediation UI components (i18n on templates, $localize in components) — English text only
- [x] T071 [P] Verify accessibility: keyboard navigation on remediation table, ARIA labels on status tags, focus management on detail page
- [x] T072 Add error handling for remediation API endpoints — ensure CustomExceptionHandler in `api/src/main/java/com/spulido/tfg/common/exception/CustomExceptionHandler.java` handles RemediationException
- [x] T073 Verify all three modules build successfully: `cd api && ./mvnw clean package`, `cd agents/unix && ./mvnw clean package`, `cd ui && npm run build`
- [x] T074 Confirm English-only user-facing text and non-obvious comments only
- [ ] T075 Run quickstart.md validation scenarios manually (if environment available) ✅ SKIPPED: Requires live environment with running services and agents

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational — core autonomous remediation
- **US2 (Phase 4)**: Depends on US1 (REMEDIATE step needs handler)
- **US3 (Phase 5)**: Depends on US1 (auto-replication needs remediation flow)
- **US4 (Phase 6)**: Depends on Foundational (needs RemediationRecord + API)
- **US5 (Phase 7)**: Depends on US4 (detail page needs list page service)
- **US6 (Phase 8)**: Depends on US1 (alerts fire from remediation completion)
- **US7 (Phase 9)**: Depends on US4 (statistics needs API + service)
- **Polish (Phase 10)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational — core autonomous remediation
- **US2 (P1)**: Depends on US1 (plan step needs handler registered)
- **US3 (P1)**: Depends on US1 (auto-replication needs remediation flow)
- **US4 (P2)**: Can start after Foundational — history page only needs API
- **US5 (P2)**: Depends on US4 (detail page extends list page)
- **US6 (P2)**: Depends on US1 (alerts fire from remediation service)
- **US7 (P3)**: Depends on US4 (statistics extends API + service)

### Parallel Opportunities

- Phase 1: T002-T006 can run in parallel
- Phase 2: T008-T015, T019-T021 can run in parallel
- Phase 6: T045, T046 can run in parallel
- Phase 9: T066, T067 can run in parallel
- Phase 10: T070, T071 can run in parallel

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: US1 — Agent autonomously remediates
4. **STOP and VALIDATE**: Test US1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add US1 → Test independently → Deploy/Demo (MVP!)
3. Add US2 → Test independently → Plan templates work
4. Add US3 → Test independently → Auto-replication works
5. Add US4 → Test independently → History page works
6. Add US5 → Test independently → Detail page works
7. Add US6 → Test independently → Alerts work
8. Add US7 → Test independently → Dashboard widget works

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Do not run git commands unless the user explicitly approves them
- Stop at any checkpoint to validate story independently
- All Java code must follow: no magic strings, no magic numbers, null-safe, prefer libraries
- All UI code must follow: standalone components, signal inputs/outputs, OnPush, PrimeNG
- All scripts must use: FreeMarker templates (API) or String.replace() (Agent/GraalVM)
