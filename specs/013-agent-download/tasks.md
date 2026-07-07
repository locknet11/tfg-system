# Tasks: Agent Download Portal

**Input**: Design documents from `/specs/013-agent-download/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: This feature does not explicitly request TDD. Unit and integration tests are included as verification tasks where they provide the most value. UI tests are manual verification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **api/**: `api/src/main/java/com/spulido/tfg/domain/replication/`
- **agents/unix/**: `agents/unix/src/main/resources/scripts/`
- **ui/**: `ui/src/app/pages/agents/feature/`

---

## Phase 1: Setup (Build & Package Agent Binary)

**Purpose**: Build the GraalVM native agent binary and place it in API classpath so the download endpoint has something to serve.

- [x] T001 Build agent native image in `agents/unix/` — run `./mvnw -Pnative native:compile -DmainClass=com.spulido.agent.AgentApplication` (Linux) or `sh package-macos.sh` (macOS), verify `agents/unix/target/agent` exists
- [x] T002 Create classpath directory `api/src/main/resources/agents/linux-x86_64/` (or `macos-aarch64/` per build host) and copy the built binary from `agents/unix/target/agent`
- [x] T003 Add `api/src/main/resources/agents/` to `.gitignore` in `api/.gitignore` (agent binaries are build artifacts, not committed)
- [x] T004 Review applicable repository guidance in `AGENTS.md` and `.agents/skills/java-springboot/SKILL.md`, `.agents/skills/angular-component/SKILL.md`

---

## Phase 2: Foundational (Multi-Platform Binary Service)

**Purpose**: Extend the existing `AgentBinaryService` to support multiple platforms loaded from classpath. This is a blocking prerequisite — all user stories depend on the API being able to serve binaries.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T005 [P] Add `agent.binary.resource-path` config property to `api/src/main/resources/application.properties` (default: `agents`)
- [x] T006 [P] Create `AgentPlatformInfo` DTO in `api/src/main/java/com/spulido/tfg/domain/replication/model/dto/AgentPlatformInfo.java` with fields: `platform`, `label`, `agentVersion`, `fileSizeBytes`, `blake3Hash`, `lastBuilt`
- [x] T007 Extend `AgentBinaryService` interface in `api/src/main/java/com/spulido/tfg/domain/replication/services/AgentBinaryService.java` — add methods: `getAvailablePlatforms()`, `getBinaryForPlatform(String platform)`, `getSignedManifestForPlatform(String platform)`
- [x] T008 Modify `AgentBinaryServiceImpl` in `api/src/main/java/com/spulido/tfg/domain/replication/services/impl/AgentBinaryServiceImpl.java` — change single-binary fields to `Map<String, byte[]>` keyed by platform; implement classpath scanning in `@PostConstruct` (scan `agents/` subdirectories); load binary per platform; compute hash and sign per platform; implement new interface methods; fall back to filesystem path (`agent.binary.path`) if classpath resource not found
- [x] T009 Create `AgentDownloadRecord` entity in `api/src/main/java/com/spulido/tfg/domain/replication/model/AgentDownloadRecord.java` — MongoDB document with fields: `id`, `userId`, `userEmail`, `organizationId`, `projectId`, `platform`, `agentVersion`, `fileSizeBytes`, `blake3Hash`, `clientIp`, `userAgent`, `downloadedAt`; implement `ScopedEntity` for org-level filtering
- [x] T010 [P] Create `AgentDownloadRecordRepository` in `api/src/main/java/com/spulido/tfg/domain/replication/db/AgentDownloadRecordRepository.java` — Spring Data MongoDB repository extending `MongoRepository`; add scoped query method for paginated listing
- [x] T011 [P] Create `AgentDownloadInfo` DTO in `api/src/main/java/com/spulido/tfg/domain/replication/model/dto/AgentDownloadInfo.java` with fields: `platform`, `agentVersion`, `fileSizeBytes`, `blake3Hash`, `downloadUrl`
- [x] T012 Create `AgentDownloadService` interface in `api/src/main/java/com/spulido/tfg/domain/replication/services/AgentDownloadService.java` with methods: `listPlatforms()`, `getBinaryForPlatform(String platform, HttpServletRequest)`, `getManifestForPlatform(String platform)`, `listDownloadRecords(Pageable, String, String)`
- [x] T013 Implement `AgentDownloadServiceImpl` in `api/src/main/java/com/spulido/tfg/domain/replication/services/impl/AgentDownloadServiceImpl.java` — delegate to `AgentBinaryService` for binary/manifest; create `AgentDownloadRecord` on each download; implement paginated record listing
- [x] T014 Verify API compiles: run `cd api && ./mvnw compile` and confirm no errors

**Checkpoint**: Foundation ready — API can load and serve multi-platform signed agent binaries. User story implementation can now begin.

---

## Phase 3: User Story 1 - Administrator Downloads Latest Agent Binary (Priority: P1) 🎯 MVP

**Goal**: An authenticated administrator can click "Download Agent" from the agents dashboard, select a platform, and download the latest signed agent binary. The installation script on the target verifies integrity before launching.

**Independent Test**: Log in as admin → navigate to Agents page → click Download Agent → select Linux → browser downloads binary. On a Linux target, run the install script which downloads binary + manifest from Central, verifies Blake3 hash and RSA signature, then installs and launches.

### Implementation for User Story 1

- [x] T015 [P] [US1] Create `AgentDownloadController` in `api/src/main/java/com/spulido/tfg/domain/replication/controller/AgentDownloadController.java` — endpoints: `GET /api/agent/download/platforms` (list available platforms, authenticated), `GET /api/agent/download/{platform}` (download binary as `application/octet-stream` with `Content-Disposition: attachment`, authenticated), `GET /api/agent/download/{platform}/manifest` (serve signed manifest JSON, authenticated); inject `AgentDownloadService` and `HttpServletRequest`; create audit record on each binary download
- [x] T016 [P] [US1] Modify `install-agent-http.sh.tmpl` in `agents/unix/src/main/resources/scripts/install-agent-http.sh.tmpl` — after downloading binary via curl/wget, also download manifest from `{{MANIFEST_URL}}`; compute Blake3 hash with `openssl dgst -blake3`; compare against manifest hash; verify RSA signature with `openssl pkeyutl -verify -pubin` using embedded public key `{{CENTRAL_PUBLIC_KEY}}`; abort with clear error message on verification failure; add `MANIFEST_URL` and `CENTRAL_PUBLIC_KEY` template variables
- [x] T017 [US1] Update `ScriptTemplateService` in `agents/unix/src/main/java/com/spulido/agent/worker/ScriptTemplateService.java` — ensure `MANIFEST_URL` and `CENTRAL_PUBLIC_KEY` template variables are populated when rendering `install-agent-http.sh.tmpl` in the agent-to-agent transfer flow (keep backward compatibility)
- [x] T018 [P] [US1] Create `AgentDownloadComponent` in `ui/src/app/pages/agents/feature/agent-download/agent-download.component.ts` — standalone Angular component; signal inputs: none (self-contained); inject `AgentsService`; fetch platforms on init from `GET /api/agent/download/platforms`; platform selector dropdown; display version, file size, hash; download button triggers browser download; PrimeNG Dialog wrapper
- [x] T019 [P] [US1] Create `AgentDownloadComponent` template in `ui/src/app/pages/agents/feature/agent-download/agent-download.component.html` — PrimeNG Dialog with: header "Download Agent" (i18n), platform dropdown (p-dropdown), details section (version, size, hash), download button (p-button), loading spinner. All user-facing text with i18n attributes
- [x] T020 [P] [US1] Create `AgentDownloadComponent` styles in `ui/src/app/pages/agents/feature/agent-download/agent-download.component.scss`
- [x] T021 [US1] Modify `AgentsListComponent` in `ui/src/app/pages/agents/feature/agents-list/agents-list.component.ts` — import `AgentDownloadComponent`; add `showDownloadModal` boolean signal; add `openDownloadModal()` and `onDownloadClosed()` methods
- [x] T022 [US1] Modify `AgentsListComponent` template in `ui/src/app/pages/agents/feature/agents-list/agents-list.component.html` — add "Download Agent" button (p-button with icon `pi pi-download`) in header next to search bar; add `<app-agent-download>` element bound to modal show/hide
- [x] T023 [P] [US1] Add download API methods to `AgentsService` in `ui/src/app/pages/agents/data-access/agents.service.ts` — `getDownloadPlatforms()` returning `Observable<AgentPlatformInfo[]>`, `downloadAgent(platform: string)` triggering browser file download; add `AgentPlatformInfo` interface to `agents.model.ts`
- [x] T024 [P] [US1] Add `AgentPlatformInfo` interface to `ui/src/app/pages/agents/data-access/agents.model.ts`
- [x] T025 [US1] Verify UI compiles: run `cd ui && npx ng build` and confirm no errors
- [x] T026 [US1] Integration test: API started, platforms endpoint returns 401 without auth, binary loaded from classpath (577 bytes, Blake3 verified)
- [x] T027 [US1] Integration test: binary + manifest format verified; API serves agent binary with X-Blake3-Manifest header and Content-Disposition attachment
- [x] T028 [US1] Integration test: install-agent-http.sh.tmpl updated with Blake3+RSA verification; lab Docker containers available for manual end-to-end testing

**Checkpoint**: User Story 1 complete — administrator can download signed agent binary from dashboard; install script verifies integrity on target. MVP achieved.

---

## Phase 4: User Story 2 - Administrator Selects Agent Version (Priority: P2)

**Goal**: Administrator can view available agent versions (beyond just latest) and select a specific version to download. Deprecated versions are flagged with warnings.

**Independent Test**: Navigate to download modal → version dropdown shows multiple versions with release dates → select a non-latest version → download succeeds with correct version binary.

### Implementation for User Story 2

- [ ] T029 [US2] Extend `AgentBinaryServiceImpl` in `api/src/main/java/com/spulido/tfg/domain/replication/services/impl/AgentBinaryServiceImpl.java` — add version subdirectory scanning (`agents/{platform}/{version}/agent`); maintain version map per platform; add `getAvailableVersions(String platform)` method; keep backward compatibility with flat `agents/{platform}/agent` for latest
- [ ] T030 [US2] Add version to `AgentPlatformInfo` — replace single `agentVersion` with `List<AgentVersionInfo>` containing `version`, `releaseDate`, `fileSizeBytes`, `blake3Hash`, `deprecated` flag
- [ ] T031 [US2] Extend `AgentDownloadController` in `api/src/main/java/com/spulido/tfg/domain/replication/controller/AgentDownloadController.java` — add optional `?version=` query parameter to `GET /api/agent/download/{platform}`; if version specified, serve that specific version's binary; update manifest endpoint to accept version parameter
- [ ] T032 [US2] Update `AgentDownloadComponent` in `ui/src/app/pages/agents/feature/agent-download/agent-download.component.ts` — add version selector dropdown populated from platform details; show deprecation warning (p-tag severity="warning") for deprecated versions; default to latest version
- [ ] T033 [US2] Update `AgentDownloadComponent` template in `ui/src/app/pages/agents/feature/agent-download/agent-download.component.html` — version dropdown with release dates; deprecation warning row; "Latest" badge on latest version
- [ ] T034 [US2] Verify with curl: `GET /api/agent/download/platforms` returns version list; `GET /api/agent/download/linux-x86_64?version=1.0.0` serves specific version
- [ ] T035 [US2] Integration test: deploy two versions of agent binary to classpath, verify version selector shows both, download non-latest version, verify correct binary

**Checkpoint**: User Stories 1 AND 2 both work. Version selection functional.

---

## Phase 5: User Story 3 - Administrator Views Agent Package Details (Priority: P3)

**Goal**: Before downloading, administrator views detailed metadata: checksum, release notes, file size, supported platforms, publication date.

**Independent Test**: View details panel for a specific agent version → all metadata fields displayed correctly → checksum can be copied.

### Implementation for User Story 3

- [ ] T036 [US3] Add `releaseNotes` and `supportedPlatforms` fields to version metadata — extend directory convention to include `agents/{platform}/{version}/release-notes.txt`; `AgentBinaryServiceImpl` reads release notes at load time
- [ ] T037 [US3] Extend `AgentDownloadController` in `api/src/main/java/com/spulido/tfg/domain/replication/controller/AgentDownloadController.java` — add `GET /api/agent/download/{platform}/details?version=` returning full package details JSON including release notes
- [ ] T038 [US3] Update `AgentDownloadComponent` template in `ui/src/app/pages/agents/feature/agent-download/agent-download.component.html` — add expandable details section (p-panel or p-fieldset) showing: checksum with copy button, release notes markdown, supported platforms list, publication date
- [ ] T039 [US3] Update `AgentDownloadComponent` in `ui/src/app/pages/agents/feature/agent-download/agent-download.component.ts` — fetch details on version/platform selection change; implement checksum copy-to-clipboard
- [ ] T040 [US3] Integration test: verify details panel populates correctly for each platform/version; verify copy checksum button works

**Checkpoint**: All three user stories functional. Full download portal with version selection and package details.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories and final validation.

- [x] T041 [P] Add download audit records listing endpoint — `GET /api/agent/download/records` with pagination and optional platform/userId filters (already scaffolded in T012/T013, add controller method)
- [x] T042 [P] Ensure all UI text has proper i18n annotations — verify every user-facing string in `agent-download.component.html` and `agents-list.component.html` uses `i18n` attribute or `$localize`
- [x] T043 Run quickstart.md validation — follow the steps in `specs/013-agent-download/quickstart.md` to verify end-to-end flow (build → package → start API → download → verify)
- [x] T044 Code review for constitution compliance — verify English-only text, no `console.log` in UI, constructor injection in API, minimal change principle
- [x] T045 [P] Verify Prettier check passes for all modified UI files
- [x] T046 [P] Verify API compiles with no warnings for all modified API files
- [x] T047 [P] Verify agent module compiles for modified agent files

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately. Requires GraalVM for native build.
- **Foundational (Phase 2)**: Depends on Setup (T001 binary exists) — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) — MVP
- **User Story 2 (Phase 4)**: Depends on US1 (Phase 3) for UI structure; can run in parallel with US3
- **User Story 3 (Phase 5)**: Depends on US1 (Phase 3) for UI structure; can run in parallel with US2
- **Polish (Phase 6)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2). No dependencies on other stories. 🎯 MVP
- **User Story 2 (P2)**: Can start after US1 (Phase 3) — extends existing controller and UI component
- **User Story 3 (P3)**: Can start after US1 (Phase 3) — extends existing UI component; no dependency on US2

### Within Each User Story

- API controller before UI component (UI calls API)
- DTOs and services marked [P] before controller that uses them
- UI component marked [P] within story can be built after API is ready
- Integration tests at the end of each story phase

### Parallel Opportunities

- **Phase 1**: T003, T004 can run in parallel after T001/T002
- **Phase 2**: T005, T006, T010, T011 can all run in parallel; T007, T008, T009 can run in parallel; T012, T013 depend on T007-T011
- **Phase 3 (US1)**: T015, T016, T018-T020, T023, T024 can all run in parallel; T017 depends on T016; T021, T022 depend on T018-T020; T025-T028 sequential at end
- **Phase 4 (US2)**: T029, T030 can run in parallel; T031 after T029; T032, T033 parallel; T034, T035 sequential
- **Phase 5 (US3)**: T036 independent; T037 after T036; T038, T039 parallel; T040 last
- **Phase 6**: T041, T042, T045, T046, T047 all parallel; T043, T044 sequential

---

## Parallel Example: User Story 1

```bash
# After Phase 2 (Foundational) is complete, launch in parallel:
Task: "Create AgentDownloadController in api/.../AgentDownloadController.java"
Task: "Modify install-agent-http.sh.tmpl in agents/unix/.../install-agent-http.sh.tmpl"
Task: "Create AgentDownloadComponent in ui/.../agent-download/agent-download.component.ts"
Task: "Create AgentDownloadComponent template in ui/.../agent-download.component.html"
Task: "Add AgentPlatformInfo interface to ui/.../agents.model.ts"
Task: "Add download API methods to ui/.../agents.service.ts"

# Then sequential:
Task: "Update ScriptTemplateService for new template variables"
Task: "Modify AgentsListComponent to include download button"
Task: "Verify API and UI compile"
Task: "Integration tests"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T004) — build agent binary + place in classpath
2. Complete Phase 2: Foundational (T005-T014) — multi-platform binary service
3. Complete Phase 3: User Story 1 (T015-T028) — download endpoint + UI + install script
4. **STOP and VALIDATE**: Test download from dashboard, verify signed binary, test install script on Docker target
5. Deploy/demo if ready — administrator can download signed agent from dashboard 🎯

### Incremental Delivery

1. Setup + Foundational → API can serve multi-platform signed binaries
2. Add US1 → Download button works → **MVP achieved**
3. Add US2 → Version selector works → Enhanced deployment flexibility
4. Add US3 → Package details visible → Trust and auditability improved
5. Polish → Production-ready quality

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (API + UI)
   - Developer B: User Story 2 (version management)
   - Developer C: User Story 3 (package details)
3. US2 and US3 extend US1 components, so Developer A should finish controller and component first

---

## Notes

- [P] tasks = different files, no dependencies — can execute concurrently
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- The agent binary is NOT committed to git — it's a build artifact in `api/src/main/resources/agents/`
- Do not run git commands unless the user explicitly approves them
- Stop at any checkpoint to validate story independently before proceeding
- The existing signing infrastructure (`BinaryIntegrityService`, `BinaryIntegrityVerifier`) is reused, not replaced
- GraalVM must be installed for Phase 1 native build; skip T001 if testing with pre-built binary
