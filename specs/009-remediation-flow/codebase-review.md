# Codebase Review — Remediation Feature Gap Analysis

**Date:** 2026-06-26  
**Scope:** End-to-end remediation workflow in TFG autonomous cybersecurity system  
**Reviewer:** Automated codebase review agent

---

## 1. What Exists and Works Well

### 1.1 Vulnerability Detection Pipeline

The current vulnerability detection pipeline is solid:

- **`ServiceVulnerabilityRecord`** (`api/.../vulnerability/model/ServiceVulnerabilityRecord.java`) is a well-structured MongoDB document with `serviceKey`, `serviceName`, `serviceVersion`, `CVEs`, `fetchedAt`, `status` (`FETCHED | NO_RESULTS`), and `totalCves`. Uses `@CompoundIndex` on `serviceKey` for unique lookups.

- **`VulnerabilityLookupService`** (`api/.../vulnerability/services/VulnerabilityLookupService.java`) provides lazy-loading through `lookup()` (cache-then-NVD) and `refreshRecord()` for force-refresh. The implementation in `VulnerabilityLookupServiceImpl` correctly resolves CPE names via `NvdApiClient`, fetches CVEs with exploit references from NVD, and persists the result.

- **`ServiceVulnerabilityRepository`** (`api/.../vulnerability/db/ServiceVulnerabilityRepository.java`) supports queries by service name, CVE severity, and combined service+severity with pagination.

- **Agent-side vulnerability lookup** — `AgentCommunicationController.lookupVulnerabilities()` (line ~96) exposes a `POST /api/agent/comm/vulnerabilities/lookup` endpoint that the agent can call to get vulnerability data for discovered services.

- **`CveEntry`** carries rich data: `cveId`, `description`, `cvssScore`, `severity`, `cvssVector`, `affectedVersions`, `exploits`, `publishedDate`. The `ExploitReference` sub-document captures `source`, `description`, and `url` for each known exploit.

- **`VulnerabilityMapper`** cleanly transforms records into `VulnerabilityLookupResponse` (full detail) and `VulnerabilityListItem` (list view with `maxSeverity`).

### 1.2 Agent Execution Engine

- **Agent job execution** (`agents/unix/.../worker/TaskExecutionService.java`) processes sequences of tasks (`TaskDefinition`) mapped to `StepAction` enums. Each task executes a shell command via `CommandExecutor`, then passes through an optional `StepHandler` that enriches the result context.

- **Step context propagation** — Results from earlier steps (e.g., `SERVICE_SCAN` → `EXPLOITATION_KNOWLEDGE`) are forwarded through a `Map<StepAction, StepResult>` context, enabling step-to-step data flow.

- **Strong state machine** — `AgentTask` enforces valid transitions (`PENDING→RUNNING→COMPLETED/FAILED`, etc.), and `AgentJob` tracks overall job completion.

- **SSH remote execution** — `SshRemoteCommandExecutor` can execute commands on remote targets via SSH and transfer files via SCP (with base64 pipe fallback). `SshSessionProvisioner` verifies SSH connectivity with up to 3 retries.

- **Step handler registry pattern** — `WorkerCoordinator.createDefaultStepHandlers()` maps `StepAction` to `StepHandler` implementations using a `Map`, making it easy to add new handlers for new actions (including remediation).

### 1.3 Alert System (Pre-configured for Remediation)

- **`WhenCondition`** (`api/.../alerts/model/WhenCondition.java`) already defines `ON_REMEDIATION_SUCCESS` and `ON_REMEDIATION_FAILURE` alongside other conditions (`ON_VULNERABILITY_DETECTED`, `ON_HIGH_SEVERITY_VULNERABILITY`, `ON_SCAN_COMPLETED`).

- **`AlertEvent.AlertEventType`** (`api/.../alerts/model/AlertEvent.java`) includes `REMEDIATION_COMPLETED` as an event type.

- **`AlertTriggerServiceImpl.matchesCondition()`** correctly handles `ON_REMEDIATION_SUCCESS` (checks `event.type == REMEDIATION_COMPLETED && payload.status == "SUCCESS"`) and `ON_REMEDIATION_FAILURE` (same type with `"FAILURE"` status).

- **Email delivery** via Resend with FreeMarker templates works for all event types.

- **UI alert UI** (`ui/.../alerts/`) already displays `ON_REMEDIATION_SUCCESS` and `ON_REMEDIATION_FAILURE` as configurable conditions in create/edit modals.

### 1.4 Plan and Template System

- **`StepAction`** enum on the API side (`api/.../plan/model/StepAction.java`) has 11 actions: `SYSTEM_SCAN`, `SERVICE_SCAN`, `NETWORK_SCAN`, `GENERATE_REPORT`, `SEND_REPORT`, `EXPLOITATION_KNOWLEDGE`, `REQUEST_REPLICATION`, `EXECUTE_EXPLOIT`, `TRANSFER_AGENT`, `REPLICATE`, `SELF_DESTRUCT`.

- Agent-side `StepAction` (`agents/unix/.../domain/task/StepAction.java`) mirrors this but adds `ECHO` as a fallback action.

- **`Template`** documents store reusable plans (a `Plan` with `List<Step>`) scoped to org/project via `ScopedEntity`.

- **`Agent.communication`** — `AgentCommunicationController.getPlan()` returns the current plan (including target IP from assigned target). `AgentPlanServiceImpl.assignPlanFromTemplate()` copies a template's steps into the agent's plan, resetting step states to `PENDING`.

### 1.5 Target and Exploitation Knowledge

- **Target model** — `Target` includes `ipOrDomain`, `status`, `assignedAgent`, `preauthCode`. Agents are authorized for a specific target, enforced by `ExploitationKnowledgeServiceImpl.validateAgentAuthorization()`.

- **Exploit script generation** — `ScriptServiceImpl.generateExploitScript()` generates bash scripts via FreeMarker (`exploit.sh.ftl`). The generated script is currently a stub (echo + comment), which is appropriate for a prototype.

---

## 2. What's Missing for a Remediation Feature

### 2.1 No `Remediation` Entity Anywhere

- There is **no MongoDB document** representing a remediation action. Nothing tracks:
  - Which vulnerability (`CVE ID`) is being remediated
  - Which target (host/instance) it applies to
  - What action was taken (patch apply, config change, service restart, etc.)
  - The remediation status (pending, in-progress, success, failed, rolled-back)
  - Timestamps for creation, execution start, completion
  - Output logs from the remediation command
  - Which agent executed it
  - Which plan/step triggered it

### 2.2 No `StepAction.REMEDIATE` (or equivalent)

- The `StepAction` enum on both API and agent sides has **no remediation action**. The current actions are discovery/exploitation/replication focused:
  - `SYSTEM_SCAN`, `SERVICE_SCAN` — discovery
  - `EXPLOITATION_KNOWLEDGE`, `EXECUTE_EXPLOIT` — exploitation
  - `REQUEST_REPLICATION`, `TRANSFER_AGENT`, `REPLICATE` — replication
  - No `APPLY_PATCH`, `REMEDIATE`, `FIX_VULNERABILITY`, or similar action.

### 2.3 No Remediation-to-Vulnerability Linking

- `ServiceVulnerabilityRecord` stores CVEs but has **no field linking to remediation efforts**.
- `CveEntry` has no `remediationStatus` or `remediationId` reference.
- There is no way to query "which CVEs have been remediated" or "which are still open."

### 2.4 No Remediation Command Execution Flow

- SSH execution exists (`SshRemoteCommandExecutor`) and can run arbitrary commands on remote targets, but there is **no orchestration code** that:
  1. Takes a CVE ID (e.g., `CVE-2025-12345`)
  2. Resolves it to a remediation action (e.g., "run `apt-get update && apt-get upgrade openssh-server`")
  3. Executes that action via SSH on the target
  4. Validates the fix (e.g., re-scan the service version)
  5. Reports the result back to the platform

### 2.5 No Remediation Status Tracking

- `VulnerabilityStatus` enum only has `FETCHED` and `NO_RESULTS`. No `REMEDIATED`, `REMEDIATION_IN_PROGRESS`, `REMEDIATION_FAILED`.
- `StepExecutionStatus` enum (`PENDING`, `RUNNING`, `COMPLETED`, `FAILED`) could partially cover step-level tracking, but there's no vulnerability-level status.
- There's no history of remediation attempts for a given vulnerability.

### 2.6 No Remediation Strategy / Knowledge Base

- The system has an `ExploitationKnowledgeService` that assembles exploit scripts for services/CVEs, but there's **no equivalent `RemediationKnowledgeService`** that:
  - Maps CVE IDs to known fixes (patches, version bumps, config changes)
  - Determines which fix is applicable based on the target OS/distribution
  - Generates remediation scripts (similar to `generateExploitScript` but for fixes)

### 2.7 Alert Events Defined But Never Fired for Remediation

- `ON_REMEDIATION_SUCCESS` and `ON_REMEDIATION_FAILURE` exist as `WhenCondition` values.
- The `AlertTriggerService.checkAndTrigger()` method is in place and functional.
- However, **no code anywhere calls `alertTriggerService.checkAndTrigger()` with a `REMEDIATION_COMPLETED` event**, because there's no remediation execution to trigger from.

### 2.8 UI Gaps

- **Vulnerability list page** (`vulnerabilities.component.html`): No "Remediate" action button per row.
- **Vulnerability detail page** (`vulnerability-detail.component.html`): Shows CVE details and exploit references, but no "Remediate this CVE" button or remediation workflow initiation.
- **No remediation history page**: No UI to view past remediation actions, their status, logs, or results.
- **No remediation dashboard**: No widgets showing remediation stats (e.g., "5 vulnerabilities open, 2 in progress, 12 remediated").

### 2.9 No API Endpoints for Remediation

- `VulnerabilityController` has `GET /api/vulnerabilities`, `GET /api/vulnerabilities/{serviceKey}`, `POST /api/vulnerabilities/{serviceKey}/refresh`.
- No `POST /api/vulnerabilities/{serviceKey}/cves/{cveId}/remediate` to trigger remediation.
- No `GET /api/remediations` or `GET /api/remediations/{id}` to query remediation history.
- No `PUT /api/remediations/{id}` to update remediation status (e.g., from agent).

---

## 3. Suggested Implementation Approach

### 3.1 Data Model Additions

#### New `RemediationRecord` Entity (API side)

```java
@Document(collection = "remediation_records")
public class RemediationRecord extends BaseEntity implements ScopedEntity {
    @Field private String vulnerabilityRecordId;   // FK → ServiceVulnerabilityRecord.id
    @Field private String cveId;                    // e.g., "CVE-2025-12345"
    @Field private String targetId;                 // FK → Target.id
    @Field private String agentId;                  // FK → Agent.id (executing agent)
    @Field private String planId;                   // optional: which plan triggered this
    @Field private RemediationStatus status;        // PENDING, IN_PROGRESS, SUCCESS, FAILED, ROLLED_BACK
    @Field private String action;                   // e.g., "apt upgrade openssh-server", "systemctl restart sshd"
    @Field private String strategy;                 // e.g., "PACKAGE_UPGRADE", "CONFIG_CHANGE", "SERVICE_RESTART"
    @Field private List<String> preValidationLogs;  // logs from pre-check commands
    @Field private List<String> executionLogs;      // logs from remediation commands
    @Field private List<String> postValidationLogs; // logs from post-fix verification
    @Field private Instant startedAt;
    @Field private Instant completedAt;
    @Field private String errorMessage;
    @Field private String organizationId;
    @Field private String projectId;
}
```

#### New `RemediationStatus` Enum

```java
public enum RemediationStatus {
    PENDING,         // Created but not yet dispatched
    IN_PROGRESS,     // Agent is executing
    SUCCESS,         // Remediation completed and verified
    FAILED,          // Remediation command failed
    ROLLED_BACK,     // Fix was applied but caused issues, rolled back
    SKIPPED          // CVE was not applicable or fix was unnecessary
}
```

#### Extend `StepAction` Enum (Both API and Agent)

Add a new value:
```java
APPLY_REMEDIATION   // Execute a remediation fix on a target
```

This sits alongside the existing exploitation-focused actions. Alternatively, a more generic `REMEDIATE` could work. The naming should mirror the existing pattern (`EXECUTE_EXPLOIT` → `APPLY_REMEDIATION` or `EXECUTE_REMEDIATION`).

#### Extend `VulnerabilityStatus` (Optional)

Add:
```java
REMEDIATED,              // All applicable CVEs for this service have been remediated
REMEDIATION_IN_PROGRESS, // Remediation is underway for some CVEs
```

Or keep `VulnerabilityStatus` as a fetch-status only and track remediation through the new `RemediationRecord` entity independently.

#### Extend `AlertEvent.AlertEventType`

Already has `REMEDIATION_COMPLETED`. No extension needed, just start firing events.

### 3.2 New Service Layer

#### `RemediationService` (API side)

```java
public interface RemediationService {
    // Initiate remediation for a CVE on a target
    RemediationRecord initiateRemediation(String serviceKey, String cveId, String targetId);
    
    // Get remediation status
    RemediationRecord getRemediation(String remediationId);
    
    // List remediations with filtering
    Page<RemediationRecord> listRemediations(String targetId, RemediationStatus status, Pageable pageable);
    
    // Update remediation status (called by agent)
    RemediationRecord updateStatus(String remediationId, RemediationStatus status, 
                                    List<String> logs, String errorMessage);
}
```

#### `RemediationKnowledgeService` (API side)

```java
public interface RemediationKnowledgeService {
    // Given a CVE and target OS, determine the fix action
    RemediationStrategy resolveStrategy(String cveId, OperatingSystem targetOs, 
                                         String serviceName, String serviceVersion);
}

// Value object
public class RemediationStrategy {
    String action;        // shell command to run
    String strategy;      // PACKAGE_UPGRADE, CONFIG_CHANGE, etc.
    List<String> preCheckCommands;   // commands to run before fix
    List<String> postCheckCommands;  // commands to run after fix to verify
}
```

#### `AgentSideRemediationStepHandler` (Agent side)

A new `StepHandler` implementation in `agents/unix/.../worker/step/`:

```java
public class ApplyRemediationStepHandler implements StepHandler {
    // Uses SshSessionProvisioner + SshRemoteCommandExecutor
    // 1. Reads target info from context (from EXPLOIT or SERVICE_SCAN step)
    // 2. Fetches remediation strategy from central API
    // 3. Runs pre-check commands on target via SSH
    // 4. Runs remediation action via SSH
    // 5. Runs post-check commands via SSH
    // 6. Reports results back to central API
}
```

### 3.3 API Endpoint Recommendations

| Method | Path | Purpose |
|--------|------|---------|
| `POST` | `/api/vulnerabilities/{serviceKey}/cves/{cveId}/remediate` | Initiate remediation for a CVE on a specified target. Body: `{ targetId }`. Returns `RemediationRecord`. |
| `GET` | `/api/remediations` | List remediation records with pagination, filter by target, status, CVE. |
| `GET` | `/api/remediations/{id}` | Get single remediation record with full logs. |
| `PUT` | `/api/remediations/{id}` | Update remediation status (called by agent). Body: `{ status, logs, errorMessage }`. |
| `POST` | `/api/agent/comm/remediation-strategy` | Agent endpoint: given CVE + target info, return a remediation strategy. |
| `PUT` | `/api/agent/comm/remediation/{id}` | Agent endpoint: report remediation progress/result back to platform. |

### 3.4 UI Component Recommendations

1. **"Remediate" button on vulnerability detail page** (`vulnerability-detail.component.html`):
   - Add a button per CVE row (or a global "Remediate All" button)
   - Opens a dialog/modal to select a target (from list of targets in the project)
   - Calls `POST /api/vulnerabilities/{serviceKey}/cves/{cveId}/remediate`

2. **New "Remediation History" page** (`/remediations`):
   - Table with columns: Target, CVE ID, Service, Status (tag), Started, Completed, Actions
   - Filter by status (dropdown), target, date range
   - Click row → remediation detail page

3. **Remediation detail page** (`/remediations/{id}`):
   - Shows target info, CVE details, action performed, strategy
   - Expandable log sections (pre-validation, execution, post-validation)
   - Status timeline (PENDING → IN_PROGRESS → SUCCESS/FAILED)

4. **Remediation dashboard widgets** (for project dashboard):
   - Remediation status pie/bar chart (Open / In Progress / Remediated / Failed)
   - Recent remediation activity feed
   - Mean time to remediate (MTTR) metric

5. **Alert integration in UI**: The existing alert configuration UI already supports `ON_REMEDIATION_SUCCESS` and `ON_REMEDIATION_FAILURE`. No UI changes needed — the conditions are already displayed correctly.

### 3.5 Step Execution Flow

A complete remediation plan step sequence could be:

```
Template "Full Remediation Workflow":
1. SYSTEM_SCAN        → Discover OS, packages
2. SERVICE_SCAN       → Identify running services and versions
3. EXPLOITATION_KNOWLEDGE → Check vulnerability DB for discovered services
4. APPLY_REMEDIATION  → [NEW] Fix identified vulnerabilities
5. SEND_REPORT        → Report remediation results
```

The `APPLY_REMEDIATION` step handler would:
1. Read the `SERVICE_SCAN` context for discovered services
2. Call the central API to get remediation strategies for the CVEs
3. For each strategy, SSH into the target and run pre-checks → action → post-checks
4. Report each remediation as a `RemediationRecord` via the central API
5. Fire `AlertEvent` with `REMEDIATION_COMPLETED` and appropriate `status` in payload

### 3.6 Files That Need Changes (Summary)

**API (new files):**
- `api/.../domain/remediation/model/RemediationRecord.java`
- `api/.../domain/remediation/model/RemediationStatus.java`
- `api/.../domain/remediation/model/RemediationStrategy.java`
- `api/.../domain/remediation/model/dto/*.java` (request/response DTOs)
- `api/.../domain/remediation/db/RemediationRepository.java`
- `api/.../domain/remediation/controller/RemediationController.java`
- `api/.../domain/remediation/services/RemediationService.java`
- `api/.../domain/remediation/services/RemediationKnowledgeService.java`
- `api/.../domain/remediation/services/impl/RemediationServiceImpl.java`
- `api/.../domain/remediation/services/impl/RemediationKnowledgeServiceImpl.java`
- `api/.../domain/remediation/services/RemediationMapper.java`
- `api/.../domain/remediation/exception/RemediationException.java`
- `api/src/main/resources/scripts/remediate.sh.ftl` — FreeMarker template for remediation scripts

**API (modified files):**
- `api/.../domain/plan/model/StepAction.java` — add `APPLY_REMEDIATION`
- `api/.../domain/ScopedEntity.java` — add `RemediationRecord` to instanceof chain
- `api/.../domain/vulnerability/model/VulnerabilityStatus.java` — optionally add `REMEDIATED`, `REMEDIATION_IN_PROGRESS`
- `api/.../domain/agent/controller/AgentCommunicationController.java` — add remediation strategy + status endpoints

**Agent (new files):**
- `agents/unix/.../worker/step/ApplyRemediationStepHandler.java`
- `agents/unix/.../worker/http/dto/RemediationStrategyRequest.java`
- `agents/unix/.../worker/http/dto/RemediationStrategyResponse.java`

**Agent (modified files):**
- `agents/unix/.../domain/task/StepAction.java` — add `APPLY_REMEDIATION`
- `agents/unix/.../worker/WorkerCoordinator.java` — register new handler, add command mapping
- `agents/unix/.../worker/http/AgentHttpClient.java` — add remediation API methods

**UI (new files):**
- `ui/src/app/pages/remediations/` — full feature with list, detail, routing, service, models
- Possibly a remediation modal component for the vulnerability detail page

**UI (modified files):**
- `ui/src/app/pages/vulnerabilities/feature/vulnerability-detail/vulnerability-detail.component.html` — add remediate button
- `ui/src/app/pages/vulnerabilities/feature/vulnerability-detail/vulnerability-detail.component.ts` — add remediate logic
- App routing — add `/remediations` route

---

## 4. Risks and Considerations

1. **Remediation Knowledge Base is the hardest part.** Unlike exploitation (where CVEs map to known exploit scripts), remediation actions are OS/distribution-specific. A `CVE-2025-12345` for OpenSSH might need `apt upgrade openssh-server` on Ubuntu, `yum update openssh-server` on RHEL, or a source rebuild on Alpine. Start with a simple mapping (Ubuntu/Debian only) and expand.

2. **Dry-run / approval mode.** Consider adding a `requireApproval` flag to remediation, similar to `ReplicationApprovalMode` in replication policy. Auto-remediation without human review is risky in production.

3. **Rollback support.** If a remediation breaks something, the system should ideally support rollback (e.g., `apt install --reinstall` with previous version, or snapshot restore). This is a stretch goal.

4. **Verification after remediation.** The `APPLY_REMEDIATION` step handler should optionally re-run `SERVICE_SCAN` after fixing to verify the service version changed and the CVE is no longer reported. This validates the fix was effective.

5. **The exploit script template is currently a stub.** The same will be true for remediation scripts initially. Consider integrating with actual package managers or configuration management tools (Ansible, apt, yum) rather than generating raw shell commands.

6. **`ScopedEntity.setOrganizationIdValue()` / `setProjectIdValue()` uses hardcoded instanceof chains.** Any new entity implementing `ScopedEntity` must be added to these methods. Consider refactoring to use a registry or visitor pattern in a separate PR, but for now, just add the new `RemediationRecord` case.

---

## 5. Summary Assessment

| Area | Status | Action Needed |
|------|--------|---------------|
| Vulnerability detection | ✅ Complete | None |
| Exploit knowledge base | ✅ Complete | None |
| Agent SSH execution | ✅ Complete | Reuse for remediation |
| Step execution engine | ✅ Complete | Add `APPLY_REMEDIATION` step handler |
| Alert configurations | ✅ Complete (pre-wired) | Start firing events from remediation code |
| Template/Plan system | ✅ Complete | Add remediation templates |
| **Remediation data model** | ❌ Missing | Create `RemediationRecord` entity + repository |
| **Remediation strategy KB** | ❌ Missing | Create `RemediationKnowledgeService` |
| **Remediation step handler** | ❌ Missing | Create `ApplyRemediationStepHandler` |
| **Remediation API endpoints** | ❌ Missing | Create controller + DTOs |
| **Remediation UI** | ❌ Missing | Create pages + add buttons to vuln detail |
