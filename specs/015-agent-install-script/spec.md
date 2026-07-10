# Feature Specification: Agent Self-Installing Shell Script

**Feature Branch**: `015-agent-install-script`  
**Created**: 2026-07-08  
**Status**: Draft  
**Input**: User description: "Right now the installation shell script for the agent only echoes the relevant info, but this must download from central the agent binary and run it in background in the target system. The agent binary receives configs that are in the script. So as user I want to be able to run the agent with the typical installation command (i.e: curl -sSL -X POST 'http://host.docker.internal:8080/api/agent/418J/H2QC/14SGQ?preauthCode=6apjydimyfhe' | bash) and show/echo the info actually is logging but also to download the binary and run it."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - One-Command Agent Installation via Curl Pipe Bash (Priority: P1)

An operator wants to install the security agent on a target Linux machine. They issue a single command — `curl -sSL -X POST "http://central/api/agent/{org}/{project}/{target}?preauthCode=xxx" | bash` — and the agent is automatically downloaded, verified, configured, and launched as a background process on the target. The operator sees informative output during the process but needs no further manual steps.

**Why this priority**: This is the core installation flow. Without it, operators must manually download the binary, configure it, and launch it — which is error-prone and time-consuming. The pipe-to-bash pattern is the industry-standard, frictionless way to install agents on remote machines.

**Independent Test**: Can be fully tested by running the curl-pipe-bash command against a test central platform targeting a clean Linux machine, then verifying the agent process is running in background with the correct configuration.

**Acceptance Scenarios**:

1. **Given** a registered target with a valid preauth code exists in the central platform, **When** the operator runs `curl -sSL -X POST "http://central/api/agent/{org}/{proj}/{target}?preauthCode=xxx" | bash`, **Then** the script outputs identification info (API URL, organization, project, target ID), downloads the agent binary from central, verifies its integrity, writes configuration with the correct central URL and preauth code, launches the agent in background, and prints a success confirmation with the agent's process ID.

2. **Given** the target machine has `curl` but not `wget`, **When** the operator pipes the install script to bash, **Then** the script uses curl to download the binary and proceeds with installation successfully.

3. **Given** the target machine has `wget` but not `curl`, **When** the operator pipes the install script to bash, **Then** the script uses wget to download the binary and proceeds with installation successfully.

4. **Given** the target machine has neither curl nor wget, **When** the install script executes, **Then** the script immediately fails with a clear error message ("Neither curl nor wget available — cannot download agent") and exits with a non-zero code, preventing a partial installation.

5. **Given** a valid target with an invalid or expired preauth code, **When** the operator runs the curl-pipe-bash command, **Then** the central platform returns an error script that clearly states the reason (e.g., "Invalid preauth code" or "Preauth code expired") and exits with a non-zero code.

---

### User Story 2 - Binary Integrity Verification During Installation (Priority: P2)

During the automated installation, the script verifies that the downloaded agent binary has not been tampered with or corrupted. The script computes a cryptographic hash of the downloaded binary and compares it against a manifest provided by the central platform. If verification fails, the installation is aborted to prevent running a compromised binary.

**Why this priority**: Security is a core concern for a cybersecurity agent. Running an unverified binary creates a risk of deploying compromised or corrupted software. However, the agent can still be deployed without integrity verification (with reduced security), so this is a P2 enhancement to the core P1 flow.

**Independent Test**: Can be tested by downloading a binary via the install script, verifying the hash check passes for a valid binary, and separately verifying that a deliberately corrupted binary causes the script to abort with a clear error message.

**Acceptance Scenarios**:

1. **Given** the central platform serves a valid agent binary with its manifest (containing the expected hash), **When** the install script downloads and verifies the binary hash, **Then** the script reports "Hash verification: OK" and proceeds with installation.

2. **Given** the downloaded binary has been corrupted or modified (hash mismatch), **When** the install script compares computed hash against the manifest, **Then** the script prints a "Hash mismatch" error, aborts the installation, and exits with a non-zero code.

3. **Given** the target machine does not have a supported hash tool (no openssl 3.0+ or b3sum), **When** the install script attempts verification, **Then** the script prints a warning that hash verification was skipped and proceeds with installation (non-blocking).

---

### User Story 3 - Agent Runs with Correct Configuration in Background (Priority: P2)

After the binary is downloaded and verified, the script writes a configuration file containing the connection details needed by the agent to communicate with the central platform, then launches the agent as a background (daemon) process. The agent starts successfully and can be seen running on the target machine.

**Why this priority**: The agent being correctly configured and running in background is essential for the agent to fulfill its purpose. However, this is part of the core success path of the P1 flow — separated here as a distinct concern for validation purposes.

**Independent Test**: Can be tested by running the full install script, then verifying via `ps` or process inspection that the agent is running in background, and verifying via the agent's log file or the central platform that the agent successfully connects using the provided configuration.

**Acceptance Scenarios**:

1. **Given** the agent binary has been downloaded and verified, **When** the script writes configuration and launches the agent, **Then** the agent process is running in background (detached from the terminal), and the script reports the agent's PID.

2. **Given** the agent launches with valid configuration (central URL and preauth code), **When** the agent starts, **Then** the central platform can see the agent come online and the agent logs confirm successful startup.

3. **Given** the agent binary fails to start (e.g., incompatible architecture), **When** the script launches the agent, **Then** the script detects the failure within a short timeout (2-3 seconds) and prints a warning with the log file location for troubleshooting.

---

### User Story 4 - Informative Progress Output During Installation (Priority: P3)

Throughout the installation process, the script outputs clear, human-readable status messages so the operator can follow what is happening and diagnose issues if something goes wrong. Each major step (startup info, downloading, verifying, configuring, launching) is clearly separated and reported.

**Why this priority**: Clear output improves operator confidence and debuggability, but the agent can install successfully even with minimal output. This is a quality-of-life enhancement for the P1 flow.

**Independent Test**: Can be tested by running the install script and verifying the output contains clearly labeled sections for each installation phase, with success/failure messages for each step.

**Acceptance Scenarios**:

1. **Given** the operator runs the installation command, **When** the script executes, **Then** the output shows: a header with target identification details, a "Downloading..." message with file size, a verification status message, a configuration and launch status message, and a final "Installation complete" or error message.

2. **Given** an installation step fails, **When** the error occurs, **Then** the script prints a clear error message describing what failed (not a stack trace or raw error code) and the installation steps that succeeded before the failure.

---

### Edge Cases

- What happens when the download URL is unreachable (central platform down or network issue)? The script must retry at least once with a brief delay, then fail with a clear "Cannot reach central platform" error.
- What happens when the downloaded file is empty (0 bytes)? The script must detect this before attempting verification or launch and fail with "Downloaded file is empty".
- What happens when `/tmp` is not writable? The script must detect this early and fail with a clear error suggesting an alternative or asking the operator to fix permissions.
- What happens when the target machine runs out of disk space during download? The script should detect incomplete downloads (file size mismatch) and report the issue.
- What happens when the same target runs the install command while an agent is already running? The script should detect an existing agent process (via PID file or process check) and either skip installation with a warning or stop the old agent and replace it, depending on configuration.
- What happens when the binary architecture doesn't match the target (e.g., x86_64 binary on ARM)? The launch attempt fails; the script should detect the failure within a short timeout and report it.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The install script served by the central platform API MUST download the agent binary from a URL provided within the script, without requiring the operator to manually specify any download location.
- **FR-002**: The central platform MUST generate a valid, time-limited download URL for the agent binary and embed it in the install script so that the binary can be downloaded without additional authentication.
- **FR-003**: The install script MUST detect whether `curl` or `wget` is available on the target system and use whichever is present for the download. If neither is available, the script MUST fail with a clear error message.
- **FR-004**: The install script MUST verify the integrity of the downloaded binary against a cryptographic hash (manifest) provided by the central platform. If hash verification fails, the installation MUST abort.
- **FR-005**: Hash verification MUST be non-blocking when the required tools are not available — the script SHOULD warn the operator but proceed with installation.
- **FR-006**: The install script MUST write a configuration file containing at minimum the central platform URL and the preauth code so that the agent can connect to and authenticate with the central platform.
- **FR-007**: The install script MUST launch the agent binary as a background (detached) process so that the operator's terminal session is not tied to the agent's lifetime.
- **FR-008**: The install script MUST check, within a short timeout after launch, whether the agent process started successfully, and report the PID on success or a warning with log location on failure.
- **FR-009**: The install script MUST continue to output the same identification information it currently shows (target ID, API URL, organization, project) before starting the download and installation process.
- **FR-010**: The install script MUST clean up temporary download artifacts after successful installation.
- **FR-011**: The error install script (served when registration fails) MUST continue to output a clear error message and exit with a non-zero code, unchanged from current behavior.
- **FR-012**: The install script MUST handle network failures during download gracefully — at minimum, retrying once before failing with a human-readable error.

### Cross-Cutting Requirements

- **Internationalization**: All output messages in the install script must be authored in English.
- **Security Constraints**: The binary download URL embedded in the script must not expose internal infrastructure details. The preauth code must be passed only via the configuration file, not echoed in clear text in the script output. Downloaded binaries must be verified against a cryptographic manifest before execution. The manifest (hash and optional signature) must be sourced from the central platform, not from an untrusted source.
- **Validation and Error Handling**: Every failure path must produce a clear, human-readable error message and exit with a non-zero code. Partial installations (where some steps succeed and others fail) must leave the system in a clean state, with temporary files removed where possible.

### Key Entities

- **Install Script**: The shell script returned by the central platform API that orchestrates the download, verification, configuration, and launch of the agent. Contains: download URL, manifest URL, central platform URL, preauth code, and target identification metadata.
- **Agent Binary Manifest**: A structured descriptor (e.g., JSON) containing at minimum the expected cryptographic hash of the agent binary. May also include a digital signature for additional verification. Served alongside or embedded within the download response.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An operator can install a running agent on a clean Linux target by executing a single curl-pipe-bash command, with no additional manual steps required.
- **SC-002**: The entire installation process (download, verify, configure, launch) completes in under 60 seconds on a target with a standard broadband connection.
- **SC-003**: 100% of binary integrity verification failures result in the installation being aborted (no compromised binary is executed).
- **SC-004**: The install script succeeds on any Linux target that has either curl or wget available, covering 99%+ of realistic target environments.
- **SC-005**: When installation succeeds, the agent is running in background and the central platform receives the agent's first heartbeat within 30 seconds of the script completing.

## Assumptions

- The agent binary is already built and accessible from the central platform via a URL that can be exposed to target machines (through the API's download endpoints, either public or token-protected).
- Target machines have either `curl` or `wget`, which are present on virtually all standard Linux distributions. Systems without either are considered out of scope for automated installation.
- The agent binary is compiled for the target's architecture (Linux x86_64); cross-platform binary distribution (macOS, ARM) may be addressed in a future feature.
- The central platform has an existing endpoint that can serve the agent binary with a short-lived token or preauth-code-based authorization, ensuring the binary download does not require operator authentication.
- The install script uses `/tmp` as the working directory, which is writable on all standard Linux systems.
- The existing agent binary supports reading configuration from a properties file (`agent.properties`) — this is the current convention and will remain unchanged.
- The preauth code mechanism already exists for target registration and authorization; this feature extends its use to secure the binary download phase.
- Hash verification uses Blake3 (as already implemented in the agent's existing install script template), with SHA256 or similar as a fallback when Blake3 tools are not available.

## Constitution Notes

- Repository guidance from `AGENTS.md` applies: all shell scripts and structured text must live in resource template files (`src/main/resources/scripts/*.ftl`). Never build scripts inline with `StringBuilder` or string concatenation.
- Stack: Angular 17 (UI), Spring Boot 3 + FreeMarker (API), anything relevant for `agents/unix/`.
- The `java-springboot` skill provides Spring Boot conventions for any new API endpoint needed to serve the binary download URL.
- The existing `unix.sh.ftl` FreeMarker template will be extended — no new template creation pattern is needed.
