# Data Model: Expand Remediation Strategies Knowledge Base

**Feature**: 019-expand-remediation-strategies  
**Date**: 2026-07-12

## Existing Entities (Unchanged or Extended)

### RemediationStrategy (MongoDB Document)

Collection: `remediation_strategies`

Existing schema — no changes to the document structure. The feature expands the _data_ within this collection, not the schema.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | ObjectId | auto | MongoDB document ID |
| `cveId` | String | yes | CVE identifier (e.g., "CVE-2023-38408") |
| `operatingSystem` | String | yes | OS identifier (e.g., "ubuntu-22.04") |
| `packageName` | String | yes | Affected package (e.g., "openssh-server") |
| `remediationType` | Enum | yes | SERVICE_UPDATE, REBOOT_REQUIRED, KERNEL_UPDATE, CONTAINER_DETECTED, UNKNOWN |
| `action` | Enum | yes | APT_UPGRADE, APT_INSTALL, CONFIG_UPDATE, SYSTEMCTL_RESTART, MANUAL |
| `targetVersion` | String | yes | Fixed version string (e.g., "1:9.3p1-1ubuntu3.2") |
| `preCheckCommands` | List\<String\> | yes | Commands to verify vulnerability before fix |
| `fixCommands` | List\<String\> | yes | Commands to apply the fix |
| `postCheckCommands` | List\<String\> | yes | Commands to verify fix after application |
| `serviceName` | String | no | Associated service (e.g., "ssh", "nginx") or null |
| `requiresReboot` | Boolean | yes | Whether system reboot is required after fix |
| `notes` | String | yes | Human-readable description of vulnerability and fix |

**Unique Index**: Compound on `(cveId, operatingSystem)` — unchanged.

**Validation Rules** (enforced at load time):
- `cveId` must match pattern `CVE-\d{4}-\d{4,}` (new validation)
- `operatingSystem` must be non-empty
- `packageName` must be non-empty
- `remediationType` must be a valid enum value
- `action` must be a valid enum value
- `targetVersion` must be non-empty unless `action` is MANUAL
- `fixCommands` may be empty only if `action` is MANUAL
- `notes` must be non-empty

## New Entities

### RemediationStrategyResponse (DTO)

API response object for the strategy catalog endpoint. Follows existing DTO patterns in `com.spulido.tfg.domain.remediation.model.dto`.

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | MongoDB document ID |
| `cveId` | String | CVE identifier |
| `operatingSystem` | String | Target OS |
| `packageName` | String | Affected package |
| `remediationType` | String | Enum value as string for API |
| `action` | String | Enum value as string for API |
| `targetVersion` | String | Fixed version |
| `preCheckCommands` | List\<String\> | Pre-check commands |
| `fixCommands` | List\<String\> | Fix commands |
| `postCheckCommands` | List\<String\> | Post-check commands |
| `serviceName` | String or null | Service name |
| `requiresReboot` | Boolean | Reboot required flag |
| `notes` | String | Description |

### StrategyResponse (UI Model)

TypeScript interface in `ui/src/app/pages/remediations/data-access/remediations.model.ts`.

```typescript
export interface RemediationStrategy {
  readonly id: string;
  readonly cveId: string;
  readonly operatingSystem: string;
  readonly packageName: string;
  readonly remediationType: RemediationType;
  readonly action: RemediationAction;
  readonly targetVersion: string;
  readonly preCheckCommands: readonly string[];
  readonly fixCommands: readonly string[];
  readonly postCheckCommands: readonly string[];
  readonly serviceName: string | null;
  readonly requiresReboot: boolean;
  readonly notes: string;
}

export type RemediationAction =
  | 'APT_UPGRADE'
  | 'APT_INSTALL'
  | 'CONFIG_UPDATE'
  | 'SYSTEMCTL_RESTART'
  | 'MANUAL';

export interface StrategyListResponse {
  readonly content: readonly RemediationStrategy[];
  readonly totalElements: number;
}
```

## Entity Relationships

```
RemediationStrategy (shared knowledge base, no org/project scope)
    │
    │ resolved by cveId + operatingSystem
    ▼
RemediationRecord (per-instance execution record, scoped to target/agent)
    │
    │ references strategy via cveId + target OS
    ▼
Target (lab container or cloud instance)
```

The `RemediationStrategy` is a lookup table — agents resolve a strategy by matching `cveId` and `operatingSystem` during vulnerability detection. Each strategy can produce multiple `RemediationRecord` instances (one per affected target). There is no foreign key relationship in MongoDB — the link is implicit via the `cveId` and `operatingSystem` fields.

## State Transitions

No state transitions apply to `RemediationStrategy` — it is a static reference document. State transitions apply to `RemediationRecord` (unchanged):

```
PENDING → IN_PROGRESS → SUCCESS
                      → FAILED
                      → PENDING_REBOOT (if requiresReboot)
PENDING → SKIPPED (if container detected)
```
