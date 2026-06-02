# Feature Specification: Remote Agent Transfer

**Feature Branch**: `005-remote-agent-transfer`  
**Created**: 2026-06-02  
**Status**: Draft  
**Input**: User description: "Remote Command Execution Layer and two-path agent transfer for agent auto-replication"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Remote Command Execution on Exploited Target (Priority: P1)

The agent must be able to execute shell commands on a target machine after the exploit step has successfully established SSH access. This is the foundational capability that enables all subsequent transfer operations — without it, no binary transfer can occur.

**Why this priority**: Remote execution is the prerequisite for both transfer paths; all downstream functionality depends on it.

**Independent Test**: Can be fully tested by provisioning SSH access to a target, then executing a simple command (e.g., `echo OK`) remotely and verifying stdout/stderr/exit code are captured correctly.

**Acceptance Scenarios**:

1. **Given** the exploit step has established SSH access to a target at IP 172.31.128.4 as user `root`, **When** the agent executes `echo HELLO` via the remote executor, **Then** stdout returns `HELLO` and exit code is 0.
2. **Given** SSH access exists, **When** the agent executes a command with a 10-second timeout and the command takes longer, **Then** the execution is terminated and a timeout error is returned.
3. **Given** SSH access exists, **When** the agent transfers a small text file to `/tmp/config.properties` with permissions `644`, **Then** the file exists on the target with the correct content and permissions.
4. **Given** SSH access does not exist (target unreachable), **When** a command is executed, **Then** the execution fails with a clear connectivity error (not a silent hang).

---

### User Story 2 - HTTP Binary Download to Target (Priority: P1)

When the exploited target has internet access to the Central platform and has download tools (curl or wget), the agent instructs the target to download the agent binary directly from Central via HTTP. This is the primary transfer path because it minimizes load on the parent agent and avoids pushing large binaries over SSH.

**Why this priority**: This is the preferred and most efficient transfer mechanism; it should be attempted first and handles the majority of real-world scenarios.

**Independent Test**: Can be tested by providing a target with curl/wget and network access to Central, running the transfer, and verifying the target downloads, configures, and launches the agent binary successfully.

**Acceptance Scenarios**:

1. **Given** the target has curl and can reach Central, **When** the agent initiates Path A transfer, **Then** the target downloads the binary via curl from Central within 30 seconds, the install script launches it, and a health check on the target's agent endpoint returns healthy status.
2. **Given** the target has wget (but not curl) and can reach Central, **When** the agent initiates Path A transfer, **Then** the target downloads the binary via wget and launches successfully.
3. **Given** the target has neither curl nor wget, **When** the agent probes the target, **Then** Path A is skipped and Path B is attempted instead.
4. **Given** the target has curl but cannot reach Central (health endpoint unreachable), **When** the agent probes connectivity, **Then** Path A is skipped and Path B is attempted instead.

---

### User Story 3 - Agent-Pushed Binary Transfer (Priority: P2)

When the target cannot download the binary directly from Central (no internet access to Central, or missing download tools), the agent must download the binary itself and push it to the target over the SSH channel. This fallback path ensures replication works even in restricted network environments.

**Why this priority**: This is the fallback transfer mechanism essential for targets in isolated network segments; without it, replication cannot complete in many real-world deployments.

**Independent Test**: Can be tested by blocking the target's access to Central, running the transfer, and verifying the agent pushes the binary via SCP or base64 pipe, configures, and launches it.

**Acceptance Scenarios**:

1. **Given** the target cannot reach Central, **When** the agent initiates Path B transfer, **Then** the agent downloads the binary from Central locally, pushes it to the target via SCP, and the binary launches successfully.
2. **Given** SCP transfer fails (e.g., no SCP on target), **When** the agent retries and SCP still fails, **Then** the agent falls back to base64 pipe transfer and the binary launches successfully.
3. **Given** the binary size exceeds the configured maximum for pipe transfer (100MB by default), **When** base64 pipe is attempted as last resort, **Then** the transfer is marked FAILED with a clear reason indicating binary too large.

---

### User Story 4 - Auto-Fallback Between Transfer Paths (Priority: P2)

The agent must automatically detect transfer failures and fall back from Path A to Path B without operator intervention. This ensures maximum resilience in dynamic network environments where conditions may change between probe and download.

**Why this priority**: Auto-fallback eliminates manual intervention during replication failures, which is core to the autonomous cybersecurity vision.

**Independent Test**: Can be tested by simulating a Path A download failure (e.g., Central returns 500 during download) and verifying automatic fallback to Path B completes the transfer.

**Acceptance Scenarios**:

1. **Given** Path A is initially selected (target has curl + Central reachable), **When** the target's download attempt fails with non-zero exit and the retry also fails, **Then** the agent automatically falls back to Path B and completes the transfer.
2. **Given** Path A is in progress, **When** Central becomes unreachable mid-transfer, **Then** the agent auto-fallbacks to Path B using the binary it already downloaded locally.
3. **Given** the replication token has expired during transfer, **When** the agent detects the expiry, **Then** a fresh replication token is requested and Path A restarts from the beginning.

---

### User Story 5 - Session Context Propagation (Priority: P3)

The exploit step must produce a verified, usable SSH session that the transfer step can consume. Currently the exploit step only records a session ID string with no actual connectivity — this must become a real, verified session with target IP, username, and confirmed SSH reachability.

**Why this priority**: Session propagation is the glue between exploit and transfer steps; without it, the two steps cannot communicate, but the logic is straightforward once the interfaces are defined.

**Independent Test**: Can be tested by running the exploit step, verifying it records structured connection info (targetIp, targetUser, SSH active status), then having the transfer step read that context and successfully build a session.

**Acceptance Scenarios**:

1. **Given** the exploit script completes successfully and prints `REVERSE_SHELL_READY`, **When** the session provisioner verifies SSH connectivity, **Then** the step result includes `targetIp`, `targetUser`, and `reverseShellActive: true`.
2. **Given** SSH verification fails after exploit (target unreachable after 3 retries with 5s intervals), **When** the exploit step completes, **Then** the step is marked FAILED with the reason documented.
3. **Given** the exploit establishes access under a known user (e.g., `root`), **When** the transfer step reads the exploit context, **Then** it constructs a valid session with targetIp and targetUser populated.

---

### User Story 6 - Integrity Verification Gate (Priority: P3)

Before any binary is transferred to or executed on a target, its integrity must be cryptographically verified. This is a non-negotiable security requirement that prevents compromised or tampered binaries from being deployed.

**Why this priority**: While critical for security, integrity verification is an existing capability that needs to be integrated into the new transfer flow rather than built from scratch.

**Independent Test**: Can be tested by providing a binary with an invalid cryptographic hash or invalid digital signature and verifying the transfer is blocked and the step marked FAILED.

**Acceptance Scenarios**:

1. **Given** the agent downloads a binary from Central, **When** the cryptographic hash of the downloaded binary does not match the expected hash, **Then** the transfer is immediately aborted and the step is marked FAILED.
2. **Given** the binary passes both hash and signature integrity checks, **When** the agent proceeds with transfer, **Then** the binary is not executed on the target until both verifications succeed.
3. **Given** integrity verification fails, **When** the step is marked FAILED, **Then** Central is notified of the integrity failure.

---

### Edge Cases

- What happens when the target has SSH but the connection drops mid-transfer? The transfer step retries with the configured retry count and escalates to FAILED if all attempts exhaust.
- How does the system handle a target that has curl installed but it's a broken/non-functional installation? The tool probe tests actual execution (`which curl && curl --version`), so a broken binary would fail the probe and skip Path A.
- What happens when the base64 pipe transfer hits OS command-line length limits? The system checks binary size against the configured maximum size limit and fails early with a clear message.
- What if the health check returns healthy but the agent process crashes seconds later? The step is marked PARTIAL_SUCCESS — the binary was installed but the health check couldn't confirm it within the retry window.
- What happens if the preauth code is intercepted during SSH transmission? The preauth code is transmitted only over the encrypted SSH channel, never over plain HTTP, to prevent interception.
- What happens to the parent agent's filesystem after transfer? All temporary files (binary copies, rendered scripts) are cleaned up after the transfer step completes, regardless of success or failure.
- How does the system handle a target that speaks SSH over a non-standard port? The current scope assumes standard port 22; non-standard ports are out of scope for v1.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a remote command execution capability that runs shell commands on an exploited target via SSH, capturing stdout, stderr, and exit code.
- **FR-002**: System MUST provide a remote file transfer capability that copies files to the target via SCP, with automatic fallback to base64-encoded pipe over SSH when SCP is unavailable.
- **FR-003**: System MUST verify SSH connectivity to the target after an exploit step completes, retrying up to 3 times with 5-second intervals before marking the step as failed.
- **FR-004**: System MUST record structured session information (target IP, target user, SSH reachability status) in the exploit step result for consumption by subsequent steps.
- **FR-005**: System MUST probe the target for available download tools (curl, wget) and Central platform reachability before selecting a transfer path.
- **FR-006**: System MUST preferentially instruct the target to download the agent binary directly from Central via HTTP when the target has download tools and network access (Path A).
- **FR-007**: System MUST download the agent binary locally and push it to the target over SSH when Path A is not viable (Path B).
- **FR-008**: System MUST automatically fall back from Path A to Path B when the target's download attempt fails, including one retry on Path A before falling back.
- **FR-009**: System MUST verify the cryptographic integrity (hash and signature) of every agent binary before transferring or executing it on any target.
- **FR-010**: System MUST perform a health check on the newly launched agent on the target after transfer, retrying up to 3 times with 5-second delays before concluding the step status.
- **FR-011**: System MUST request a fresh replication token and restart Path A from the beginning if the current token expires during transfer.
- **FR-012**: System MUST clean up all temporary files from the parent agent's filesystem after the transfer step completes, regardless of success or failure.
- **FR-013**: System MUST support configurable transfer behavior including: default target user, transfer method preference (auto/http/transfer), max retries, and max binary size for pipe transfer.
- **FR-014**: System MUST NOT log target SSH credentials or base64-encoded binary data at INFO or higher log levels.
- **FR-015**: System MUST transmit the pre-authorization code to the target only over the encrypted SSH channel.
- **FR-016**: System MUST mark the transfer as PARTIAL_SUCCESS (rather than FAILED) when the binary is installed but the health check cannot confirm it within the retry window.

### Cross-Cutting Requirements

- **Validation and Error Handling**: All remote commands must enforce timeouts; connectivity failures must produce clear error messages; retries must respect configured limits; unknown host keys are accepted only for exploit-established sessions.
- **Security Constraints**: Binary integrity (hash + signature) must be verified before any execution on any target; SSH credentials must never appear in log output above DEBUG level; base64-encoded binary data must be truncated in log output; pre-authorization codes must only travel over SSH.
- **Observability**: Each transfer path decision (why Path A or Path B was chosen) must be logged with sufficient context for post-mortem analysis; tool probe results must be recorded; integrity check results must be recorded; health check results with retry counts must be recorded.

### Key Entities

- **TargetSession**: Represents an established connection to an exploited target. Attributes: target IP address, target username, optional SSH identity file path. Created from exploit step output, consumed by transfer step.
- **StepResult**: The structured output of a workflow step execution. For exploit steps, includes session information (targetIp, targetUser, reverseShellActive). For transfer steps, includes transfer path used, probe results, integrity verification status, health check status.
- **TransferPath**: An enumeration capturing which transfer mechanism was used (HTTP download, SCP push, base64 pipe) and the outcome (success, failure, partial success).
- **ReplicationToken**: A single-use, time-limited (5-minute TTL) credential that authorizes binary download from Central. Expired tokens trigger automatic renewal.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Remote command execution completes with correct stdout/stderr/exit code in under 15 seconds for simple commands (e.g., `echo`, `which`).
- **SC-002**: Path A transfer (HTTP download) completes end-to-end — from transfer step start to healthy target agent — within 30 seconds.
- **SC-003**: Path B transfer (agent push) completes end-to-end within 60 seconds for binaries up to 50MB.
- **SC-004**: Auto-fallback from Path A to Path B adds no more than 15 seconds of additional latency beyond the Path B baseline.
- **SC-005**: 100% of transfer attempts are blocked when binary integrity verification fails (zero false negatives for integrity).
- **SC-006**: Session propagation success rate exceeds 95% when the exploit script correctly produces the `REVERSE_SHELL_READY` signal.
- **SC-007**: Transfer step leaves zero residual files (binary copies, rendered scripts) on the parent agent's filesystem after completion.

## Assumptions

- SSH access on the target is established by the exploit script (via key copy or equivalent mechanism) before the transfer step begins.
- The agent binary is a single self-contained executable file under 50MB — chunked transfer for larger binaries is out of scope.
- The Central platform serves agent binaries over HTTP with immutable content (cached at startup), so the agent and target receive identical bytes.
- Standard SSH port 22 is used; non-standard ports are out of scope for v1.
- The target operating system is Unix-like with standard tools (bash, base64) available.
- Windows target support is out of scope for v1.
- The exploit script is responsible for printing `REVERSE_SHELL_READY` on stdout upon establishing SSH access.
- A maximum of 3 connection retries (with 5s intervals) is sufficient to cover transient network issues without excessive delay.
- Replication tokens have a 5-minute TTL, which is sufficient for Path A but may require renewal for Path B with very large binaries.

## Out of Scope

- Multi-hop replication (agent replicating through multiple intermediate hosts)
- Cross-Central replication (agent from one Central instance replicating to a target managed by another)
- Binary chunking for files larger than 100MB
- Custom SSH key management UI — keys are provisioned by the exploit script
- Windows target support
- Non-standard SSH port support
