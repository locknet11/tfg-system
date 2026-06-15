# Quickstart: Unified Table Views

**Created**: 2026-06-15  
**Feature**: [spec.md](./spec.md)

## Prerequisites

- Node.js 18+ and npm installed
- Angular CLI 17 (`npm install -g @angular/cli@17`)
- Repository cloned and dependencies installed: `cd ui && npm ci`
- Backend (`api/`) running or mocked for API responses

## Verification Steps

### 1. TypeScript Compilation

```bash
cd ui
npx tsc --noEmit
```

Expected: zero errors. No `any` type leaks, all signal accesses through `()`, all i18n directive attributes recognized.

### 2. Prettier Format Check

```bash
cd ui
npx prettier --check .
```

Expected: "All matched files use Prettier code style!" or zero diffs.

### 3. Unit Tests

```bash
cd ui
npx ng test --watch=false --browsers=ChromeHeadless
```

Focus areas:
- Component renders table with lazy loading
- Search debounce triggers after 300ms
- Empty state shown when data array is empty
- Loading indicator visible when `loading` signal is true
- Status tag renders correct severity for each status value

### 4. Manual Visual Verification

Start the dev server:

```bash
cd ui
npm run start
```

Navigate to each view and verify:

| Check | Targets | Agents | Templates | Vulnerabilities | Alerts |
|-------|---------|--------|-----------|----------------|--------|
| Page header layout (title left, button right) | ✓ | ✓ | ✓ | ✓ | ✓ |
| Table uses unified style class | ✓ | ✓ | ✓ | ✓ | ✓ |
| Search box visible (not Alerts) | ✓ | ✓ | ✓ | ✓ | ✗ |
| Search filters results on type (300ms debounce) | ✓ | ✓ | ✓ | ✓ | ✗ |
| Clear search restores all results | ✓ | ✓ | ✓ | ✓ | ✗ |
| Loading spinner during data fetch | ✓ | ✓ | ✓ | ✓ | ✓ |
| Empty state message when no data | ✓ | ✓ | ✓ | ✓ | ✓ |
| Paginator shows [10, 25, 50] options | ✓ | ✓ | ✓ | ✓ | ✓ |
| Paginator always visible | ✓ | ✓ | ✓ | ✓ | ✓ |
| Status columns use p-tag with severity | ✓ | ✓ | N/A | ✓ | ✓ |
| Action buttons are icon-only, rounded, text style | ✓ | ✓ | ✓ | ✓ | ✓ |
| Tooltips on action buttons | ✓ | ✓ | ✓ | ✓ | ✓ |

### 5. i18n Verification

Switch locale to Spanish and repeat the visual checks. Verify:
- All page titles translated
- All table headers translated
- All button labels translated
- All placeholders translated
- All tooltips translated
- All status labels translated
- All paginator text translated
- All empty state messages translated
- All toast messages translated
- All confirmation dialogs translated

### 6. Memory Leak Check

Using Angular DevTools or browser profiler:
- Navigate between views rapidly
- Verify no subscription leaks (no "ghost" API calls from destroyed components)
- Verify search debounce subjects are cleaned up on navigation

## Key Files to Modify

```text
# Targets (reference impl; add search + loading + emptymessage + destroyRef)
ui/src/app/pages/targets/feature/targets.component.ts
ui/src/app/pages/targets/feature/targets.component.html
ui/src/app/pages/targets/feature/targets.component.scss
ui/src/app/pages/targets/data-access/targets.service.ts

# Agents (add search + loading + emptymessage + destroyRef + inject())
ui/src/app/pages/agents/feature/agents-list/agents-list.component.ts
ui/src/app/pages/agents/feature/agents-list/agents-list.component.html
ui/src/app/pages/agents/feature/agents-list/agents-list.component.scss
ui/src/app/pages/agents/data-access/agents.service.ts

# Templates (refactor to lazy load + unify + destroyRef)
ui/src/app/pages/templates/feature/templates-list/templates-list.component.ts
ui/src/app/pages/templates/feature/templates-list/templates-list.component.html
ui/src/app/pages/templates/feature/templates-list/templates-list.component.scss

# Vulnerabilities (unify search pattern + layout + destroyRef)
ui/src/app/pages/vulnerabilities/feature/vulnerabilities.component.ts
ui/src/app/pages/vulnerabilities/feature/vulnerabilities.component.html
ui/src/app/pages/vulnerabilities/feature/vulnerabilities.component.scss
ui/src/app/pages/vulnerabilities/data-access/vulnerabilities.service.ts

# Alerts (unify layout + loading + emptymessage + destroyRef; NO search)
ui/src/app/pages/alerts/feature/alerts-list/alerts-list.component.ts
ui/src/app/pages/alerts/feature/alerts-list/alerts-list.component.html
ui/src/app/pages/alerts/feature/alerts-list/alerts-list.component.scss

# i18n
ui/src/i18n/messages.json
ui/src/i18n/messages.es.json

# Backend (if search params needed)
api/src/main/java/.../TargetController.java
api/src/main/java/.../AgentController.java
api/src/main/java/.../VulnerabilityController.java
```

## Backend Changes (if needed)

If the backend endpoints for Targets and Agents do not support a `query` parameter:

```bash
cd api
./mvnw clean package
```

The Spring Boot controllers need an optional `@RequestParam(required = false) String query` parameter that filters the repository query by name/description fields.
