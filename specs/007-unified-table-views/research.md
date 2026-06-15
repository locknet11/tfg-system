# Research: Unified Table Views

**Created**: 2026-06-15  
**Feature**: [spec.md](./spec.md) | [plan.md](./plan.md)

## Research Topics

### 1. Search Parameter Naming Convention

**Decision**: Use `query` as the parameter name across all services for consistency.

**Rationale**: The Templates service already uses `query`. The Vulnerabilities service uses `search` but this is inconsistent. Renaming Vulnerabilities `search` to `query` aligns all services. The backend APIs for Targets and Agents will be extended to accept a `query` parameter.

**Alternatives considered**:
- Keep `search` for Vulnerabilities, use `query` for new ones → inconsistent, violates unification goal
- Use `search` everywhere → requires renaming Templates service, more churn
- Keep inconsistent naming → defeats the purpose of unification

**Backend impact**: Targets (`/api/targets`) and Agents (`/api/agent`) endpoints need a `query` request parameter added. Vulnerabilities `/api/vulnerabilities` endpoint parameter `search` should be renamed to `query` (or aliased). Templates and Alerts endpoints require no changes (Templates already has `query`; Alerts excluded from search).

---

### 2. Search Debounce Strategy

**Decision**: 300ms debounce using RxJS `debounceTime` + `distinctUntilChanged`, triggered on keystroke (not just Enter).

**Rationale**: 300ms is the industry standard for search-as-you-type (balances responsiveness with API call frequency). Using RxJS operators with `Subject` for the search term keeps the pattern clean and cancellable via `takeUntilDestroyed()`.

**Alternatives considered**:
- `setTimeout` debounce → imperative, harder to cancel, no `distinctUntilChanged`
- Search on Enter only → slower UX, inconsistent with modern web patterns
- 500ms or 1000ms debounce → too slow for responsive UX
- Client-side filtering → doesn't scale with large datasets, inconsistent with lazy-loading

**Implementation pattern**:
```typescript
private searchSubject = new Subject<string>();
private destroyRef = inject(DestroyRef);

constructor() {
  this.searchSubject.pipe(
    debounceTime(300),
    distinctUntilChanged(),
    takeUntilDestroyed(this.destroyRef)
  ).subscribe(query => {
    this.currentQuery = query;
    this.loadData(0); // reset to first page
  });
}

onSearchInput(value: string) {
  this.searchSubject.next(value.trim());
}
```

---

### 3. Backend API Gaps (Targets and Agents Endpoints)

**Decision**: Backend changes are required in `api/` module to add `query` request parameter support to the Targets and Agents list endpoints.

**Rationale**: The spec requires server-side search for all four views. The Targets service (`GET /api/targets`) and Agents service (`GET /api/agent`) currently accept only `page` and `size`. Without backend changes, search cannot be implemented server-side. Client-side filtering is rejected because it breaks the lazy-loading pagination model (filtering client-side on the current page would miss results on other pages).

**Required backend changes** (in `api/` module):
1. `GET /api/targets` — add optional `query` request parameter; search across `systemName` and `description` fields
2. `GET /api/agent` — add optional `query` request parameter; search across `name` field
3. `GET /api/vulnerabilities` — rename `search` param to `query` (or alias both) for consistency

**If backend changes are deferred**: A fallback option is to implement client-side search for Targets and Agents, but this would only search currently loaded records and produce inconsistent behavior across views. NOT recommended.

---

### 4. Loading State Pattern

**Decision**: Use an Angular `signal<boolean>` for `loading` state, matching the Agents and Templates views pattern.

**Rationale**: The Targets view currently has no loading indicator. The Agents and Templates views already use `loading = signal(false)` pattern with `[loading]="loading()"` on `<p-table>`. This is the correct Angular 17+ approach — signals are preferred over `BehaviorSubject` per user directive ("Always prefer signals over observables").

**Implementation**:
```typescript
loading = signal(false);

loadData(page: number) {
  this.loading.set(true);
  this.service.list(query, page, size).subscribe({
    next: res => {
      this.data.set(res.content);
      this.total.set(res.totalElements);
      this.loading.set(false);
    },
    error: () => {
      this.loading.set(false); // always clear loading on error
    }
  });
}
```

Project-level pattern: `[loading]="loading()"` on `<p-table>` enables the built-in PrimeNG loading overlay.

---

### 5. DestroyRef / takeUntilDestroyed Pattern

**Decision**: All components that subscribe to observables MUST use `inject(DestroyRef)` + `takeUntilDestroyed()` for automatic subscription cleanup.

**Rationale**: The existing codebase has NO subscription cleanup — components subscribe to service observables without unsubscribing, which leaks subscriptions when components are destroyed (e.g., navigation away). The user explicitly requires: "always remember to have ondestroy (you can use takeUntilDestroyed or similar), always remember to unsubscribe when using observables."

The Angular 17 idiomatic approach is `takeUntilDestroyed()` which requires no manual `ngOnDestroy` or `Subject` boilerplate.

**Pattern**:
```typescript
import { DestroyRef, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({ ... })
export class MyComponent {
  private destroyRef = inject(DestroyRef);

  loadData() {
    this.service.list(...)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ ... });
  }
}
```

**Scope**: Apply to all 5 view components and any new subscriptions introduced by search debounce Subjects.

---

### 6. Rows-Per-Page and Paginator Standardization

**Decision**: Standardize all 5 views to `[rowsPerPageOptions]="[10, 25, 50]"`, `[alwaysShowPaginator]="true"`, `[showCurrentPageReport]="true"`, and use the Targets view's `currentPageReportTemplate`.

**Rationale**: Targets uses a fixed `rows=10` with no `rowsPerPageOptions`. Agents uses `[5,10,20]`. Templates uses `[5,10,20]`. Vulnerabilities uses `[10,25,50]`. Alerts uses fixed `rows=10`. The `[10, 25, 50]` option is the most commonly expected set in enterprise UIs and matches the Vulnerabilities view (the only one with a proper range). All views should converge on this.

**Target currentPageReportTemplate**:
```
Showing {first} to {last} of {totalRecords} entries
```

This string must be marked with `i18n-currentPageReportTemplate` and registered in both i18n files. Vulnerabilities currently uses a different format (`{first} - {last} / {totalRecords}`) which must be changed.

---

### 7. Empty State Message Pattern

**Decision**: All 5 views add `<ng-template pTemplate="emptymessage">` with an i18n message. Each view gets a context-appropriate message.

**Rationale**: The Targets, Agents, and Alerts views currently have NO empty state handling. Vulnerabilities already has an `emptymessage` template. This is a clear gap — users see a blank table with no indication why.

**Per-view empty messages**:
- Targets: "No targets found" (when search active: "No targets match your search")
- Agents: "No agents found" / "No agents match your search"
- Templates: "No templates found" / "No templates match your search"
- Vulnerabilities: "No vulnerabilities found" / "No vulnerabilities match your search"
- Alerts: "No alerts configured"

Empty state messages vary based on whether a search query is active (FR-001 acceptance scenario 7).

---

### 8. Table Style Class Unification

**Decision**: Apply a single shared `p-datatable-lg` style class (or rename all to use `targets-table` consistently) with identical SCSS rules.

**Rationale**: Each view currently has its own `styleClass`: `targets-table`, `agents-table`, `templates-table`, `alerts-table`, `p-datatable-sm`. These produce visually similar but technically separate styling. The simplest correct change is to keep each table's existing style class name but apply the same SCSS rules (from Targets) to all of them. Alternatively, use a single shared class.

**Chosen approach**: Keep per-table `styleClass` names for now (each view already imports its own SCSS) but copy the exact `surface-card` + `border-radius: 12px` + spacing rules from `targets.component.scss` into the other views' SCSS files. This is the minimal change — no CSS refactoring or shared global styles needed.

The SCSS pattern to replicate from Targets:
```scss
:host ::ng-deep .<view>-table {
  .p-datatable-header { /* ... */ }
  // surface-card background, border-radius 12px, etc.
}
```

---

### 9. Component Decomposition (Avoiding Giant Components)

**Decision**: Extract the search bar into an inline pattern within each view's template rather than a shared component. Keep each component focused on its entity's data flow.

**Rationale**: Per the user's directive ("Do not have giant components, think as an engineer") and Principle III (minimal changes), we avoid creating a shared search component prematurely. The search bar is a simple `<span class="p-input-icon-left">` + `<input pInputText>` pattern that's 5 lines of template. Creating a shared component would introduce:
- A new component file with inputs/outputs
- A new module import chain
- Increased coupling between views

If in the future a 6th or 7th view needs the same pattern, we can extract then. For 4 views, inline is correct.

**Component size boundaries**: Each view component should:
- Have a single responsibility: manage its entity's table data + search
- Not exceed ~200 lines of TypeScript
- Delegate CRUD operations to modal child components (already the case)
- Delegate data fetching to the service layer (already the case)

---

### 10. Angular 17+ Best Practices for This Feature

**Summary of practices to apply**:

| Practice | Current State | Required Change |
|----------|--------------|-----------------|
| **Signal state** | Targets uses signals; Agents uses signals; Templates uses signals; Vulnerabilities uses signals; Alerts uses signals | All good — continue using signals |
| **inject() over constructor DI** | Targets uses constructor; Agents uses constructor; Templates/Vulns/Alerts use inject() | Convert Targets and Agents to `inject()` pattern |
| **DestroyRef + takeUntilDestroyed** | NONE of the 5 components use it | Add to all components with subscriptions |
| **Standalone components** | All 5 already standalone | No change needed |
| **New control flow (@if, @for)** | All use `*ngIf`/`*ngFor` | No change needed (migration out of scope) |
| **No `any` types** | Some modals use `any` (e.g., `onTargetCreated(target?: any)`) | Replace `any` with proper types |
| **No console.log in production** | `targets.component.ts:132` uses `console.error(err)` | Replace with toast notification or proper error handling |
| **Readonly signals** | Not consistently applied | Use `readonly` for signal declarations where appropriate |
| **Computed signals** | Only Alerts has a trivial `computed` | Use where derived state is needed |

---

## Summary of Resolved Unknowns

| Unknown | Resolution |
|---------|-----------|
| Search param name | `query` (consistent with Templates) |
| Debounce timing | 300ms via RxJS `debounceTime` |
| Backend search for Targets/Agents | Required — add `query` param to `/api/targets` and `/api/agent` |
| Loading state | `signal<boolean>` bound to `[loading]` on p-table |
| Subscription cleanup | `DestroyRef` + `takeUntilDestroyed()` on all subscriptions |
| Rows-per-page | `[10, 25, 50]` across all 5 views |
| Paginator report format | `"Showing {first} to {last} of {totalRecords} entries"` |
| Empty state | `pTemplate="emptymessage"` with context-aware i18n message |
| Table style class | Keep per-table names, share identical SCSS rules |
| Shared search component | No — inline pattern, extract later if needed |
| DI pattern | `inject()` everywhere (convert Targets, Agents) |
| Error handling | Toast notification instead of `console.error` |
