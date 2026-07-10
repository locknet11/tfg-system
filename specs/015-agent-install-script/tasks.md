# Tasks: Agent Self-Installing Shell Script

**Input**: Design documents from `/specs/015-agent-install-script/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Manual integration tests via curl-pipe-bash are the primary verification mechanism for the install script. Unit tests for Java service/controller changes.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **API module**: `api/src/main/java/com/spulido/tfg/`, `api/src/main/resources/`
- **Agent module**: `agents/unix/src/main/java/com/spulido/agent/`, `agents/unix/src/main/resources/`
- **Tests (API)**: `api/src/test/java/com/spulido/tfg/`

---

## Phase 1: Setup (Verify Existing Infrastructure)

**Purpose**: Confirm prerequisites are in place before implementing changes

- [x] T001 Verify agent binary can be loaded by `AgentBinaryServiceImpl` — run API and check startup logs for "Agent binaries loaded"
- [x] T002 [P] Review current `unix.sh.ftl` template, `ScriptServiceImpl.generateInstallScript()`, and `AgentServiceImpl.registerAgent()` to confirm understanding of the current flow

**Checkpoint**: All prerequisites confirmed — ready to begin implementation

---

## Phase 2: Foundational (API + Agent Infrastructure)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work (install script changes) can begin until this phase is complete

### API: Install Token & Binary Download Endpoint

- [x] T003 Add `installToken` (String) and `installTokenExpiresAt` (Instant) fields to Agent entity in `api/src/main/java/com/spulido/tfg/domain/agent/model/Agent.java`
- [x] T004 [P] Add `generateInstallToken()` method (secure random 32-char token, 5-min expiry) to `api/src/main/java/com/spulido/tfg/domain/agent/services/impl/AgentServiceImpl.java`. Call it during `registerAgent()` alongside `generateApiKey()`, and store on the Agent entity
- [x] T005 [P] Create `AgentInstallBinaryController` at `api/src/main/java/com/spulido/tfg/domain/replication/controller/AgentInstallBinaryController.java`:
  - `GET /api/agent/binary/download/{installToken}` — public endpoint
  - Validates token exists, not expired, not already consumed
  - Serves binary bytes + embedded manifest (reuse `AgentBinaryService`)
  - Consumes (nulls out) token on first successful download
  - Returns 404 for invalid/expired, 410 for already-consumed
- [x] T006 Update `ScriptServiceImpl.generateInstallScript()` in `api/src/main/java/com/spulido/tfg/domain/script/services/impl/ScriptServiceImpl.java` to accept and pass new FreeMarker template variables: `downloadUrl`, `apiKey`, `agentId`, `centralPublicKey` (in addition to existing `apiUrl`, `organizationIdentifier`, `projectIdentifier`, `targetUniqueId`, `preauthCode`)
- [x] T007 Update `AgentServiceImpl.registerAgent()` in `api/src/main/java/com/spulido/tfg/domain/agent/services/impl/AgentServiceImpl.java` to:
  - Call `generateInstallToken()` and save the token
  - Construct `downloadUrl` as `apiBaseUrl + "/api/agent/binary/download/" + installToken`
  - Pass `apiKey`, `agentId` (saved agent's ID), `downloadUrl`, and `centralPublicKey` (from config) to `scriptService.generateInstallScript()`
- [x] T008 Update `WebSecurity` in `api/src/main/java/com/spulido/tfg/config/security/WebSecurity.java` to add `.requestMatchers(HttpMethod.GET, "/api/agent/binary/download/**").permitAll()` if not already covered by existing `/api/agent/binary/**` rule

### Agent: Fix RestTemplate Auth Interceptor

- [x] T009 Add `ClientHttpRequestInterceptor` to `restTemplate()` bean in `agents/unix/src/main/java/com/spulido/agent/config/WorkerPoolConfig.java` that reads `agentConfig.getApiKey()` and `agentConfig.getAgentId()` and adds `X-Agent-Api-Key` and `X-Agent-Id` request headers
- [x] T010 [P] Update `install-agent-http.sh.tmpl` in `agents/unix/src/main/resources/scripts/install-agent-http.sh.tmpl`: replace `agent.preauth-code` config line with `agent.api-key`, `agent.agent-id`, `agent.central-public-key`, and optional `agent.organization-identifier`, `agent.project-identifier`, `agent.target-unique-id` (keep backward-compatible `agent.central-url` line)
- [x] T011 [P] Update `install-agent-transfer.sh.tmpl` in `agents/unix/src/main/resources/scripts/install-agent-transfer.sh.tmpl`: same config property fixes as T010

**Checkpoint**: Foundation ready — binary can be served via install token, agent can authenticate to platform, install script template variables are wired up. Now ready to build the actual install script.

---

## Phase 3: User Story 1 - One-Command Agent Installation via Curl Pipe Bash (Priority: P1) 🎯 MVP

**Goal**: The `unix.sh.ftl` script, when piped to bash, downloads the agent binary, performs basic hash verification, configures the agent, and launches it in background — all without manual intervention.

**Independent Test**: Run `curl -sSL -X POST "http://central/api/agent/{org}/{proj}/{target}?preauthCode=xxx" | bash` against a test central platform targeting a clean Linux machine, then verify the agent process is running in background with correct configuration.

### Implementation for User Story 1

- [x] T012 [US1] Rewrite `api/src/main/resources/scripts/unix.sh.ftl` as a complete install script with these sections:
  1. **Header** — echo identification info (API URL, org, project, target ID) — preserve existing output
  2. **Pre-flight checks** — verify `/tmp` writable; detect curl vs wget; fail if neither available
  3. **Download** — download binary from `${DOWNLOAD_URL}` via curl or wget with retry on failure; detect empty download
  4. **Manifest extraction** — extract JSON manifest from last line of binary; parse blake3Hash with jq/python3/grep fallback
  5. **Hash verification** (basic) — compute Blake3 hash via openssl 3.0+ or b3sum; compare to manifest; abort on mismatch; warn and skip if tools unavailable
  6. **Configuration** — write `/tmp/agent.properties` with `agent.central-url`, `agent.api-key`, `agent.agent-id`, `agent.central-public-key`, `agent.organization-identifier`, `agent.project-identifier`, `agent.target-unique-id`
  7. **Launch** — `chmod +x /tmp/agent` then `nohup /tmp/agent > /tmp/agent.log 2>&1 &`; sleep 2; check PID via `kill -0`; report PID or warning with log path
  8. **Cleanup** — remove `/tmp/agent_raw` temp file; print `INSTALL_OK`
  9. **Error handling** — every failure path prints clear `FATAL:` message and exits non-zero; use `set -e`
- [x] T013 [US1] Verify the FreeMarker template renders correctly: build API, call the endpoint with valid parameters, capture the output, and confirm it is valid POSIX shell with all sections
- [x] T014 [US1] Manual end-to-end test: run the full `curl | bash` command against a running API with agent binary loaded, verify agent process launches and logs to `/tmp/agent.log`

**Checkpoint**: User Story 1 complete — operator can install an agent with a single curl-pipe-bash command. The agent downloads, basic hash verifies, configures, and launches in background.

---

## Phase 4: User Story 2 - Binary Integrity Verification with RSA Signature (Priority: P2)

**Goal**: Add RSA signature verification to the install script so that the agent binary's authenticity (not just integrity) is validated before execution.

**Independent Test**: Run the install script against a valid binary (signature verifies) and a corrupted/unsigned binary (signature fails) — confirm the first succeeds and the second aborts.

### Implementation for User Story 2

- [x] T015 [US2] Add RSA signature verification section to `api/src/main/resources/scripts/unix.sh.ftl` (between hash verification and configuration):
  - Parse `signature` and `algorithm` fields from manifest JSON
  - If `${CENTRAL_PUBLIC_KEY}` is provided: write public key to temp file, decode base64 signature, verify via `openssl pkeyutl -verify -pubin`
  - On verification failure: print `FATAL: RSA signature verification FAILED` and exit 1
  - On no public key configured: print warning and skip signature verification (non-blocking)
  - Clean up temp key/signature files after verification
- [x] T016 [US2] Manual test: download binary via the script from a running API (with valid signature), confirm "RSA signature: VERIFIED" appears. Then manually tamper with the manifest signature in the binary and confirm verification fails and script aborts.

**Checkpoint**: User Story 2 complete — binary integrity AND authenticity are verified before execution.

---

## Phase 5: User Story 3 - Agent Runs with Correct Configuration & Authentication (Priority: P2)

**Goal**: Ensure the agent binary receives the correct configuration (API key, agent ID, org/project/target identifiers) and launches with proper PID detection so the central platform can identify and authenticate it.

**Independent Test**: Run the full install script, verify via `ps` the agent PID matches the script output, and verify the central platform receives the agent's heartbeat (proving auth works).

### Implementation for User Story 3

- [x] T017 [US3] Refine configuration section in `api/src/main/resources/scripts/unix.sh.ftl`:
  - Write `${apiKey}` to `agent.api-key` (NEVER echo it in clear text — the apiKey appears only in the config file)
  - Write `${agentId}` to `agent.agent-id`
  - Write `${organizationIdentifier}` to `agent.organization-identifier`
  - Write `${projectIdentifier}` to `agent.project-identifier`
  - Write `${targetUniqueId}` to `agent.target-unique-id`
  - Mask apiKey in any log/echo output (show only first/last 4 chars if needed for debugging)
- [x] T018 [US3] Refine launch section: improve PID detection with a `/tmp/agent.pid` file written by the agent or via `echo $!`; add 3-second startup grace period; add `WARNING:` if agent exits during grace period; log suggestion to check `/tmp/agent.log`
- [x] T019 [US3] Manual test: run full install, capture PID, verify `ps -p $PID` shows the agent, verify `/tmp/agent.properties` contains correct values (not the preauth code as api key), verify central platform sees the agent come online (heartbeat within 30s)

**Checkpoint**: User Story 3 complete — agent connects to platform with correct authentication credentials.

---

## Phase 6: User Story 4 - Informative Progress Output (Priority: P3)

**Goal**: The install script outputs clearly labeled, sectioned progress messages so operators can follow each step and diagnose issues easily.

**Independent Test**: Run the install script and verify output contains distinct sections with clear headers for each phase, and error messages are descriptive (not raw error codes).

### Implementation for User Story 4

- [x] T020 [US4] Format output sections in `api/src/main/resources/scripts/unix.sh.ftl` with consistent section headers and status indicators:
  - `=== Agent Installation ===` (header)
  - `=== Downloading agent binary ===` with byte count on success
  - `=== Verifying Blake3 hash ===` with `OK` or `MISMATCH`
  - `=== Verifying RSA signature ===` with `VERIFIED` or `SKIPPED`
  - `=== Installing agent ===` with PID or warning
  - Ensure all `FATAL:` and `WARNING:` messages are prefix-consistent
  - Ensure error messages describe WHAT failed (not stack traces)
- [x] T021 [US4] Add download retry logic: on failure, print `Retrying download...` and attempt once more before giving up with `FATAL: Cannot reach central platform after 2 attempts`
- [x] T022 [US4] Manual test: run the script and visually confirm all sections appear, status messages are clear, and the output is readable at a glance

**Checkpoint**: User Story 4 complete — output is professional, sectioned, and debuggable.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verification across all modules and final validation

- [x] T023 [P] Add unit test for `AgentServiceImpl.registerAgent()` in `api/src/test/java/com/spulido/tfg/domain/agent/services/impl/AgentServiceImplTest.java` — verify install token is generated, passed to ScriptService, and stored on Agent
- [ ] T024 [P] Add unit test for `AgentInstallBinaryController` in `api/src/test/java/com/spulido/tfg/domain/replication/controller/AgentInstallBinaryControllerTest.java` — verify valid token serves binary, consumed token returns 410, expired token returns 404
- [ ] T025 [P] Add unit test for RestTemplate interceptor in `agents/unix/src/test/java/com/spulido/agent/config/WorkerPoolConfigTest.java` — verify `X-Agent-Api-Key` and `X-Agent-Id` headers are added when apiKey/agentId are set, and omitted when they are null/empty
- [x] T026 Verify `unix-error.sh.ftl` in `api/src/main/resources/scripts/unix-error.sh.ftl` still works — error script is returned correctly when preauth code is invalid (unchanged from current behavior per FR-011)
- [ ] T027 Run quickstart.md validation scenario from `specs/015-agent-install-script/quickstart.md` — execute the full curl-pipe-bash flow end-to-end and confirm all steps pass
- [x] T028 Code review checklist: confirm all user-facing text is English, no secrets echoed in script output, POSIX-compatible shell syntax (no bashisms), FreeMarker variables properly escaped, no inline script building (all in .ftl templates)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (all API+agent infrastructure ready)
- **User Story 2 (Phase 4)**: Depends on US1 (script structure exists), enhances verification section
- **User Story 3 (Phase 5)**: Depends on US1 (script structure exists), enhances config + launch sections
- **User Story 4 (Phase 6)**: Depends on US1 (script structure exists), enhances output formatting
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational — No dependencies on US2/US3/US4. This is the MVP.
- **User Story 2 (P2)**: Can start after US1 is complete. Edits the verification section of the same template. Can be done in parallel with US3.
- **User Story 3 (P2)**: Can start after US1 is complete. Edits the config + launch sections. Can be done in parallel with US2.
- **User Story 4 (P3)**: Can start after US1 is complete. Edits output formatting across the template. Can be done in parallel with US2 and US3.

**Important**: Since US2, US3, and US4 all modify the same `unix.sh.ftl` template, parallel work on them requires careful merge coordination or sequential execution. Recommended: complete US1 → then do US2+US3+US4 sequentially (or in a feature branch with careful section isolation).

### Within Each Phase

- Foundational tasks T003-T008 (API side) can be parallelized where marked [P]
- Foundational tasks T009-T011 (Agent side) can be parallelized where marked [P]
- API and Agent foundational tasks are independent and can be done in parallel
- Within US1: T012 (template rewrite) is the main task; T013 and T014 are verification tasks
- Polish tasks T023-T026 are all independent and can run in parallel

### Parallel Opportunities

```bash
# Phase 2 - API foundational tasks (parallel):
Task: T004 "Add generateInstallToken() to AgentServiceImpl"
Task: T005 "Create AgentInstallBinaryController"
# (T003 Agent entity change must come first)

# Phase 2 - Agent foundational tasks (parallel with API tasks):
Task: T009 "Add RestTemplate interceptor in WorkerPoolConfig"
Task: T010 "Update install-agent-http.sh.tmpl"
Task: T011 "Update install-agent-transfer.sh.tmpl"

# Phase 7 - All polish tasks (parallel):
Task: T023 "Unit test for AgentServiceImpl"
Task: T024 "Unit test for AgentInstallBinaryController"
Task: T025 "Unit test for RestTemplate interceptor"
Task: T026 "Verify unix-error.sh.ftl"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T002)
2. Complete Phase 2: Foundational (T003-T011) — **CRITICAL — blocks all stories**
3. Complete Phase 3: User Story 1 (T012-T014)
4. **STOP and VALIDATE**: Run `curl | bash` end-to-end. Agent should download, verify (basic hash), configure, and launch.
5. Deploy/demo if ready — this is a working, shippable agent installer.

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 → curl-pipe-bash works end-to-end → **MVP!**
3. Add US2 → RSA signature verification added → Agent binary authenticity validated
4. Add US3 → Agent correctly authenticates to platform with API key → Platform sees agent online
5. Add US4 → Professional sectioned output → Better operator experience
6. Each story adds value without breaking the install flow

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (core script) — **blocks others for the same file**
   - After US1 complete:
     - Developer A: User Story 2 (verification enhancement)
     - Developer B: User Story 3 (config + launch enhancement) — coordinate on same file
     - Developer C: User Story 4 (output formatting) — coordinate on same file
3. All developers: Polish phase (unit tests can be parallelized)

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US1 is the MVP — a single curl-pipe-bash that gets a running agent (without RSA signature auth, with basic config)
- US2/US3/US4 are incremental quality/security enhancements to the same template
- The `unix.sh.ftl` template is the central artifact — all user stories enhance different sections of it
- Agent auth (RestTemplate interceptor) fix is in Phase 2 because without it, the agent can't talk to the platform even if installed correctly
- Do not run git commands unless explicitly approved
- Stop at any checkpoint to validate story independently
- All shell scripts MUST go through FreeMarker templates (`.ftl`) — never build scripts inline with `StringBuilder` or string concatenation per AGENTS.md
