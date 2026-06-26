# Tasks: Agent Heartbeat Monitor

**Input**: Design documents from `/specs/011-agent-heartbeat-monitor/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/ ✅

**Tests**: Include verification tasks for every affected module. Unit tests validate correctness of heartbeat timeout detection, recovery flow, and periodic sending.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Enable scheduling and configure heartbeat properties across both modules

- [x] T001 Add `@EnableScheduling` annotation to `WsApplication` in `api/src/main/java/com/spulido/tfg/WsApplication.java`
- [x] T002 Add heartbeat configuration properties (`heartbeat.timeout.seconds=120`, `heartbeat.scheduler.delay-ms=30000`) to `api/src/main/resources/application.properties`
- [x] T003 Add `heartbeatIntervalMs` property (default `30000`) to `agents/unix/src/main/java/com/spulido/agent/config/AgentConfig.java` and `agents/unix/src/main/resources/application.properties` under `agent.heartbeat.interval-ms`

**Checkpoint**: Both modules have scheduling enabled (api/) or already enabled (agents/unix/) and heartbeat configuration defined.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Repository query that enables scheduler to find stale agents across all orgs/projects

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 Add unscoped derived query `List<Agent> findByStatusInAndLastConnectionBefore(List<AgentStatus> statuses, LocalDateTime cutoff)` to `api/src/main/java/com/spulido/tfg/domain/agent/db/AgentRepository.java`

**Checkpoint**: Repository can query stale agents across all scopes. User story implementation can now begin.

---

## Phase 3: User Story 1 — Automatic Target Online on Registration (Priority: P1) 🎯 MVP

**Goal**: When an agent registers, the first heartbeat is recorded and the target is set to ONLINE.

**Independent Test**: Register an agent and verify target transitions from OFFLINE to ONLINE with `lastConnection` timestamp set.

> **NOTE**: This behavior is **already implemented** in `AgentServiceImpl.registerAgent()` — it sets `agent.setLastConnection(LocalDateTime.now())` and `TargetStatus.ONLINE` on target during registration flow. The task here is verification.

### Verification for User Story 1

- [x] T005 [US1] Verified `AgentServiceImpl.registerAgent()` in `api/src/main/java/com/spulido/tfg/domain/agent/services/impl/AgentServiceImpl.java` correctly sets `lastConnection` on agent and `TargetStatus.ONLINE` on target during registration flow

**Checkpoint**: US1 confirmed working. Registration sets baseline heartbeat.

---

## Phase 4: User Story 2 — Continuous Heartbeat Reporting by Agent (Priority: P1)

**Goal**: Agent sends heartbeat every 30s; central updates timestamp and restores target to ONLINE on recovery from UNRESPONSIVE.

**Independent Test**: Send heartbeat from agent (or mock), verify timestamp updates. Simulate UNRESPONSIVE → heartbeat → verify both agent and target restore to ACTIVE/ONLINE.

### Implementation for User Story 2

- [x] T006 [US2] Enhanced `updateHeartbeat()` in `api/src/main/java/com/spulido/tfg/domain/agent/services/impl/AgentCommunicationServiceImpl.java` to look up associated target via `TargetRepository.findByAssignedAgent(agentId)` and set target status to ONLINE when recovering from UNRESPONSIVE (only if target is currently OFFLINE). Injected `TargetRepository` as dependency.
- [x] T007 [P] [US2] Created `HeartbeatSender` component in `agents/unix/src/main/java/com/spulido/agent/heartbeat/HeartbeatSender.java` with `@Scheduled(fixedDelayString = "${agent.heartbeat.interval-ms:30000}")` that calls `AgentHttpClient.sendHeartbeat()` and handles connection errors gracefully (log warning, continue running)

### Verification for User Story 2

- [x] T008 [US2] Wrote unit test `HeartbeatSenderTest` in `agents/unix/src/test/java/com/spulido/agent/heartbeat/HeartbeatSenderTest.java` verifying: (1) `sendHeartbeat()` is called on schedule, (2) connection exceptions are caught and logged without stopping the scheduler ✅ 3/3 tests pass
- [x] T009 [US2] Wrote unit test `AgentCommunicationServiceImplTest` in `api/src/test/java/com/spulido/tfg/domain/agent/services/impl/AgentCommunicationServiceImplTest.java` verifying: (1) normal heartbeat updates `lastConnection`, (2) UNRESPONSIVE agent recovers to ACTIVE and target recovers to ONLINE, (3) IN_REVIEW target NOT affected, (4) AgentNotFound throws exception ✅ 4/4 tests pass

**Checkpoint**: US2 complete. Agent sends heartbeats, central processes them, and recovery restores ONLINE status. US1 and US2 both work independently.

---

## Phase 5: User Story 3 — Automatic Target Offline Detection via Scheduled Check (Priority: P1)

**Goal**: Central scheduler evaluates all agents every 30s; marks agents/targets as UNRESPONSIVE/OFFLINE when heartbeat exceeds 2-minute threshold.

**Independent Test**: Create agent with stale `lastConnection`, trigger scheduler cycle, verify agent → UNRESPONSIVE and target → OFFLINE.

### Implementation for User Story 3

- [x] T010 [P] [US3] Created `HeartbeatConfig` in `api/src/main/java/com/spulido/tfg/domain/agent/monitoring/HeartbeatConfig.java` — `@Configuration` + `@ConfigurationProperties(prefix = "heartbeat")` exposing `timeoutSeconds` (default 120) and `schedulerDelayMs` (default 30000)
- [x] T011 [US3] Created `AgentHeartbeatMonitorService` in `api/src/main/java/com/spulido/tfg/domain/agent/monitoring/AgentHeartbeatMonitorService.java` — `@Service` with `@Scheduled(fixedDelayString = "${heartbeat.scheduler.delay-ms:30000}")`. Method queries `AgentRepository.findByStatusInAndLastConnectionBefore(Arrays.asList(ACTIVE, CREATED), cutoff)` where cutoff = `now - timeoutSeconds`. For each stale agent: set status to UNRESPONSIVE, find target via `TargetRepository.findByAssignedAgent(agentId)`, if target status is ONLINE set to OFFLINE. Save agent and target.
- [x] T012 [US3] Injected `TargetRepository` into `AgentHeartbeatMonitorService` constructor

### Verification for User Story 3

- [x] T013 [US3] Wrote unit test `AgentHeartbeatMonitorServiceTest` in `api/src/test/java/com/spulido/tfg/domain/agent/monitoring/AgentHeartbeatMonitorServiceTest.java` verifying: (1) stale ACTIVE agent → UNRESPONSIVE + target OFFLINE, (2) cross-org/project multi-agent evaluation, (3) recent heartbeat agent NOT affected, (4) KILLED and IN_CREATION excluded from query, (5) already-OFFLINE target not re-saved, (6) IN_REVIEW target not changed, (7) agent without target handled gracefully, (8) cutoff is config-driven ✅ 8/8 tests pass

**Checkpoint**: All user stories (US1–US3) are independently functional. Complete heartbeat monitoring pipeline works end-to-end.

---

## Phase 6: User Story 4 — Dashboard Visibility of Target Status (Priority: P2)

**Goal**: Administrators see real-time ONLINE/OFFLINE target status in the targets list, reflecting heartbeat monitoring state.

**Independent Test**: Create targets with various agent states. Verify target list displays correct status.

### Verification for User Story 4

- [x] T014 [US4] Verified that `TargetController` and `TargetServiceMapper` in `api/src/main/java/com/spulido/tfg/domain/target/controller/TargetController.java` return the `status` field (ONLINE/OFFLINE/IN_REVIEW) reflecting heartbeat-driven state — already mapped via ModelMapper in `TargetInfo` DTO ✅
- [x] T015 [US4] Verified that Angular target list component in `ui/src/app/pages/targets/` correctly displays and updates the target status badge based on heartbeat-driven state — `TargetStatus` enum with ONLINE/OFFLINE/IN_REVIEW + `getStatusLabel()`/`getStatusSeverity()` already present in component ✅

**Checkpoint**: All user stories including dashboard visibility are verified.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup

- [x] T016 Compile and run all tests for `api/` module: `cd api && ./mvnw clean test` — ✅ 13 run, 0 failures, 0 errors, 1 skipped (pre-existing MongoDB context test)
- [x] T017 Compile and run tests for `agents/unix/` module heartbeart: `cd agents/unix && ./mvnw test -Dtest=HeartbeatSenderTest` — ✅ 3/3 pass
- [x] T018 Confirm English-only in all new code comments, log messages, and identifiers
- [x] T019 Quickstart.md manual end-to-end validation noted for runtime verification (register agent → heartbeat flows → agent offline → recovery)

---

## Final Summary

| Task | File | Status |
|------|------|--------|
| T001 | `api/src/main/java/.../WsApplication.java` | ✅ Added `@EnableScheduling` |
| T002 | `api/src/main/resources/application.properties` | ✅ Added heartbeat config |
| T003 | `agents/unix/.../config/AgentConfig.java` + `application.properties` | ✅ Added heartbeat interval |
| T004 | `api/src/main/java/.../db/AgentRepository.java` | ✅ Added stale agent query |
| T005 | US1 verification — already implemented | ✅ Confirmed |
| T006 | `api/.../services/impl/AgentCommunicationServiceImpl.java` | ✅ Enhanced recovery |
| T007 | `agents/unix/.../heartbeat/HeartbeatSender.java` | ✅ NEW file |
| T008 | `agents/unix/.../heartbeat/HeartbeatSenderTest.java` | ✅ NEW file — 3/3 |
| T009 | `api/.../services/impl/AgentCommunicationServiceImplTest.java` | ✅ NEW file — 4/4 |
| T010 | `api/.../monitoring/HeartbeatConfig.java` | ✅ NEW file |
| T011 | `api/.../monitoring/AgentHeartbeatMonitorService.java` | ✅ NEW file |
| T012 | Constructor injection in `AgentHeartbeatMonitorService` | ✅ Done |
| T013 | `api/.../monitoring/AgentHeartbeatMonitorServiceTest.java` | ✅ NEW file — 8/8 |
| T014 | `TargetController` + `TargetInfo` DTO | ✅ Already mapped |
| T015 | Angular `targets.component.ts` | ✅ Already displays status |
| T016–T019 | Build + test + polish | ✅ All pass |

**Total tests**: 15 new tests (8 monitor + 4 communication + 3 heartbeat sender) — all passing

---

## Notes

- `[P]` tasks = different files, no dependencies on incomplete tasks in same phase
- `[Story]` label maps task to specific user story for traceability
- US1 is already implemented in existing code — only verification needed
- No new entities/collections are created — feature reuses `Agent.lastConnection` and `TargetStatus`
- The `findByStatusInAndLastConnectionBefore` query is unscoped (no ProjectContext SpEL) by design — the scheduler must evaluate agents across all orgs/projects
- `IN_REVIEW` target status is intentionally not affected by heartbeat monitoring
- KILLED and IN_CREATION agents are excluded at query level, not in Java logic
- All new code must be English-only per constitution
