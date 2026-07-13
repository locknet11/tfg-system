# Tasks: Agent Tool Bundling

**Input**: Design documents from `/specs/020-agent-tool-bundling/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/step-handler-interface.md, quickstart.md

**Tests**: Unit test tasks are included per the module's existing pattern (JUnit 5 + Mockito, mocked `CommandExecutor`, no real network/tool access in unit scope, per `AGENTS.md`).

**Organization**: Tasks are grouped by user story (US1/US2/US3 from spec.md) to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Maps task to US1, US2, or US3
- All paths are relative to the repository root

## Phase 1: Setup

- [X] T001 Review `AGENTS.md` (agents/unix stack conventions) and `specs/020-agent-tool-bundling/plan.md` before starting implementation
- [X] T002 [P] Create resource directories `agents/unix/src/main/resources/tools/linux-amd64/` and `agents/unix/src/main/resources/tools/darwin-arm64/`
- [X] T003 [P] Create package directory `agents/unix/src/main/java/com/spulido/agent/worker/tools/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Bundled-tool extraction, PATH wiring, and the `StepHandler` signature change that every user story below depends on.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 Source/build statically-linked `nmap`, `rustscan`, `nc`, and `curl` binaries for `linux-amd64` and `darwin-arm64`, place them under `agents/unix/src/main/resources/tools/<os-arch>/`, and document version/provenance in `agents/unix/src/main/resources/tools/README.md`
- [X] T005 [P] Create `BundledTool` enum (`NETWORK_DISCOVERY`, `PORT_SERVICE_SCAN`, `RAW_TCP`, `FILE_RETRIEVAL` with `binaryName`/`capabilityDescription`) in `agents/unix/src/main/java/com/spulido/agent/worker/tools/BundledTool.java`
- [X] T006 [P] Create `ToolExtractionException` in `agents/unix/src/main/java/com/spulido/agent/worker/tools/ToolExtractionException.java`
- [X] T007 Implement `BundledToolProvisioner` (detect `os.name`/`os.arch`, extract matching `tools/<os-arch>/*` classpath resources to a runtime temp directory, set the executable bit, expose resolved absolute paths and the extraction directory, throw `ToolExtractionException` when no matching variant exists) in `agents/unix/src/main/java/com/spulido/agent/worker/tools/BundledToolProvisioner.java` (depends on T005, T006)
- [X] T008 Register native-image resource hints for `tools/**` classpath resources via a `RuntimeHintsRegistrar` wired with `@ImportRuntimeHints` on `agents/unix/src/main/java/com/spulido/agent/AgentApplication.java`, implemented in `agents/unix/src/main/java/com/spulido/agent/config/BundledToolResourceHints.java`
- [X] T009 Change the `StepHandler` interface to `handle(StepAction action, Map<StepAction, StepResult> context, String targetIp)` in `agents/unix/src/main/java/com/spulido/agent/worker/step/StepHandler.java`
- [X] T010 [P] Update `EchoStepHandler` for the new signature (parameter unused) in `agents/unix/src/main/java/com/spulido/agent/worker/step/EchoStepHandler.java`
- [X] T011 [P] Update `RemediationStepHandler` for the new signature (parameter unused) in `agents/unix/src/main/java/com/spulido/agent/worker/step/RemediationStepHandler.java`
- [X] T012 [P] Update `ExecuteExploitStepHandler` for the new signature (parameter unused) in `agents/unix/src/main/java/com/spulido/agent/worker/step/ExecuteExploitStepHandler.java`
- [X] T013 [P] Update `ExploitationKnowledgeStepHandler` for the new signature (parameter unused) in `agents/unix/src/main/java/com/spulido/agent/worker/step/ExploitationKnowledgeStepHandler.java`
- [X] T014 [P] Update `RequestReplicationStepHandler` for the new signature (parameter unused) in `agents/unix/src/main/java/com/spulido/agent/worker/step/RequestReplicationStepHandler.java`
- [X] T015 [P] Update `TransferAgentStepHandler` for the new signature (parameter unused) in `agents/unix/src/main/java/com/spulido/agent/worker/step/TransferAgentStepHandler.java`
- [X] T016 Update `TaskExecutionService.executeJob` to accept and thread a `targetIp` parameter into every `handler.handle(stepAction, context, targetIp)` call in `agents/unix/src/main/java/com/spulido/agent/worker/TaskExecutionService.java` (depends on T009–T015)
- [X] T017 Update `WorkerCoordinator.runJob` to pass `planResponse.getTargetIp()` into `taskExecutionService.executeJob(...)` in `agents/unix/src/main/java/com/spulido/agent/worker/WorkerCoordinator.java` (depends on T016)
- [X] T018 Update the `CommandExecutor` bean to prepend `BundledToolProvisioner`'s extraction directory to the `PATH` environment variable used by its `ProcessBuilder` in `agents/unix/src/main/java/com/spulido/agent/config/WorkerPoolConfig.java` (depends on T007)
- [X] T019 [P] Update `ExecuteExploitStepHandlerTest` for the new 3-arg `handle()` signature in `agents/unix/src/test/java/com/spulido/agent/worker/step/ExecuteExploitStepHandlerTest.java`
- [X] T020 [P] Update `TransferAgentStepHandlerTest` for the new 3-arg `handle()` signature in `agents/unix/src/test/java/com/spulido/agent/worker/step/TransferAgentStepHandlerTest.java`
- [X] T021 [P] Add `BundledToolProvisionerTest` covering successful extraction and `ToolExtractionException` for an unsupported os/arch in `agents/unix/src/test/java/com/spulido/agent/worker/tools/BundledToolProvisionerTest.java` (depends on T007)

**Checkpoint**: Foundation ready — bundled tools extract at startup, resolve via `PATH`, and every `StepHandler` receives `targetIp`. User story implementation can now begin.

---

## Phase 3: User Story 1 - Reconnaissance steps produce real results on any target host (Priority: P1) 🎯 MVP

**Goal**: `NETWORK_SCAN` and `SERVICE_SCAN`/`SYSTEM_SCAN` plan steps use bundled `nmap`/`rustscan` to report real discovered hosts, ports, and services instead of the current `EchoStepHandler` placeholder.

**Independent Test**: Deploy the agent to a host with no scanning tools installed, run a plan with a network scan and a service/port scan step, and confirm the reported `StepResult` contains real findings.

### Implementation for User Story 1

- [X] T022 [P] [US1] Create `NetworkScanStepHandler` (bundled `nmap` host-discovery against `targetIp`, emits `HOST_FOUND:<address>` log lines, returns `StepResult.success`) in `agents/unix/src/main/java/com/spulido/agent/worker/step/NetworkScanStepHandler.java` (depends on T007, T009, T018)
- [X] T023 [P] [US1] Create `ServiceScanStepHandler` (bundled `rustscan` port sweep + `nmap` service/version detection against `targetIp`, populates `ServiceInfo` entries) in `agents/unix/src/main/java/com/spulido/agent/worker/step/ServiceScanStepHandler.java` (depends on T007, T009, T018)
- [X] T024 [US1] Wire `NetworkScanStepHandler` into `WorkerCoordinator.createDefaultStepHandlers` for `StepAction.NETWORK_SCAN`, replacing the `EchoStepHandler` placeholder, in `agents/unix/src/main/java/com/spulido/agent/worker/WorkerCoordinator.java` (depends on T022)
- [X] T025 [US1] Wire `ServiceScanStepHandler` into `WorkerCoordinator.createDefaultStepHandlers` for `StepAction.SERVICE_SCAN` and `StepAction.SYSTEM_SCAN`, replacing the `EchoStepHandler` placeholders, in `agents/unix/src/main/java/com/spulido/agent/worker/WorkerCoordinator.java` (depends on T023)
- [X] T026 [P] [US1] Add `NetworkScanStepHandlerTest` (mocked `CommandExecutor`, assert `HOST_FOUND` logs parsed and `StepResult.success`) in `agents/unix/src/test/java/com/spulido/agent/worker/step/NetworkScanStepHandlerTest.java` (depends on T022)
- [X] T027 [P] [US1] Add `ServiceScanStepHandlerTest` (mocked `CommandExecutor`, assert `ServiceInfo` entries populated and `StepResult.success`) in `agents/unix/src/test/java/com/spulido/agent/worker/step/ServiceScanStepHandlerTest.java` (depends on T023)
- [X] T028 [US1] Verify User Story 1 acceptance scenarios against the `lab/` target per `specs/020-agent-tool-bundling/quickstart.md` ("Verify a scan step produces real results")

**Checkpoint**: User Story 1 is fully functional and independently testable.

---

## Phase 4: User Story 2 - Steps that need to fetch a remote resource work without host tooling (Priority: P2)

**Goal**: Any command executed by the agent (dedicated handler or free-form remediation/exploit script) can retrieve a remote file using the bundled `curl`, without the target host having `wget`/`curl` installed.

**Independent Test**: On a host without `wget`/`curl`, run a plan whose step fetches a remote file and confirm it succeeds using the bundled binary.

### Implementation for User Story 2

- [X] T029 [US2] Add a unit test asserting the `CommandExecutor` `ProcessBuilder`'s environment `PATH` includes `BundledToolProvisioner`'s extraction directory in `agents/unix/src/test/java/com/spulido/agent/config/WorkerPoolConfigTest.java` (depends on T018)
- [X] T030 [US2] Add a regression test confirming a command string that references `curl` by bare name (as `RemediationStepHandler`/`ExecuteExploitStepHandler` would execute) resolves to the bundled binary via the augmented `PATH` in `agents/unix/src/test/java/com/spulido/agent/worker/step/RemediationStepHandlerCommandResolutionTest.java` (depends on T018)
- [X] T031 [US2] Verify User Story 2 acceptance scenarios per `specs/020-agent-tool-bundling/quickstart.md` (fetch a remote file on a host without `wget`/`curl` installed)

**Checkpoint**: User Stories 1 and 2 both work independently.

---

## Phase 5: User Story 3 - Missing/incompatible tooling fails loudly instead of silently (Priority: P3)

**Goal**: When a bundled tool cannot complete its task (missing for the platform, execution failure, timeout), the affected step is reported as failed with a descriptive error rather than a false success.

**Independent Test**: Force a bundled-tool failure (unsupported platform, forced timeout) and confirm the plan step is reported as failed with an explanatory message.

### Implementation for User Story 3

- [X] T032 [P] [US3] Map bundled-tool execution failures and timeouts to `StepResult.failure` with a `TOOL_ERROR:<tool>:<reason>` log line in `NetworkScanStepHandler` (`agents/unix/src/main/java/com/spulido/agent/worker/step/NetworkScanStepHandler.java`) (depends on T022)
- [X] T033 [P] [US3] Same failure/timeout mapping in `ServiceScanStepHandler` (`agents/unix/src/main/java/com/spulido/agent/worker/step/ServiceScanStepHandler.java`) (depends on T023)
- [X] T034 [US3] Handle `ToolExtractionException` from `BundledToolProvisioner` at agent startup: log clearly and ensure steps depending on a missing tool fail with a descriptive error instead of a false success, in `agents/unix/src/main/java/com/spulido/agent/config/WorkerPoolConfig.java` (depends on T007, T018)
- [X] T035 [P] [US3] Extend `NetworkScanStepHandlerTest` with a forced-timeout/failure case asserting `StepResult.failure` and a `TOOL_ERROR` log line in `agents/unix/src/test/java/com/spulido/agent/worker/step/NetworkScanStepHandlerTest.java` (depends on T032)
- [X] T036 [P] [US3] Extend `ServiceScanStepHandlerTest` with a forced-timeout/failure case asserting `StepResult.failure` and a `TOOL_ERROR` log line in `agents/unix/src/test/java/com/spulido/agent/worker/step/ServiceScanStepHandlerTest.java` (depends on T033)
- [X] T037 [US3] Verify User Story 3 acceptance scenarios per `specs/020-agent-tool-bundling/quickstart.md` ("Verify failure is reported honestly")

**Checkpoint**: All user stories are independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T038 [P] Confirm English-only logs/messages and that any new comments are non-obvious-only across all files touched by this feature
- [X] T039 Run `./mvnw test` in `agents/unix` and fix any regressions across all changed `StepHandler` implementations
- [X] T040 Run the native build (`sh agents/unix/package-macos.sh` or `./mvnw -Pnative native:compile -DmainClass=com.spulido.agent.AgentApplication`) to confirm bundled resources and native-image resource hints still produce a working native binary
- [X] T041 Run the full `specs/020-agent-tool-bundling/quickstart.md` validation end-to-end against the `lab/` Docker targets
- [X] T042 Update `agents/unix/src/main/resources/tools/README.md` with the final binary versions/checksums used in the shipped build

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately.
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories (bundled-tool extraction, `PATH` wiring, and the `StepHandler` signature change are shared prerequisites).
- **User Stories (Phase 3–5)**: All depend on Foundational completion.
  - US1 (P1) has no dependency on US2/US3.
  - US2 (P2) depends only on the Foundational `PATH` wiring (T018), not on US1.
  - US3 (P3) extends the handlers created in US1 (T022, T023) with failure-path behavior — implement after US1; can proceed in parallel with US2.
- **Polish (Phase 6)**: Depends on all desired user stories being complete.

### Parallel Opportunities

- T002, T003 (Setup) in parallel.
- T005, T006 (Foundational, different files) in parallel; T010–T015 (six `StepHandler` implementations, different files) in parallel once T009 is done; T019–T021 in parallel.
- T022, T023 (US1 handlers, different files) in parallel; T026, T027 (US1 tests) in parallel once their respective handler exists.
- T032, T033 (US3 failure-mapping, different files) in parallel; T035, T036 (US3 tests) in parallel.

---

## Parallel Example: Foundational Phase

```bash
# After T009 (StepHandler interface change) lands, update all six implementations together:
Task: "Update EchoStepHandler for the new signature in agents/unix/src/main/java/com/spulido/agent/worker/step/EchoStepHandler.java"
Task: "Update RemediationStepHandler for the new signature in agents/unix/src/main/java/com/spulido/agent/worker/step/RemediationStepHandler.java"
Task: "Update ExecuteExploitStepHandler for the new signature in agents/unix/src/main/java/com/spulido/agent/worker/step/ExecuteExploitStepHandler.java"
Task: "Update ExploitationKnowledgeStepHandler for the new signature in agents/unix/src/main/java/com/spulido/agent/worker/step/ExploitationKnowledgeStepHandler.java"
Task: "Update RequestReplicationStepHandler for the new signature in agents/unix/src/main/java/com/spulido/agent/worker/step/RequestReplicationStepHandler.java"
Task: "Update TransferAgentStepHandler for the new signature in agents/unix/src/main/java/com/spulido/agent/worker/step/TransferAgentStepHandler.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: run the quickstart.md scan verification against the `lab/` targets
5. This alone replaces the placeholder `EchoStepHandler` behavior for all three scan-type `StepAction`s, the core problem stated in the spec.

### Incremental Delivery

1. Setup + Foundational → bundled tools extract and resolve via `PATH`; every handler receives `targetIp`.
2. Add User Story 1 → real scan findings (MVP).
3. Add User Story 2 → remote file retrieval works without host `curl`/`wget`.
4. Add User Story 3 → tool failures are reported honestly instead of silently succeeding.
5. Polish → full test/native-build/quickstart validation.

---

## Notes

- [P] tasks touch different files with no unresolved dependency between them.
- Do not run git commands (branch, commit, push) beyond what the `speckit.git.feature` hook already did, without explicit user approval, per the repository constitution.
- Binaries added in T004 are build assets, not source code — document their origin/license (see `research.md` §4 on nmap/rustscan GPL licensing) in the same task's README.
- Stop at each checkpoint to validate a story independently before moving to the next.
