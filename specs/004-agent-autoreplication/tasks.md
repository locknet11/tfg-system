# Tasks: Agent Autoreplication

**Input**: Design documents from `specs/004-agent-autoreplication/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api-contracts.md

**Tests**: Verification tasks included for every affected module per constitution requirement III (minimal, correct, verifiable changes).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **API module**: `api/src/main/java/com/spulido/tfg/`
- **Agent module**: `agents/unix/src/main/java/com/spulido/agent/`
- **UI module**: `ui/src/app/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Dependencies and repository guidance review

- [x] T001 Review applicable repository guidance in AGENTS.md and skills `.agents/skills/java-springboot/SKILL.md` and `.agents/skills/angular-component/SKILL.md`
- [x] T002 Add Bouncy Castle dependency (`org.bouncycastle:bcprov-jdk18on`) and Blake3 Java library to `api/pom.xml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared models, enums, services, and infrastructure that ALL user stories depend on

**CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 [P] Extend StepAction enum with REQUEST_REPLICATION, EXECUTE_EXPLOIT, TRANSFER_AGENT, REPLICATE in `api/src/main/java/com/spulido/tfg/domain/plan/model/StepAction.java`
- [x] T004 [P] Extend StepAction enum with REQUEST_REPLICATION, EXECUTE_EXPLOIT, TRANSFER_AGENT, REPLICATE in `agents/unix/src/main/java/com/spulido/agent/domain/task/StepAction.java`
- [x] T005 [P] Extend WhenCondition enum with ON_REPLICATION_REQUESTED, ON_AGENT_REPLICATED in `api/src/main/java/com/spulido/tfg/domain/alerts/model/WhenCondition.java`
- [x] T006 [P] Add replication error codes (REPLICATION_REQUEST_NOT_FOUND, REPLICATION_REQUEST_NOT_PENDING, REPLICATION_DUPLICATE_REQUEST, REPLICATION_TOKEN_NOT_FOUND, REPLICATION_TOKEN_EXPIRED, REPLICATION_TOKEN_CONSUMED, REPLICATION_POLICY_NOT_FOUND, BINARY_INTEGRITY_CHECK_FAILED) to `api/src/main/java/com/spulido/tfg/common/exception/ErrorCode.java`
- [x] T007 [P] Create ReplicationRequestStatus enum (PENDING, APPROVED, DENIED, EXPIRED) and ReplicationApprovalMode enum (AUTO_APPROVE, MANUAL_APPROVE) in `api/src/main/java/com/spulido/tfg/domain/replication/model/`
- [x] T008 [P] Create ReplicationPolicy value object (mode, minSeverity, notifyAdmin) and ReplicationExploitInfo value object (cveId, exploitId) in `api/src/main/java/com/spulido/tfg/domain/replication/model/`
- [x] T009 [P] Create ReplicationRequest document model with all fields per data-model.md, implementing ScopedEntity and extending BaseEntity, with compound indexes in `api/src/main/java/com/spulido/tfg/domain/replication/model/ReplicationRequest.java`
- [x] T010 Create ReplicationRequestRepository with scoped queries, duplicate detection query, and token lookup in `api/src/main/java/com/spulido/tfg/domain/replication/db/ReplicationRequestRepository.java`
- [x] T011 [P] Extend Agent model with replicatedFrom, replicatedAt, replicationExploit fields in `api/src/main/java/com/spulido/tfg/domain/agent/model/Agent.java` and extend AgentInfo DTO with matching fields in `api/src/main/java/com/spulido/tfg/domain/agent/model/dto/AgentInfo.java`
- [x] T012 [P] Create ReplicationException domain exception in `api/src/main/java/com/spulido/tfg/domain/replication/exception/ReplicationException.java` and add replication error code mappings to `api/src/main/java/com/spulido/tfg/common/exception/CustomExceptionHandler.java`
- [x] T013 Create BinaryIntegrityService interface and BinaryIntegrityServiceImpl (Blake3 hashing + PKI signing with Bouncy Castle, loads private key from REPLICATION_PRIVATE_KEY env var, computes hash and signature at startup) in `api/src/main/java/com/spulido/tfg/domain/replication/services/`
- [x] T014 Create AgentBinaryService interface and AgentBinaryServiceImpl (serves binary from configurable filesystem path agent.binary.path, caches binary bytes and BinaryManifest in memory at startup) in `api/src/main/java/com/spulido/tfg/domain/replication/services/`
- [x] T015 Implement real CommandExecutor using ProcessBuilder with configurable timeout, capturing stdout/stderr, returning TaskResult based on exit code — replace the stub in `agents/unix/src/main/java/com/spulido/agent/config/WorkerPoolConfig.java`
- [x] T016 Add central-public-key property to AgentConfig in `agents/unix/src/main/java/com/spulido/agent/config/AgentConfig.java` and add `agent.central-public-key=${CENTRAL_PUBLIC_KEY:}` to `agents/unix/src/main/resources/application.properties`
- [x] T017 Update WebSecurity to add permitAll for GET /api/agent/binary/** in `api/src/main/java/com/spulido/tfg/config/security/WebSecurity.java`
- [x] T018 Verify API module compiles: run `cd api && ./mvnw compile`

**Checkpoint**: Foundation ready — all shared models, enums, repositories, and services exist. User story implementation can now begin.

---

## Phase 3: User Story 1 - Autonomous Agent Self-Replication (Priority: P1) MVP

**Goal**: Agent detects exploitable vulnerability, requests approval from Central, executes exploit, transfers binary with integrity verification, installs and registers new agent.

**Independent Test**: Deploy a parent agent with AUTO_APPROVE policy, simulate a vulnerable service on an unregistered target, verify the full chain from replication request through new agent registration completes.

### API — Replication Request Service

- [x] T019 [P] [US1] Create ReplicationRequestService interface (createRequest, getRequest, approveRequest, denyRequest, expireRequests, findDuplicates) in `api/src/main/java/com/spulido/tfg/domain/replication/services/ReplicationRequestService.java`
- [x] T020 [P] [US1] Create ReplicationPolicyService interface and ReplicationPolicyServiceImpl (evaluatePolicy: loads project policy, checks mode + severity threshold, returns APPROVED or PENDING) in `api/src/main/java/com/spulido/tfg/domain/replication/services/`
- [x] T021 [US1] Implement ReplicationRequestServiceImpl (create request with policy evaluation, generate UUID token on approval, generate preauthCode, create/update Target, compute downloadUrl, detect duplicates, handle expiration) in `api/src/main/java/com/spulido/tfg/domain/replication/services/impl/ReplicationRequestServiceImpl.java`

### API — Agent-Facing Endpoints

- [x] T022 [P] [US1] Create agent-facing DTOs: CreateReplicationRequest (with Jakarta validation), ReplicationRequestResponse, ReplicationStatusResponse in `api/src/main/java/com/spulido/tfg/domain/replication/model/dto/`
- [x] T023 [US1] Create ReplicationRequestController (POST /api/agent/comm/replication-request, GET /api/agent/comm/replication-request/{id}/status) with @PreAuthorize("hasRole('AGENT')") and @AuthenticationPrincipal agentId in `api/src/main/java/com/spulido/tfg/domain/replication/controller/ReplicationRequestController.java`
- [x] T024 [US1] Create AgentBinaryController (GET /api/agent/binary/{replicationToken}) — validates token, checks TTL, serves multipart response with binary bytes + JSON BinaryManifest, marks token consumed in `api/src/main/java/com/spulido/tfg/domain/replication/controller/AgentBinaryController.java`

### Agent — HTTP Client and DTOs

- [x] T025 [P] [US1] Create agent-side DTOs (ReplicationRequestBody, ReplicationRequestResponse, ReplicationStatusResponse) and extend AgentHttpClient with submitReplicationRequest(), pollReplicationStatus(), downloadBinary() methods in `agents/unix/src/main/java/com/spulido/agent/worker/http/`

### Agent — Step Handlers

- [x] T026 [US1] Create RequestReplicationStepHandler — reads EXPLOITATION_KNOWLEDGE context for scripts/severity, submits replication request to Central, polls status with exponential backoff if PENDING, stores replicationToken/downloadUrl/preauthCode/centralUrl in StepResult logs in `agents/unix/src/main/java/com/spulido/agent/worker/step/RequestReplicationStepHandler.java`
- [x] T027 [US1] Create ExecuteExploitStepHandler — reads exploit script from EXPLOITATION_KNOWLEDGE context, executes via CommandExecutor, establishes SSH reverse shell, validates connectivity, stores session info in StepResult logs in `agents/unix/src/main/java/com/spulido/agent/worker/step/ExecuteExploitStepHandler.java`
- [x] T028 [US1] Create TransferAgentStepHandler — reads downloadUrl from REQUEST_REPLICATION context, downloads binary+manifest via reverse shell (curl/wget), delegates integrity verification to BinaryIntegrityVerifier, writes config file (central-url, api-key, agent-id), chmod +x, launches binary, verifies healthcheck in `agents/unix/src/main/java/com/spulido/agent/worker/step/TransferAgentStepHandler.java`
- [x] T029 [P] [US1] Create BinaryIntegrityVerifier — computes Blake3 hash of downloaded binary, decrypts signed hash file using Central's public key (from AgentConfig), compares hashes, returns pass/fail in `agents/unix/src/main/java/com/spulido/agent/worker/BinaryIntegrityVerifier.java`

### Agent — Wiring

- [x] T030 [US1] Register new step handlers (RequestReplicationStepHandler, ExecuteExploitStepHandler, TransferAgentStepHandler) in WorkerCoordinator.createDefaultStepHandlers() in `agents/unix/src/main/java/com/spulido/agent/worker/WorkerCoordinator.java`

### UI — Model Updates

- [x] T031 [P] [US1] Add REQUEST_REPLICATION, EXECUTE_EXPLOIT, TRANSFER_AGENT, REPLICATE to StepAction enum and stepActionLabel() function in `ui/src/app/pages/agents/data-access/agents.model.ts` and `ui/src/app/pages/templates/data-access/templates.model.ts`

### Verification

- [x] T032 [US1] Verify API module compiles and agent module compiles: run `cd api && ./mvnw compile` and `cd agents/unix && ./mvnw compile`

**Checkpoint**: User Story 1 is complete — agent can self-replicate with AUTO_APPROVE policy. The full chain from vulnerability detection to new agent registration works.

---

## Phase 4: User Story 2 - Administrator Manages Replication Approvals (Priority: P2)

**Goal**: Admin views pending replication requests in a dedicated UI page, evaluates context, and approves or denies each request with full audit trail.

**Independent Test**: Configure MANUAL_APPROVE policy, trigger a replication request, verify it appears in the admin UI, approve it, confirm agent proceeds.

### API — Admin DTOs and Mapper

- [x] T033 [P] [US2] Create ReplicationRequestServiceMapper (ReplicationRequest to ReplicationRequestInfo, with parent agent name resolution) in `api/src/main/java/com/spulido/tfg/domain/replication/services/ReplicationRequestServiceMapper.java`
- [x] T034 [P] [US2] Create ReplicationRequestInfo DTO (admin list item with all display fields including parentAgentName) and ReplicationRequestsList (extends PageImpl) in `api/src/main/java/com/spulido/tfg/domain/replication/model/dto/`

### API — Admin Endpoints

- [x] T035 [US2] Create ReplicationAdminController with GET /api/replication-requests (paginated, filterable by status and severity), PUT /api/replication-requests/{id}/approve, PUT /api/replication-requests/{id}/deny — all with @PreAuthorize("isAuthenticated()") in `api/src/main/java/com/spulido/tfg/domain/replication/controller/ReplicationAdminController.java`

### API — Service Extensions

- [x] T036 [US2] Add approveRequest(id, userId) and denyRequest(id, userId) methods to ReplicationRequestServiceImpl — validate PENDING status, generate token/preauthCode on approve, set resolvedAt and approvedBy, trigger AlertTriggerService, update Agent with replication metadata on approve in `api/src/main/java/com/spulido/tfg/domain/replication/services/impl/ReplicationRequestServiceImpl.java`

### UI — Replication Requests Page

- [x] T037 [P] [US2] Create replication-requests.model.ts with ReplicationRequest interface, ReplicationRequestsList interface, ReplicationRequestStatus enum, and ReplicationStatusResponse interface in `ui/src/app/pages/replication-requests/data-access/replication-requests.model.ts`
- [x] T038 [P] [US2] Create replication-requests.service.ts with list(page, size, status?, severity?), approve(id), deny(id) HTTP methods in `ui/src/app/pages/replication-requests/data-access/replication-requests.service.ts`
- [x] T039 [US2] Create replication-requests.component.ts and replication-requests.component.html — standalone component with p-table (lazy loading, pagination), status filter p-dropdown, severity filter p-dropdown, Approve/Deny p-button actions per row, p-tag for status severity, ConfirmationService for approve/deny, MessageService for feedback in `ui/src/app/pages/replication-requests/feature/`
- [x] T040 [US2] Add replication-requests route to app-routing.module.ts as child of LayoutComponent with loadComponent in `ui/src/app/app-routing.module.ts`

### UI — Agent List Badge

- [x] T041 [P] [US2] Extend Agent interface with replicatedFrom and replicatedAt fields in `ui/src/app/pages/agents/data-access/agents.model.ts` and add replication badge column (icon + pTooltip showing parent agent info) to agents-list.component.ts and its template in `ui/src/app/pages/agents/feature/agents-list/`

### API — Alert Integration

- [x] T042 [US2] Extend AlertTriggerServiceImpl to handle ON_REPLICATION_REQUESTED and ON_AGENT_REPLICATED WhenCondition types — trigger matching alert configurations when replication events occur in `api/src/main/java/com/spulido/tfg/domain/alerts/services/impl/AlertTriggerServiceImpl.java`

### Verification

- [x] T043 [US2] Verify all modules compile: run `cd api && ./mvnw compile`, `cd agents/unix && ./mvnw compile`, and `cd ui && npx tsc --noEmit`

**Checkpoint**: User Story 2 is complete — admins can view, approve, and deny replication requests through the UI with notifications.

---

## Phase 5: User Story 3 - Administrator Configures Replication Policies (Priority: P3)

**Goal**: Admin configures per-project replication policy (AUTO_APPROVE/MANUAL_APPROVE, severity threshold, notification toggle) through project settings.

**Independent Test**: Set AUTO_APPROVE with severity threshold HIGH, trigger a CRITICAL replication request, verify it auto-approves. Change to MANUAL_APPROVE, verify subsequent requests require admin action.

### API — Policy Configuration

- [x] T044 [P] [US3] Create UpdateReplicationPolicyRequest DTO with @NotNull mode, nullable minSeverity, @NotNull notifyAdmin in `api/src/main/java/com/spulido/tfg/domain/project/model/dto/UpdateReplicationPolicyRequest.java`
- [x] T045 [P] [US3] Extend Project model with replicationPolicy field (ReplicationPolicy embedded) in `api/src/main/java/com/spulido/tfg/domain/project/model/Project.java` and extend ProjectInfo DTO with replicationPolicy field in `api/src/main/java/com/spulido/tfg/domain/project/model/dto/ProjectInfo.java`
- [x] T046 [US3] Add PUT /api/projects/{id}/replication-policy endpoint to ProjectController — validates request, updates project.replicationPolicy, returns updated ProjectInfo in `api/src/main/java/com/spulido/tfg/domain/project/controller/ProjectController.java`
- [x] T047 [US3] Add updateReplicationPolicy(id, request) method to ProjectServiceImpl — load project, set replicationPolicy from DTO, save in `api/src/main/java/com/spulido/tfg/domain/project/services/impl/ProjectServiceImpl.java`

### UI — Policy Settings

- [x] T048 [P] [US3] Add ReplicationPolicy interface (mode, minSeverity, notifyAdmin) and UpdateReplicationPolicyRequest interface to `ui/src/app/pages/project-selector/data-access/projects.model.ts` and add updateReplicationPolicy(id, request) method to `ui/src/app/pages/project-selector/data-access/project.service.ts`
- [x] T049 [US3] Add replication policy settings section to project-selector.component.ts and template — p-dropdown for mode (AUTO_APPROVE/MANUAL_APPROVE), p-dropdown for minSeverity (CRITICAL/HIGH/MEDIUM/LOW + null option), p-checkbox for notifyAdmin, save button calling updateReplicationPolicy in `ui/src/app/pages/project-selector/feature/project-selector.component.ts`

### Verification

- [x] T050 [US3] Verify all modules compile: run `cd api && ./mvnw compile` and `cd ui && npx tsc --noEmit`

**Checkpoint**: All user stories are now independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: i18n, formatting, and final validation

- [x] T051 [P] Update i18n translation files with all new user-facing text (replication requests page labels, policy settings labels, badge tooltips, alert condition names) in `ui/src/i18n/messages.json`
- [x] T052 [P] Update Spanish translations for all new i18n entries in `ui/src/i18n/messages.es.json`
- [x] T053 Run Prettier formatting on UI module: `cd ui && npx prettier --write .`
- [x] T054 Run quickstart.md validation steps and confirm English-only user-facing text

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational phase — core replication flow
- **User Story 2 (Phase 4)**: Depends on Foundational phase — needs ReplicationRequest model and service from Phase 2, integrates with US1 API endpoints
- **User Story 3 (Phase 5)**: Depends on Foundational phase — needs ReplicationPolicy model from Phase 2
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — no dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) — admin approve/deny calls ReplicationRequestService from Phase 2; UI is independent of US1 agent code
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) — policy config extends Project model; policy evaluation is consumed by US1 but can be built independently

### Within Each User Story

- Models/DTOs before services
- Services before controllers/endpoints
- API before UI (UI depends on API contracts)
- Core implementation before integration (alerts, badges)
- Story complete before moving to next priority
- Ask instead of guessing when requirements or implementation details are unclear

### Parallel Opportunities

- T003-T009: All foundational model/enum tasks can run in parallel (different files)
- T011-T012: Agent model extension and exception creation in parallel
- T019-T020: Service interfaces in parallel
- T022, T025: API DTOs and agent DTOs in parallel
- T029: BinaryIntegrityVerifier is independent of step handlers
- T033-T034: Admin mapper and DTOs in parallel
- T037-T038: UI model and service in parallel
- T041: Agent list badge is independent of replication requests page
- T044-T045, T048: Policy DTOs and UI models in parallel

---

## Parallel Example: User Story 1

```text
# Launch all service interfaces together:
Task: T019 "Create ReplicationRequestService interface"
Task: T020 "Create ReplicationPolicyService interface and impl"

# Launch all DTOs together:
Task: T022 "Create agent-facing DTOs"
Task: T025 "Create agent-side DTOs and extend AgentHttpClient"

# Launch independent step handlers together:
Task: T026 "Create RequestReplicationStepHandler"
Task: T027 "Create ExecuteExploitStepHandler"
Task: T029 "Create BinaryIntegrityVerifier"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test full replication cycle with AUTO_APPROVE
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Admin can manage approvals
4. Add User Story 3 → Test independently → Admin can configure policies
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (agent-side + API replication service)
   - Developer B: User Story 2 (admin UI + admin API)
   - Developer C: User Story 3 (policy config)
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Do not run git commands unless the user explicitly approves them
- Stop at any checkpoint to validate story independently
- Blake3 library choice: evaluate available Java libraries at implementation time (see research.md R-001)
- PKI signing: use Bouncy Castle with Ed25519 or RSA-2048 (see research.md R-002)
- CommandExecutor: ProcessBuilder-based implementation (see research.md R-006)
- Reverse shell: SSH-based (see research.md R-005)
