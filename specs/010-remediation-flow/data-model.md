# Data Model: Autonomous Remediation Flow

**Date**: 2026-06-26  
**Feature**: 010-remediation-flow

---

## Entity Relationship Overview

```
Organization ─┬── Project ─┬── Target ──────── RemediationRecord
              │            ├── Agent ────────┘
              │            ├── ServiceVulnerabilityRecord ─┘
              │            └── AlertConfiguration
              └── RemediationStrategy (KB, org-scoped)
```

---

## 1. RemediationRecord (NEW)

**Collection**: `remediation_records`  
**Module**: `api/`  
**Extends**: `BaseEntity` (id, createdAt, updatedAt)  
**Implements**: `ScopedEntity` (organizationId, projectId)

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | String (auto) | ✅ | From BaseEntity |
| `vulnerabilityRecordId` | String | ✅ | FK → ServiceVulnerabilityRecord.id |
| `cveId` | String | ✅ | CVE identifier (e.g., CVE-2023-38408) |
| `targetId` | String | ✅ | FK → Target.id |
| `agentId` | String | ✅ | FK → Agent.id (executing agent) |
| `planId` | String | ❌ | FK → Plan.id (which plan triggered this) |
| `remediationType` | RemediationType | ✅ | SERVICE_UPDATE, REBOOT_REQUIRED, KERNEL_UPDATE, UNKNOWN |
| `status` | RemediationStatus | ✅ | PENDING, IN_PROGRESS, SUCCESS, FAILED, PENDING_REBOOT, SKIPPED |
| `packageName` | String | ❌ | Package being remediated (e.g., openssh-server) |
| `packageVersionBefore` | String | ❌ | Version before fix |
| `packageVersionAfter` | String | ❌ | Version after fix |
| `actionDescription` | String | ❌ | Human-readable description of action taken |
| `preCheckLogs` | List\<String\> | ❌ | Output from pre-check commands |
| `executionLogs` | List\<String\> | ❌ | Output from fix commands |
| `postCheckLogs` | List\<String\> | ❌ | Output from post-verification commands |
| `startedAt` | Instant | ❌ | When execution began |
| `completedAt` | Instant | ❌ | When execution finished |
| `errorMessage` | String | ❌ | Error details when FAILED |
| `rollbackHint` | String | ❌ | Manual rollback commands |
| `organizationId` | String | ✅ | From ScopedEntity |
| `projectId` | String | ✅ | From ScopedEntity |
| `createdAt` | LocalDateTime | ✅ | From BaseEntity |
| `updatedAt` | LocalDateTime | ✅ | From BaseEntity |

### Indexes

- `compoundIndex`: `{ organizationId: 1, projectId: 1 }` (multi-tenancy queries)
- `indexed`: `cveId` (CVE-based lookups)
- `indexed`: `targetId` (target-based queries)
- `indexed`: `status` (status filtering)
- `indexed`: `agentId` (agent-based queries)

### State Transitions

```
PENDING ──────► IN_PROGRESS ──────► SUCCESS
                  │    │
                  │    └────────────► FAILED
                  │
                  └─────────────────► PENDING_REBOOT
                  
PENDING ──────► SKIPPED  (for KERNEL_UPDATE)
```

---

## 2. RemediationStrategy (NEW — Knowledge Base)

**Collection**: `remediation_strategies`  
**Module**: `api/`  
**Scope**: Global (not org/project scoped — shared knowledge)

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | String (auto) | ✅ | Unique identifier |
| `cveId` | String | ✅ | CVE this strategy addresses |
| `operatingSystem` | String | ✅ | Target OS (e.g., "ubuntu-22.04") |
| `packageName` | String | ✅ | Package to update/configure |
| `remediationType` | RemediationType | ✅ | SERVICE_UPDATE, REBOOT_REQUIRED, KERNEL_UPDATE |
| `action` | RemediationAction | ✅ | APT_UPGRADE, CONFIG_UPDATE, etc. |
| `targetVersion` | String | ❌ | Minimum safe version |
| `preCheckCommands` | List\<String\> | ✅ | Commands to verify current state |
| `fixCommands` | List\<String\> | ✅ | Commands to apply the fix |
| `postCheckCommands` | List\<String\> | ✅ | Commands to verify fix success |
| `serviceName` | String | ❌ | Service to restart after fix |
| `requiresReboot` | boolean | ✅ | Whether reboot is needed |
| `notes` | String | ❌ | Additional context |

### Indexes

- `compoundIndex`: `{ cveId: 1, operatingSystem: 1 }` (unique per CVE+OS)
- `indexed`: `packageName` (package-based lookups)

### Initial Data Seeding

The `remediation_strategies` collection will be seeded from a JSON resource file at `api/src/main/resources/remediation/strategies.json`. On application startup, a `RemediationStrategyLoader` component checks if the collection is empty and seeds it.

---

## 3. RemediationType (NEW Enum)

```java
public enum RemediationType {
    SERVICE_UPDATE,    // Type A: Update/reconfigure service, restart it
    REBOOT_REQUIRED,   // Type B: Fix applied but needs reboot
    KERNEL_UPDATE,     // Type C: Kernel update, report only
    UNKNOWN            // Cannot determine type
}
```

---

## 4. RemediationStatus (NEW Enum)

```java
public enum RemediationStatus {
    PENDING,           // Created, not yet started
    IN_PROGRESS,       // Agent is executing
    SUCCESS,           // Fix applied and verified
    FAILED,            // Fix failed or verification failed
    PENDING_REBOOT,    // Fix applied, reboot needed
    SKIPPED            // Kernel update, manual action required
}
```

---

## 5. RemediationAction (NEW Enum)

```java
public enum RemediationAction {
    APT_UPGRADE,       // apt-get upgrade <package>
    APT_INSTALL,       // apt-get install <package>=<version>
    CONFIG_UPDATE,     // Modify configuration file
    SYSTEMCTL_RESTART, // systemctl restart <service>
    MANUAL             // Requires manual intervention
}
```

---

## 6. StepAction Extension (EXISTING — Modified)

Add new value to both `api/.../plan/model/StepAction.java` and `agents/unix/.../domain/task/StepAction.java`:

```java
REMEDIATE   // Execute remediation on discovered vulnerabilities
```

---

## 7. AlertEvent Extension (EXISTING — No changes needed)

`AlertEvent.AlertEventType.REMEDIATION_COMPLETED` already exists.  
`WhenCondition.ON_REMEDIATION_SUCCESS` and `ON_REMEDIATION_FAILURE` already exist.

Payload structure for `REMEDIATION_COMPLETED` events:

```json
{
    "remediationId": "rem-123",
    "cveId": "CVE-2023-38408",
    "targetId": "tgt-456",
    "agentId": "agt-789",
    "status": "SUCCESS",
    "remediationType": "SERVICE_UPDATE",
    "packageName": "openssh-server"
}
```

---

## 8. Modifications to Existing Entities

### ScopedEntity (api/.../domain/ScopedEntity.java)

Add `RemediationRecord` to the `instanceof` chain in both `setOrganizationIdValue()` and `setProjectIdValue()`:

```java
} else if (this instanceof com.spulido.tfg.domain.remediation.model.RemediationRecord) {
    ((com.spulido.tfg.domain.remediation.model.RemediationRecord) this).setOrganizationId(organizationId);
}
```

### ServiceVulnerabilityRecord (EXISTING — Optional extension)

No direct modification needed. The link from `RemediationRecord` to `ServiceVulnerabilityRecord` is via `vulnerabilityRecordId` field. This keeps the entities decoupled.

---

## 9. UI Models (TypeScript)

### RemediationRecord Interface

```typescript
export interface RemediationRecord {
  readonly id: string;
  readonly vulnerabilityRecordId: string;
  readonly cveId: string;
  readonly targetId: string;
  readonly targetName?: string;     // Joined from Target
  readonly agentId: string;
  readonly remediationType: RemediationType;
  readonly status: RemediationStatus;
  readonly packageName?: string;
  readonly packageVersionBefore?: string;
  readonly packageVersionAfter?: string;
  readonly actionDescription?: string;
  readonly preCheckLogs?: readonly string[];
  readonly executionLogs?: readonly string[];
  readonly postCheckLogs?: readonly string[];
  readonly startedAt?: string;      // ISO 8601
  readonly completedAt?: string;    // ISO 8601
  readonly errorMessage?: string;
  readonly rollbackHint?: string;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export enum RemediationType {
  SERVICE_UPDATE = 'SERVICE_UPDATE',
  REBOOT_REQUIRED = 'REBOOT_REQUIRED',
  KERNEL_UPDATE = 'KERNEL_UPDATE',
  UNKNOWN = 'UNKNOWN'
}

export enum RemediationStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  SUCCESS = 'SUCCESS',
  FAILED = 'FAILED',
  PENDING_REBOOT = 'PENDING_REBOOT',
  SKIPPED = 'SKIPPED'
}
```
