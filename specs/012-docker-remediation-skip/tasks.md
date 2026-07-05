# Tasks: Docker Container Remediation Skip

**Input**: Design documents from `/specs/012-docker-remediation-skip/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Unit tests are included per module convention. Each module has its own test infrastructure (JUnit 5 + Mockito for agents/unix/; spring-boot-starter-test + Mockito for api/).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Agent module**: `agents/unix/src/main/java/com/spulido/agent/` and `agents/unix/src/test/java/com/spulido/agent/`
- **API module**: `api/src/main/java/com/spulido/tfg/` and `api/src/test/java/com/spulido/tfg/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify build environment and review all design artifacts

- [x] T001 Verify agent module builds: `cd agents/unix && ./mvnw clean compile -DskipTests`
- [x] T002 [P] Verify API module builds: `cd api && ./mvnw clean compile -DskipTests`
- [x] T003 [P] Review existing `StepResult.java`, `TaskExecutionService.java`, and `RemediationStepHandler.java` to confirm current skip/abort behavior
- [x] T004 [P] Review existing `RemediationStatus.SKIPPED` enum value and `RemediationRecord` schema in `api/src/main/java/com/spulido/tfg/domain/remediation/model/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T005 [P] Create `ContainerDetectionResult` record with fields `container: boolean`, `confidence: DetectionConfidence`, `detectionMethod: DetectionMethod`, `matchedIndicators: Set<DetectionMethod>`, `runtimeName: String` in `agents/unix/src/main/java/com/spulido/agent/container/ContainerDetectionResult.java`
- [x] T006 [P] Create `DetectionMethod` enum with values `DOCKERENV_FILE`, `CGROUP_V1`, `MOUNTINFO_V2`, `PID1_SCHED`, `CONTAINERENV_FILE`, `NONE` in `agents/unix/src/main/java/com/spulido/agent/container/DetectionMethod.java`
- [x] T007 [P] Create `DetectionConfidence` enum with values `CONFIRMED`, `INCONCLUSIVE` in `agents/unix/src/main/java/com/spulido/agent/container/DetectionConfidence.java`
- [x] T008 Create `ContainerDetector` class with `detect()` method implementing 5 filesystem checks (/.dockerenv, /proc/1/cgroup for /docker/ /kubepods/ /containerd/ /lxc/, /proc/self/mountinfo for /docker/containers/, /proc/1/sched for PID 1 name, /run/.containerenv for Podman) in `agents/unix/src/main/java/com/spulido/agent/container/ContainerDetector.java`
- [x] T009 Modify `StepResult` to add `boolean skipped` field and `public static StepResult skipped(StepAction action, List<String> logs)` factory method in `agents/unix/src/main/java/com/spulido/agent/domain/task/StepResult.java`
- [x] T010 Modify `TaskExecutionService.executeJob()` to treat skipped results as non-failure: after handler returns, check `isSkipped()` and continue to next step instead of aborting the job in `agents/unix/src/main/java/com/spulido/agent/worker/TaskExecutionService.java`
- [x] T011 [P] Add `private String skipReason` field (nullable) to `RemediationReportRequest` in `agents/unix/src/main/java/com/spulido/agent/worker/http/dto/RemediationReportRequest.java`
- [x] T012 [P] Add `skipReason` field to `RemediationReportRequest` DTO in `api/src/main/java/com/spulido/tfg/domain/remediation/model/dto/RemediationReportRequest.java`
- [x] T013 [P] Add `@Field private String skipReason` field (nullable) to `RemediationRecord` entity in `api/src/main/java/com/spulido/tfg/domain/remediation/model/RemediationRecord.java`
- [x] T014 [P] Add `private String skipReason` field (nullable) to `RemediationInfo` DTO in `api/src/main/java/com/spulido/tfg/domain/remediation/model/dto/RemediationInfo.java`

**Checkpoint**: Foundation ready — all shared infrastructure is in place. User story implementation can now begin.

---

## Phase 3: User Story 1 - Agent Detects Docker Environment and Skips Remediation (Priority: P1) 🎯 MVP

**Goal**: When the agent detects it is running inside a Docker container (or any container runtime), it skips the remediation step entirely without executing any fix commands, and reports the skip reason.

**Independent Test**: Deploy agent inside a Docker container with a plan that includes a remediation step. Execute the plan. Verify the remediation step is skipped, no fix commands run, skip reason is logged.

### Tests for User Story 1

- [x] T015 [P] [US1] Write unit test for `ContainerDetector` — test all 5 detection methods (mock filesystem with temp files), test inconclusive detection (files unreadable), test no-container scenario (none of the indicators present), test detection completes under 1 second in `agents/unix/src/test/java/com/spulido/agent/container/ContainerDetectorTest.java`
- [x] T016 [US1] Write unit test for `RemediationStepHandler` with container detected: verify handler returns `StepResult.skipped()`, does NOT call `httpClient.lookupVulnerabilities()`, does NOT call `httpClient.requestRemediationStrategy()`, does NOT call `commandExecutor.execute()`, calls `httpClient.reportRemediationResult()` with `status=SKIPPED` and non-null `skipReason` in `agents/unix/src/test/java/com/spulido/agent/worker/step/RemediationStepHandlerTest.java` (add new test methods)

### Implementation for User Story 1

- [x] T017 [US1] Modify `RemediationStepHandler` constructor to accept `ContainerDetector` parameter (alongside existing `AgentHttpClient` and `CommandExecutor`) in `agents/unix/src/main/java/com/spulido/agent/worker/step/RemediationStepHandler.java`
- [x] T018 [US1] Add pre-condition guard clause at top of `RemediationStepHandler.handle()`: call `containerDetector.detect()`, if `container=true` return `StepResult.skipped(action, logs)` immediately with detection evidence in logs, before any vulnerability lookup or remediation commands execute. Use filesystem-only checks (no external process spawn) per GraalVM constraints in `agents/unix/src/main/java/com/spulido/agent/worker/step/RemediationStepHandler.java`
- [x] T019 [US1] Add detection result details (which indicator matched, runtime name) to the skip logs at INFO level for auditability in `agents/unix/src/main/java/com/spulido/agent/worker/step/RemediationStepHandler.java`
- [x] T020 [US1] Verify feature: run `ContainerDetectorTest` and `RemediationStepHandlerTest` unit tests, confirm detection logic and skip behavior work correctly

**Checkpoint**: Agent running in a container skips remediation and logs detection evidence. Proceed to User Story 2.

---

## Phase 4: User Story 2 - Skip Reason Is Documented in the Remediation Report (Priority: P1)

**Goal**: When remediation is skipped due to container detection, the remediation report sent to the central platform includes a clear, human-readable `skipReason` field. Security operators can see this reason in the remediation history and detail views.

**Independent Test**: Deploy agent in Docker container, run plan with remediation, inspect remediation report on central platform via API — verify `skipReason` field is populated and visible.

### Tests for User Story 2

- [x] T021 [P] [US2] Write unit test for `RemediationService.createRemediation()` verifying `skipReason` is persisted in MongoDB when provided in the request in `api/src/test/java/com/spulido/tfg/domain/remediation/services/RemediationServiceTest.java` (add new test method)
- [x] T022 [P] [US2] Write unit test for `RemediationController` verifying `GET /api/remediations/{id}` returns `skipReason` field when present, and `GET /api/remediations` includes it in the paginated response in `api/src/test/java/com/spulido/tfg/domain/remediation/controller/RemediationControllerTest.java` (add new test methods)

### Implementation for User Story 2

- [x] T023 [US2] In `RemediationStepHandler`, when container is detected, populate `RemediationReportRequest.skipReason` with the appropriate human-readable string based on detection confidence and runtime. Use three distinct messages: (a) Docker confirmed, (b) non-Docker container confirmed, (c) inconclusive/precaution in `agents/unix/src/main/java/com/spulido/agent/worker/step/RemediationStepHandler.java`
- [x] T024 [US2] Set `cveId` to `"CONTAINER-DETECTED"`, `remediationType` to `"CONTAINER_DETECTED"`, and `status` to `"SKIPPED"` in the report when container skip occurs — per contract specification in `agents/unix/src/main/java/com/spulido/agent/worker/step/RemediationStepHandler.java`
- [x] T025 [US2] In `api/`, ensure `RemediationService.createRemediation()` maps the incoming `skipReason` field from the request DTO to the `RemediationRecord` entity. No additional logic needed — field propagation is automatic via existing Lombok/setter patterns in `api/src/main/java/com/spulido/tfg/domain/remediation/services/RemediationService.java` (verify existing code handles the new field)
- [x] T026 [US2] In `api/`, verify `RemediationService.toInfo()` maps `skipReason` from `RemediationRecord` to `RemediationInfo` DTO. If manual mapping exists, add the field; if using Lombok builders, confirm field propagation in `api/src/main/java/com/spulido/tfg/domain/remediation/services/RemediationService.java`
- [x] T027 [US2] Verify feature: run API unit tests, confirm `skipReason` is persisted and returned in API responses

**Checkpoint**: Remediation reports include clear skip reasons visible to operators through the API. Proceed to User Story 3.

---

## Phase 5: User Story 3 - Agent Proceeds to Next Plan Step After Skipping (Priority: P2)

**Goal**: After skipping a remediation step due to container detection, the agent continues executing the remaining steps in its plan rather than aborting. The plan completes with PARTIAL status.

**Independent Test**: Deploy agent in Docker container with a multi-step plan (e.g., scan → remediation → report). Verify after skipping remediation, the agent still executes the report step.

### Tests for User Story 3

- [x] T028 [US3] Write unit test for `TaskExecutionService.executeJob()`: with a multi-step plan and a step handler that returns `StepResult.skipped()`, verify the job continues to the next step and completes with `JobStatus.COMPLETED` (not FAILED) in `agents/unix/src/test/java/com/spulido/agent/worker/TaskExecutionServiceTest.java` (add new test method)
- [x] T029 [US3] Write integration-style unit test for `RemediationStepHandler` on a real host (no container): verify normal flow still works — vulnerabilities are looked up, strategies fetched, commands executed, and reports sent with `status=SUCCESS` in `agents/unix/src/test/java/com/spulido/agent/worker/step/RemediationStepHandlerTest.java` (add new test method)

### Implementation for User Story 3

- [x] T030 [US3] Verify `TaskExecutionService.executeJob()` handles `StepResult.isSkipped()` correctly — job status should be `COMPLETED` (not `FAILED`) when all steps succeed or skip, with plan summary indicating partial completion in `agents/unix/src/main/java/com/spulido/agent/worker/TaskExecutionService.java` (review changes from T010)
- [x] T031 [US3] If the remediation step is the only step in the plan and is skipped, ensure the plan reports PARTIAL status to the central platform via `StepStatusUpdate`. Verify no alert is triggered for skipped status in `agents/unix/src/main/java/com/spulido/agent/worker/step/RemediationStepHandler.java` (add log line indicating plan-partial outcome)
- [x] T032 [US3] Verify feature: run all agent unit tests, confirm skipped steps don't abort plans and normal remediation flow is unaffected

**Checkpoint**: All three user stories are implemented and independently verifiable. Proceed to Polish.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation, edge case handling, and documentation

- [x] T033 [P] Write unit test for `ContainerDetector` edge cases: empty `/proc/1/cgroup`, large mountinfo with no Docker entries, mixed signals (`.dockerenv` present but no cgroup evidence — should still detect), Podman detection via `/run/.containerenv` in `agents/unix/src/test/java/com/spulido/agent/container/ContainerDetectorTest.java` (add edge case test methods)
- [x] T034 [P] Verify `ContainerDetector` makes NO network calls (detection is purely filesystem-based) — review implementation for compliance
- [x] T035 [P] Verify existing kernel-skip flow is unaffected: kernel updates (package matching `linux-(image|headers|modules)-.*`) still produce `SKIPPED` status with `skipReason=null`, and the UI handles null `skipReason` gracefully
- [x] T036 Build both modules and run full test suites: `cd agents/unix && ./mvnw clean test` and `cd api && ./mvnw clean test`
- [x] T037 Review all user-facing strings: `skipReason` messages must be in English, clear, and follow existing i18n conventions per AGENTS.md
- [x] T038 Run quickstart.md validation scenarios: unit tests, integration test (Docker container), API persistence test
- [x] T039 Verify GraalVM native compatibility: `ContainerDetector` uses only `java.nio.file.Files` (no reflection, no classpath scanning, no external process spawn). Build native image: `cd agents/unix && ./mvnw -Pnative native:compile -DmainClass=com.spulido.agent.AgentApplication` and confirm compilation succeeds

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational phase (ContainerDetector, StepResult) — P1
- **User Story 2 (Phase 4)**: Depends on US1 (needs detection + skip to produce report with skipReason) — P1
- **User Story 3 (Phase 5)**: Depends on US1 (needs skip behavior to verify continuation) and US2 (needs report) — P2
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Phase 2. Creates ContainerDetector + integrates into RemediationStepHandler. Foundational for all other stories.
- **User Story 2 (P1)**: Depends on US1. Adds skipReason field handling in report DTOs and API. Builds on US1's report generation.
- **User Story 3 (P2)**: Depends on US1. Verifies plan continuation through TaskExecutionService. Primarily validation of foundational changes from T010.

### Within Each Phase (Foundational — Phase 2)

- T005, T006, T007 (ContainerDetectionResult + enums) can all run in parallel — different files
- T008 (ContainerDetector) depends on T005, T006, T007
- T009 (StepResult) is independent — can run parallel with T005-T008
- T010 (TaskExecutionService) depends on T009
- T011, T012, T013, T014 (skipReason DTO/entity fields) can all run in parallel — different modules/files

### Parallel Opportunities

- All Setup tasks (T001-T004) can run in parallel
- In Foundational phase: T005+T006+T007 in parallel, T009 in parallel with T005-T008, T011-T014 in parallel group
- In US1: T015 can run parallel with US1 implementation since it tests ContainerDetector independently
- In US2: T021 and T022 (API tests) can run in parallel
- In Polish: T033, T034, T035 can run in parallel (different concerns)

---

## Parallel Example: Foundational Phase

```bash
# Launch all enum/value-object tasks together:
Task: "T005 Create ContainerDetectionResult in agents/unix/src/main/java/com/spulido/agent/container/ContainerDetectionResult.java"
Task: "T006 Create DetectionMethod enum in agents/unix/src/main/java/com/spulido/agent/container/DetectionMethod.java"
Task: "T007 Create DetectionConfidence enum in agents/unix/src/main/java/com/spulido/agent/container/DetectionConfidence.java"

# After enums complete, launch ContainerDetector + StepResult in parallel:
Task: "T008 Create ContainerDetector in agents/unix/src/main/java/com/spulido/agent/container/ContainerDetector.java"
Task: "T009 Modify StepResult in agents/unix/src/main/java/com/spulido/agent/domain/task/StepResult.java"

# Launch all DTO/entity field additions in parallel:
Task: "T011 Add skipReason to RemediationReportRequest (agent) in .../agent/worker/http/dto/RemediationReportRequest.java"
Task: "T012 Add skipReason to RemediationReportRequest (api) in .../remediation/model/dto/RemediationReportRequest.java"
Task: "T013 Add skipReason to RemediationRecord in .../remediation/model/RemediationRecord.java"
Task: "T014 Add skipReason to RemediationInfo in .../remediation/model/dto/RemediationInfo.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Run ContainerDetectorTest and RemediationStepHandlerTest
5. Agent detects Docker → skips remediation → MVP delivered

### Incremental Delivery

1. Setup + Foundational → Building blocks in place
2. Add US1 → Detection + skip core → MVP!
3. Add US2 → Report with skipReason visible to operators
4. Add US3 → Plan continuation verified, no regressions
5. Polish → Edge cases, native image compatibility, full test suite

### Key Files Summary

| File | Module | Change | Tasks |
|------|--------|--------|-------|
| `ContainerDetectionResult.java` | agents/unix | NEW | T005 |
| `DetectionMethod.java` | agents/unix | NEW | T006 |
| `DetectionConfidence.java` | agents/unix | NEW | T007 |
| `ContainerDetector.java` | agents/unix | NEW | T008 |
| `StepResult.java` | agents/unix | MODIFY | T009 |
| `TaskExecutionService.java` | agents/unix | MODIFY | T010, T030 |
| `RemediationReportRequest.java` | agents/unix | MODIFY | T011 |
| `RemediationStepHandler.java` | agents/unix | MODIFY | T017-T019, T023-T024, T031 |
| `ContainerDetectorTest.java` | agents/unix | NEW | T015, T033 |
| `RemediationStepHandlerTest.java` | agents/unix | MODIFY | T016, T029 |
| `TaskExecutionServiceTest.java` | agents/unix | MODIFY | T028 |
| `RemediationRecord.java` | api | MODIFY | T013 |
| `RemediationReportRequest.java` | api | MODIFY | T012 |
| `RemediationInfo.java` | api | MODIFY | T014 |
| `RemediationService.java` | api | MODIFY | T025-T026 |
| `RemediationServiceTest.java` | api | MODIFY | T021 |
| `RemediationControllerTest.java` | api | MODIFY | T022 |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- All detection methods are filesystem-only (`java.nio.file.Files`) — GraalVM-safe
- The `RemediationStatus.SKIPPED` enum value already exists — no new status needed
- Kernel-update skips continue to produce `SKIPPED` with `skipReason=null` — backward compatible
- Agent detection uses allowlist strategy: only declare container when a known indicator matches
- Do not run git commands unless the user explicitly approves them
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
