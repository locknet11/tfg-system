# Research: Remote Agent Transfer

**Feature**: Remote Agent Transfer (005)
**Date**: 2026-06-02

## Research Topics

### R-001: SSH Remote Command Execution from Java

**Decision**: Use `ProcessBuilder` to invoke the system `ssh` command directly, matching the existing `CommandExecutor` pattern.

**Rationale**:
- The agent already uses `ProcessBuilder` for local command execution (`CommandExecutor` lambda in `WorkerPoolConfig`).
- No SSH Java library (JSch, Apache SSHD) is in the dependency tree, and adding one would increase the GraalVM native image size and introduce GraalVM compatibility issues.
- The target SSH credentials (key file) are managed by the exploit script — the agent only needs to invoke `ssh -i <key> user@host 'command'`.
- ProcessBuilder is GraalVM-safe and well-understood in this codebase.

**Alternatives considered**:
- JSch: Adds dependency, GraalVM native-image compatibility uncertain, additional maintenance burden.
- Apache MINA SSHD: Heavy dependency, adds significant binary size, overkill for simple command execution.
- Runtime.exec(): ProcessBuilder is preferred for redirecting error streams and controlling timeouts.

**Implementation notes**:
- SSH command template: `ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 <user>@<ip> '<command>'`
- Optional identity file: `ssh -o StrictHostKeyChecking=no -o ConnectTimeout=10 -i <identityFile> <user>@<ip> '<command>'`
- Timeout enforced via `Process.waitFor(timeoutSeconds, TimeUnit.SECONDS)` with `destroyForcibly()` on timeout.
- stdout and stderr captured via `redirectErrorStream(true)`.

### R-002: SCP File Transfer and Base64 Pipe Fallback

**Decision**: Implement SCP as primary transfer method with base64 pipe as fallback, both via ProcessBuilder.

**Rationale**:
- SCP is the standard secure file copy mechanism for SSH and is available on all Unix targets with SSH.
- Base64 pipe (`echo '<base64>' | ssh ... 'base64 -d > /path'`) is a lightweight fallback when SCP is unavailable (e.g., `scp` binary missing, restrictive path).
- Both methods use the same SSH credentials already established by the exploit.
- No additional dependencies needed — `java.util.Base64` is part of the JDK.

**Alternatives considered**:
- HTTP file server: Would require the agent to run a temporary HTTP server on itself to serve the binary to the target — security risk (exposes binary on network) and complexity.
- rsync: Not universally available on targets; SCP is more ubiquitous.
- Custom SSH protocol implementation: Massive over-engineering for file transfer.

**Implementation notes**:
- SCP command: `scp -o StrictHostKeyChecking=no <localFile> <user>@<ip>:<remotePath>`
- Base64 pipe: `echo '<encoded>' | ssh -o StrictHostKeyChecking=no <user>@<ip> 'base64 -d > <remotePath>'`
- Binary size limit for pipe (configurable, default 100MB) checked before attempting base64 transfer.
- SCP retry once before falling back to base64; base64 failure is terminal.
- ProcessBuilder used for both methods; timeout enforced.

### R-003: Tool Probing via SSH

**Decision**: Probe the target for download tools (curl, wget) and Central reachability by executing SSH commands that test each tool.

**Rationale**:
- The simplest way to determine target capabilities is to try to run them and check exit codes.
- Each probe is a single SSH command, keeping the transfer decision fast.
- The probe results determine Path A vs Path B selection and are logged for observability.

**Alternatives considered**:
- Manifest-based capability declaration: Would require modifying the target agent startup to report capabilities — not available before transfer.
- Script-based probing: A single script that tests all tools and reports in one SSH call — considered but the individual probes are simpler to implement and debug individually.

**Implementation notes**:
- curl probe: `ssh target 'which curl && curl --version'` (exit 0 = available)
- wget probe: `ssh target 'which wget && wget --version'` (exit 0 = available)
- Central reachability: `ssh target 'curl -s --connect-timeout 5 <centralUrl>/actuator/health'`
- Decision matrix:
  - (curl OR wget) AND Central reachable → Path A
  - Otherwise → Path B

### R-004: Session Propagation Pattern

**Decision**: Pass session information through the existing `StepResult.logs` field (structured as `key:value` pairs) rather than adding new fields to `StepResult`.

**Rationale**:
- The existing `StepResult` has `services`, `scripts`, `logs`, and `success` — extending it with session-specific fields would bloat the class for all step handlers.
- The `logs` field is a `List<String>` already used for human-readable and machine-parseable messages.
- `TransferAgentStepHandler` already reads `EXECUTE_EXPLOIT` context logs to extract `targetIp` — this extends that pattern to include `targetUser` and `reverseShellActive`.
- Minimal change, maximum compatibility with existing code.

**Alternatives considered**:
- New `TargetSession` field on `StepResult`: Would require changing all `StepResult` factory methods and consumers. More type-safe but more invasive.
- Separate context map for session data: Adds complexity to the context propagation model.

**Implementation notes**:
- `ExecuteExploitStepHandler` records in logs:
  ```
  targetIp:172.31.128.4
  targetUser:root
  reverseShellActive:true
  ```
- `TransferAgentStepHandler` parses these from the `EXECUTE_EXPLOIT` context logs and builds a `TargetSession`.
- `SshSessionProvisioner.verify(String targetIp, String targetUser, String identityFile)` returns boolean.

### R-005: Template Strategy for Two Paths

**Decision**: Create two separate shell templates with distinct content rather than one parameterized template.

**Rationale**:
- Path A template (`install-agent-http.sh.tmpl`) contains download logic (curl/wget to Central).
- Path B template (`install-agent-transfer.sh.tmpl`) assumes the binary is already at `/tmp/agent` (transferred by the agent).
- Each template is ~10 lines, independently verifiable, and has no conditional branching.
- The existing single template (`install-agent.sh.tmpl`) is renamed to `install-agent-transfer.sh.tmpl` and stripped of the base64 blob.

**Alternatives considered**:
- Single template with `{{TRANSFER_METHOD}}` conditional: Would require bash `if/else` inside the template, making it harder to test and read.

**Implementation notes**:
- Template variables for Path A: `{{DOWNLOAD_URL}}`, `{{CENTRAL_URL}}`, `{{PREAUTH_CODE}}`
- Template variables for Path B: `{{CENTRAL_URL}}`, `{{PREAUTH_CODE}}`
- GraalVM-safe rendering: `ScriptTemplateService.renderTemplate()` already handles `{{KEY}}` replacement via `String.replace()`.

### R-006: Configuration Property Design

**Decision**: Add `agent.exploit.*` prefixed properties to `AgentConfig` and `application.properties`.

**Rationale**:
- Follows existing `agent.*` property prefix convention.
- `agent.exploit.default-target-user` maps to the fallback user when the exploit doesn't print one.
- `agent.exploit.transfer-method` controls auto/http/transfer selection.
- `agent.exploit.transfer-method.retries` controls max retry attempts.
- `agent.exploit.transfer-file-max-size-mb` controls the binary size limit for base64 pipe.

**Alternatives considered**:
- Hardcoded defaults: Less flexible for different deployment environments.
- Separate config file: Overkill for 4 properties; application.properties is sufficient.

## Resolved Unknowns

All technical unknowns from the feature specification have been resolved by the detailed requirements (REQ-1 through REQ-8). No outstanding NEEDS CLARIFICATION items remain.
