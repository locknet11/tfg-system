# Data Model: Remote Agent Transfer

**Feature**: Remote Agent Transfer (005)
**Date**: 2026-06-02

## Entities

### TargetSession (Value Object)

Represents a verified SSH connection to an exploited target. Immutable.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `targetIp` | `String` | Yes | IPv4 address of the target machine |
| `targetUser` | `String` | Yes | SSH username (e.g., "root") |
| `sshIdentityFile` | `String` | No | Path to SSH private key file; null if key-based auth not needed |

**Validation Rules**:
- `targetIp` must be a valid IPv4 address (non-blank, dotted-quad format)
- `targetUser` must be non-blank
- `sshIdentityFile` if present must be a non-blank path

**Construction**: Created by `TransferAgentStepHandler` by parsing `EXECUTE_EXPLOIT` step context logs. The `ExecuteExploitStepHandler` records these values after `SshSessionProvisioner` verifies connectivity.

### StepResult Extensions (Modified Entity)

The existing `StepResult` class gains no new fields. Session information is passed through the `logs` field as structured `key:value` pairs.

**ExecuteExploit StepResult logs format** (after successful SSH verification):

```
Service scan completed. Found 3 open ports.
ExploitationKnowledge returned 2 scripts
Executing exploit against target 172.31.128.4:22
Exploit execution completed. Reverse shell session: session-1717000000
targetIp:172.31.128.4
targetUser:root
reverseShellActive:true
```

**TransferAgent StepResult logs format** (after transfer):

```
targetIp=172.31.128.4, probe: curl=available, wget=missing, centralReachable=true
Selected Path A (HTTP download)
Target download completed (exit 0)
Agent installed successfully
Health check: UP (attempt 1)
```

### TransferPath (Enumeration Concept)

Not a separate class — represented as log entries and the final `StepResult.success/failure` value.

| Value | Meaning |
|-------|---------|
| `HTTP_DOWNLOAD` | Path A: target downloaded binary from Central via curl/wget |
| `SCP_PUSH` | Path B: agent pushed binary to target via SCP |
| `BASE64_PIPE` | Path B fallback: agent pushed binary via base64 pipe over SSH |

### SshSessionProvisioner Verification Result

Not a persisted entity — runtime-only. The provisioner returns a simple boolean:

```
true  → SSH connectivity confirmed, session is usable
false → SSH connectivity failed, exploit step should be marked FAILED
```

The provisioner retries internally (3 attempts, 5s intervals) before returning `false`.

### Configuration Model (AgentConfig Extensions)

New properties added to the existing `AgentConfig` configuration class:

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `agent.exploit.default-target-user` | `String` | `root` | Fallback SSH user when exploit doesn't specify one |
| `agent.exploit.transfer-method` | `String` | `auto` | `auto` (probe), `http` (Path A only), `transfer` (Path B only) |
| `agent.exploit.transfer-method.retries` | `int` | `3` | Max retries for transfer operations |
| `agent.exploit.transfer-file-max-size-mb` | `int` | `100` | Max binary size in MB for base64 pipe fallback |

### Template Variables

#### install-agent-http.sh.tmpl

| Variable | Source | Description |
|----------|--------|-------------|
| `{{DOWNLOAD_URL}}` | ReplicationRequestResponse.downloadUrl | Central URL to download the agent binary |
| `{{CENTRAL_URL}}` | ReplicationRequestResponse.centralUrl | Central platform base URL |
| `{{PREAUTH_CODE}}` | ReplicationRequestResponse.preauthCode | Pre-authorization code for the new agent |

#### install-agent-transfer.sh.tmpl

| Variable | Source | Description |
|----------|--------|-------------|
| `{{CENTRAL_URL}}` | ReplicationRequestResponse.centralUrl | Central platform base URL |
| `{{PREAUTH_CODE}}` | ReplicationRequestResponse.preauthCode | Pre-authorization code for the new agent |

Note: The binary is already at `/tmp/agent` on the target when this template is executed (pushed by the agent before running the script).

### State Transitions

The transfer flow state machine:

```
TRANSFER_AGENT step starts
    │
    ├─→ Read EXECUTE_EXPLOIT context → build TargetSession
    ├─→ Read REQUEST_REPLICATION context → get downloadUrl, preauthCode, centralUrl
    │
    ├─→ Probe target tools (curl, wget, Central reachability)
    │       │
    │       ├─→ (curl OR wget) AND Central reachable → PATH A
    │       │       ├─→ Agent downloads binary from Central
    │       │       ├─→ Verify integrity (Blake3 + signature)
    │       │       │       ├─→ FAIL → FAILED (integrity)
    │       │       │       └─→ PASS → continue
    │       │       ├─→ Render install-agent-http.sh.tmpl
    │       │       ├─→ Transfer script to target (RemoteCommandExecutor.transferFile)
    │       │       ├─→ Execute script on target (RemoteCommandExecutor.execute)
    │       │       ├─→ Target download attempt
    │       │       │       ├─→ SUCCESS → Health check
    │       │       │       └─→ FAIL → Retry once → FAIL → fallback to PATH B
    │       │       └─→ Health check (up to 3 retries, 5s delay)
    │       │               ├─→ UP → SUCCESS
    │       │               └─→ Still down after retries → PARTIAL_SUCCESS
    │       │
    │       └─→ No tools OR Central unreachable → PATH B
    │               ├─→ Agent downloads binary from Central
    │               ├─→ Verify integrity (Blake3 + signature)
    │               │       ├─→ FAIL → FAILED (integrity)
    │               │       └─→ PASS → continue
    │               ├─→ Transfer binary to target (RemoteCommandExecutor.transferFile)
    │               │       ├─→ Try SCP → FAIL → Retry SCP → FAIL → Try base64 pipe
    │               │       └─→ All methods fail → FAILED (transfer)
    │               ├─→ Render install-agent-transfer.sh.tmpl
    │               ├─→ Transfer script to target
    │               ├─→ Execute script on target
    │               └─→ Health check (up to 3 retries, 5s delay)
    │                       ├─→ UP → SUCCESS
    │                       └─→ Still down after retries → PARTIAL_SUCCESS
    │
    └─→ Clean up temp files (binary copy, rendered scripts)
```

## Relationships

```
SshSessionProvisioner ──verifies──→ TargetSession
                                  │
ExecuteExploitStepHandler ──records──→ StepResult.logs (targetIp, targetUser, reverseShellActive)
                                  │
TransferAgentStepHandler ──reads──→ TargetSession (built from EXECUTE_EXPLOIT context)
TransferAgentStepHandler ──uses───→ RemoteCommandExecutor (execute + transferFile on target)
TransferAgentStepHandler ──uses───→ AgentHttpClient (download binary from Central)
TransferAgentStepHandler ──uses───→ BinaryIntegrityVerifier (verify binary before transfer)
TransferAgentStepHandler ──uses───→ ScriptTemplateService (render install templates)
WorkerPoolConfig ──wires──→ SshRemoteCommandExecutor, SshSessionProvisioner
WorkerCoordinator ──wires──→ StepHandlers with new dependencies
```

## Migration from 004

| Current (004) | New (005) |
|---------------|-----------|
| `install-agent.sh.tmpl` contains base64 blob | Split into `install-agent-http.sh.tmpl` (Path A) and `install-agent-transfer.sh.tmpl` (Path B, no blob) |
| `TransferAgentStepHandler` runs install script locally | `TransferAgentStepHandler` uses `RemoteCommandExecutor` to run install on target |
| `ExecuteExploitStepHandler` records `reverseShellSessionId` string | `ExecuteExploitStepHandler` verifies SSH and records `targetIp`, `targetUser`, `reverseShellActive` |
| `CommandExecutor` used for both local and "remote" | `CommandExecutor` remains local-only; `RemoteCommandExecutor` handles remote SSH execution |
| No session propagation | `TargetSession` flows from exploit to transfer step |
