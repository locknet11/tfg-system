# Contract: Remediation Report API

**Feature**: Docker Container Remediation Skip
**Endpoint**: `POST /api/agent/comm/remediation/report`
**Direction**: Agent → Central Platform

## Overview

This contract documents the existing remediation report endpoint with the new `skipReason` field added. The endpoint allows an agent to report the outcome of a remediation attempt to the central platform.

## Request

### Method & Path

```
POST /api/agent/comm/remediation/report
```

### Authentication

Agent API key in `Authorization` header (existing mechanism, unchanged).

### Request Body

```json
{
  "cveId": "CVE-2025-12345",
  "targetId": "target-uuid-here",
  "remediationType": "SERVICE_UPDATE",
  "status": "SUCCESS",
  "packageName": "openssh-server",
  "packageVersionBefore": "1:8.9p1-3",
  "packageVersionAfter": "1:9.2p1-2",
  "actionDescription": "Applied apt-get install --only-upgrade openssh-server",
  "preCheckLogs": [
    "$ dpkg -l openssh-server",
    "ii  openssh-server  1:8.9p1-3  amd64  secure shell (SSH) server"
  ],
  "executionLogs": [
    "$ apt-get install --only-upgrade -y openssh-server",
    "Setting up openssh-server (1:9.2p1-2)..."
  ],
  "postCheckLogs": [
    "$ dpkg -l openssh-server",
    "ii  openssh-server  1:9.2p1-2  amd64  secure shell (SSH) server"
  ],
  "errorMessage": null,
  "rollbackHint": null,
  "skipReason": null
}
```

### Field Reference

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `cveId` | String | Yes | CVE identifier (e.g., `CVE-2025-12345`). For docker-skip: set to `"CONTAINER-DETECTED"` |
| `targetId` | String | Yes | Target system identifier |
| `remediationType` | String | Yes | `SERVICE_UPDATE`, `REBOOT_REQUIRED`, `KERNEL_UPDATE`, or `CONTAINER_DETECTED` |
| `status` | String | Yes | `SUCCESS`, `FAILED`, `PENDING_REBOOT`, or `SKIPPED` |
| `packageName` | String | No | Affected package name |
| `packageVersionBefore` | String | No | Version before remediation |
| `packageVersionAfter` | String | No | Version after remediation |
| `actionDescription` | String | No | Human-readable description of remediation action |
| `preCheckLogs` | String[] | No | Pre-check command outputs |
| `executionLogs` | String[] | No | Execution command outputs |
| `postCheckLogs` | String[] | No | Post-verification command outputs |
| `errorMessage` | String | No | Error details if status is FAILED |
| `rollbackHint` | String | No | Rollback instructions if available |
| **`skipReason`** | String | No | **NEW** — Why remediation was skipped. Required when `status` is `SKIPPED` for container detection |

### Docker Container Skip Example

When the agent detects it is running inside a Docker container, it sends:

```json
{
  "cveId": "CONTAINER-DETECTED",
  "targetId": "target-uuid-here",
  "remediationType": "CONTAINER_DETECTED",
  "status": "SKIPPED",
  "packageName": null,
  "packageVersionBefore": null,
  "packageVersionAfter": null,
  "actionDescription": "Remediation skipped: Docker container detected",
  "preCheckLogs": [
    "DETECTION: /.dockerenv exists",
    "DETECTION: /proc/1/cgroup contains docker path"
  ],
  "executionLogs": [],
  "postCheckLogs": [],
  "errorMessage": null,
  "rollbackHint": null,
  "skipReason": "Docker container detected (/.dockerenv + /proc/1/cgroup) — remediation skipped to avoid ineffective or destructive changes in an ephemeral environment"
}
```

### Inconclusive Detection Skip Example

```json
{
  "cveId": "CONTAINER-DETECTED",
  "targetId": "target-uuid-here",
  "remediationType": "CONTAINER_DETECTED",
  "status": "SKIPPED",
  "packageName": null,
  "packageVersionBefore": null,
  "packageVersionAfter": null,
  "actionDescription": "Remediation skipped: container detection inconclusive",
  "preCheckLogs": [
    "DETECTION: Unable to read /proc/1/cgroup (permission denied)",
    "DETECTION: /.dockerenv not found"
  ],
  "executionLogs": [],
  "postCheckLogs": [],
  "errorMessage": null,
  "rollbackHint": null,
  "skipReason": "Container detection inconclusive — unable to read detection files — remediation skipped as precaution"
}
```

## Response

### Success (201 Created)

```json
{
  "remediationId": "6655a1b2c3d4e5f6a7b8c9d0",
  "status": "SKIPPED"
}
```

### Error Responses

| Status | Condition |
|--------|-----------|
| 400 | Validation failure (e.g., invalid CVE format) |
| 401 | Missing or invalid agent API key |
| 404 | Agent or target not found |
| 500 | Internal server error |

## Backward Compatibility

- **Existing records**: Remediation records created before this change have `skipReason: null`
- **Existing consumers**: The `RemediationInfo` DTO will include `skipReason: null` for those records
- **UI**: The remediation history page should display `skipReason` when present; hide it when null
- **Kernel skips**: Existing kernel-update skips (status=SKIPPED) will continue to have `skipReason: null` — the UI should handle this gracefully
