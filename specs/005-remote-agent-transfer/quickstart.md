# Quickstart: Remote Agent Transfer

**Feature**: Remote Agent Transfer (005)
**Date**: 2026-06-02

## Prerequisites

- Java 17+
- Maven 3.8+
- The `agents/unix/` module compiles and tests pass for the base (004) feature
- SSH access to a test target machine for integration testing
- The Central platform (`api/`) running with the replication endpoints from spec 004

## Configuration

Add these properties to `agents/unix/src/main/resources/application.properties`:

```properties
# Existing properties
agent.central-url=http://localhost:8080
agent.api-key=${AGENT_API_KEY:}
agent.agent-id=${AGENT_ID:}
agent.central-public-key=${CENTRAL_PUBLIC_KEY:}

# New exploit/transfer properties (REQ-8)
agent.exploit.default-target-user=root
agent.exploit.transfer-method=auto
agent.exploit.transfer-method.retries=3
agent.exploit.transfer-file-max-size-mb=100
```

### Property Reference

| Property | Default | Description |
|----------|---------|-------------|
| `agent.exploit.default-target-user` | `root` | SSH user when exploit doesn't specify one |
| `agent.exploit.transfer-method` | `auto` | `auto` (probe+choose), `http` (Path A only), `transfer` (Path B only) |
| `agent.exploit.transfer-method.retries` | `3` | Max retries for each transfer operation |
| `agent.exploit.transfer-file-max-size-mb` | `100` | Max binary size for base64 pipe fallback |

## Build

```bash
cd agents/unix
./mvnw clean compile
```

For GraalVM native image (macOS):

```bash
./mvnw -Pnative native:compile -DmainClass=com.spulido.agent.AgentApplication
```

## Test

### Unit Tests

```bash
cd agents/unix
./mvnw test
```

Tests cover:
- `SshRemoteCommandExecutorTest`: execute and transferFile with mocked ProcessBuilder
- `TransferAgentStepHandlerTest`: Path A, Path B, auto-fallback scenarios

### Integration Test (Path A)

Prerequisites:
1. Central running on `http://localhost:8080`
2. Target machine with SSH access and `curl` installed
3. Project configured with `AUTO_APPROVE` replication policy

```bash
# Start the agent (it will poll Central for a plan)
cd agents/unix
./mvnw spring-boot:run
```

Expected: The agent polls Central, receives a plan, executes exploit → requests replication → downloads binary from Central → target downloads binary via curl → agent launches on target → health check passes.

### Integration Test (Path B)

Same as Path A but block the target's network access to Central (e.g., via iptables):

```bash
# On the target machine
sudo iptables -A OUTPUT -d <central-ip> -j DROP
```

Expected: Agent probes target tools → Central unreachable → falls back to Path B → agent pushes binary via SCP/base64 → agent launches on target → health check passes.

## File Layout After Implementation

```
agents/unix/src/main/java/com/spulido/agent/
├── remote/
│   ├── RemoteCommandExecutor.java       # NEW
│   ├── SshRemoteCommandExecutor.java    # NEW
│   ├── TargetSession.java               # NEW
│   └── SshSessionProvisioner.java       # NEW
├── config/
│   ├── AgentConfig.java                 # MODIFIED (add exploit properties)
│   └── WorkerPoolConfig.java            # MODIFIED (wire new beans)
├── worker/
│   ├── WorkerCoordinator.java           # MODIFIED (update handlers)
│   └── step/
│       ├── ExecuteExploitStepHandler.java  # MODIFIED (add provisioner)
│       └── TransferAgentStepHandler.java   # REWRITTEN
└── resources/
    └── scripts/
        ├── install-agent-http.sh.tmpl       # NEW
        └── install-agent-transfer.sh.tmpl   # RENAMED (was install-agent.sh.tmpl)

agents/unix/src/test/java/com/spulido/agent/
├── remote/
│   └── SshRemoteCommandExecutorTest.java    # NEW
└── worker/step/
    └── TransferAgentStepHandlerTest.java    # NEW
```

## Verification Checklist

- [ ] `./mvnw compile` passes with no errors
- [ ] `./mvnw test` passes with all unit tests
- [ ] `agents/unix/src/main/resources/scripts/install-agent.sh.tmpl` no longer exists (renamed)
- [ ] `agents/unix/src/main/resources/scripts/install-agent-http.sh.tmpl` exists
- [ ] `agents/unix/src/main/resources/scripts/install-agent-transfer.sh.tmpl` exists and contains no base64 blob
- [ ] `AgentConfig` has `getDefaultTargetUser()`, `getTransferMethod()`, `getTransferMethodRetries()`, `getTransferFileMaxSizeMb()`
- [ ] `WorkerPoolConfig` wires `SshRemoteCommandExecutor` and `SshSessionProvisioner` beans
- [ ] `ExecuteExploitStepHandler` records `targetIp`, `targetUser`, `reverseShellActive` in logs
- [ ] `TransferAgentStepHandler` probes tools, selects path, executes transfer, runs health check
- [ ] No base64 binary data in log output at INFO level
- [ ] No SSH credentials in log output at INFO level
