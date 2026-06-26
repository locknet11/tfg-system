# Agent Contracts: Autonomous Remediation Flow

**Date**: 2026-06-26  
**Feature**: 010-remediation-flow

---

## 1. RemediationStepHandler

**Module**: `agents/unix/`  
**Class**: `RemediationStepHandler implements StepHandler`  
**Registered in**: `WorkerCoordinator.createDefaultStepHandlers()`

### Input Context

The handler reads vulnerability data from the `EXPLOITATION_KNOWLEDGE` step context:

```java
Map<StepAction, StepResult> context
// context.get(StepAction.EXPLOITATION_KNOWLEDGE) → StepResult with vulnerability data
```

### Execution Flow

```
1. Parse vulnerability data from context
2. For each CVE:
   a. Request strategy from central API
   b. Classify remediation type
   c. Execute according to type:
      - SERVICE_UPDATE: run pre-checks → fix → restart service → post-checks
      - REBOOT_REQUIRED: run fix → report PENDING_REBOOT
      - KERNEL_UPDATE: report SKIPPED with safe version
   d. Report result to central API
3. Return StepResult with summary
```

### Output

```java
StepResult result = StepResult.builder()
    .status(StepResult.Status.COMPLETED)
    .message("Remediated 3 CVEs: 2 SUCCESS, 1 SKIPPED")
    .data(Map.of(
        "totalCves", 3,
        "successCount", 2,
        "failedCount", 0,
        "skippedCount", 1,
        "pendingRebootCount", 0
    ))
    .build();
```

---

## 2. Agent HTTP Client Extensions

New methods added to `AgentHttpClient`:

### 2.1 Request Remediation Strategy

```java
public RemediationStrategyResponse requestRemediationStrategy(
    RemediationStrategyRequest request) {
    String url = config.getCentralUrl() + "/api/agent/comm/remediation/strategy";
    return restTemplate.postForObject(url, request, RemediationStrategyResponse.class);
}
```

### 2.2 Report Remediation Result

```java
public RemediationReportResponse reportRemediationResult(
    RemediationReportRequest request) {
    String url = config.getCentralUrl() + "/api/agent/comm/remediation/report";
    return restTemplate.postForObject(url, request, RemediationReportResponse.class);
}
```

### 2.3 Update Remediation Status

```java
public RemediationStatusResponse updateRemediationStatus(
    String remediationId, RemediationStatusUpdate update) {
    String url = config.getCentralUrl() + "/api/agent/comm/remediation/" + remediationId;
    restTemplate.put(url, update);
    return new RemediationStatusResponse();
}
```

---

## 3. Agent-Side DTOs

### 3.1 RemediationStrategyRequest

```java
public class RemediationStrategyRequest {
    private final String cveId;
    private final String packageName;
    private final String currentVersion;
    private final String operatingSystem;
}
```

### 3.2 RemediationStrategyResponse

```java
public class RemediationStrategyResponse {
    private boolean found;
    private String remediationType;
    private String action;
    private String targetVersion;
    private String serviceName;
    private boolean requiresReboot;
    private List<String> preCheckCommands;
    private List<String> fixCommands;
    private List<String> postCheckCommands;
    private String notes;
}
```

### 3.3 RemediationReportRequest

```java
public class RemediationReportRequest {
    private final String cveId;
    private final String targetId;
    private final String remediationType;
    private final String status;
    private final String packageName;
    private final String packageVersionBefore;
    private final String packageVersionAfter;
    private final String actionDescription;
    private final List<String> preCheckLogs;
    private final List<String> executionLogs;
    private final List<String> postCheckLogs;
    private final String errorMessage;
    private final String rollbackHint;
}
```

### 3.4 RemediationReportResponse

```java
public class RemediationReportResponse {
    private String remediationId;
    private String status;
}
```

---

## 4. Remediation Script Template

**Location**: `agents/unix/src/main/resources/scripts/remediate.sh.tmpl`

```bash
#!/bin/bash
# Remediation script for {{CVE_ID}}
# Package: {{PACKAGE_NAME}}
# Action: {{ACTION}}

set -euo pipefail

# Pre-check
{{#PRE_CHECK_COMMANDS}}
echo "Pre-check: {{.}}"
{{.}}
{{/PRE_CHECK_COMMANDS}}

# Apply fix
{{#FIX_COMMANDS}}
echo "Executing: {{.}}"
{{.}}
{{/FIX_COMMANDS}}

# Post-check
{{#POST_CHECK_COMMANDS}}
echo "Post-check: {{.}}"
{{.}}
{{/POST_CHECK_COMMANDS}}

echo "Remediation complete"
```

---

## 5. Step Registration

The `REMEDIATE` action is registered in `WorkerCoordinator.createDefaultStepHandlers()`:

```java
handlers.put(StepAction.REMEDIATE, new RemediationStepHandler(
    httpClient, commandExecutor, sshSessionProvisioner, scriptTemplateService));
```

And in `mapActionToCommand()`:

```java
case REMEDIATE:
    return "remediate";
```
