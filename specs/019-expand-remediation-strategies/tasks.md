# Tasks: Expand Remediation Strategies Knowledge Base

**Input**: Design documents from `specs/019-expand-remediation-strategies/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Unit tests are generated for the seed loader (api/). Manual verification for lab (docker compose up) and UI (browser). No TDD requested — tests follow implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4, US5)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Review existing codebase and prepare for changes

**⚠️ CRITICAL**: This phase must complete before any implementation begins.

- [x] T001 Review AGENTS.md for Angular (ui/) and Spring Boot (api/) conventions applicable to this feature
- [x] T002 [P] Review existing `api/src/main/resources/remediation/strategies.json` structure and all 6 current entries
- [x] T003 [P] Review existing `lab/docker-compose.yml` and `lab/targets/` directory convention

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Seed Loader & Validation

- [x] T004 Modify incremental seeding logic in `api/src/main/java/com/spulido/tfg/domain/remediation/config/RemediationStrategyLoader.java` to iterate entries individually and skip duplicates using `findByCveIdAndOperatingSystem` pre-check instead of skipping entirely when collection is non-empty
- [x] T005 Add strategy entry validation in `api/src/main/java/com/spulido/tfg/domain/remediation/config/RemediationStrategyLoader.java`: validate `cveId` matches `CVE-\d{4}-\d{4,}` pattern, reject entries missing mandatory fields, log rejected entry index and reason without aborting the load
- [x] T006 Add `findAll(Pageable)` and query methods for filtering by `cveId`, `operatingSystem`, `packageName`, `remediationType`, `action` in `api/src/main/java/com/spulido/tfg/domain/remediation/db/RemediationStrategyRepository.java`
- [x] T007 Add `findAll(Pageable)` and filtered search methods to `api/src/main/java/com/spulido/tfg/domain/remediation/services/RemediationStrategyService.java` interface and `api/src/main/java/com/spulido/tfg/domain/remediation/services/impl/RemediationStrategyServiceImpl.java`

### API DTO

- [x] T008 [P] Create `StrategyCatalogEntry` DTO record in `api/src/main/java/com/spulido/tfg/domain/remediation/model/dto/StrategyCatalogEntry.java` with fields: id, cveId, operatingSystem, packageName, remediationType, action, targetVersion, preCheckCommands, fixCommands, postCheckCommands, serviceName, requiresReboot, notes. Add static `from(RemediationStrategy)` factory method
- [x] T009 [P] Create `StrategyCountResponse` DTO record in `api/src/main/java/com/spulido/tfg/domain/remediation/model/dto/StrategyCountResponse.java` with fields: total, byType (Map), byOs (Map)

### API Controller

- [x] T010 Create `RemediationStrategyController` in `api/src/main/java/com/spulido/tfg/domain/remediation/controller/RemediationStrategyController.java` with `GET /api/remediation-strategies` (paginated, filterable), `GET /api/remediation-strategies/{id}` (single), and `GET /api/remediation-strategies/count` (aggregate counts). Use `@RestController`, constructor injection, `PageRequest`, and JWT security matching existing `RemediationController` pattern

### Seed Loader Tests

- [x] T011 Create `RemediationStrategyLoaderTest` in `api/src/test/java/com/spulido/tfg/domain/remediation/config/RemediationStrategyLoaderTest.java`: test that empty collection seeds all entries, test that non-empty collection adds only new entries (no duplicates), test that invalid entries are skipped with log output, test that valid entries load despite invalid ones in the file

**Checkpoint**: Foundation ready — user story implementation can now begin. All seeds, validation, API endpoints, and DTOs are in place.

---

## Phase 3: User Story 1 - Expanded Vulnerability Detection Coverage (Priority: P1) 🎯 MVP

**Goal**: Expand strategies.json from 6 to 30+ CVE entries covering at least 15 unique packages across 7 service categories (web servers, databases, SSH, mail, DNS, containers, language runtimes)

**Independent Test**: Deploy an agent against a target with a newly defined vulnerability (e.g., vulnerable PostgreSQL) and verify the agent detects, reports, and remediates it using the corresponding strategy entry.

### Implementation for User Story 1

- [x] T012 [P] [US1] Add 4-5 web server CVE strategy entries (nginx, apache2) in `api/src/main/resources/remediation/strategies.json` — include CVEs like CVE-2024-40725 (Apache mod_rewrite), CVE-2024-7342 (nginx), CVE-2023-45802 (Apache HTTP/2). All entries on ubuntu-22.04 with SERVICE_UPDATE type and APT_UPGRADE action. Include correct target versions and pre/post check commands

- [x] T013 [P] [US1] Add 5-6 database CVE strategy entries (postgresql, mysql-server, mariadb-server) in `api/src/main/resources/remediation/strategies.json` — include CVEs like CVE-2024-4317 (PostgreSQL), CVE-2024-20963 (MySQL), CVE-2023-21930 (MariaDB). All entries on ubuntu-22.04. Include SERVICE_UPDATE and REBOOT_REQUIRED types

- [x] T014 [P] [US1] Add 2-3 SSH CVE strategy entries (openssh-server) in `api/src/main/resources/remediation/strategies.json` — add CVE-2024-6409 (OpenSSH privilege escalation) on ubuntu-22.04 as SERVICE_UPDATE

- [x] T015 [P] [US1] Add 3-4 mail server CVE strategy entries (postfix, dovecot-core, exim4) in `api/src/main/resources/remediation/strategies.json` — include CVEs like CVE-2023-51764 (Postfix SMTP smuggling), CVE-2024-23184 (Dovecot). On ubuntu-22.04. Mix SERVICE_UPDATE and REBOOT_REQUIRED

- [x] T016 [P] [US1] Add 2-3 DNS server CVE strategy entries (bind9) in `api/src/main/resources/remediation/strategies.json` — include CVE-2024-0760 (BIND9), CVE-2023-3341. On ubuntu-22.04 as SERVICE_UPDATE

- [x] T017 [P] [US1] Add 3-4 container runtime CVE strategy entries (docker.io, containerd, runc) in `api/src/main/resources/remediation/strategies.json` — include CVE-2024-21626 (runc), CVE-2024-23652 (containerd buildkit). On ubuntu-22.04. Include KERNEL_UPDATE and REBOOT_REQUIRED where applicable

- [x] T018 [P] [US1] Add 4-5 language runtime CVE strategy entries (php, python3, nodejs) in `api/src/main/resources/remediation/strategies.json` — include CVE-2024-4577 (PHP CGI argument injection), CVE-2024-4032 (Python), CVE-2024-27980 (Node.js). On ubuntu-22.04 as SERVICE_UPDATE

**Checkpoint**: At this point, strategies.json should contain 30+ entries covering 15+ packages across 7 categories on ubuntu-22.04. Run API and verify seed log shows 30+ entries seeded. MVP deliverable.

---

## Phase 4: User Story 2 + User Story 5 - Multi-OS Support & Lab Targets (Priority: P2)

**Goal**: Add OS-specific strategy entries for Ubuntu 20.04, Debian 11, Debian 12. Expand Docker Compose lab from 5 to 10+ vulnerable containers matching the strategies.

**Independent Test US2**: Deploy agents on different OS versions and verify each OS receives correct, version-appropriate commands for the same CVE.

**Independent Test US5**: Run `docker compose up` in lab/, verify new vulnerable services start successfully, deploy agent against lab network, confirm agent detects vulnerabilities on new containers.

### Implementation for User Story 2 (Multi-OS)

- [x] T019 [P] [US2] Add ubuntu-20.04 variant entries for at least 8 existing strategies in `api/src/main/resources/remediation/strategies.json` — for any CVE where the fixed version differs between 20.04 and 22.04, create a new entry with `"operatingSystem": "ubuntu-20.04"` and the correct target version from Ubuntu 20.04 security trackers

- [x] T020 [P] [US2] Add debian-11 variant entries for at least 6 existing strategies in `api/src/main/resources/remediation/strategies.json` — use `"operatingSystem": "debian-11"` with correct package versions from Debian 11 (bullseye) security trackers

- [x] T021 [P] [US2] Add debian-12 variant entries for at least 6 existing strategies in `api/src/main/resources/remediation/strategies.json` — use `"operatingSystem": "debian-12"` with correct package versions from Debian 12 (bookworm) security trackers

### Implementation for User Story 5 (Lab Containers)

- [x] T022 [P] [US5] Create vulnerable PostgreSQL container in `lab/targets/postgres/Dockerfile` using `postgres:14` base image pinned to a vulnerable minor version, expose port 5432. Add service entry in `lab/docker-compose.yml` with static IP `172.20.0.20` and port mapping `"5432:5432"`

- [x] T023 [P] [US5] Create vulnerable MySQL container in `lab/targets/mysql/Dockerfile` using `mysql:8.0` base image pinned to a vulnerable minor version, expose port 3306. Add service entry in `lab/docker-compose.yml` with static IP `172.20.0.21` and port mapping `"3306:3306"`

- [x] T024 [P] [US5] Create vulnerable BIND9 DNS container in `lab/targets/bind9/Dockerfile` using `ubuntu:22.04` base with bind9 installed at a vulnerable version via APT pinning. Expose port 53/udp. Add service entry in `lab/docker-compose.yml` with static IP `172.20.0.22` and port mapping `"5353:53/udp"`

- [x] T025 [P] [US5] Create vulnerable Postfix mail container in `lab/targets/postfix/Dockerfile` using `ubuntu:22.04` base with postfix installed at a vulnerable version via APT pinning. Expose port 25. Add service entry in `lab/docker-compose.yml` with static IP `172.20.0.23` and port mapping `"2525:25"`

- [x] T026 [P] [US5] Create vulnerable PHP-FPM container in `lab/targets/php-fpm/Dockerfile` using `php:8.1-fpm` base image pinned to a vulnerable minor version with a simple index.php. Expose port 9000. Add service entry in `lab/docker-compose.yml` with static IP `172.20.0.24` and port mapping `"9000:9000"`

- [x] T027 [P] [US5] Create vulnerable Node.js container in `lab/targets/nodejs/Dockerfile` using `node:18` base image pinned to a vulnerable minor version running a simple HTTP server on port 3000. Add service entry in `lab/docker-compose.yml` with static IP `172.20.0.25` and port mapping `"3000:3000"`

- [ ] T028 [US5] Verify lab: run `docker compose up -d` from `lab/`, confirm all 11+ containers start successfully within 60 seconds, verify each is reachable on its documented port, and check that existing services (drupal, tomcat, flask, thinkphp, docker-api) are unaffected

**Checkpoint**: Strategies.json now has 45+ entries across 4 OS variants. Lab has 11 containers (6 new + 5 existing). Run `docker compose ps` to verify.

---

## Phase 5: User Story 3 - Diverse Remediation Actions (Priority: P3)

**Goal**: Add strategy entries using at least 3 different action types beyond APT_UPGRADE (CONFIG_UPDATE, APT_INSTALL, SYSTEMCTL_RESTART) so complex fixes are automated.

**Independent Test**: Create a strategy with CONFIG_UPDATE action for a vulnerability requiring a config change, deploy agent against affected host, verify the config is updated and service restarted.

### Implementation for User Story 3

- [ ] T029 [P] [US3] Add 2-3 strategy entries using `"action": "CONFIG_UPDATE"` in `api/src/main/resources/remediation/strategies.json` — for CVEs requiring configuration file changes (e.g., disabling weak TLS ciphers in Apache via `a2dismod`, restricting Postfix SMTP auth). Include `fixCommands` that modify config files and `postCheckCommands` that verify the config change

- [ ] T030 [P] [US3] Add 2-3 strategy entries using `"action": "APT_INSTALL"` in `api/src/main/resources/remediation/strategies.json` — for CVEs where the fix involves installing a previously absent package (e.g., installing `mod_security` for Apache, installing `fail2ban`). Include `preCheckCommands` verifying the package is absent and `postCheckCommands` verifying it is present

- [ ] T031 [P] [US3] Add 2-3 strategy entries using `"action": "SYSTEMCTL_RESTART"` in `api/src/main/resources/remediation/strategies.json` — for CVEs where a service restart alone applies a pending configuration change. Include `fixCommands` with `systemctl restart` and `postCheckCommands` with `systemctl is-active`

- [ ] T032 [US3] Verify remedy action diversity: run `api/src/test/java/com/spulido/tfg/domain/remediation/config/RemediationStrategyLoaderTest.java` confirming at least 3 action types are represented and all entries pass validation

**Checkpoint**: Strategies now include APT_UPGRADE, APT_INSTALL, CONFIG_UPDATE, SYSTEMCTL_RESTART, and MANUAL actions. Run unit tests to verify all entries load.

---

## Phase 6: User Story 4 - Strategy Browsing in Dashboard (Priority: P4)

**Goal**: Add a strategy catalog view in the web dashboard where administrators can browse, search, and filter all remediation strategies.

**Independent Test**: Log into the dashboard, navigate to the strategies view, verify 30+ entries are displayed in a searchable, filterable table.

### UI Model & Service

- [x] T033 [P] [US4] Add `RemediationStrategy`, `RemediationAction`, and `StrategyListResponse` TypeScript interfaces in `ui/src/app/pages/remediations/data-access/remediations.model.ts` — follow existing patterns with `readonly` fields and `readonly` arrays

- [x] T034 [P] [US4] Add `listStrategies(page, size, filters)` and `getStrategy(id)` methods to `ui/src/app/pages/remediations/data-access/remediations.service.ts` — match the API contract from `contracts/strategy-api.yaml`. Use `HttpParams` for query parameters, return typed `Observable<StrategyListResponse>` and `Observable<RemediationStrategy>`

### UI Component

- [x] T035 [US4] Create `StrategiesListComponent` standalone component in `ui/src/app/pages/remediations/feature/strategies-list/strategies-list.component.ts` with: signal-based inputs, OnPush change detection, PrimeNG p-table with pagination, filter inputs for cveId/operatingSystem/packageName/remediationType columns, and a detail expansion panel (p-rowExpansion) showing full command lists and notes. Inject `RemediationsService` via constructor

- [x] T036 [US4] Create component template in `ui/src/app/pages/remediations/feature/strategies-list/strategies-list.component.html` with: PrimeNG p-table (paginator, sortable columns, filter row), expansion row template for detail view showing preCheckCommands/fixCommands/postCheckCommands as code blocks, and loading/empty states. Use i18n attributes on all user-facing labels

- [x] T037 [P] [US4] Create component styles in `ui/src/app/pages/remediations/feature/strategies-list/strategies-list.component.scss` — ensure command lists in expansion rows use monospace font and proper wrapping, filter inputs align with column width, responsive table for smaller screens

### Route & Integration

- [x] T038 [US4] Add strategies route in `ui/src/app/pages/remediations/remediations.routes.ts` — add child route for `strategies-list` component under the remediations path. Ensure the route is protected by existing auth guards

- [x] T039 [US4] Add i18n keys for all new dashboard labels in `ui/src/i18n/en.json` — keys for: strategy catalog title, column headers (CVE ID, OS, Package, Type, Action, Version, Reboot), filter placeholders, empty state message, detail panel labels (Pre-Check Commands, Fix Commands, Post-Check Commands, Notes), and total count label. All text in English

- [ ] T040 [US4] Verify dashboard: start API (`api/`), start UI (`ui/`), log into dashboard, navigate to strategies catalog, verify all 30+ entries display, test filter by CVE ID (partial match), test filter by OS, test filter by package name, verify detail expansion shows full commands, verify pagination works with page size 10

**Checkpoint**: Dashboard now has a fully functional strategy catalog view. Administrators can browse, search, and filter all strategies.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final verification and quality assurance across all user stories

- [x] T041 [P] Run Prettier format check on all UI changes: `cd ui && npx prettier --check src/app/pages/remediations/`

- [x] T042 [P] Verify English-only compliance: audit all strategy `notes` fields in `api/src/main/resources/remediation/strategies.json` are in English; audit all i18n keys in `ui/src/i18n/en.json` are in English; audit component template text uses `i18n` attributes

- [x] T043 Build and test API: `cd api && ./mvnw clean package` — confirm compilation succeeds, all unit tests pass including `RemediationStrategyLoaderTest`

- [x] T044 Build UI: `cd ui && npm ci && npm run build` — confirm production build succeeds with no errors

- [ ] T045 Verify end-to-end flow per `specs/019-expand-remediation-strategies/quickstart.md`: start MongoDB, start API, start UI, start lab with `docker compose up`, confirm strategy catalog shows 30+ entries, confirm agent can deploy to lab network and detect vulnerabilities

- [x] T046 [P] Update agent context reference if needed — confirm `AGENTS.md` SPECKIT block already points to `specs/019-expand-remediation-strategies/plan.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup (Phase 1) — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) — MVP data expansion
- **User Story 2+5 (Phase 4)**: Depends on US1 (Phase 3) — needs existing entries to create OS variants, needs strategies to design lab containers
- **User Story 3 (Phase 5)**: Depends on US1 (Phase 3) — needs strategies in place to add diverse action types
- **User Story 4 (Phase 6)**: Depends on Foundational (Phase 2) — uses API endpoints, does NOT depend on US1/US2/US3 completion (can browse whatever is seeded)
- **Polish (Phase 7)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational (Phase 2) — no dependencies on other stories. **This is the MVP.**
- **US2 (P2)**: Can start after US1 — needs existing strategies to create OS variants
- **US5 (P2)**: Can start after US1 — needs strategies defined to design matching lab containers. Can run in parallel with US2.
- **US3 (P3)**: Can start after US1 — needs strategies to extend with diverse actions
- **US4 (P4)**: Can start after Foundational (Phase 2) — independent of US1/US2/US3/US5. Can run in parallel with any phase after Phase 2.

### Within Each User Story

- **US1**: All tasks are [P] — all 7 category groups can be written in parallel (add web server entries, add database entries, etc.)
- **US2+US5**: OS variants (T019-T021) are [P] — can be written in parallel. Lab Dockerfiles (T022-T027) are all [P] — can be created in parallel. T028 (lab verification) runs last.
- **US3**: All [P] — CONFIG_UPDATE, APT_INSTALL, SYSTEMCTL_RESTART groups can be added in parallel
- **US4**: T033-T034 [P] (model + service), then T035 (component depends on model), T036-T037 [P] (template + styles), T038-T039 [P] (route + i18n), T040 last (verification)

### Parallel Opportunities

- All Setup tasks (T001-T003) can run in parallel
- Within Phase 2: T008-T009 [P] (DTOs), T011 [P] (tests) can run in parallel with T004-T007
- US1: All 7 category tasks (T012-T018) are fully parallelizable
- US2: All 3 OS variant tasks (T019-T021) are fully parallelizable
- US5: All 6 Dockerfile tasks (T022-T027) are fully parallelizable. US2 and US5 can run in parallel with each other.
- US3: All 3 action-type tasks (T029-T031) are fully parallelizable
- US4: T033-T034 can run in parallel with T038-T039. T036-T037 can run in parallel with each other.
- US4 can run in parallel with US1, US2, US3, and US5 after Phase 2

---

## Parallel Example: User Story 1

```bash
# Launch all category strategy expansions together:
Task: "Add 4-5 web server CVE strategy entries in strategies.json"
Task: "Add 5-6 database CVE strategy entries in strategies.json"
Task: "Add 2-3 SSH CVE strategy entries in strategies.json"
Task: "Add 3-4 mail server CVE strategy entries in strategies.json"
Task: "Add 2-3 DNS server CVE strategy entries in strategies.json"
Task: "Add 3-4 container runtime CVE strategy entries in strategies.json"
Task: "Add 4-5 language runtime CVE strategy entries in strategies.json"
```

## Parallel Example: User Story 4 (Dashboard)

```bash
# Phase 1: Model + Service + Route + i18n (all parallel):
Task: "Add TypeScript interfaces in remediations.model.ts"
Task: "Add listStrategies/getStrategy methods in remediations.service.ts"
Task: "Add strategies route in remediations.routes.ts"
Task: "Add i18n keys in en.json"

# Phase 2: Component (sequential, depends on model):
Task: "Create StrategiesListComponent"

# Phase 3: Template + Styles (parallel):
Task: "Create component template in strategies-list.component.html"
Task: "Create component styles in strategies-list.component.scss"

# Phase 4: Verification
Task: "Verify dashboard end-to-end"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T003)
2. Complete Phase 2: Foundational (T004-T011) — **CRITICAL**
3. Complete Phase 3: User Story 1 (T012-T018)
4. **STOP and VALIDATE**: Run API, verify 30+ strategies seed successfully, deploy agent against a target with a new strategy, confirm detection + remediation
5. This is a deployable MVP — the system now covers 30+ CVEs across 7 categories

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 → Test independently → **MVP!** — 30+ strategies, core value delivered
3. Add US2 + US5 → Test independently → Multi-OS support + 11 lab containers
4. Add US3 → Test independently → Diverse remediation actions
5. Add US4 → Test independently → Dashboard strategy catalog view
6. Polish → Final verification → Production ready

### Suggested MVP Scope

**Phase 1 + Phase 2 + Phase 3 (US1 only) = MVP**

This delivers 30+ strategies across 7 categories with idempotent seeding and API access — the core user value. Everything else (multi-OS variants, lab containers, diverse actions, dashboard view) enhances but doesn't block the core capability.

---

## Notes

- [P] tasks = different files or independent sections within the same file, no dependency conflicts
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- US1, US2, US3 all write to `strategies.json` — coordinate merges when parallelizing
- US5 writes to `docker-compose.yml` and `lab/targets/*/` — coordinate with lab structure
- US4 writes to UI files under `ui/src/app/pages/remediations/` — follows existing Angular patterns
- Verify the API builds successfully (`./mvnw clean package`) before committing
- Verify the UI builds successfully (`npm run build`) before committing
- Do not run git commands unless the user explicitly approves them
- Stop at any checkpoint to validate story independently
- English-only for all user-facing text, code comments, and strategy notes
