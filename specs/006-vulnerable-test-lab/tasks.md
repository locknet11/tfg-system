# Tasks: Vulnerable Test Lab

**Input**: Design documents from `/specs/006-vulnerable-test-lab/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Manual verification via curl/HTTP exploit requests (no unit test framework needed).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the lab directory structure and core orchestration files

- [x] T001 Create `lab/` directory structure per plan.md (targets/, scripts/, .env)
- [x] T002 Create `lab/.env` with host port mappings from research.md
- [x] T003 Create `lab/docker-compose.yml` with all 5 target services, isolated network (172.20.0.0/24), static IPs, and host port 8081/8082/8000/8083/2375 mappings
- [x] T004 [P] Review applicable repository guidance in AGENTS.md — confirm no Angular/Spring Boot rules apply

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T005 Create `lab/scripts/deploy-common.sh` with shared functions (docker check, echo helpers, error handling)
- [x] T006 Create `lab/scripts/deploy-all.sh` with VulHub clone logic (clone if missing, copy targets)
- [x] T007 Create `lab/scripts/stop-all.sh` with `docker compose down` logic
- [x] T008 Create `lab/scripts/reset-all.sh` with stop + cleanup + redeploy logic

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 — Deploy Lab in One Step (Priority: P1) 🎯 MVP

**Goal**: A single command deploys all 5 vulnerable targets, accessible on their designated ports.

**Independent Test**: Run `./lab/scripts/deploy-all.sh` from a clean environment and confirm all 5 targets respond within 2 minutes:
- `curl -s -o /dev/null -w "%{http_code}" http://localhost:8081` → 200
- `curl -s -o /dev/null -w "%{http_code}" http://localhost:8082` → 200
- `curl -s -o /dev/null -w "%{http_code}" http://localhost:8000` → 200
- `curl -s -o /dev/null -w "%{http_code}" http://localhost:8083` → 200
- `curl -s http://localhost:2375/version | grep -q "ApiVersion"` → exit 0

### Implementation for User Story 1

- [x] T009 [P] [US1] Copy Drupal CVE-2018-7600 VulHub target to `lab/targets/drupal/`
- [x] T010 [P] [US1] Copy Tomcat CVE-2017-12615 VulHub target to `lab/targets/tomcat/`
- [x] T011 [P] [US1] Copy Flask SSTI VulHub target to `lab/targets/flask/`
- [x] T012 [P] [US1] Copy ThinkPHP 5-rce VulHub target to `lab/targets/thinkphp/`
- [x] T013 [P] [US1] Copy Docker unauthorized-rce VulHub target to `lab/targets/docker/`
- [x] T014 [US1] Integrate deploy-common.sh into deploy-all.sh with Docker check, build, and `docker compose up -d` logic
- [x] T015 [US1] Add post-deploy status check in deploy-all.sh that reports each target's reachability
- [x] T016 [US1] Verify all 5 targets are reachable using the independent test above

**Checkpoint**: At this point, User Story 1 should be fully functional. `./lab/scripts/deploy-all.sh` brings up all 5 targets.

---

## Phase 4: User Story 2 — Agent Discovery and Exploitation (Priority: P1)

**Goal**: Autonomous agents can discover targets, execute exploit playbooks, and validate exploitation success.

**Independent Test**: Run an agent against the Flask SSTI target (port 8000). Agent sends `?name={{7*7}}` and confirms `49` in response. Agent then sends RCE payload and validates via L1 layer.

### Implementation for User Story 2

- [x] T017 [P] [US2] Document Drupal exploit playbook in `lab/playbooks/drupal.md` with exact curl commands from research.md
- [x] T018 [P] [US2] Document Tomcat exploit playbook in `lab/playbooks/tomcat.md` with PUT + JSP webshell technique
- [x] T019 [P] [US2] Document Flask SSTI exploit playbook in `lab/playbooks/flask.md` with detection and RCE payloads
- [x] T020 [P] [US2] Document ThinkPHP exploit playbook in `lab/playbooks/thinkphp.md` with URL and parameter details
- [x] T021 [P] [US2] Document Docker API exploit playbook in `lab/playbooks/docker-api.md` with container create/start/logs workflow
- [x] T022 [P] [US2] Document 3-layer validation approach in `lab/playbooks/validation.md` with L1/L2/L3 fallback commands
- [x] T023 [US2] Add security warning banner to deploy-all.sh (displayed before build)

**Checkpoint**: Agent playbooks documented and deploy warning shown. Agent teams can now target the lab.

---

## Phase 5: User Story 3 — Stop and Reset Lab (Priority: P2)

**Goal**: Researcher can stop the lab and restore it to a clean, known state for repeatable testing.

**Independent Test**: Deploy lab, create a file inside a container (`echo test > /tmp/proof`), run stop-all.sh, run reset-all.sh, redeploy, and confirm the file does not exist.

### Implementation for User Story 3

- [x] T024 [P] [US3] Implement `lab/scripts/stop-all.sh` with `docker compose down` and volume cleanup
- [x] T025 [P] [US3] Implement `lab/scripts/reset-all.sh` with stop + image re-pull/re-build + deploy
- [x] T026 [US3] Add Docker availability check to both scripts before execution
- [x] T027 [US3] Verify stop and reset work with the independent test above

**Checkpoint**: Full lab lifecycle (deploy → stop → reset → redeploy) works with clean state.

---

## Phase 6: User Story 4 — Verify Individual Target Vulnerabilities (Priority: P3)

**Goal**: Researcher can manually verify each target is exploitable using known payloads before running full agent tests.

**Independent Test**: Run the Drupal exploit curl command against localhost:8081 and confirm command output in the response body.

### Implementation for User Story 4

- [x] T028 [P] [US4] Create `lab/scripts/verify-drupal.sh` with CVE-2018-7600 exploit curl + output check
- [x] T029 [P] [US4] Create `lab/scripts/verify-tomcat.sh` with CVE-2017-12615 PUT/GET webshell test
- [x] T030 [P] [US4] Create `lab/scripts/verify-flask.sh` with SSTI detection (7*7=49) + RCE payload test
- [x] T031 [P] [US4] Create `lab/scripts/verify-thinkphp.sh` with CVE-2018-20062 system("id") payload
- [x] T032 [P] [US4] Create `lab/scripts/verify-docker-api.sh` with container create/start/logs test
- [x] T033 [US4] Create `lab/scripts/verify-all.sh` that runs all verify-*.sh scripts and reports pass/fail per target

**Checkpoint**: Each target can be individually verified, and `verify-all.sh` gives a comprehensive status report.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Security warnings, edge case handling, and final validation

- [x] T034 [P] Add port conflict detection to deploy-all.sh (check ports before starting containers)
- [x] T035 [P] Add Docker daemon check to deploy-all.sh (warn if Docker not running)
- [x] T036 Add Docker API host-level access warning (the target can compromise the host)
- [x] T037 [P] Add internet connectivity check for first-time VulHub clone
- [x] T038 [P] Add edge case handling: graceful error when a target fails to start (other targets should still start)
- [x] T039 Run full lifecycle validation: deploy → test → stop → reset → deploy → test (repeat 3 times)
- [x] T040 Run final `verify-all.sh` on fresh deploy and confirm all 5 targets pass exploitation tests

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — create scripts
- **User Stories (Phase 3-6)**: All depend on Phases 1+2
  - US1 and US2 can run in parallel (US1 is deploy infrastructure, US2 is playbook docs)
  - US3 depends on US1 (deploy must work before stop/reset makes sense)
  - US4 depends on US1 (targets must deploy before verification works)
- **Polish (Phase 7)**: Depends on all user stories

### User Story Dependencies

- **User Story 1 (P1)** 🎯 MVP: Can start after Setup + Foundational. No dependencies on other stories.
- **User Story 2 (P1)**: Can start after Setup + Foundational. Independent from US1 (playbook docs don't need working deploy).
- **User Story 3 (P2)**: Depends on US1 (stop/reset only meaningful with working deploy).
- **User Story 4 (P3)**: Depends on US1 (verify scripts need running targets).

### Within Each User Story

- Docker compose config before deploy script
- Target copies before deploy-all.sh integration
- Per-target playbook docs can be parallel
- Per-target verify scripts can be parallel
- Story complete before moving to lower priority

### Parallel Opportunities

- T003 docker-compose.yml and T004 AGENTS.md review can run in parallel
- T009-T013 all target VulHub copies can run in parallel
- T017-T022 all playbook docs can run in parallel
- T024 stop-all.sh and T025 reset-all.sh can run in parallel
- T028-T032 all per-target verify scripts can run in parallel
- T034-T038 port/docker/connectivity checks can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch all VulHub target copies together:
Task: "Copy Drupal target to lab/targets/drupal/"
Task: "Copy Tomcat target to lab/targets/tomcat/"
Task: "Copy Flask target to lab/targets/flask/"
Task: "Copy ThinkPHP target to lab/targets/thinkphp/"
Task: "Copy Docker API target to lab/targets/docker/"

# Then integrate deploy-all.sh (depends on target copies):
Task: "Implement deploy-all.sh with build + up"
```

## Parallel Example: User Story 4

```bash
# Launch all verify scripts together (independent targets):
Task: "Create verify-drupal.sh"
Task: "Create verify-tomcat.sh"
Task: "Create verify-flask.sh"
Task: "Create verify-thinkphp.sh"
Task: "Create verify-docker-api.sh"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T004)
2. Complete Phase 2: Foundational (T005-T008)
3. Complete Phase 3: User Story 1 (T009-T016)
4. **STOP and VALIDATE**: Run deploy-all.sh and verify all 5 targets respond
5. Lab is deployable — basic MVP achieved

### Incremental Delivery

1. Setup + Foundational → Lab directory and scripts ready
2. US1 deploy-all.sh → All 5 targets deployable (MVP!)
3. US2 playbooks → Agent teams can target the lab
4. US3 stop/reset → Full lifecycle management
5. US4 verify scripts → Automated vulnerability validation
6. Polish → Production-ready error handling

### Single Developer Strategy

1. Setup (T001-T004) — 1 step
2. Foundational (T005-T008) — creates all scripts skeletons
3. US1 (T009-T016) — deploy working (MVP achieved)
4. US2 (T017-T023) — playbooks documented
5. US3 (T024-T027) — lifecycle complete
6. US4 (T028-T033) — verification automated
7. Polish (T034-T040) — hardening and validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Do not run git commands unless the user explicitly approves them
- Stop at any checkpoint to validate story independently
- No unit test framework needed — verification is curl-based exploit testing
- CVE reference: research.md has exact exploit payloads for each target
