# Data Model: Unified Table Views

**Created**: 2026-06-15  
**Feature**: [spec.md](./spec.md)

## Overview

This feature does not introduce new persistent entities. It standardizes the **UI state model** shared across all five table views and defines the expected shape of extended API responses.

---

## UI State Model (Per-View)

Each view component manages the following reactive state via Angular `signal()`:

| Field | Type | Description | Initial |
|-------|------|-------------|---------|
| `data` | `signal<T[]>` | Current page of entity records | `[]` |
| `totalRecords` | `signal<number>` | Total matching records (for paginator) | `0` |
| `rows` | `signal<number>` | Current rows-per-page setting | `10` |
| `loading` | `signal<boolean>` | Whether a data fetch is in progress | `false` |
| `page` | `signal<number>` | Current zero-based page index | `0` |
| `query` | `signal<string>` | Current search query (empty = no filter) | `''` |

Where `T` is the entity type for the view: `TargetInfo`, `AgentInfo`, `PlanTemplate`, `VulnerabilityRecord`, or `AlertConfiguration`.

**State transitions**:

```
IDLE → loading=true → FETCHING → loading=false, data=results → IDLE
                                → loading=false (error toast) → IDLE (retain last data)
```

---

## Search Filter State

| Field | Type | Description |
|-------|------|-------------|
| `searchSubject` | `Subject<string>` | RxJS subject for debounced search input |
| `query` | `signal<string>` | Resolved search term sent to API |

The `searchSubject` debounces at 300ms and updates `query` signal. When `query` changes, `page` resets to 0 and data is reloaded.

---

## Extended API Request Parameters

All list endpoints accept these query parameters:

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `page` | `number` | Yes | Zero-based page index |
| `size` | `number` | Yes | Records per page |
| `query` | `string` | No | Search term; omitted or empty = no filter |

### Endpoints requiring changes

| Endpoint | Current params | New param |
|----------|---------------|-----------|
| `GET /api/targets` | `page`, `size` | Add `query` (optional) |
| `GET /api/agent` | `page`, `size` | Add `query` (optional) |
| `GET /api/vulnerabilities` | `page`, `size`, `search`, `severity` | Rename `search` → `query` (or alias) |

### Endpoints already compliant

| Endpoint | Current params |
|----------|---------------|
| `GET /api/templates` | `query`, `page`, `size` — already correct |
| `GET /api/alerts` | `page`, `size` — no search needed |

---

## API Response Shape

All list endpoints return the same paginated structure:

```typescript
interface PaginatedResponse<T> {
  content: T[];           // Records for current page
  totalElements: number;  // Total matching records (for paginator)
  totalPages: number;     // Total pages
  size: number;           // Current page size
  number: number;         // Current page number
}
```

### Per-entity response models

| View | Entity model file | Response type |
|------|-------------------|---------------|
| Targets | `targets.model.ts` | `TargetsList` (named) |
| Agents | `agents.model.ts` | `AgentsList` (named) |
| Templates | `templates.model.ts` | Inline `{ content: PlanTemplate[]; totalElements: number }` |
| Vulnerabilities | `vulnerabilities.model.ts` | `VulnerabilityListResponse` (named) |
| Alerts | `alerts.model.ts` | Inline `{ content: AlertConfiguration[]; totalElements: number }` |

No changes required to response shapes — search filtering only reduces `totalElements` and filters `content`, same pagination envelope.

---

## i18n Message Additions

New translation keys needed (registered in both `messages.json` and `messages.es.json`):

| Concept | English (en-US) | Spanish (es) |
|---------|-----------------|--------------|
| Search placeholder | `Search...` | `Buscar...` |
| Empty state (no data) | `No {entity} found` | `No se encontraron {entity}` |
| Empty state (search) | `No {entity} match your search` | `Ningún {entity} coincide con la búsqueda` |
| Loading (aria) | `Loading data` | `Cargando datos` |
| Paginator report | `Showing {first} to {last} of {totalRecords} entries` | `Mostrando {first} a {last} de {totalRecords} entradas` |
| Delete confirm title | `Delete {entity}` | `Eliminar {entity}` |
| Delete confirm message | `Do you want to delete this {entity}?` | `¿Desea eliminar este {entity}?` |
| Delete success toast | `{Entity} deleted successfully` | `{Entity} eliminado correctamente` |

Existing i18n keys for column headers, status labels, button labels, and tooltips are already registered and require no additions (they already follow the correct i18n pattern).
