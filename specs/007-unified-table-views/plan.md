# Implementation Plan: Unified Table Views

**Branch**: `007-unified-table-views` | **Date**: 2026-06-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/007-unified-table-views/spec.md`

## Summary

Unify the visual design, table configuration, pagination, loading states, and empty-state handling across five data views (Targets, Agents, Templates, Vulnerabilities, Alerts) to match the Targets view as the canonical reference. Add a server-side search box to four views (Targets, Agents, Templates, Vulnerabilities — not Alerts). Ensure 100% i18n coverage of all user-facing text using Angular's `i18n` directives and `$localize`. The Templates view must be refactored from manual pagination to built-in lazy loading. All work follows Angular 17+ best practices: signals over observables, `takeUntilDestroyed` for subscription cleanup, standalone components, and component size discipline.

## Technical Context

**Language/Version**: TypeScript 5.x (Angular 17 strict mode)  
**Primary Dependencies**: Angular 17, PrimeNG 17, RxJS 7  
**Storage**: N/A (UI-only feature; no new persistence)  
**Testing**: Angular TestBed (component unit tests), Prettier formatting check, manual visual verification  
**Target Platform**: Web browser (desktop)  
**Project Type**: Web application (Angular standalone-component frontend)  
**Performance Goals**: Search queries complete within the same latency envelope as existing lazy-load pagination (no perceptible degradation)  
**Constraints**: Signals preferred over observables for state; all subscriptions use `takeUntilDestroyed()`; components kept small (no god components); no `any` types; no `console.log` in production paths  
**Scale/Scope**: 5 existing views (~10 component files, ~10 template files, ~5 service files, ~2 model files), 2 i18n message files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- [x] **Repository guidance reviewed**: `AGENTS.md` (Angular + Spring Boot section), `.agents/skills/angular-component/SKILL.md` for component patterns, `.agents/skills/java-springboot/SKILL.md` for potential backend changes. All applicable guidance read.
- [x] **English-only rule satisfied**: All code, identifiers, UI text (human-friendly in English), comments, and documentation written in English. i18n translations registered in `ui/src/i18n/messages.json` (English) and `messages.es.json` (Spanish).
- [x] **Proposed design is the smallest correct change**: No new abstractions, shared components, or base classes introduced. Each view is updated in-place to adopt the Targets pattern. A reusable search box is the only minor abstraction (inline `<input>` with debounce, not a separate component, to avoid premature generalization).
- [x] **Stack rules captured for affected modules**: `ui/` — Angular 17 standalone components, signals for state, `i18n`/`i18n-label`/`i18n-pTooltip`/`i18n-placeholder` directives, `$localize` in TS, Prettier formatting, centralized HTTP error handling via interceptor. `api/` — only affected if search parameters need backend support (see research.md).
- [x] **Verification steps identified**: Prettier `--check`; TypeScript compilation (`npx tsc --noEmit`); Angular TestBed unit tests for components with search and pagination; manual visual walkthrough of all 5 views in both locales.
- [x] **Git actions identified**: None needed during planning. Branch `007-unified-table-views` already exists. Commits require explicit user approval per Constitution V.
- [x] **Unknown or ambiguous requirements resolved**: Search parameter naming (`query` vs `search`), debounce timing (300ms), rows-per-page options ([10, 25, 50]), and paginator config resolved in Phase 0 research. Backend search support for Targets and Agents endpoints identified as a dependency.

## Project Structure

### Documentation (this feature)

```text
specs/007-unified-table-views/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (UI component contracts)
└── tasks.md             # Phase 2 output (NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
ui/src/app/
├── pages/
│   ├── targets/
│   │   ├── data-access/
│   │   │   ├── targets.model.ts          # [MODIFY] Add search param to getTargets
│   │   │   └── targets.service.ts        # [MODIFY] Add query parameter support
│   │   └── feature/
│   │       ├── targets.component.ts      # [MODIFY] Add search, destroyRef, loading
│   │       ├── targets.component.html    # [MODIFY] Add search box, loading, emptymessage
│   │       └── targets.component.scss    # [MODIFY] Unify table style class
│   ├── agents/
│   │   ├── data-access/
│   │   │   ├── agents.model.ts           # [MODIFY] Add search response typing
│   │   │   └── agents.service.ts         # [MODIFY] Add query parameter support
│   │   └── feature/
│   │       ├── agents.component.html     # [REVIEW] Shell wrapper stays
│   │       └── agents-list/
│   │           ├── agents-list.component.ts    # [MODIFY] Add search, destroyRef, loading
│   │           ├── agents-list.component.html  # [MODIFY] Add search, loading, emptymessage, unify
│   │           └── agents-list.component.scss  # [MODIFY] Unify table style class
│   ├── templates/
│   │   ├── data-access/
│   │   │   ├── templates.model.ts        # [REVIEW] Existing query support
│   │   │   └── templates.service.ts      # [REVIEW] Already has query param
│   │   └── feature/
│   │       └── templates-list/
│   │           ├── templates-list.component.ts    # [MODIFY] Refactor to lazy load, destroyRef
│   │           ├── templates-list.component.html  # [MODIFY] Unify, add loading, emptymessage
│   │           └── templates-list.component.scss  # [MODIFY] Unify table style class
│   ├── vulnerabilities/
│   │   ├── data-access/
│   │   │   ├── vulnerabilities.model.ts   # [REVIEW] Existing search support
│   │   │   └── vulnerabilities.service.ts # [REVIEW] Already has search param
│   │   └── feature/
│   │       ├── vulnerabilities.component.ts      # [MODIFY] Unify search pattern, destroyRef
│   │       ├── vulnerabilities.component.html    # [MODIFY] Unify layout, loading, emptymessage
│   │       └── vulnerabilities.component.scss    # [MODIFY] Unify table style class
│   └── alerts/
│       ├── data-access/
│       │   ├── alerts.model.ts            # [REVIEW] No search needed
│       │   └── alerts.service.ts          # [REVIEW] No search param needed
│       └── feature/
│           └── alerts-list/
│               ├── alerts-list.component.ts      # [MODIFY] Unify, destroyRef
│               ├── alerts-list.component.html    # [MODIFY] Unify layout, loading, emptymessage
│               └── alerts-list.component.scss    # [MODIFY] Unify table style class
├── shared/
│   └── interceptors/
│       └── request.interceptor.ts         # [NO CHANGE] Central error handling already OK
└── i18n/
    ├── messages.json                      # [MODIFY] Add new i18n entries
    └── messages.es.json                   # [MODIFY] Add Spanish translations
```

**Structure Decision**: No new directories or files created. All changes are modifications to existing view components, services, models, and i18n files. This follows Principle III (minimal, correct, verifiable changes) — we update what exists rather than building new abstractions.

## Complexity Tracking

> No constitution violations. All principles satisfied. No complexity justification needed.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |
