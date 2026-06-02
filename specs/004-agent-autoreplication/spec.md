# Feature Specification: Agent Autoreplication

**Feature Branch**: `004-agent-autoreplication`  
**Created**: 2026-06-01  
**Status**: Draft  
**Input**: User description: "PRD: Autoreplicación de Agentes - Extender el sistema actual para que un agent activo pueda detectar una vulnerabilidad explotable en un target no registrado, obtener autorización de Central (según política configurable), ejecutar el exploit, transferir su binario vía HTTP desde Central al target comprometido, auto-instalarse y registrarse como un nuevo agent gestionado."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Autonomous Agent Self-Replication (Priority: P1)

An active security agent scans its assigned targets, discovers a vulnerability on an unregistered host, obtains approval from Central according to the configured policy, executes the exploit to gain remote access, transfers its own binary to the compromised host, installs it, and verifies that the new agent registers successfully with Central as a managed entity. The original agent reports completion and continues its normal operation.

**Why this priority**: This is the core capability — without it, autonomous agent replication does not exist. It delivers the fundamental value proposition of automated lateral expansion of the security monitoring mesh.

**Independent Test**: Deploy a parent agent with scan targets, simulate a vulnerable service on an unregistered host, verify the entire chain from detection through new agent registration completes without human intervention (in AUTO_APPROVE mode).

**Acceptance Scenarios**:

1. **Given** a parent agent has completed SERVICE_SCAN and EXPLOITATION_KNOWLEDGE steps, identifying a viable exploit (script available, severity High or Critical) against an unregistered target, **When** the agent sends a replication request to Central, **Then** Central evaluates the replication policy and responds with APPROVED status, a replication token, and a download URL.
2. **Given** the agent received APPROVED status, **When** the agent executes the exploit script against the target, **Then** the agent establishes remote access (reverse shell) and validates connectivity to the target.
3. **Given** remote access is established, **When** the agent transfers the binary and its signed Blake3 hash to the target via the reverse shell, **Then** the binary integrity is validated using Central's public key before the binary is configured and launched on the target host successfully.
4. **Given** the new agent binary is running on the target host, **When** the new agent registers with Central using the provided credentials, **Then** Central creates the agent record, links it to the target, assigns an initial scan plan, records replication metadata (parent agent ID, timestamp, exploit used), and the system notifies the administrator.
5. **Given** replication completed successfully, **When** the parent agent reports the REPLICATE step status, **Then** Central records the step as COMPLETED and the parent agent continues with its normal plan.

---

### User Story 2 - Administrator Manages Replication Approvals (Priority: P2)

An administrator reviews pending replication requests from agents, evaluates the context (target, vulnerability, severity), and either approves or denies each request. The system records the decision for audit purposes.

**Why this priority**: Manual approval mode is the secure default for sensitive environments. Administrators need visibility and control over which targets get compromised and receive new agents, especially in production or multi-tenant environments.

**Independent Test**: Configure a project with MANUAL_APPROVE policy, trigger a replication request from an agent, verify the request appears in the admin UI, approve it, and confirm the agent proceeds with exploitation and replication.

**Acceptance Scenarios**:

1. **Given** a project has MANUAL_APPROVE replication policy, **When** an agent submits a replication request, **Then** the request is created with PENDING status, the administrator receives a notification, and the agent enters polling mode waiting for the decision.
2. **Given** a PENDING replication request exists, **When** an administrator views the replication requests list with filters for status and severity, **Then** all matching requests are displayed with target IP, vulnerability details, severity, requesting agent, and age.
3. **Given** an administrator reviews a PENDING request, **When** the administrator clicks Approve, **Then** the request status changes to APPROVED, the decision is logged with the administrator's identity and timestamp, and the polling agent receives the approval with replication token and download URL.
4. **Given** an administrator reviews a PENDING request, **When** the administrator clicks Deny, **Then** the request status changes to DENIED, the decision is logged, and the polling agent receives the denial and abandons the replication attempt.
5. **Given** a replication request has been PENDING beyond its expiration time, **When** the system checks the expiration, **Then** the request is automatically marked as EXPIRED and the polling agent receives an expired status response.

---

### User Story 3 - Administrator Configures Replication Policies (Priority: P3)

An administrator sets the replication approval mode for each project or organization, choosing between automatic approval (for non-production or lab environments) and manual approval (for sensitive or production environments). The administrator can also set severity thresholds and notification preferences.

**Why this priority**: Policy configuration enables the system to adapt to different risk profiles across projects. Without it, every environment would require the same level of human oversight, reducing efficiency.

**Independent Test**: Configure a project with AUTO_APPROVE policy and severity threshold of "High", trigger a replication request with Critical severity, and verify it is auto-approved. Change the policy to MANUAL_APPROVE and verify subsequent requests require manual intervention.

**Acceptance Scenarios**:

1. **Given** an administrator accesses project settings, **When** the administrator toggles the replication policy from MANUAL_APPROVE to AUTO_APPROVE and optionally sets a minimum severity threshold, **Then** subsequent replication requests for that project are evaluated according to the new policy.
2. **Given** AUTO_APPROVE policy with severity threshold "Critical", **When** an agent submits a replication request with severity "High", **Then** the request falls back to MANUAL_APPROVE behavior because it does not meet the threshold.
3. **Given** the administrator enables replication notifications, **When** any replication request is created or a new agent registers via replication, **Then** the administrator receives a notification regardless of the approval mode.

---

### Edge Cases

- **What happens when the exploit execution fails?** The agent retries the exploit up to a configured number of attempts (default: 2). After all retries fail, the step is marked as FAILED, the replication request (if already approved) is marked with a failure note, and the agent continues with its next plan step. The administrator is notified of the failure.
- **What happens when the binary download from Central fails?** The agent retries the download up to a configured number of attempts. If all retries fail, the step is marked as FAILED and the administrator is notified. The reverse shell session is terminated.
- **What happens when the replication token expires before the agent uses it?** The download endpoint returns an expired-token error. The agent must request a new replication request (generating a new token). Expired tokens cannot be reused.
- **What happens if the target host already has a registered agent?** Before submitting the replication request, the agent queries Central to check if the target IP is already associated with an active agent. If so, the agent skips replication and reports the target as already covered.
- **What happens if Central is unreachable during replication request polling?** The agent implements exponential backoff for polling (starting at 10s, max 5 minutes). If Central remains unreachable beyond a timeout (default: 30 minutes), the agent abandons the request and marks the step as FAILED.
- **What happens when multiple agents detect the same vulnerable target simultaneously?** Central detects duplicate replication requests (same target IP + same exploit) and processes only the first one, returning "DUPLICATE" status to subsequent requests so other agents skip replication.
- **What happens if the new agent crashes shortly after registration?** Central detects the agent heartbeat timeout (existing mechanism) and marks the agent as OFFLINE. The administrator is notified. The replication record remains for audit.
- **What happens when replication is attempted against a target outside the authorized IP range?** Central validates the target IP against the project's authorized network ranges and denies the request if the target is outside scope.
- **What happens when binary integrity validation fails?** The agent discards the downloaded binary and hash, reports the integrity failure to Central, and the step is marked as FAILED. The administrator is notified. The replication request may be retried with a fresh token and download.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow an active agent to detect exploitable vulnerabilities (CVSS severity High or Critical with available exploit scripts) on unregistered target hosts and initiate a replication request.
- **FR-002**: The system MUST support a replication request flow where the agent sends target and vulnerability details to Central and receives an approval status (APPROVED, PENDING, DENIED) along with a secure, time-limited replication token and binary download URL when approved.
- **FR-003**: Central MUST evaluate each replication request against the configured replication policy for the project or organization, determining whether to auto-approve or require manual administrator approval.
- **FR-004**: Administrators MUST be able to configure replication policy per project, choosing between automatic approval (AUTO_APPROVE) and manual approval (MANUAL_APPROVE), with an optional minimum severity threshold for auto-approval.
- **FR-005**: Administrators MUST be able to view all replication requests in a dedicated interface, filterable by status (PENDING, APPROVED, DENIED, EXPIRED) and severity, and take approve/deny actions on PENDING requests.
- **FR-006**: The system MUST allow the approved agent to execute the exploit script, establish remote access to the target via a reverse shell, and validate connectivity before proceeding with binary transfer.
- **FR-007**: Central MUST serve the agent binary through a secured, token-authenticated endpoint that is time-limited (5-minute TTL after approval) and does not require additional authentication beyond the replication token. Central MUST also serve a signed Blake3 hash manifest for the binary at the same endpoint scope, enabling integrity verification by the downloading agent.
- **FR-008**: The approved agent MUST be able to transfer, configure, and launch the agent binary on the compromised target via the established reverse shell, injecting runtime configuration (Central URL, pre-authorization code) post-download.
- **FR-009**: The newly installed agent MUST register with Central using the existing registration endpoint, and Central MUST create an agent record that includes replication metadata: parent agent identifier, replication timestamp, and the exploit used.
- **FR-010**: Central MUST automatically assign an initial scan plan (SYSTEM_SCAN → SERVICE_SCAN → EXPLOITATION_KNOWLEDGE) to the newly replicated agent upon successful registration.
- **FR-011**: The system MUST notify administrators when a new agent is registered via autoreplication, regardless of the configured approval mode.
- **FR-012**: The system MUST maintain a complete audit trail for every replication event, including: who approved/denied (if manual), timestamps for each phase, exploit details, target information, and final outcome.
- **FR-013**: Replication requests that remain PENDING beyond their configured expiration time MUST be automatically marked as EXPIRED and the polling agent notified.
- **FR-014**: The parent agent MUST remain operational and continue its assigned plan after completing a replication, regardless of whether the replication succeeded or failed.
- **FR-015**: The system MUST detect and prevent duplicate replication attempts against the same target with the same exploit, returning an appropriate status to subsequent requesting agents.
- **FR-016**: The binary served for replication MUST be a generic build that contains no embedded configuration, with all instance-specific settings injected at installation time on the target.
- **FR-017**: Central MUST compute a Blake3 cryptographic hash of the agent binary and sign it with its private key before serving. The downloading agent MUST verify the binary integrity by validating the signed hash against the binary using Central's public key before executing the binary on the target.

### Cross-Cutting Requirements

- **Internationalization**: All administrator-facing UI text (replication requests page, policy settings, agent list badges, notifications) MUST be authored in English following the project's existing i18n conventions.
- **Accessibility**: The replication requests management page MUST support keyboard navigation, proper ARIA labels for approve/deny actions, and readable status indicators. The replication badge on agent lists MUST have accessible tooltip text.
- **Validation and Error Handling**: Central MUST validate all replication request payloads (target IP format, severity values, required fields). The agent MUST handle network failures during request submission, polling, and binary download with retry and exponential backoff. All failure states MUST be logged and surfaced to administrators.
- **Security Constraints**: The replication token MUST be a cryptographically random UUID with a maximum 5-minute lifetime. The binary download endpoint MUST NOT expose agent binaries without a valid, non-expired replication token. The pre-authorization code MUST be generated server-side and transmitted only to the parent agent over the authenticated channel. All replication audit logs MUST be immutable after creation. The agent binary MUST NOT be cached or stored by the parent agent. The agent binary integrity MUST be protected via Blake3 hashing and PKI signing: Central holds a private key for signing; agents hold the corresponding public key for verification. The binary MUST NOT be executed on any target without passing integrity validation. The signed hash file MUST be transmitted alongside the binary through the same token-authenticated channel.

### Key Entities *(include if feature involves data)*

- **Replication Request**: Records a request from an agent to self-replicate onto a target. Tracks the parent agent, target IP/port, vulnerability details (CVE ID, severity, service info), current status (PENDING/APPROVED/DENIED/EXPIRED), the secure replication token, download URL, applicable policy, approver identity (if manual), and lifecycle timestamps (created, expires, resolved).
- **Replication Policy**: Defines the approval behavior for a project or organization. Contains the approval mode (AUTO_APPROVE or MANUAL_APPROVE), an optional minimum severity threshold for auto-approval, and a notification flag.
- **Agent** (extended): The existing agent entity gains optional replication provenance fields: the identifier of the parent agent that replicated it, the timestamp of replication, and a reference to the exploit used for replication.
- **Replication Audit Entry**: An immutable log record for each replication lifecycle event (request created, policy evaluated, approval/denial decision, exploit execution started/completed, binary transferred, new agent registered, failure). Includes actor identity, timestamp, and event type.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An agent can complete the full self-replication cycle (from vulnerability detection to new agent registration) in under 3 minutes when operating in AUTO_APPROVE mode with a reachable target.
- **SC-002**: Administrators can view and act on replication requests within 2 minutes of receiving a notification, with the management interface loading the requests list in under 3 seconds for up to 500 requests.
- **SC-003**: The system correctly evaluates replication policies for 100% of incoming requests, with no policy bypasses or incorrect mode applications during validation testing.
- **SC-004**: The new agent binary starts and successfully registers with Central within 30 seconds of launch on the target host in 95% of replication attempts.
- **SC-005**: Every replication event (successful or failed) produces a complete, immutable audit trail with all required fields (who, what, when, outcome) with zero missing entries in audit logging tests.
- **SC-006**: The system prevents 100% of duplicate replication attempts against the same target-exploit pair, correctly identifying and rejecting subsequent requests.
- **SC-007**: Network or service disruptions during the replication process result in clean failure handling (logged, notified, no orphaned state) in 100% of test scenarios, with the parent agent resuming normal operation.
- **SC-008**: 100% of binary integrity validation failures result in the agent refusing to execute the binary, with the failure logged and the administrator notified within 30 seconds.

## Clarifications

### Session 2026-06-01

- Q: How should binary integrity be validated during replication? → A: Blake3 hashing with PKI signing — Central signs the binary hash with its private key; agents verify using Central's public key before execution.

## Assumptions

- The agent binary (GraalVM native image or JAR fallback) is already compiled and available for Central to serve through the download endpoint. Binary compilation is a build/deployment concern, not a feature concern.
- A real CommandExecutor implementation exists or will be created as a prerequisite dependency, enabling actual command execution on agent hosts.
- The reverse shell mechanism uses a well-known protocol (SSH or equivalent) that is available on target hosts; the specific protocol choice is an implementation detail that does not change the feature's functional requirements.
- The existing agent registration endpoint (POST /api/agent/{orgId}/{projId}/{targetId}) supports the pre-authorization code mechanism and can be reused without modification for replicated agents.
- The existing agent heartbeat monitoring and alerting system is functional and can be extended to include replication-specific notifications.
- Network connectivity between agents and Central, and between the parent agent and the target host, is generally reliable with transient failures handled by retry logic.
- Target hosts are assumed to have standard shell utilities (curl or wget, chmod) available; the absence of these tools would cause a replication failure that is logged and surfaced to the administrator.
- This feature focuses on replication within a single Central deployment. Cross-Central or multi-cluster replication is out of scope for v1.
- The exploit scripts provided by the EXPLOITATION_KNOWLEDGE step are assumed to be functional and appropriate for the target service/version. Validation of exploit correctness is the responsibility of that step, not the replication feature.
- Central has a public/private key pair loaded as environment configuration. The agent binary shipping process includes Blake3 hash computation and private-key signing as a build/deployment step. Agents are provisioned with Central's public key at installation time.

## Constitution Notes

- Repository guidance from AGENTS.md applies: Angular 17 for UI, Spring Boot 3 for API, GraalVM native for agents.
- Skills in `.agents/skills/` (java-springboot, angular-component) provide implementation patterns for the stack.
- The existing plan at `specs/003-exploitation-knowledge-request/plan.md` provides context on the exploitation knowledge step that feeds into the replication flow.
- Binary serving from Central and pre-authorization code generation are new capabilities that must be designed with security as a primary concern.
