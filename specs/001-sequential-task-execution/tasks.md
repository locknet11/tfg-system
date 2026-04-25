# Tasks: Sequential Task Execution

**Input**: Design documents from `/specs/001-sequential-task-execution/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/, quickstart.md

**Tests**: Include verification tasks for every affected module. Add unit, integration,
or contract tests when required by the specification, risk profile, or existing patterns.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- Unix agent code lives in `agents/unix/src/main/java/com/spulido/agent/`
- Unix agent tests live in `agents/unix/src/test/java/com/spulido/agent/`
- Feature docs live in `specs/001-sequential-task-execution/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare the unix agent module for task-sequencing work and align the implementation surface with the plan.

- [x] T001 Review existing worker flow in `agents/unix/src/main/java/com/spulido/agent/worker/WorkerCoordinator.java`
- [x] T002 Review lifecycle and executor configuration in `agents/unix/src/main/java/com/spulido/agent/utils/AgentLifecycle.java` and `agents/unix/src/main/java/com/spulido/agent/config/WorkerPoolConfig.java`
- [x] T003 Create task execution package structure under `agents/unix/src/main/java/com/spulido/agent/domain/` and `agents/unix/src/main/java/com/spulido/agent/worker/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Build the shared job and task model plus the execution contract used by all user stories.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Create `JobStatus` and `TaskStatus` enums in `agents/unix/src/main/java/com/spulido/agent/domain/`
- [x] T005 [P] Create `TaskResult` and `TaskDefinition` domain classes in `agents/unix/src/main/java/com/spulido/agent/domain/`
- [x] T006 [P] Create `AgentTask` and `AgentJob` domain classes in `agents/unix/src/main/java/com/spulido/agent/domain/`
- [x] T007 Create job validation and task ordering logic in `agents/unix/src/main/java/com/spulido/agent/worker/`
- [x] T008 Define command execution abstraction for unix agent steps in `agents/unix/src/main/java/com/spulido/agent/worker/`
- [x] T009 Configure shared task-state logging and error reporting hooks in `agents/unix/src/main/java/com/spulido/agent/worker/`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Run Ordered Agent Work (Priority: P1) 🎯 MVP

**Goal**: Break a received job into separate tasks and execute them strictly in order.

**Independent Test**: Submit a multi-step in-memory job and verify the agent creates one task per step, executes them in order, and marks the job complete only after the last task succeeds.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T010 [P] [US1] Add ordered task execution unit tests in `agents/unix/src/test/java/com/spulido/agent/worker/TaskExecutionServiceTest.java`
- [x] T011 [P] [US1] Add worker coordination sequencing tests in `agents/unix/src/test/java/com/spulido/agent/worker/WorkerCoordinatorTest.java`

### Implementation for User Story 1

- [x] T012 [P] [US1] Implement job-to-task mapping in `agents/unix/src/main/java/com/spulido/agent/worker/`
- [x] T013 [US1] Implement sequential task execution service in `agents/unix/src/main/java/com/spulido/agent/worker/`
- [x] T014 [US1] Update `agents/unix/src/main/java/com/spulido/agent/worker/WorkerCoordinator.java` to delegate job execution to the task execution service
- [x] T015 [US1] Add task start, completion, and job completion state transitions in `agents/unix/src/main/java/com/spulido/agent/domain/`
- [x] T016 [US1] Verify sequential execution behavior with `agents/unix/src/test/java/com/spulido/agent/worker/TaskExecutionServiceTest.java` and `agents/unix/src/test/java/com/spulido/agent/worker/WorkerCoordinatorTest.java`

**Checkpoint**: User Story 1 should be independently functional and demonstrate ordered task execution.

---

## Phase 4: User Story 2 - Stop On Task Failure (Priority: P2)

**Goal**: Stop remaining task execution as soon as one task fails, times out, or is interrupted.

**Independent Test**: Submit a multi-step job where the middle task fails and verify the remaining tasks do not start, the failed task captures the reason, and the job ends in a non-complete state.

### Tests for User Story 2

- [x] T017 [P] [US2] Add fail-fast execution tests in `agents/unix/src/test/java/com/spulido/agent/worker/TaskExecutionFailureTest.java`
- [x] T018 [P] [US2] Add interruption and timeout state tests in `agents/unix/src/test/java/com/spulido/agent/worker/TaskStateTransitionTest.java`

### Implementation for User Story 2

- [x] T019 [US2] Implement task failure handling and failure result capture in `agents/unix/src/main/java/com/spulido/agent/worker/`
- [x] T020 [US2] Implement timeout and interruption transitions in `agents/unix/src/main/java/com/spulido/agent/domain/`
- [x] T021 [US2] Update `agents/unix/src/main/java/com/spulido/agent/worker/WorkerCoordinator.java` to stop remaining tasks after failure
- [x] T022 [US2] Add failure reason propagation to task and job records in `agents/unix/src/main/java/com/spulido/agent/domain/`
- [x] T023 [US2] Verify fail-fast behavior with `agents/unix/src/test/java/com/spulido/agent/worker/TaskExecutionFailureTest.java` and `agents/unix/src/test/java/com/spulido/agent/worker/TaskStateTransitionTest.java`

**Checkpoint**: User Stories 1 and 2 should both work independently, with fail-fast semantics enforced.

---

## Phase 5: User Story 3 - Observe Task Progress (Priority: P3)

**Goal**: Surface task-level progress and final outcomes so operators can understand job execution history.

**Independent Test**: Execute a job and verify task records expose pending, running, completed, failed, skipped, or interrupted states together with execution order and timestamps.

### Tests for User Story 3

- [x] T024 [P] [US3] Add task progress visibility tests in `agents/unix/src/test/java/com/spulido/agent/worker/TaskProgressReportingTest.java`
- [x] T025 [P] [US3] Add job history state tests in `agents/unix/src/test/java/com/spulido/agent/worker/JobHistoryViewTest.java`

### Implementation for User Story 3

- [x] T026 [US3] Implement task progress snapshot or view model in `agents/unix/src/main/java/com/spulido/agent/domain/`
- [x] T027 [US3] Add task status reporting hooks in `agents/unix/src/main/java/com/spulido/agent/worker/`
- [x] T028 [US3] Update `agents/unix/src/main/java/com/spulido/agent/worker/WorkerCoordinator.java` to expose current and final task progress data
- [x] T029 [US3] Add ordered job history rendering support in `agents/unix/src/main/java/com/spulido/agent/domain/`
- [x] T030 [US3] Verify progress reporting behavior with `agents/unix/src/test/java/com/spulido/agent/worker/TaskProgressReportingTest.java` and `agents/unix/src/test/java/com/spulido/agent/worker/JobHistoryViewTest.java`

**Checkpoint**: All user stories should now be independently functional with visible task-level progress.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Finalize documentation, tighten code quality, and validate the planned quickstart path.

- [x] T031 Update unix agent module documentation in `agents/unix/HELP.md` if task execution behavior needs operator guidance
- [x] T032 Update feature documentation notes in `specs/001-sequential-task-execution/quickstart.md` if verification steps change during implementation
- [x] T033 Run full unix agent verification in `agents/unix/` with `./mvnw test`
- [x] T034 Confirm English-only logs, messages, and non-obvious comments across `agents/unix/src/main/java/com/spulido/agent/`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational completion
- **User Story 2 (Phase 4)**: Depends on Foundational completion and builds on the sequencing service from User Story 1
- **User Story 3 (Phase 5)**: Depends on Foundational completion and uses task/job state produced by User Stories 1 and 2
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - establishes the execution backbone and is the MVP
- **User Story 2 (P2)**: Depends on User Story 1 because failure handling extends the same execution pipeline
- **User Story 3 (P3)**: Depends on User Stories 1 and 2 because progress reporting relies on final task states and failure outcomes

### Within Each User Story

- Tests MUST be written and fail before implementation
- Domain state support before coordinator integration
- Execution services before worker wiring
- Story verification before moving to the next priority
- Ask instead of guessing when implementation details are unclear

### Parallel Opportunities

- `T005` and `T006` can run in parallel after `T004`
- `T010` and `T011` can run in parallel for User Story 1
- `T017` and `T018` can run in parallel for User Story 2
- `T024` and `T025` can run in parallel for User Story 3
- Documentation updates `T031` and `T032` can run in parallel after implementation stabilizes

---

## Parallel Example: User Story 1

```bash
# Launch User Story 1 tests together:
Task: "Add ordered task execution unit tests in agents/unix/src/test/java/com/spulido/agent/worker/TaskExecutionServiceTest.java"
Task: "Add worker coordination sequencing tests in agents/unix/src/test/java/com/spulido/agent/worker/WorkerCoordinatorTest.java"

# Launch foundational domain work together once statuses are defined:
Task: "Create TaskResult and TaskDefinition domain classes in agents/unix/src/main/java/com/spulido/agent/domain/"
Task: "Create AgentTask and AgentJob domain classes in agents/unix/src/main/java/com/spulido/agent/domain/"
```

---

## Parallel Example: User Story 2

```bash
# Launch User Story 2 tests together:
Task: "Add fail-fast execution tests in agents/unix/src/test/java/com/spulido/agent/worker/TaskExecutionFailureTest.java"
Task: "Add interruption and timeout state tests in agents/unix/src/test/java/com/spulido/agent/worker/TaskStateTransitionTest.java"
```

---

## Parallel Example: User Story 3

```bash
# Launch User Story 3 tests together:
Task: "Add task progress visibility tests in agents/unix/src/test/java/com/spulido/agent/worker/TaskProgressReportingTest.java"
Task: "Add job history state tests in agents/unix/src/test/java/com/spulido/agent/worker/JobHistoryViewTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Run `cd agents/unix && ./mvnw test` and verify ordered task execution behavior

### Incremental Delivery

1. Finish Setup and Foundational work
2. Deliver User Story 1 for ordered task execution
3. Extend with User Story 2 for fail-fast handling
4. Extend with User Story 3 for task-level visibility
5. Finish with documentation and full verification

### Parallel Team Strategy

1. One developer completes Setup and Foundational phases
2. After foundation is ready:
   - Developer A: User Story 1 execution service and coordinator wiring
   - Developer B: User Story 2 failure-state handling after User Story 1 service shape is stable
   - Developer C: User Story 3 progress view and reporting after state model stabilizes

---

## Notes

- All tasks follow the required checklist format with checkbox, task ID, labels, and file paths
- The suggested MVP scope is Phase 3 / User Story 1 only
- Verification is centered on deterministic tests in `agents/unix/src/test/java/com/spulido/agent/worker/`
- Do not run git commands unless the user explicitly approves them
