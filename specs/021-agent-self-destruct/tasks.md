---

description: "Task list for Unix Agent Self-Destruction & Self-Cleanup"
---

# Tasks: Unix Agent Self-Destruction & Self-Cleanup

**Input**: Design documents from `/specs/021-agent-self-destruct/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included. Teardown is irreversible and destructive, the plan enumerates specific test targets, and the repo has an established JUnit/Mockito suite — unit tests plus one sandboxed integration test are required.

**Organization**: Grouped by user story. US1 and US2 are both P1 (the two triggers); US3 (P2) adds best-effort/idempotent/verifiable guarantees + central audit reporting.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1 / US2 / US3
- All paths are repo-relative.

## Path Conventions

- Agent: `agents/unix/src/main/java/com/spulido/agent/…`, resources `agents/unix/src/main/resources/scripts/…`, tests `agents/unix/src/test/java/com/spulido/agent/…`
- API: `api/src/main/java/com/spulido/tfg/domain/agent/…`, tests `api/src/test/java/com/spulido/tfg/domain/agent/…`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Ground the work in repo conventions before writing code.

- [X] T001 Review repository guidance for this work: `CLAUDE.md`/`AGENTS.md` (script/template boundary, English-only, minimal change) and any `.agents/skills/*` relevant to `agents/unix`; confirm the `ScriptTemplateService` `{{PLACEHOLDER}}` + `ClassPathResource` pattern used by `agents/unix/src/main/resources/scripts/install-agent-http.sh.tmpl`.
- [X] T002 [P] Confirm the existing lifecycle/trigger touchpoints compile and are understood: `agents/unix/.../utils/AgentLifecycle.java` (`stop()`), `agents/unix/.../domain/task/StepAction.java` (`SELF_DESTRUCT` already present), `agents/unix/.../worker/WorkerCoordinator.java` (plan loop + `SELF_DESTRUCT` currently `EchoStepHandler`), `agents/unix/.../heartbeat/HeartbeatSender.java`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared teardown machinery both P1 triggers depend on. No user story can begin until this is complete.

**⚠️ CRITICAL**: US1 and US2 both call `TeardownService.selfDestruct(trigger)`.

- [X] T003 [P] Create `TeardownTrigger` enum (`PLAN_COMPLETION`, `PLATFORM_DEPROVISION`, `AUTH_REVOKED`, `SELF_DESTRUCT_STEP`) in `agents/unix/src/main/java/com/spulido/agent/teardown/TeardownTrigger.java`.
- [X] T004 [P] Create `ArtifactType` enum (`AGENT_BINARY`, `AGENT_CONFIG`, `AGENT_LOG`, `DOWNLOADED_TOOLS`, `WORKING_DIR`, `INSTALL_SCRIPT`, `OS_REGISTRATION`, `RAW_DOWNLOAD`) and `RemovalStatus` enum (`REMOVED`, `FAILED`, `NOT_PRESENT`) in `agents/unix/.../teardown/ArtifactType.java` and `agents/unix/.../teardown/RemovalStatus.java`.
- [X] T005 [P] Create `ArtifactRemovalResult` (`type`, `path`, `status`, `detail`) in `agents/unix/.../teardown/ArtifactRemovalResult.java` (Lombok where consistent with repo).
- [X] T006 [P] Create `TeardownOutcome` (`agentId`, `trigger`, `timestamp`, `results`, `binaryRemoval`) in `agents/unix/.../teardown/TeardownOutcome.java`.
- [X] T007 Create `ArtifactSet` in `agents/unix/.../teardown/ArtifactSet.java`: resolve host paths per `ArtifactType` from `AgentConfig`, the known install layout (`/tmp/agent`, `/tmp/agent.properties`, `/tmp/agent.log`, `/tmp/agent_raw`), `BundledToolProvisioner.getExtractionDirectory()`, and the JVM binary path via `ProcessHandle.current().info().command()`. Never glob unrelated host dirs (FR-016). Depends on T004.
- [X] T008 Create `self-destruct.sh.tmpl` in `agents/unix/src/main/resources/scripts/self-destruct.sh.tmpl`: POSIX, detached-safe; placeholders `{{AGENT_BINARY}}`, `{{AGENT_PID}}`, `{{RESIDUAL_PATHS}}`, `{{OS_REGISTRATION_PATHS}}`. Each removal uses `rm -f … || true` (best-effort); waits until the agent PID exits (`kill -0` loop) before unlinking the binary; removes any residual artifact paths and OS registration (systemd unit / launchd plist / cron); removes itself last. No inline construction — rendered via `ScriptTemplateService`.
- [X] T009 Create `TeardownReportRequest` DTO in `agents/unix/.../worker/http/dto/TeardownReportRequest.java` mirroring `TeardownOutcome` (contract: `contracts/teardown-report.md`).
- [X] T010 Add `reportTeardownOutcome(TeardownReportRequest)` to `agents/unix/.../worker/http/AgentHttpClient.java` → `POST {centralUrl}/api/agent/comm/teardown` (follow existing `RestTemplate` method style). Depends on T009.
- [X] T011 Create `TeardownService` in `agents/unix/.../teardown/TeardownService.java`: `selfDestruct(TeardownTrigger)` guarded single-shot via `AtomicBoolean.compareAndSet` (FR-014); resolves `ArtifactSet`; performs Java-side removal of non-binary artifacts recording `ArtifactRemovalResult` per artifact; renders `self-destruct.sh.tmpl` via `ScriptTemplateService`; spawns it fully detached (`nohup sh <script> &`, new process group); then calls `AgentLifecycle.stop()`. Constructor injection of `AgentConfig`, `ScriptTemplateService`, `AgentLifecycle`, `AgentHttpClient`, `BundledToolProvisioner`. (Best-effort/idempotent/report wiring completed in US3.) Depends on T003–T010.

**Checkpoint**: Teardown machinery exists and is callable by any trigger.

---

## Phase 3: User Story 1 - Self-Destruct on Plan Completion (Priority: P1) 🎯 MVP

**Goal**: When all plan steps reach terminal completed state, report final plan status, then tear down.

**Independent Test**: Assign a plan whose steps all complete on a test host; verify final plan status is recorded in central, the agent process exits, and its artifacts are removed.

### Tests for User Story 1 ⚠️

- [X] T012 [P] [US1] Unit test `WorkerCoordinator` plan-completion trigger in `agents/unix/src/test/java/com/spulido/agent/worker/WorkerCoordinatorTest.java` (extend): all steps COMPLETED → final status reported before `TeardownService.selfDestruct(PLAN_COMPLETION)` invoked (verify ordering with a Mockito `InOrder`); FAILED plan does not tear down.
- [X] T013 [P] [US1] Unit test `SelfDestructStepHandler` in `agents/unix/src/test/java/com/spulido/agent/worker/step/SelfDestructStepHandlerTest.java`: a `SELF_DESTRUCT` step delegates to `TeardownService.selfDestruct(SELF_DESTRUCT_STEP)`.

### Implementation for User Story 1

- [X] T014 [US1] Create `SelfDestructStepHandler` in `agents/unix/.../worker/step/SelfDestructStepHandler.java` implementing `StepHandler`, delegating to `TeardownService`; register it in `WorkerCoordinator.createDefaultStepHandlers(...)` replacing the `SELF_DESTRUCT` → `EchoStepHandler` stub. Depends on T011.
- [X] T015 [US1] Edit `agents/unix/.../worker/WorkerCoordinator.java`: after a job/plan reaches `JobStatus.COMPLETED` with all steps terminal, report final plan status via existing step/plan reporting, then invoke `TeardownService.selfDestruct(PLAN_COMPLETION)`. Guard the poll/step loop with the `tearingDown` state so no new job/step is picked up once teardown starts (research Decision 3: let the current atomic step finish, start no new one). Inject `TeardownService`. Depends on T011.
- [X] T016 [US1] Verify: `cd agents/unix && ./mvnw test` (WorkerCoordinator + SelfDestructStepHandler pass); confirm no new wildcard imports and English-only log text.

**Checkpoint**: Plan-completion teardown works end-to-end and is independently testable.

---

## Phase 4: User Story 2 - Self-Destruct on Platform-Initiated Deletion (Priority: P1)

**Goal**: Operator deletes the agent; next authenticated heartbeat carries `deprovision=true` → teardown. Sustained authenticated rejection (revoked credentials) → implicit teardown.

**Independent Test**: Delete an agent from the platform; verify its next heartbeat receives the signal, it tears down and does not restart. Separately, force 3 consecutive 401/403/404 heartbeats and verify `AUTH_REVOKED` teardown.

### Tests for User Story 2 ⚠️

- [X] T017 [P] [US2] Extend `agents/unix/src/test/java/com/spulido/agent/heartbeat/HeartbeatSenderTest.java`: `deprovision=true` → `selfDestruct(PLATFORM_DEPROVISION)`; 3 consecutive authenticated-rejection responses → `selfDestruct(AUTH_REVOKED)`; a transport failure (timeout/5xx) does NOT trigger teardown and does not advance the rejection counter; a success resets the counter.
- [X] T018 [P] [US2] Create `api/src/test/java/com/spulido/tfg/domain/agent/AgentCommunicationServiceTeardownTest.java`: a de-provisioned/`KILLED` agent → `updateHeartbeat` yields `deprovision=true`; a normal agent → `deprovision=false`; the signal is only produced for the authenticated matching agent.

### Implementation for User Story 2

- [X] T019 [P] [US2] Edit agent DTO `agents/unix/.../worker/http/dto/HeartbeatResponse.java`: add `boolean deprovision` and `String deprovisionReason` (getters/setters, keep existing fields).
- [X] T020 [P] [US2] Edit api DTO `api/src/main/java/com/spulido/tfg/domain/agent/model/dto/HeartbeatResponse.java`: add `deprovision` and `deprovisionReason` to the Lombok builder.
- [X] T021 [US2] Edit `agents/unix/.../heartbeat/HeartbeatSender.java`: on `deprovision=true` (and `agentId` matches configured id) call `TeardownService.selfDestruct(PLATFORM_DEPROVISION)`; track consecutive authenticated-rejection count (HTTP 401/403/404-agent-not-found via `RestClientResponseException` status) and on 3 consecutive call `selfDestruct(AUTH_REVOKED)`; reset on success; ignore transport failures for counting (research Decision 2). Inject `TeardownService`. Depends on T011, T019.
- [X] T022 [US2] Edit `api/.../model/Agent.java` (add `deprovisioned` boolean + nullable `deprovisionReason`) and `api/.../services/impl/AgentServiceImpl.java` `deleteAgent(id)` to soft-mark de-provisioned (set `deprovisioned=true`, `status=KILLED`) instead of immediate hard delete (contract: `contracts/agent-deprovision-flag.md`). Preserve existing delete response.
- [X] T023 [US2] Edit `api/.../services/impl/AgentCommunicationServiceImpl.java` + `api/.../controller/AgentCommunicationController.java`: `updateHeartbeat` sets `deprovision=true`/`deprovisionReason` in the response when the agent record is de-provisioned/`KILLED`. Depends on T020, T022.
- [X] T024 [US2] Verify: `cd agents/unix && ./mvnw test` and `cd api && ./mvnw test` (heartbeat + communication-service tests pass).

**Checkpoint**: Both P1 triggers (plan-completion and platform-deletion, plus auth-revoked fallback) drive teardown.

---

## Phase 5: User Story 3 - Complete, Best-Effort, Verifiable Artifact Removal (Priority: P2)

**Goal**: Teardown is thorough, resilient to per-step failure, idempotent, and its outcome is recorded centrally for audit.

**Independent Test**: Trigger teardown with one removal step forced to fail; verify all other artifacts are still removed, the process still exits, the per-artifact outcome is reported, and a re-run is a harmless no-op.

### Tests for User Story 3 ⚠️

- [X] T025 [P] [US3] Create `agents/unix/src/test/java/com/spulido/agent/teardown/TeardownServiceTest.java`: single-shot (second `selfDestruct` is a no-op); best-effort (one failing artifact removal records `FAILED` and does not abort the rest); ordering (teardown outcome report attempted before the detached binary script is spawned / before `AgentLifecycle.stop()`); offline report failure still proceeds with removal + exit.
- [X] T026 [P] [US3] Create `agents/unix/src/test/java/com/spulido/agent/teardown/ArtifactSetTest.java`: path resolution from config/install-layout/tools dir; missing paths resolve to `NOT_PRESENT`; never enumerates unrelated dirs.
- [X] T027 [P] [US3] Create integration test `agents/unix/src/test/java/com/spulido/agent/integration/SelfDestructSandboxIT.java`: render `self-destruct.sh.tmpl` via `ScriptTemplateService` into a temp sandbox seeded with fake artifacts, run it, and assert — all seeded artifacts removed; idempotent re-run exits 0; one permission-locked artifact does not block the others; the script removes itself last.

### Implementation for User Story 3

- [X] T028 [US3] Complete `TeardownService` (from T011) resilience: wrap each Java-side removal independently (record `REMOVED`/`FAILED`/`NOT_PRESENT`, never throw out of the loop — FR-011); make removals idempotent (missing → `NOT_PRESENT` — FR-012); build `TeardownOutcome` and call `AgentHttpClient.reportTeardownOutcome(...)` best-effort BEFORE spawning the detached binary script (FR-013, SC-002 ordering); `binaryRemoval=PENDING_DETACHED`.
- [X] T029 [P] [US3] Create api DTO `api/.../model/dto/TeardownReportRequest.java` with `jakarta.validation` (agentId not blank, trigger a known enum, results non-null) per `contracts/teardown-report.md`.
- [X] T030 [P] [US3] Create MongoDB document `api/.../model/AgentTeardownRecord.java` (id, agentId, org/project/target, trigger, reportedAt, agentTimestamp, embedded results, binaryRemoval) + its Spring Data repository `api/.../db/AgentTeardownRepository.java`.
- [X] T031 [US3] Add `POST /api/agent/comm/teardown` to `api/.../controller/AgentCommunicationController.java` + service method in `api/.../services/impl/AgentCommunicationServiceImpl.java`: validate, persist `AgentTeardownRecord`, reap/tombstone the agent, idempotent on duplicate report. Depends on T029, T030.
- [X] T032 [US3] Verify: run `SelfDestructSandboxIT` (`cd agents/unix && ./mvnw -Dtest=SelfDestructSandboxIT verify`) and `cd api && ./mvnw test`; run the boundary grep from quickstart to confirm no inline script construction.

**Checkpoint**: All three stories independently functional; teardown is thorough, resilient, idempotent, and audited.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T033 [P] Run the full `quickstart.md` manual verification (plan-completion, platform-deprovision, auth-revoked fallback) on a test host/container; confirm no restart over a 10-minute watch (SC-004).
- [ ] T034 [P] Native build sanity check: `cd agents/unix && sh package-macos.sh` (or `./mvnw -Pnative native:compile`) to confirm the teardown code is GraalVM-native-safe (no reflection/String-built scripts).
- [X] T035 Confirm English-only text, comments limited to non-obvious logic, ordered imports and no wildcards across all new/edited Java files; confirm secrets (`agent.properties`) are removed by teardown and not logged.
- [X] T036 Update `agents/unix` docs/README if it enumerates lifecycle behaviors, to include the teardown lifecycle and triggers.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: no dependencies.
- **Foundational (Phase 2)**: after Setup — BLOCKS all user stories (US1 & US2 both call `TeardownService`).
- **US1 (Phase 3)** and **US2 (Phase 4)**: both P1, both after Foundational; independent of each other (US1 = worker loop + step handler; US2 = heartbeat + api). Can be built in parallel by different developers.
- **US3 (Phase 5)**: after Foundational; completes `TeardownService` resilience (T028 refines T011) and adds the audit report path. Best sequenced after at least one P1 trigger exists to exercise it, but its tests are self-contained.
- **Polish (Phase 6)**: after all desired stories.

### Within Each User Story

- Tests written before implementation and expected to fail first.
- DTOs/models before services before endpoints.
- Ask instead of guessing on the open items already resolved in `research.md` (auth threshold, mid-plan behavior, binary self-removal) if reality diverges.

### Parallel Opportunities

- Foundational T003–T006 and T009 are `[P]` (distinct new files).
- US1 and US2 phases can proceed in parallel after Foundational.
- Within US2, DTO edits T019/T020 are `[P]`; within US3, T029/T030 are `[P]`.
- All `[P]` test-authoring tasks can run together.

---

## Parallel Example: Foundational

```bash
Task: "Create TeardownTrigger enum in agents/unix/.../teardown/TeardownTrigger.java"
Task: "Create ArtifactType + RemovalStatus enums in agents/unix/.../teardown/"
Task: "Create ArtifactRemovalResult in agents/unix/.../teardown/ArtifactRemovalResult.java"
Task: "Create TeardownOutcome in agents/unix/.../teardown/TeardownOutcome.java"
```

## Parallel Example: after Foundational (two developers)

```bash
# Developer A — US1
Task: "Wire plan-completion trigger in WorkerCoordinator.java + SelfDestructStepHandler"
# Developer B — US2
Task: "Heartbeat deprovision signal + auth-revoked counter + api soft-mark deleteAgent"
```

---

## Implementation Strategy

### MVP First (US1)

1. Phase 1 Setup → Phase 2 Foundational (teardown machinery).
2. Phase 3 US1 (plan-completion teardown).
3. STOP and validate US1 independently on a test host.

### Incremental Delivery

1. Setup + Foundational → machinery ready.
2. US1 (plan-completion) → validate → demo (MVP).
3. US2 (platform de-provision + auth-revoked) → validate → demo.
4. US3 (best-effort/idempotent/verifiable + central audit) → validate → demo.

---

## Notes

- `[P]` = different files, no dependencies. `[Story]` maps to spec user stories.
- All teardown shell MUST live in `self-destruct.sh.tmpl` and be rendered via `ScriptTemplateService` — never built inline (`String.format`/`StringBuilder`/concatenation).
- Do not run git commands unless the user explicitly approves them.
- Teardown is irreversible: verify tests (single-shot, best-effort, idempotent, authenticated-signal-only) before exercising on any real host.
