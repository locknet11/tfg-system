# API Contracts: Autonomous Remediation Flow

**Date**: 2026-06-26  
**Feature**: 010-remediation-flow

---

## 1. User-Facing Endpoints

Base path: `/api/remediations`  
Authentication: Session-based (JWT)  
Authorization: Role-based (ADMIN, OPERATOR)

---

### 1.1 List Remediation Records

```
GET /api/remediations
```

**Query Parameters:**

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | int | No | 0 | Page number (0-based) |
| `size` | int | No | 20 | Page size (max 100) |
| `status` | String | No | — | Filter by status (PENDING, IN_PROGRESS, SUCCESS, FAILED, PENDING_REBOOT, SKIPPED) |
| `targetId` | String | No | — | Filter by target |
| `cveId` | String | No | — | Filter by CVE ID |
| `remediationType` | String | No | — | Filter by remediation type |
| `from` | String | No | — | Start date (ISO 8601) |
| `to` | String | No | — | End date (ISO 8601) |
| `sort` | String | No | `createdAt,desc` | Sort field and direction |

**Response:** `200 OK`

```json
{
    "content": [
        {
            "id": "rem-001",
            "cveId": "CVE-2023-38408",
            "targetId": "tgt-456",
            "targetName": "web-server-01",
            "agentId": "agt-789",
            "remediationType": "SERVICE_UPDATE",
            "status": "SUCCESS",
            "packageName": "openssh-server",
            "packageVersionBefore": "8.9p1",
            "packageVersionAfter": "9.3p2",
            "actionDescription": "Upgraded openssh-server from 8.9p1 to 9.3p2",
            "startedAt": "2026-06-26T08:00:00Z",
            "completedAt": "2026-06-26T08:02:15Z",
            "createdAt": "2026-06-26T07:59:50Z"
        }
    ],
    "pageable": {
        "pageNumber": 0,
        "pageSize": 20
    },
    "totalElements": 42,
    "totalPages": 3
}
```

**Error Responses:**

| Status | Code | Description |
|--------|------|-------------|
| 400 | INVALID_FILTER | Invalid filter parameter value |
| 401 | UNAUTHORIZED | Not authenticated |
| 403 | FORBIDDEN | Insufficient permissions |

---

### 1.2 Get Remediation Record Detail

```
GET /api/remediations/{id}
```

**Path Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `id` | String | Remediation record ID |

**Response:** `200 OK`

```json
{
    "id": "rem-001",
    "vulnerabilityRecordId": "vuln-123",
    "cveId": "CVE-2023-38408",
    "targetId": "tgt-456",
    "targetName": "web-server-01",
    "agentId": "agt-789",
    "planId": "plan-101",
    "remediationType": "SERVICE_UPDATE",
    "status": "SUCCESS",
    "packageName": "openssh-server",
    "packageVersionBefore": "8.9p1",
    "packageVersionAfter": "9.3p2",
    "actionDescription": "Upgraded openssh-server from 8.9p1 to 9.3p2",
    "preCheckLogs": [
        "$ dpkg -l openssh-server",
        "ii  openssh-server  1:8.9p1-3  amd64  secure shell server"
    ],
    "executionLogs": [
        "$ apt-get update",
        "Get:1 http://archive.ubuntu.com/ubuntu jammy InRelease [270 kB]",
        "$ apt-get install -y openssh-server=1:9.3p2-1",
        "Preparing to unpack .../openssh-server_1%3a9.3p2-1_amd64.deb",
        "Unpacking openssh-server (1:9.3p2-1) over (1:8.9p1-3)",
        "Setting up openssh-server (1:9.3p2-1)",
        "$ systemctl restart sshd"
    ],
    "postCheckLogs": [
        "$ dpkg -l openssh-server",
        "ii  openssh-server  1:9.3p2-1  amd64  secure shell server",
        "$ systemctl is-active sshd",
        "active",
        "$ ssh -V",
        "OpenSSH_9.3p2, OpenSSL 3.0.2 15 Mar 2022"
    ],
    "startedAt": "2026-06-26T08:00:00Z",
    "completedAt": "2026-06-26T08:02:15Z",
    "errorMessage": null,
    "rollbackHint": "apt-get install openssh-server=1:8.9p1-3",
    "createdAt": "2026-06-26T07:59:50Z",
    "updatedAt": "2026-06-26T08:02:15Z"
}
```

**Error Responses:**

| Status | Code | Description |
|--------|------|-------------|
| 404 | REMEDIATION_NOT_FOUND | Remediation record not found |

---

### 1.3 Get Remediation Statistics

```
GET /api/remediations/statistics
```

**Response:** `200 OK`

```json
{
    "totalCount": 156,
    "byStatus": {
        "SUCCESS": 120,
        "FAILED": 12,
        "PENDING": 5,
        "IN_PROGRESS": 3,
        "PENDING_REBOOT": 8,
        "SKIPPED": 8
    },
    "meanTimeToRemediateSeconds": 127,
    "recentActivity": [
        {
            "id": "rem-156",
            "cveId": "CVE-2024-5678",
            "targetName": "db-server-02",
            "status": "SUCCESS",
            "completedAt": "2026-06-26T09:15:00Z"
        }
    ]
}
```

---

## 2. Agent-Facing Endpoints

Base path: `/api/agent/comm/remediation`  
Authentication: API key (X-Agent-Api-Key header)  
Authorization: Role AGENT

---

### 2.1 Request Remediation Strategy

```
POST /api/agent/comm/remediation/strategy
```

**Request Body:**

```json
{
    "cveId": "CVE-2023-38408",
    "packageName": "openssh-server",
    "currentVersion": "8.9p1",
    "operatingSystem": "ubuntu-22.04"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `cveId` | String | ✅ | CVE identifier |
| `packageName` | String | ✅ | Affected package name |
| `currentVersion` | String | ✅ | Current package version |
| `operatingSystem` | String | ✅ | Target operating system |

**Response:** `200 OK`

```json
{
    "found": true,
    "remediationType": "SERVICE_UPDATE",
    "action": "APT_UPGRADE",
    "targetVersion": "9.3p2",
    "serviceName": "sshd",
    "requiresReboot": false,
    "preCheckCommands": [
        "dpkg -l openssh-server"
    ],
    "fixCommands": [
        "apt-get update",
        "apt-get install -y openssh-server=1:9.3p2-1"
    ],
    "postCheckCommands": [
        "dpkg -l openssh-server",
        "systemctl is-active sshd",
        "ssh -V"
    ],
    "notes": null
}
```

**Response (no strategy found):** `200 OK`

```json
{
    "found": false,
    "remediationType": "UNKNOWN",
    "action": null,
    "targetVersion": null,
    "serviceName": null,
    "requiresReboot": false,
    "preCheckCommands": [],
    "fixCommands": [],
    "postCheckCommands": [],
    "notes": "No remediation strategy available for this CVE and OS combination"
}
```

---

### 2.2 Report Remediation Result

```
POST /api/agent/comm/remediation/report
```

**Request Body:**

```json
{
    "cveId": "CVE-2023-38408",
    "targetId": "tgt-456",
    "remediationType": "SERVICE_UPDATE",
    "status": "SUCCESS",
    "packageName": "openssh-server",
    "packageVersionBefore": "8.9p1",
    "packageVersionAfter": "9.3p2",
    "actionDescription": "Upgraded openssh-server from 8.9p1 to 9.3p2 and restarted sshd",
    "preCheckLogs": ["..."],
    "executionLogs": ["..."],
    "postCheckLogs": ["..."],
    "errorMessage": null,
    "rollbackHint": "apt-get install openssh-server=1:8.9p1-3"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `cveId` | String | ✅ | CVE being remediated |
| `targetId` | String | ✅ | Target ID |
| `remediationType` | String | ✅ | RemediationType enum value |
| `status` | String | ✅ | RemediationStatus enum value |
| `packageName` | String | ❌ | Package name |
| `packageVersionBefore` | String | ❌ | Version before fix |
| `packageVersionAfter` | String | ❌ | Version after fix |
| `actionDescription` | String | ❌ | Human-readable description |
| `preCheckLogs` | List\<String\> | ❌ | Pre-check output |
| `executionLogs` | List\<String\> | ❌ | Execution output |
| `postCheckLogs` | List\<String\> | ❌ | Post-verification output |
| `errorMessage` | String | ❌ | Error details (when FAILED) |
| `rollbackHint` | String | ❌ | Manual rollback commands |

**Response:** `201 Created`

```json
{
    "remediationId": "rem-001",
    "status": "SUCCESS"
}
```

**Error Responses:**

| Status | Code | Description |
|--------|------|-------------|
| 400 | INVALID_REQUEST | Missing required fields |
| 401 | UNAUTHORIZED | Invalid API key |
| 403 | FORBIDDEN | Agent not authorized for target |

---

### 2.3 Update Remediation Status

```
PUT /api/agent/comm/remediation/{id}
```

**Request Body:**

```json
{
    "status": "IN_PROGRESS",
    "logs": ["Starting remediation for CVE-2023-38408..."]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `status` | String | ✅ | New RemediationStatus |
| `logs` | List\<String\> | ❌ | Progress logs to append |

**Response:** `200 OK`

```json
{
    "remediationId": "rem-001",
    "status": "IN_PROGRESS",
    "updatedAt": "2026-06-26T08:00:05Z"
}
```
