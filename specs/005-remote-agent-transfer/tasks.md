# Tasks: Remote Agent Transfer

**Input**: Design documents from `/specs/005-remote-agent-transfer/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Unit tests included for `SshRemoteCommandExecutor` (mocked ProcessBuilder) and `TransferAgentStepHandler` (both paths + fallback). Integration tests for Path A/B end-to-end with AUTO_APPROVE.

**Organization**: Tasks grouped by user story from spec.md to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4, US5, US6)
- Exact file paths in descriptions

## Path Conventions

- **Agent module**: `agents/unix/src/main/java/com/spulido/agent/`
- **Agent resources**: `agents/unix/src/main/resources/`
- **Agent tests**: `agents/unix/src/test/java/com/spulido/agent/`
- **Working directory**: `agents/unix/` for maven commands

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Value objects, interfaces, and configuration that all user stories depend on

- [x] T001 [P] Create TargetSession value object in `agents/unix/src/main/java/com/spulido/agent/remote/TargetSession.java` — immutable class with targetIp, targetUser, optional sshIdentityFile, non-blank validation
- [x] T002 [P] Create RemoteCommandExecutor interface in `agents/unix/src/main/java/com/spulido/agent/remote/RemoteCommandExecutor.java` — methods: execute(TargetSession, String command, long timeoutSeconds), transferFile(TargetSession, byte[] content, String remotePath, String permissions)
- [x] T003 [P] Add agent.exploit.* config properties to AgentConfig in `agents/unix/src/main/java/com/spulido/agent/config/AgentConfig.java` — add fields defaultTargetUser (default "root"), transferMethod (default "auto"), transferMethodRetries (default 3), transferFileMaxSizeMb (default 100) with getters/setters, bind to `agent.exploit.*` prefix
- [x] T004 [P] Add agent.exploit.* entries to `agents/unix/src/main/resources/application.properties` — agent.exploit.default-target-user=root, agent.exploit.transfer-method=auto, agent.exploit.transfer-method.retries=3, agent.exploit.transfer-file-max-size-mb=100

---

## Phase 2: Foundational — Session Context Propagation (US5, Priority: P3)

**Purpose**: Core remote execution infrastructure and session propagation that MUST be complete before transfer stories (US2, US3, US4). US5 is P3 because the logic is straightforward but it blocks all transfer functionality.

**⚠️ CRITICAL**: No transfer user story work (US2, US3, US4) can begin until this phase is complete

**Independent Test**: Run ExecuteExploitStepHandler after an exploit, verify StepResult logs contain `targetIp:<ip>`, `targetUser:<user>`, `reverseShellActive:true`. Then read EXECUTE_EXPLOIT context from TransferAgentStepHandler and successfully build a TargetSession.

### Implementation for US5

- [x] T005 Implement SshSessionProvisioner in `agents/unix/src/main/java/com/spulido/agent/remote/SshSessionProvisioner.java` — verify SSH connectivity via `ssh -o ConnectTimeout=5 <user>@<ip> 'echo OK'`, retry 3x with 5s intervals, return boolean; use AgentConfig.getDefaultTargetUser() as fallback; ProcessBuilder-based, GraalVM-safe
- [x] T006 Implement SshRemoteCommandExecutor in `agents/unix/src/main/java/com/spulido/agent/remote/SshRemoteCommandExecutor.java` — implement RemoteCommandExecutor: execute() builds `ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 [ -i <identityFile> ] <user>@<ip> '<command>'` via ProcessBuilder, captures stdout/stderr, enforces timeout; transferFile() writes content to temp file, tries SCP then base64 pipe, checks size limit; security: truncate base64 in logs, hide credentials at INFO+
- [x] T007 Refactor ExecuteExploitStepHandler in `agents/unix/src/main/java/com/spulido/agent/worker/step/ExecuteExploitStepHandler.java` — add SshSessionProvisioner constructor parameter; after exploit execution, call provisioner.verify() to check SSH connectivity; record `targetIp:<ip>`, `targetUser:<user>`, `reverseShellActive:true/false` in StepResult.logs; if SSH verification fails after retries, return StepResult.failure()
- [x] T008 Update WorkerPoolConfig in `agents/unix/src/main/java/com/spulido/agent/config/WorkerPoolConfig.java` — add @Bean for SshRemoteCommandExecutor, add @Bean for SshSessionProvisioner; update TaskExecutionService constructor call to pass new beans to WorkerCoordinator.createDefaultStepHandlers()
- [x] T009 Update createDefaultStepHandlers in WorkerCoordinator in `agents/unix/src/main/java/com/spulido/agent/worker/WorkerCoordinator.java` — accept SshSessionProvisioner and RemoteCommandExecutor parameters; pass SshSessionProvisioner to ExecuteExploitStepHandler; pass RemoteCommandExecutor to TransferAgentStepHandler

**Checkpoint**: Foundation ready — SSH session propagation works end-to-end, remote commands can execute on targets. US1 can now be validated.

---

## Phase 3: User Story 1 — Remote Command Execution (Priority: P1) 🎯 MVP

**Goal**: Agent can execute shell commands on an exploited target via SSH and capture stdout/stderr/exit code

**Independent Test**: Mock ProcessBuilder returning "HELLO\n" exit 0, verify TaskResult.success() with output captured. Mock process timeout, verify TaskResult.failure(). Mock SCP failure → base64 fallback in transferFile.

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T010 [P] [US1] Unit test execute() success path in `agents/unix/src/test/java/com/spulido/agent/remote/SshRemoteCommandExecutorTest.java` — mock ProcessBuilder to return "OK\n" exit 0, assert result.success=true and output contains "OK"
- [x] T011 [P] [US1] Unit test execute() timeout in `agents/unix/src/test/java/com/spulido/agent/remote/SshRemoteCommandExecutorTest.java` — mock process.waitFor timeout, assert destroyForcibly called and result is failure with timeout message
- [x] T012 [P] [US1] Unit test execute() non-zero exit in `agents/unix/src/test/java/com/spulido/agent/remote/SshRemoteCommandExecutorTest.java` — mock exit code 1, assert result.success=false with exit code in message
- [x] T013 [P] [US1] Unit test transferFile() SCP success in `agents/unix/src/test/java/com/spulido/agent/remote/SshRemoteCommandExecutorTest.java` — mock SCP ProcessBuilder success, assert temp file created with content and SCP command contains correct user@host:path
- [x] T014 [P] [US1] Unit test transferFile() SCP fail → base64 fallback in `agents/unix/src/test/java/com/spulido/agent/remote/SshRemoteCommandExecutorTest.java` — mock SCP failure twice, assert base64 pipe used, verify base64 content integrity
- [x] T015 [P] [US1] Unit test transferFile() binary too large for pipe in `agents/unix/src/test/java/com/spulido/agent/remote/SshRemoteCommandExecutorTest.java` — provide content exceeding transferFileMaxSizeMb, assert immediate failure before any transfer attempt

**Checkpoint**: Remote command execution is verified and tested. Agent can run commands and transfer files over SSH.

---

## Phase 4: User Story 2 — HTTP Binary Download to Target / Path A (Priority: P1)

**Goal**: When target has curl/wget and can reach Central, the agent instructs the target to download the binary directly from Central and launch it

**Independent Test**: Provide target with curl + Central reachable, verify agent renders install-agent-http.sh.tmpl, transfers script to target, executes it, target downloads binary from Central, health check returns UP within 30s

### Implementation for User Story 2

- [x] T016 [P] [US2] Create install-agent-http.sh.tmpl in `agents/unix/src/main/resources/scripts/install-agent-http.sh.tmpl` — bash script with `set -e`; ensure /tmp exists; check curl vs wget; download from `{{DOWNLOAD_URL}}`; chmod +x /tmp/agent; write agent.properties with `{{CENTRAL_URL}}` and `{{PREAUTH_CODE}}`; nohup launch; echo INSTALL_OK
- [x] T017 [P] [US2] Rename install-agent.sh.tmpl → install-agent-transfer.sh.tmpl and remove base64 blob in `agents/unix/src/main/resources/scripts/` — rename file; strip `echo '{{BINARY_BASE64}}' | base64 -d > /tmp/agent &&` line; keep chmod, config write, nohup launch, echo INSTALL_OK; use `{{CENTRAL_URL}}` and `{{PREAUTH_CODE}}` placeholders
- [x] T018 [US2] Rewrite TransferAgentStepHandler in `agents/unix/src/main/java/com/spulido/agent/worker/step/TransferAgentStepHandler.java` — complete rewrite with: (1) extract TargetSession from EXECUTE_EXPLOIT context logs; (2) extract downloadUrl/preauthCode/centralUrl from REQUEST_REPLICATION context; (3) probe target for curl/wget/Central-reachability via RemoteCommandExecutor.execute(); (4) if Path A viable and transfer-method allows: agent downloads binary from Central via httpClient.downloadBinary(), verify integrity via BinaryIntegrityVerifier; (5) render install-agent-http.sh.tmpl via ScriptTemplateService; (6) transfer script to target via RemoteCommandExecutor.transferFile(); (7) execute install script on target via RemoteCommandExecutor.execute(); (8) health check target agent (curl localhost:1222/actuator/health) up to 3 retries 5s apart; (9) return StepResult with path decision and health status in logs; (10) cleanup temp files on completion

**Checkpoint**: Path A transfer works end-to-end. Target with curl/wget + Central access gets the agent binary and runs it.

---

## Phase 5: User Story 3 — Agent-Pushed Binary Transfer / Path B (Priority: P2)

**Goal**: When target cannot download directly from Central, the agent pushes the binary over SSH via SCP or base64 pipe

**Independent Test**: Block target's Central access, verify agent downloads binary locally, pushes via SCP/base64 to target, target launches binary, health check returns UP

### Implementation for User Story 3

- [x] T019 [US3] Implement Path B transfer logic in TransferAgentStepHandler in `agents/unix/src/main/java/com/spulido/agent/worker/step/TransferAgentStepHandler.java` — extend the handler: if probe shows no curl/wget OR Central unreachable OR transfer-method=transfer: (1) agent downloads binary locally via httpClient.downloadBinary(); (2) verify integrity via BinaryIntegrityVerifier; (3) transfer binary to target via RemoteCommandExecutor.transferFile() (SCP→base64 fallback); (4) render install-agent-transfer.sh.tmpl via ScriptTemplateService; (5) transfer config script to target; (6) execute launch script; (7) health check; (8) cleanup temp files; handle SCP failure→retry→base64 fallback; handle binary-too-large error

**Checkpoint**: Path B transfer works end-to-end. Target without Central access or without curl/wget still gets the agent binary via SSH push.

---

## Phase 6: User Story 4 — Auto-Fallback Between Transfer Paths (Priority: P2)

**Goal**: Agent automatically falls back from Path A to Path B when target download fails, without operator intervention

**Independent Test**: Simulate Path A target download returning non-zero exit, verify agent retries once then falls back to Path B and completes transfer

### Tests for User Story 4

- [x] T020 [P] [US4] Unit test Path A → Path B fallback in `agents/unix/src/test/java/com/spulido/agent/worker/step/TransferAgentStepHandlerTest.java` — mock target has curl+Central, mock download returns exit 1 twice, assert handler falls back to Path B and completes via SCP/base64
- [x] T021 [P] [US4] Unit test token expiry during transfer in `agents/unix/src/test/java/com/spulido/agent/worker/step/TransferAgentStepHandlerTest.java` — mock token expired response, assert handler requests fresh token and restarts Path A
- [x] T022 [P] [US4] Unit test Central unreachable mid-transfer in `agents/unix/src/test/java/com/spulido/agent/worker/step/TransferAgentStepHandlerTest.java` — mock Central unreachable during Path A target download, assert auto-fallback to Path B using locally-downloaded binary
- [x] T023 [P] [US4] Unit test health check failure → PARTIAL_SUCCESS in `agents/unix/src/test/java/com/spulido/agent/worker/step/TransferAgentStepHandlerTest.java` — mock health check returns non-UP 3 times, assert StepResult.isSuccess()=true but logs indicate PARTIAL_SUCCESS

### Implementation for User Story 4

- [x] T024 [US4] Implement fallback and retry logic in TransferAgentStepHandler in `agents/unix/src/main/java/com/spulido/agent/worker/step/TransferAgentStepHandler.java` — Path A download failure: retry once, then auto-switch to Path B; SCP failure: retry once, fall back to base64; health check: 3 retries with 5s delay, PARTIAL_SUCCESS on final failure; token expiry: request fresh token, restart Path A; Central unreachable during Path A: auto-fallback to Path B; all retries respect transfer-method.retries config

**Checkpoint**: Auto-fallback between transfer paths works. Transient network or download failures don't block replication.

---

## Phase 7: User Story 6 — Integrity Verification Gate (Priority: P3)

**Goal**: Agent binary must pass cryptographic integrity verification before any transfer or execution on any target

**Independent Test**: Provide binary with invalid hash/signature, verify transfer is blocked and step marked FAILED

### Implementation for User Story 6

- [x] T025 [US6] Harden integrity gate in TransferAgentStepHandler in `agents/unix/src/main/java/com/spulido/agent/worker/step/TransferAgentStepHandler.java` — verify BinaryIntegrityVerifier.verify() is called immediately after any binary download (both agent-side for Path B and before any transfer); on verification failure, return StepResult.failure() with reason "Binary integrity verification failed", do NOT proceed to transfer or execution; log integrity result at INFO level without hash values

**Checkpoint**: Integrity verification gates all binary execution. Compromised binaries cannot reach targets.

---

## Phase 8: User Story 5 — Session Context Propagation Validation (Priority: P3)

**Goal**: Validate that session propagation works end-to-end from exploit to transfer

**Independent Test**: Run full exploit→verify→transfer pipeline, confirm TargetSession built from EXECUTE_EXPLOIT context has correct IP and user

### Tests for User Story 5

- [x] T026 [P] [US5] Unit test SshSessionProvisioner in `agents/unix/src/test/java/com/spulido/agent/remote/SshSessionProvisionerTest.java` — mock SSH process returning "OK\n", assert verify returns true; mock all 3 retries failing, assert verify returns false; verify retry count and 5s intervals
- [x] T027 [P] [US5] Unit test ExecuteExploitStepHandler session recording in `agents/unix/src/test/java/com/spulido/agent/worker/step/ExecuteExploitStepHandlerTest.java` — mock provisioner returns true, assert StepResult.logs contain targetIp, targetUser, reverseShellActive; mock provisioner returns false after retries, assert StepResult.failure()
- [x] T028 [US5] Unit test TargetSession construction from context in `agents/unix/src/test/java/com/spulido/agent/worker/step/TransferAgentStepHandlerTest.java` — provide EXECUTE_EXPLOIT StepResult with targetIp/targetUser/reverseShellActive in logs, assert TargetSession correctly built

**Checkpoint**: Session propagation validated. Exploit step produces usable session consumed by transfer step.

---

## Phase 9: Integration Tests

**Purpose**: End-to-end validation of both transfer paths with real SSH and Central

- [x] T029 [P] Integration test Path A end-to-end in `agents/unix/src/test/java/com/spulido/agent/integration/PathAIntegrationTest.java` — requires: Central running, target with SSH+curl, project with AUTO_APPROVE; flow: exploit→replication request→Path A transfer→health check UP within 30s
- [x] T030 [P] Integration test Path B end-to-end in `agents/unix/src/test/java/com/spulido/agent/integration/PathBIntegrationTest.java` — requires: Central running, target with SSH but no Central access, project with AUTO_APPROVE; flow: exploit→replication request→Path B transfer (SCP/base64)→health check UP within 60s

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Verification, cleanup, and final validation

- [x] T031 Verify agents/unix compiles successfully: `cd agents/unix && ./mvnw compile`
- [x] T032 Run all unit tests and confirm pass: `cd agents/unix && ./mvnw test`
- [x] T033 Run quickstart.md validation checklist — confirm all items checked
- [x] T034 Verify security constraints: no base64 binary data or SSH credentials at INFO+ log levels; preauthCode only over SSH channel; integrity gate blocks tainted binaries
- [x] T035 Verify parent agent cleanup: confirm no residual files (binary copies, rendered scripts) on filesystem after transfer step completes

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup (Phase 1) — BLOCKS all user stories
- **US1 Tests (Phase 3)**: Depends on Foundational (Phase 2) — SshRemoteCommandExecutor must exist
- **US2 Path A (Phase 4)**: Depends on Foundational (Phase 2) — RemoteCommandExecutor + Session Propagation needed
- **US3 Path B (Phase 5)**: Depends on US2 (Phase 4) — extends TransferAgentStepHandler
- **US4 Fallback (Phase 6)**: Depends on US2+US3 (Phases 4+5) — extends TransferAgentStepHandler with retry/fallback
- **US6 Integrity (Phase 7)**: Depends on US2+US3 (Phases 4+5) — hardens existing TransferAgentStepHandler
- **US5 Validation (Phase 8)**: Depends on Foundational (Phase 2) — tests SshSessionProvisioner and ExecuteExploitStepHandler
- **Integration Tests (Phase 9)**: Depends on all user stories complete
- **Polish (Phase 10)**: Depends on all phases complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational — No dependencies on other stories. Tests SshRemoteCommandExecutor independently.
- **US5 (P3)**: Foundation — Session propagation is implemented in Phase 2 as prerequisite. Phase 8 validates it.
- **US2 (P1)**: Depends on US5 (Foundation) for session propagation. First story to touch TransferAgentStepHandler.
- **US3 (P2)**: Extends US2's TransferAgentStepHandler — sequential on same file.
- **US4 (P2)**: Extends US2+US3's TransferAgentStepHandler — sequential on same file.
- **US6 (P3)**: Depends on US2+US3 — hardens the same TransferAgentStepHandler file.

### Within Each Phase

- Models/interfaces before implementations
- Tests before implementation (where tests are specified first)
- Core implementation before integration
- Phase complete before moving to next priority

### Parallel Opportunities

- **Phase 1**: T001, T002, T003, T004 all editable in parallel (different files)
- **Phase 3**: T010-T015 all test methods in same file but can be written together
- **Phase 4**: T016 (template) and T017 (rename) parallel with each other; T018 (handler) sequential after
- **Phase 6**: T020-T023 all parallel (different test methods in TransferAgentStepHandlerTest)
- **Phase 8**: T026, T027 parallel (different test files)
- **Phase 9**: T029, T030 parallel (different integration tests)
- **Phase 10**: T031-T035 parallel (independent verification tasks)

---

## Parallel Example: Phase 3 — US1 Tests

```bash
# All US1 unit tests can be written together in the same file:
Task: "Unit test execute() success path in SshRemoteCommandExecutorTest.java"
Task: "Unit test execute() timeout in SshRemoteCommandExecutorTest.java"
Task: "Unit test execute() non-zero exit in SshRemoteCommandExecutorTest.java"
Task: "Unit test transferFile() SCP success in SshRemoteCommandExecutorTest.java"
Task: "Unit test transferFile() SCP fail → base64 fallback in SshRemoteCommandExecutorTest.java"
Task: "Unit test transferFile() binary too large in SshRemoteCommandExecutorTest.java"
```

---

## Implementation Strategy

### MVP First (US1 + US5 Only)

1. Complete Phase 1: Setup — TargetSession, RemoteCommandExecutor interface, config
2. Complete Phase 2: Foundational — SshSessionProvisioner, SshRemoteCommandExecutor, ExecuteExploitStepHandler, wiring
3. Complete Phase 3: US1 — Unit tests for remote command execution
4. **STOP and VALIDATE**: Test SshRemoteCommandExecutor independently with `./mvnw test`
5. At this point, agents can execute commands and transfer files remotely over SSH

### Incremental Delivery

1. Setup + Foundational → SSH session propagation works (exploit→verify→session)
2. Add US1 → Remote command execution tested and verified
3. Add US2 → Path A transfer: target downloads from Central via HTTP
4. Add US3 → Path B transfer: agent pushes binary via SSH
5. Add US4 → Auto-fallback: resilient transfer with retry/fallback
6. Add US6 → Integrity gate: no binary execution without verification
7. Add US5 validation → Session propagation end-to-end verified
8. Each phase adds incremental value without breaking prior functionality

### Parallel Team Strategy

With multiple developers after Foundational:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: US1 tests (Phase 3)
   - Developer B: US2 Path A (Phase 4)
   - Developer C: US5 validation tests (Phase 8)
3. After US2 complete:
   - Developer B: US3 Path B (Phase 5)
   - Developer C: US4 Fallback + US6 Integrity (Phases 6+7)

---

## Notes

- [P] tasks = different files or independent test methods, no dependencies
- [Story] label maps task to specific user story for traceability
- TransferAgentStepHandler is a single file modified by US2→US3→US4→US6 phases sequentially
- SshRemoteCommandExecutorTest has 6 test methods in same file — write together, mark [P] for parallel authoring
- Integration tests (Phase 9) require running Central and SSH target — skip in CI, run manually
- Do not run git commands unless the user explicitly approves them
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
