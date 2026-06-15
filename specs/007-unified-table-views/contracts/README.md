# UI Component Contracts: Unified Table Views

**Created**: 2026-06-15  
**Feature**: [spec.md](../spec.md)

## 1. Table View Component Contract

Every table view component (Targets, Agents, AgentsList, TemplatesList, Vulnerabilities, AlertsList) must conform to this contract after unification.

### Template Structure

```html
<div class="{entity}-container">
  <!-- HEADER: title left, action button right -->
  <div class="flex justify-content-between align-items-center mb-4">
    <h1 class="text-3xl font-bold m-0" i18n>{Entity Name}</h1>
    <!-- Search bar (4 views only, not Alerts) -->
    <span class="p-input-icon-left" *ngIf="showSearch">
      <i class="pi pi-search"></i>
      <input pInputText type="text"
             [placeholder]="searchPlaceholder"
             i18n-placeholder
             (input)="onSearchInput($event)" />
    </span>
    <p-button (onClick)="openCreate()" icon="pi pi-plus"
              label="{Create Label}" styleClass="p-button-success"
              i18n-label></p-button>
  </div>

  <!-- TABLE -->
  <p-table
    [value]="data()"
    [tableStyle]="{ 'min-width': '50rem' }"
    styleClass="{entity}-table"
    [lazy]="true"
    (onLazyLoad)="onLazyLoad($event)"
    [rows]="rows()"
    [totalRecords]="totalRecords()"
    [paginator]="true"
    [alwaysShowPaginator]="true"
    [rowsPerPageOptions]="[10, 25, 50]"
    [loading]="loading()"
    [showCurrentPageReport]="true"
    i18n-currentPageReportTemplate
    currentPageReportTemplate="Showing {first} to {last} of {totalRecords} entries">

    <!-- Colgroup: fixed-width actions -->
    <ng-template pTemplate="header">
      <tr>
        <th i18n>COLUMN 1</th>
        <th i18n>COLUMN 2</th>
        <!-- ... -->
        <th style="width: {actions_width}"></th>
      </tr>
    </ng-template>

    <ng-template pTemplate="body" let-item>
      <tr>
        <td>{{ item.field || '—' }}</td>
        <!-- Status column pattern -->
        <td>
          <p-tag [value]="getStatusLabel(item.status)"
                 [severity]="getStatusSeverity(item.status)"></p-tag>
        </td>
        <!-- Actions column pattern -->
        <td>
          <div class="flex gap-2 justify-content-end">
            <p-button icon="pi pi-{icon}" (onClick)="action(item)"
                      [rounded]="true" [text]="true"
                      severity="{severity}"
                      pTooltip="{tooltip}" i18n-pTooltip
                      tooltipPosition="top"></p-button>
          </div>
        </td>
      </tr>
    </ng-template>

    <!-- Empty state -->
    <ng-template pTemplate="emptymessage">
      <tr>
        <td [attr.colspan]="columnCount">
          {{ query() ? emptySearchMessage : emptyMessage }}
        </td>
      </tr>
    </ng-template>
  </p-table>
</div>
```

### Required Signals (component TS)

| Signal | Type | Purpose |
|--------|------|---------|
| `data` | `signal<T[]>` | Entity records for current page |
| `totalRecords` | `signal<number>` | Total matching records |
| `rows` | `signal<number>` | Rows per page |
| `loading` | `signal<boolean>` | Loading state for p-table `[loading]` |
| `page` | `signal<number>` | Current page index (internal tracking) |
| `query` | `signal<string>` | Current search query |

### Required Methods (component TS)

| Method | Signature | Purpose |
|--------|-----------|---------|
| `onLazyLoad` | `(event: TableLazyLoadEvent) => void` | Handles pagination/sort from p-table |
| `onSearchInput` | `(event: Event) => void` | Feeds input value to debounced search subject |
| `loadData` | `(page?: number) => void` | Fetches data from service with current filters |

---

## 2. Search Bar Contract

Applied to Targets, Agents, TemplatesList, Vulnerabilities components. NOT applied to AlertsList.

### Template

```html
<span class="p-input-icon-left">
  <i class="pi pi-search"></i>
  <input pInputText type="text"
         [placeholder]="'Search...'"
         i18n-placeholder
         (input)="onSearchInput($any($event.target)?.value)" />
</span>
```

### Behavior Contract

| Trigger | Action |
|---------|--------|
| User types in search input | `searchSubject.next(value.trim())` |
| After 300ms debounce + distinct value | Set `query` signal, reset `page` to 0, call `loadData(0)` |
| User clears search input (empty value) | Set `query` to `''`, reload all data at page 0 |
| Component destroyed | `searchSubject` cleaned up via `takeUntilDestroyed(destroyRef)` |

---

## 3. Service Method Contract

Each service's list method must conform to:

```typescript
list(query: string, page: number, size: number): Observable<PaginatedResponse<T>>
```

Where:
- `query` — search term (empty string = no filter)
- `page` — zero-based page index (sent as query param `page`)
- `size` — rows per page (sent as query param `size`)
- Returns `Observable<{ content: T[]; totalElements: number; totalPages: number; size: number; number: number }>`

### Per-Service Conformance

| Service | Current Signature | Change Required |
|---------|-------------------|-----------------|
| `TargetsService.getTargets(page, size)` | Needs `query` param | Add `query` param, rename to `list` |
| `AgentsService.list(page, size)` | Needs `query` param | Add `query` param |
| `TemplatesService.list(query, page, size)` | Already conforms | None |
| `VulnerabilitiesService.list(page, size, search?, severity?)` | Rename `search` to `query`, reorder | Change param name to `query`, reorder to `(query, page, size, severity?)` |
| `AlertsService.list(page, size)` | No search needed | None (unify signature to `(query, page, size)` with query unused) |

---

## 4. SCSS Contract

Each view's SCSS file must apply the same styling rules. Reference implementation from `targets.component.scss`:

```scss
:host ::ng-deep .{entity}-table {
  .p-datatable-header {
    background-color: var(--surface-card);
    border-radius: 12px 12px 0 0;
    padding: 1.25rem;
    border: 1px solid var(--surface-border);
    border-bottom: none;
  }

  .p-datatable-thead > tr > th {
    background-color: var(--surface-card);
    color: var(--text-color-secondary);
    font-weight: 600;
    font-size: 0.875rem;
    padding: 1rem 1.25rem;
    border-color: var(--surface-border);
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .p-datatable-tbody > tr > td {
    padding: 0.75rem 1.25rem;
    border-color: var(--surface-border);
  }

  .p-datatable-tbody > tr {
    background-color: var(--surface-card);
  }

  .p-paginator {
    background-color: var(--surface-card);
    border-radius: 0 0 12px 12px;
    padding: 0.75rem 1.25rem;
    border: 1px solid var(--surface-border);
    border-top: none;
  }
}
```

---

## 5. i18n Contract

### Template Directives Required

| Directive | Usage |
|-----------|-------|
| `i18n` | Standalone text: `<th i18n>NAME</th>`, `<h1 i18n>Targets</h1>` |
| `i18n-label` | Button labels: `label="Create" i18n-label` |
| `i18n-header` | PrimeNG component headers: `<p-dropdown header="Severity" i18n-header>` |
| `i18n-placeholder` | Input placeholders: `placeholder="Search..." i18n-placeholder` |
| `i18n-pTooltip` | Tooltip text: `pTooltip="Edit" i18n-pTooltip` |
| `i18n-currentPageReportTemplate` | Paginator report template text |

### TypeScript $localize Required For

- Status label functions (e.g., `getStatusLabel`)
- Confirmation dialog title and message
- Toast notification messages
- Empty state messages (when computed based on context)
