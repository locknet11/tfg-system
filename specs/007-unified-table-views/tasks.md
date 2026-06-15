# Tasks: Unified Table Views

**Input**: Design documents from `/specs/007-unified-table-views/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: No test tasks (not explicitly requested in spec). Verification via TypeScript compilation, Prettier check, and manual visual walkthrough per quickstart.md.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

All paths relative to repository root. `ui/` for Angular frontend, `api/` for Spring Boot backend.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify environment readiness and review applicable guidance

- [x] T001 Review AGENTS.md and `.agents/skills/angular-component/SKILL.md` for Angular 17+ component conventions
- [x] T002 Verify `ui/` dependencies installed: `cd ui && npm ci`
- [x] T003 Verify `api/` builds successfully: `cd api && ./mvnw clean compile`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Backend search param support and i18n message additions that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Backend — Search Query Param Support

- [x] T004 [P] Add optional `query` request param to `GET /api/targets` endpoint in `api/src/main/java/.../TargetController.java`; search across `systemName` and `description` fields
- [x] T005 [P] Add optional `query` request param to `GET /api/agent` endpoint in `api/src/main/java/.../AgentController.java`; search across `name` field
- [x] T006 [P] Rename `search` request param to `query` (or alias both) in `GET /api/vulnerabilities` endpoint in `api/src/main/java/.../VulnerabilityController.java`
- [x] T007 Verify backend builds after changes: `cd api && ./mvnw clean package`

### i18n — Message File Additions

- [ ] T008 Add new i18n entries to `ui/src/i18n/messages.json` (English): search placeholder, empty state messages, paginator report, delete confirmation text per data-model.md i18n table
- [ ] T009 [P] Add new i18n entries to `ui/src/i18n/messages.es.json` (Spanish): corresponding translations per data-model.md i18n table

**Checkpoint**: Foundation ready — backend search APIs available, i18n keys registered. User story implementation can now begin.

---

## Phase 3: User Story 1 - Search Across Data Tables (Priority: P1) 🎯 MVP

**Goal**: Add a debounced search box to Targets, Agents, Templates, and Vulnerabilities views that triggers server-side filtering. Exclude Alerts from search.

**Independent Test**: Navigate to any of the four search-enabled views, type a search term, and verify the table filters results after 300ms debounce. Clear search restores all data. Alerts view has no search box.

### Targets — Add Search

- [x] T010 [P] [US1] Add `query` param to `TargetsService.getTargets()` in `ui/src/app/pages/targets/data-access/targets.service.ts`; rename method to `list(query, page, size)` per service contract
- [x] T011 [P] [US1] Add search state (`query` signal, `searchSubject`, debounce pipeline), `DestroyRef` + `takeUntilDestroyed()`, and `loading` signal to `TargetsComponent` in `ui/src/app/pages/targets/feature/targets.component.ts`; replace `console.error` with toast on error
- [x] T012 [US1] Add search box UI, loading indicator (`[loading]="loading()"`), and empty state message (`<ng-template pTemplate="emptymessage">`) to `ui/src/app/pages/targets/feature/targets.component.html`

### Agents — Add Search

- [x] T013 [P] [US1] Add `query` param to `AgentsService.list()` in `ui/src/app/pages/agents/data-access/agents.service.ts`
- [x] T014 [P] [US1] Add search state (`query` signal, `searchSubject`, debounce pipeline), `DestroyRef` + `takeUntilDestroyed()`, and `loading` signal to `AgentsListComponent` in `ui/src/app/pages/agents/feature/agents-list/agents-list.component.ts`; convert constructor DI to `inject()` pattern
- [x] T015 [US1] Add search box UI, loading indicator (`[loading]="loading()"`), and empty state message to `ui/src/app/pages/agents/feature/agents-list/agents-list.component.html`

### Templates — Unify Search Pattern

- [x] T016 [US1] Verify `TemplatesService.list()` already sends `query` param; add `DestroyRef` + `takeUntilDestroyed()` to `TemplatesListComponent` in `ui/src/app/pages/templates/feature/templates-list/templates-list.component.ts`
- [x] T017 [US1] Refactor `TemplatesListComponent` from manual paginator to lazy loading with built-in paginator per FR-013; add `loading` signal and empty state message in `ui/src/app/pages/templates/feature/templates-list/templates-list.component.html`

### Vulnerabilities — Unify Search Pattern

- [x] T018 [P] [US1] Rename `search` param to `query` and reorder params to `(query, page, size, severity?)` in `VulnerabilitiesService.list()` in `ui/src/app/pages/vulnerabilities/data-access/vulnerabilities.service.ts`; update debounce to use `searchSubject` pattern matching other views
- [x] T019 [P] [US1] Add `DestroyRef` + `takeUntilDestroyed()`, unify search debounce to `searchSubject` + 300ms + `distinctUntilChanged` pattern in `VulnerabilitiesComponent` at `ui/src/app/pages/vulnerabilities/feature/vulnerabilities.component.ts`; add `loading` signal
- [x] T020 [US1] Update search UI to match unified pattern, add loading indicator and empty state message in `ui/src/app/pages/vulnerabilities/feature/vulnerabilities.component.html`

**Checkpoint**: All four search-enabled views allow text search with 300ms debounce, display loading spinner during fetch, and show empty state when no results match. Alerts view has no search box.

---

## Phase 4: User Story 2 - Consistent Visual Experience (Priority: P2)

**Goal**: Unify the visual design of all five table views to match the Targets view: header layout, table styling, paginator configuration, action buttons as icon-only with tooltips, status tags with severity, and em-dash for null values.

**Independent Test**: Navigate between all five views and verify identical header layout (title left, button right), table styling (surface-card, 12px radius), paginator ([10, 25, 50], always visible, "Showing X to Y of Z entries"), loading spinners, empty states, status tags, and icon-only action buttons.

### Targets — Reference Model (Already Compliant, Verify + Fill Gaps)

- [x] T021 [US2] Add `[rowsPerPageOptions]="[10, 25, 50]"` to p-table in `ui/src/app/pages/targets/feature/targets.component.html`; ensure `[alwaysShowPaginator]="true"` and `[showCurrentPageReport]="true"` are present
- [x] T022 [US2] Verify null values render as "—" in template bindings (`{{ target.description \|\| '—' }}` pattern); verify action buttons use icon-only pattern (`[rounded]="true"`, `[text]="true"`, `pTooltip`, `i18n-pTooltip`)

### Agents — Unify Layout & Styling

- [x] T023 [P] [US2] Unify page header layout (h1 title left, action button right) in `ui/src/app/pages/agents/feature/agents-list/agents-list.component.html`
- [x] T024 [P] [US2] Standardize p-table config: add `[rowsPerPageOptions]="[10, 25, 50]"`, `[showCurrentPageReport]="true"`, `i18n-currentPageReportTemplate`, and `currentPageReportTemplate="Showing {first} to {last} of {totalRecords} entries"` to `ui/src/app/pages/agents/feature/agents-list/agents-list.component.html`
- [x] T025 [US2] Copy unified SCSS rules from Targets to `ui/src/app/pages/agents/feature/agents-list/agents-list.component.scss` per SCSS contract (already matching)
- [x] T026 [US2] Add null value handling ("—") for nullable columns in `ui/src/app/pages/agents/feature/agents-list/agents-list.component.html`

### Templates — Unify Layout & Styling

- [x] T027 [P] [US2] Unify page header layout in `ui/src/app/pages/templates/feature/templates-list/templates-list.component.html`
- [x] T028 [P] [US2] Standardize p-table config: `[rowsPerPageOptions]="[10, 25, 50]"`, `[showCurrentPageReport]="true"`, `i18n-currentPageReportTemplate` with unified template string in `ui/src/app/pages/templates/feature/templates-list/templates-list.component.html`
- [x] T029 [US2] Copy unified SCSS rules from Targets to `ui/src/app/pages/templates/feature/templates-list/templates-list.component.scss` per SCSS contract (already matching)
- [x] T030 [US2] Add null value handling ("—") for nullable columns; verify action buttons are icon-only pattern

### Vulnerabilities — Unify Layout & Styling

- [x] T031 [P] [US2] Unify page header layout (h1 title left, search + action button right) in `ui/src/app/pages/vulnerabilities/feature/vulnerabilities.component.html`
- [x] T032 [P] [US2] Replace `styleClass="p-datatable-sm"` with `styleClass="vulnerabilities-table"`; standardize p-table config: `[rowsPerPageOptions]="[10, 25, 50]"`, `[showCurrentPageReport]="true"`, `i18n-currentPageReportTemplate` with unified template string in `ui/src/app/pages/vulnerabilities/feature/vulnerabilities.component.html`
- [x] T033 [US2] Copy unified SCSS rules from Targets to `ui/src/app/pages/vulnerabilities/feature/vulnerabilities.component.scss` per SCSS contract
- [x] T034 [US2] Add null value handling ("—") for nullable columns; verify severity dropdown uses `i18n-header` and `$localize`

### Alerts — Unify Layout & Styling (No Search)

- [x] T035 [P] [US2] Unify page header layout in `ui/src/app/pages/alerts/feature/alerts-list/alerts-list.component.html` (search bar NOT added per FR-002)
- [x] T036 [P] [US2] Standardize p-table config: add `[rowsPerPageOptions]="[10, 25, 50]"`, `[showCurrentPageReport]="true"`, `i18n-currentPageReportTemplate` with unified template string to `ui/src/app/pages/alerts/feature/alerts-list/alerts-list.component.html`; add `[loading]="loading()"` and empty state message
- [x] T037 [US2] Copy unified SCSS rules from Targets to `ui/src/app/pages/alerts/feature/alerts-list/alerts-list.component.scss` per SCSS contract (already matching)
- [x] T038 [US2] Add `DestroyRef` + `takeUntilDestroyed()`, `loading` signal, and empty state message logic to `AlertsListComponent` in `ui/src/app/pages/alerts/feature/alerts-list/alerts-list.component.ts`; add null value handling ("—") for nullable columns in template

**Checkpoint**: All five views are visually identical. Tables, paginators, headers, action buttons, status tags, loading states, and empty states are consistent across views.

---

## Phase 5: User Story 3 - Fully Internationalized Interface (Priority: P3)

**Goal**: Ensure 100% i18n coverage: every user-visible string across all five views uses Angular i18n directives or `$localize`. Zero hardcoded English strings remain.

**Independent Test**: Switch locale between English and Spanish; verify every text element (title, headers, buttons, placeholders, tooltips, status labels, paginator, empty messages, confirm dialogs, toasts) renders in the selected language.

### i18n Audit — Targets

- [x] T039 [P] [US3] Audit `ui/src/app/pages/targets/feature/targets.component.html` — verify all `<th>`, button labels, tooltips, and paginator text have correct i18n directive; verify `targets.component.ts` uses `$localize` for status labels, confirm dialog text, and toast messages
- [x] T040 [P] [US3] Add missing i18n: search input placeholder (`i18n-placeholder`), empty state messages in template

### i18n Audit — Agents

- [x] T041 [P] [US3] Audit `ui/src/app/pages/agents/feature/agents-list/agents-list.component.html` — verify all `<th>`, button labels, tooltips, paginator text, search placeholder, empty state message have correct i18n directive
- [x] T042 [P] [US3] Verify `agents-list.component.ts` uses `$localize` for status labels, confirm dialog text, and toast messages; audit `agents.model.ts` status helper functions

### i18n Audit — Templates

- [x] T043 [P] [US3] Audit `ui/src/app/pages/templates/feature/templates-list/templates-list.component.html` — verify all `<th>`, button labels, tooltips, paginator text, search placeholder, empty state message have correct i18n directive
- [x] T044 [P] [US3] Verify `templates-list.component.ts` uses `$localize` for confirm dialog text and toast messages; audit `templates.model.ts` `stepActionLabel()` for hardcoded strings

### i18n Audit — Vulnerabilities

- [x] T045 [P] [US3] Audit `ui/src/app/pages/vulnerabilities/feature/vulnerabilities.component.html` — verify all `<th>`, button labels, dropdown header, tooltips, paginator text, search placeholder, empty state message have correct i18n directive
- [x] T046 [P] [US3] Verify `vulnerabilities.component.ts` uses `$localize` for severity options, status labels, toast messages, and empty state text; verify `vulnerability-detail.component` i18n coverage

### i18n Audit — Alerts

- [x] T047 [P] [US3] Audit `ui/src/app/pages/alerts/feature/alerts-list/alerts-list.component.html` — verify all `<th>`, button labels, tooltips, paginator text, empty state message have correct i18n directive
- [x] T048 [P] [US3] Verify `alerts-list.component.ts` uses `$localize` for condition labels, status labels, confirm dialog text, and toast messages; audit `alerts.model.ts` `whenConditionLabel()` for hardcoded strings

### i18n — Modals & Child Components

- [x] T049 [P] [US3] Audit all modal child components across all 5 views (create-target-modal, edit-target-modal, agent-setup-modal, assign-plan-modal, create-template-modal, edit-template-modal, create-alert-modal, edit-alert-modal) for i18n coverage — verify form labels, placeholders, button text, validation messages, and dialog titles use i18n directives

**Checkpoint**: 100% i18n coverage across all five views. Switch locale and every text element renders in the selected language. Zero hardcoded English strings.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: TypeScript/formatting compliance, Angular 17+ best practices, and final verification

- [x] T050 [P] Convert `TargetsComponent` constructor DI to `inject()` pattern in `ui/src/app/pages/targets/feature/targets.component.ts`
- [x] T051 [P] Replace `console.error(err)` with ToastService error notification in `TargetsComponent.deleteTarget()` in `ui/src/app/pages/targets/feature/targets.component.ts` (line 132)
- [x] T052 [P] Replace `any` types in modal event handlers (e.g., `onTargetCreated(target?: any)`) with proper types across all 5 view components (`onLazyLoad` uses `TableLazyLoadEvent`)
- [x] T053 Run TypeScript compilation check: `cd ui && npx tsc --noEmit` — ensure zero errors
- [x] T054 Run Prettier format check: `cd ui && npx prettier --check .` — ensure all files comply
- [x] T055 Run Prettier format fix if needed: `cd ui && npx prettier --write .`
- [ ] T056 Verify i18n extraction works: `cd ui && npx ng extract-i18n --format json --output-path src/i18n` — verify no broken i18n references
- [ ] T057 Run quickstart.md manual verification checklist: all 12 visual checks + i18n checks + memory leak check per `specs/007-unified-table-views/quickstart.md`
- [x] T058 Confirm no `console.log` calls remain in production code paths across all modified files
- [x] T059 Confirm all modified components have `DestroyRef` + `takeUntilDestroyed()` on every subscription (data fetch + search debounce)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational — search APIs and i18n keys ready
- **User Story 2 (Phase 4)**: Depends on US1 (search already added; US2 unifies styling on top)
- **User Story 3 (Phase 5)**: Depends on US2 (all UI elements in place before i18n audit)
- **Polish (Phase 6)**: Depends on all user stories complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Phase 2. No dependency on US2 or US3.
- **User Story 2 (P2)**: Builds on US1 — search UI is already present. US2 unifies remaining visual aspects. Can be tested independently by verifying visual consistency across views.
- **User Story 3 (P3)**: Builds on US1+US2 — all UI elements exist. US3 audits and fixes i18n coverage. Can be tested independently by locale switching.

### Within Each User Story

- Services before component TS
- Component TS before template
- Core view implementation before polish
- Story complete before moving to next priority

### Parallel Opportunities

- **Phase 2**: T004, T005, T006 (backend controllers) can run in parallel — different files
- **Phase 2**: T008 and T009 (i18n files) can run in parallel — different files
- **Phase 3**: T010+T011 (Targets service+TS), T013+T014 (Agents service+TS), T018+T019 (Vulnerabilities service+TS) can run in parallel — different views
- **Phase 4**: T023+T024 (Agents HTML), T027+T028 (Templates HTML), T031+T032 (Vulnerabilities HTML), T035+T036 (Alerts HTML) can all run in parallel — different views
- **Phase 5**: T039–T048 (i18n audits per view) can all run in parallel — different views/files
- **Phase 6**: T050, T051, T052 (code quality fixes) can run in parallel — different patterns

---

## Parallel Example: User Story 1

```bash
# Launch all service changes in parallel:
Task: "Add query param to TargetsService in ui/src/app/pages/targets/data-access/targets.service.ts"
Task: "Add query param to AgentsService in ui/src/app/pages/agents/data-access/agents.service.ts"
Task: "Rename search→query in VulnerabilitiesService in ui/src/app/pages/vulnerabilities/data-access/vulnerabilities.service.ts"

# Then launch all component TS changes in parallel:
Task: "Add search state + destroyRef to TargetsComponent in ui/src/app/pages/targets/feature/targets.component.ts"
Task: "Add search state + destroyRef to AgentsListComponent in ui/src/app/pages/agents/feature/agents-list/agents-list.component.ts"
Task: "Add destroyRef to TemplatesListComponent in ui/src/app/pages/templates/feature/templates-list/templates-list.component.ts"
Task: "Unify debounce + destroyRef in VulnerabilitiesComponent in ui/src/app/pages/vulnerabilities/feature/vulnerabilities.component.ts"

# Finally, template changes (order matters within each view — TS before HTML):
Task: "Add search box + loading + emptymessage to targets.component.html"
Task: "Add search box + loading + emptymessage to agents-list.component.html"
Task: "Refactor to lazy load + loading + emptymessage in templates-list.component.html"
Task: "Update search UI + loading + emptymessage in vulnerabilities.component.html"
```

## Parallel Example: User Story 2

```bash
# Launch all view-level layout changes in parallel (different views):
Task: "Unify Agents layout in agents-list.component.html"
Task: "Unify Templates layout in templates-list.component.html"
Task: "Unify Vulnerabilities layout in vulnerabilities.component.html"
Task: "Unify Alerts layout in alerts-list.component.html"

# SCSS changes can also run in parallel:
Task: "Copy unified SCSS to agents-list.component.scss"
Task: "Copy unified SCSS to templates-list.component.scss"
Task: "Copy unified SCSS to vulnerabilities.component.scss"
Task: "Copy unified SCSS to alerts-list.component.scss"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (backend + i18n)
3. Complete Phase 3: User Story 1 (Search)
4. **STOP and VALIDATE**: Test search on all 4 views independently — verify debounce, filtering, clear, empty state
5. Deploy/demo search MVP

### Incremental Delivery

1. Complete Setup + Foundational → Backend search APIs + i18n keys ready
2. Add User Story 1 (Search) → Test independently → Search MVP! 🎯
3. Add User Story 2 (Visual Consistency) → Test independently → Unified UX
4. Add User Story 3 (i18n) → Test independently → Full i18n coverage
5. Each story adds value without breaking previous stories

### Sequential Strategy (Single Developer)

Execute phases in order: Setup → Foundational → US1 → US2 → US3 → Polish. Within each phase, parallel tasks can still be batched for efficiency.

---

## Notes

- [P] tasks = different files, no dependencies — safe to parallelize
- [Story] label maps task to specific user story (US1, US2, US3) for traceability
- Each user story is independently completable and testable
- Backend tasks (T004–T007) require Spring Boot / Java knowledge
- Frontend tasks (T008–T059) require Angular 17 + PrimeNG knowledge
- All subscriptions MUST use `takeUntilDestroyed()` — verify in Phase 6
- All text MUST be internationalized — audit in Phase 5
- Do not run git commands unless the user explicitly approves them (Constitution V)
- Stop at any checkpoint to validate story independently
