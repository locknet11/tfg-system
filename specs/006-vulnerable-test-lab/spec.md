# Feature Specification: Vulnerable Test Lab

**Feature Branch**: `006-vulnerable-test-lab`
**Created**: 2026-06-02
**Status**: Draft
**Input**: User description: "Entorno Local de Pruebas con 5 Targets Vulnerables"

## Clarifications

### Session 2026-06-02

- Q: Should the lab include auxiliary infrastructure for complex exploits (LDAP/HTTP servers)? → A: Replace Log4j and ActiveMQ targets with simpler alternatives that require only direct HTTP payloads (Tomcat PUT RCE CVE-2017-12617 + ThinkPHP 5 RCE CVE-2018-20062).
- Q: How should exploitation success be validated when agent and target run on similar Linux systems? → A: Use a 3-layer fallback approach (L1: marker file at /tmp/agent_is_present; L2: environment fingerprint via ip a / hostname / proc/1/cgroup; L3: unique timestamped artifact at /tmp/pwned_<ts>_<nonce>). Each layer falls through to the next if unavailable.

## User Scenarios & Testing

### User Story 1 - Deploy Lab in One Step (Priority: P1)

A security researcher clones the repository and runs a single command to deploy the entire test lab. Five vulnerable targets become available on known ports, each pre-configured to run a specific known vulnerability. The researcher can immediately verify the lab is operational by checking each target's port.

**Why this priority**: One-click deployment is the fundamental value proposition — without it, the lab cannot be used for testing.

**Independent Test**: Can be fully tested by running the deploy command from a clean environment and confirming all five targets respond on their designated ports within 2 minutes.

**Acceptance Scenarios**:

1. **Given** a clean environment with the required runtime installed, **When** the deploy command is executed, **Then** all five targets become reachable within 2 minutes.
2. **Given** the lab is deployed, **When** a researcher accesses each web-based target through a browser, **Then** each target returns the expected service page.
3. **Given** the lab is deployed, **When** a researcher connects to each network-based target, **Then** each target accepts connections on its designated service endpoint.

---

### User Story 2 - Agent Discovery and Exploitation (Priority: P1)

An autonomous agent system scans the lab network (172.20.0.0/24), discovers each vulnerable target, identifies the vulnerability type, executes the appropriate exploit playbook, and reports results to the central API.

**Why this priority**: The lab exists to validate that autonomous agents can discover and exploit real vulnerabilities — this is the core validation use case.

**Independent Test**: Can be fully tested by running a single agent against a single target (e.g., the Flask SSTI target) and confirming the agent successfully detects and exploits the vulnerability.

**Acceptance Scenarios**:

1. **Given** the lab is deployed, **When** an agent scans the Drupal target, **Then** the agent identifies it as a Drupal instance vulnerable to CVE-2018-7600.
2. **Given** the lab is deployed, **When** an agent sends an SSTI payload to the Flask target, **Then** the vulnerable application executes the injected template expression.
3. **Given** the lab is deployed, **When** an agent uploads a JSP webshell to the Tomcat target via HTTP PUT, **Then** the webshell is accessible and confirms RCE via file upload.
4. **Given** the lab is deployed, **When** an agent sends a crafted HTTP request to the ThinkPHP target with an RCE payload, **Then** the target executes the injected code.
5. **Given** the lab is deployed, **When** an agent calls the Docker API target, **Then** the agent can perform privileged operations (create containers, mount filesystems) without authentication.

---

### User Story 3 - Stop and Reset Lab (Priority: P2)

A researcher finishes a test run and wants to restore the lab to a clean, known state. They run commands to stop all containers and then redeploy fresh instances, ensuring no state from the previous test run contaminates the next one.

**Why this priority**: Repeatability is critical for validating agent behavior — dirty state would produce false negatives or positives.

**Independent Test**: Can be fully tested by deploying the lab, making a change (e.g., creating a file inside a container), stopping, resetting, and confirming the file is gone.

**Acceptance Scenarios**:

1. **Given** the lab is deployed and running, **When** the stop command is executed, **Then** all containers are stopped and removed within 30 seconds.
2. **Given** a stopped lab, **When** the deploy command is executed again, **Then** all targets start fresh with no residual state from the previous run.
3. **Given** any lab state, **When** the reset command is executed, **Then** the lab is restored to its initial state (clean Docker images, no container state).

---

### User Story 4 - Verify Individual Target Vulnerabilities (Priority: P3)

A researcher wants to verify that a specific target is correctly configured before running the full agent pipeline. They manually interact with one target (e.g., sending a crafted HTTP request to the Drupal target) and confirm the vulnerability is exploitable.

**Why this priority**: Isolating infrastructure issues from agent logic issues saves debugging time.

**Independent Test**: Can be tested by running a known exploit payload against each target independently and observing the expected behavior.

**Acceptance Scenarios**:

1. **Given** the Drupal target is running, **When** a researcher sends a POST request to /user/register with a `mail[#markup]` payload, **Then** the command is executed and output is returned in the response.
2. **Given** the Flask target is running, **When** a researcher sends a GET request with an SSTI payload in the `name` parameter, **Then** the injected expression result appears in the rendered page.
3. **Given** the Tomcat target is running, **When** a researcher sends an HTTP PUT request with a JSP webshell payload, **Then** the file is written and accessible on the server.
4. **Given** the ThinkPHP target is running, **When** a researcher sends a crafted HTTP request with an RCE payload, **Then** the injected code executes and the result is observable in the response.

### Edge Cases

- What happens when the required container runtime is not installed? The deploy command should fail with a clear error message.
- What happens when the service ports required by targets are already in use on the host? The lab should fail gracefully with a port conflict message indicating which port is blocked.
- What happens when a target fails to start during deployment? The remaining targets should still start, and the failure should be reported so the operator knows which target is down.
- How does the system handle the special target that requires host-level access when that access is not available? It should fail with a clear message explaining the missing capability.
- What happens when the marker file `/tmp/agent_is_present` is accidentally present on a target (e.g., from a previous test run)? The L1 check should note the collision and fall through to L2/L3 rather than reporting a false negative.

## Requirements

### Functional Requirements

- **FR-001**: The lab MUST deploy a Drupal target vulnerable to CVE-2018-7600 (Drupalgeddon 2) accessible via web interface.
- **FR-002**: The lab MUST deploy an Apache Tomcat target vulnerable to CVE-2017-12617 (PUT method file upload to RCE) accessible via HTTP.
- **FR-003**: The lab MUST deploy a Python web application target with Jinja2 Server-Side Template Injection accessible via web interface.
- **FR-004**: The lab MUST deploy a ThinkPHP target vulnerable to CVE-2018-20062 (RCE via crafted HTTP request) accessible via web interface.
- **FR-005**: The lab MUST deploy a target exposing a container orchestration API without authentication, allowing remote container management.
- **FR-006**: The lab MUST provide a single deploy command that starts all five targets.
- **FR-007**: The lab MUST provide a stop command that cleanly shuts down all targets.
- **FR-008**: The lab MUST provide a reset command that restores all targets to their initial state.
- **FR-009**: All targets MUST be isolated on a dedicated network segment with fixed addresses, separate from the host network.
- **FR-010**: Agent systems on the host MUST be able to reach all targets on their designated service ports via localhost.

### Cross-Cutting Requirements

- **Internationalization**: User-facing commands and messages must be in English for consistency with the rest of the system.
- **Accessibility**: N/A (command-line tooling, no UI).
- **Validation and Error Handling**: All commands must validate that the required runtime environment is available before attempting operations. Failures must produce clear, actionable error messages indicating the root cause.
- **Security Constraints**: The lab runs intentionally vulnerable software for testing purposes only. It must NOT be deployed on production networks or exposed to the internet. A clear warning must be displayed upon deployment stating the security risks. The special target that can compromise the host requires elevated access — the deploy process must warn the operator about the security implications before proceeding.

- **FR-011**: Each exploited target MUST be validated using a 3-layer fallback technique to confirm RCE and distinguish the target from the agent machine:
  - **L1 - Identity check**: Agent places a marker file at `/tmp/agent_is_present` on its own machine. If the exploited target lacks this file, the agent is confirmed to be on a different system.
  - **L2 - Environment fingerprinting**: If L1 is inconclusive, use `ip a`, `hostname`, and `/proc/1/cgroup` to distinguish container network interfaces and Docker cgroup paths from the host environment.
  - **L3 - Unique artifact creation**: If L1 and L2 fail, create a timestamped proof file at `/tmp/pwned_$(date +%s_%N)` on the target and verify its existence. This also serves as an audit trail.
  - If one layer is unavailable or inconclusive, fall through to the next layer. This MUST NOT rely solely on `whoami` or similar commands that return identical output across agent and target.

### Key Entities

- **Vulnerable Target**: A sandboxed instance running intentionally vulnerable software (Drupal, Tomcat, Flask, ThinkPHP, or Docker API). Each target has a known CVE identifier and a designated service endpoint.
- **Lab Network**: An isolated network segment that connects all targets and is accessible from the host machine for agent interactions.
- **Deploy Command**: An orchestration operation that provisions all vulnerability sources, builds the target images, and starts all target instances.
- **Exploitation Validation Technique**: A method used by the agent to confirm it has successfully achieved RCE on a target, distinguishing target from origin machine.

## Success Criteria

### Measurable Outcomes

- **SC-001**: A user can go from a clean environment to a fully operational lab with all five targets reachable in under 3 minutes.
- **SC-002**: Each target accepts connections on its published port within 30 seconds of the deploy command completing.
- **SC-003**: The lab can be stopped and restarted to a clean state in under 1 minute total.
- **SC-004**: An autonomous agent can discover at least 4 of the 5 targets via network scanning and successfully identify the vulnerability type.
- **SC-005**: At least 3 of the 5 targets can be successfully exploited by an autonomous agent, with exploitation results reported correctly to the central API.
- **SC-008**: The layered validation approach (L1→L2→L3) correctly distinguishes target from agent machine with 100% accuracy across all 5 targets, with no false positives when run on the agent itself.
- **SC-006**: The lab can be reset and redeployed 10 consecutive times without any degradation or state leakage between runs.
- **SC-007**: A new developer with only Docker installed can deploy the lab and verify all targets are operational using the provided scripts without referring to external documentation.

## Assumptions

- A container runtime environment is available on the host machine.
- The service ports required by all five targets are available on the host.
- The target images will be sourced from a public vulnerability repository, requiring internet access for the initial setup.
- The lab is intended for local testing only and must not be exposed to any network beyond the host machine.
- The agent system runs on the host machine and accesses targets via the host's network interfaces.
- The most privileged target (Docker API) can compromise the host — this is expected behavior for testing purposes and operators must be aware before deploying.
- No authentication is required for any target — all vulnerabilities are unauthenticated by design.
- Agent systems will determine their own exploitation strategies; the lab only provides the vulnerable environment, not the exploit tooling.
- The exploitation validation approach (marker file, `ip a`, artifact creation) works on GNU/Linux systems. No assumption is made about support for other operating systems.

## Constitution Notes

- This feature creates a standalone test environment and does not modify existing application modules (UI, API, or agents).
- Existing project skills and conventions do not impose additional constraints on this feature as it is test infrastructure, not application code.
- Security warning banners must be displayed during deployment to prevent accidental production exposure.
