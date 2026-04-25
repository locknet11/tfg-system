# Research: Sequential Task Execution

## Decision 1: Keep sequential orchestration inside `WorkerCoordinator`

- **Decision**: Extend the existing worker coordination flow to orchestrate ordered tasks for
  one job at a time, with a dedicated task execution service called from the coordinator.
- **Rationale**: The current unix agent already polls on a schedule and submits work through
  the existing executor. Keeping orchestration in-process is the smallest change that adds
  task awareness without introducing a second runtime model.
- **Alternatives considered**:
  - Introduce a separate queue or orchestration subsystem: rejected because v1 only needs
    ordered execution inside one agent instance.
  - Run every step directly inside `pollCentralPlatform()`: rejected because task tracking,
    failure handling, and testability would remain tightly coupled to scheduler logic.

## Decision 2: Model explicit job and task states in memory

- **Decision**: Introduce explicit job and task state objects with ordered positions, status,
  timestamps, and failure details held in memory during execution.
- **Rationale**: The feature requires task-level visibility and failure semantics. A clear
  in-memory model supports deterministic sequencing and can later be mapped to reporting
  payloads if needed.
- **Alternatives considered**:
  - Derive state only from logs: rejected because logs are insufficient for deterministic task
    transitions and testing.
  - Add a persistent database for task storage: rejected because the spec assumes current
    agent-local state or existing reporting integration is sufficient for v1.

## Decision 3: Stop execution on first task failure

- **Decision**: Treat task execution as a fail-fast ordered pipeline. When a task fails or
  times out, mark the task failed, mark the job incomplete/failed, and do not start later
  tasks.
- **Rationale**: This matches the spec, reduces risk on target systems, and is easier to test
  than partial recovery logic.
- **Alternatives considered**:
  - Continue executing remaining tasks after failure: rejected because it violates the main
    user story and can operate on an invalid intermediate state.
  - Add automatic retries in v1: rejected because retry policy is not in scope and would add
    ambiguity to task status transitions.

## Decision 4: Test sequencing with deterministic unit and integration-style service tests

- **Decision**: Validate the orchestration with deterministic tests that use in-memory job
  definitions and stubbed command executors.
- **Rationale**: Repository guidance requires deterministic tests and avoiding network or
  filesystem access in unit scope. Stubbed executors allow success, failure, and interruption
  scenarios to be reproduced reliably.
- **Alternatives considered**:
  - Execute real shell commands in tests: rejected because it creates flaky, environment-
    dependent tests.
  - Test only through logs: rejected because logs do not provide complete assertions for
    ordered state transitions.
