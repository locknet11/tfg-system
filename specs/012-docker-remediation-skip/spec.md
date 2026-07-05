# Feature Specification: Docker Container Remediation Skip

**Feature Branch**: `012-docker-remediation-skip`  
**Created**: 2026-07-05  
**Status**: Draft  
**Input**: User description: "As a user, I want the agent to detect if it's actually running on a Docker container. If it's actually inside a Docker container, Then it shouldn't Proceed with the remediation step, must skip it and proceed with the next step (if exists). Also In the report She'll specify why it skipped the remediation step."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Agent Detects Docker Environment and Skips Remediation (Priority: P1)

The agent, before executing a remediation step in its plan, inspects its runtime environment to determine whether it is running inside a Docker container. If it detects a Docker container environment, the agent skips the remediation step entirely — it does not attempt to apply any fixes, restart services, or modify system packages — and instead proceeds to the next step in its execution plan (if one exists).

This is critical because remediation actions performed inside a Docker container are ineffective and potentially harmful: containers are ephemeral, lack a full init system, and changes made inside a container do not persist or affect the actual host system. Attempting remediation inside a container wastes resources and may inadvertently disrupt the containerized application.

**Why this priority**: This is the core safety mechanism. Without it, agents deployed inside containers could attempt destructive or ineffective remediation actions, undermining the reliability of the autonomous security system. It must be implemented before or alongside any remediation capability.

**Independent Test**: Can be fully tested by deploying an agent inside a Docker container with a plan that includes a remediation step, executing the plan, and verifying that the remediation step is skipped with a clear reason logged — delivering safe no-op behavior in containerized environments.

**Acceptance Scenarios**:

1. **Given** an agent is running inside a Docker container and its execution plan includes a remediation step, **When** the agent reaches the remediation step, **Then** the agent detects the Docker environment, skips the remediation step entirely without executing any fix commands, and transitions to the next step in the plan (or marks the plan complete if no further steps exist).

2. **Given** an agent is running on a bare-metal host or virtual machine (not inside a container), **When** the agent reaches a remediation step, **Then** the agent proceeds with normal remediation execution — the Docker check does not interfere with legitimate remediation on real hosts.

3. **Given** an agent is running inside a Docker container and the remediation step is the last step in the plan, **When** the agent skips remediation, **Then** the plan completes with a PARTIAL status (not SUCCESS, not FAILED) and the skip reason is documented.

---

### User Story 2 - Skip Reason Is Documented in the Remediation Report (Priority: P1)

When the agent skips a remediation step due to Docker container detection, it must generate a clear, human-readable explanation in the remediation report sent to the central platform. The security operator reviewing the report must immediately understand *why* remediation was skipped — without needing to inspect agent logs or guess at the cause.

**Why this priority**: Transparency and auditability are essential. Operators need to trust that remediation was skipped for a valid reason, not due to a bug or misconfiguration. The report is the primary artifact the operator uses to assess the security posture of managed targets.

**Independent Test**: Can be fully tested by deploying an agent in a Docker container, running a plan with remediation, and inspecting the remediation report on the central platform — verifying the report clearly states the Docker detection skip reason with the specific evidence used.

**Acceptance Scenarios**:

1. **Given** an agent inside a Docker container skips a remediation step, **When** it generates the remediation report, **Then** the report includes a `skipReason` field with a value such as "Docker container detected — remediation skipped to avoid ineffective or destructive changes in an ephemeral environment" and the status is set to "skipped".

2. **Given** an agent inside a Docker container skips remediation, **When** the report is persisted on the central platform and viewed by a security operator, **Then** the remediation detail view shows the skip reason prominently, alongside the Docker detection evidence (e.g., which detection method was used and what was found).

3. **Given** an agent on a real host performs remediation successfully, **When** the report is generated, **Then** the report does NOT include a skip reason — the status is "success" or "failed" based on actual remediation outcome.

---

### User Story 3 - Agent Proceeds to Next Plan Step After Skipping (Priority: P2)

After the agent skips a remediation step due to Docker detection, it must continue executing the remaining steps in its plan rather than halting or failing the entire plan. The skip is a deliberate, safe decision — not an error — and the agent should complete as much useful work as possible (e.g., subsequent scanning, reporting, or non-remediation steps).

**Why this priority**: Plan continuity ensures that Docker-deployed agents still provide value (e.g., vulnerability scanning and reporting) even though they cannot perform remediation. Without this, a single skip would abort the entire plan, losing valuable diagnostic data.

**Independent Test**: Can be fully tested by deploying an agent in a Docker container with a plan containing: scan → remediation → report steps, and verifying that after skipping remediation, the agent still executes the report step — delivering partial but useful results.

**Acceptance Scenarios**:

1. **Given** an agent in a Docker container has a plan with steps [scan, remediation, report], **When** the agent skips the remediation step, **Then** it proceeds to execute the report step normally and the plan execution completes with PARTIAL status.

2. **Given** an agent in a Docker container has a plan with steps [remediation, scan], **When** the agent skips the remediation step, **Then** it proceeds to execute the scan step and reports scan results normally — the plan does not abort.

3. **Given** an agent in a Docker container has a plan where the remediation step is the only step, **When** the agent skips it, **Then** the plan completes immediately with PARTIAL status and the skip reason is reported to the central platform.

---

### Edge Cases

- What happens when Docker detection is ambiguous or the detection mechanism fails (e.g., missing `/proc` filesystem)? The agent MUST default to the safe behavior: if it cannot conclusively determine whether it is inside a container, it MUST skip remediation rather than risk performing destructive actions. The report must indicate that detection was inconclusive and remediation was skipped as a precaution.

- What happens when the agent is running in a non-Docker container runtime (Podman, containerd, LXC)? The agent SHOULD detect common container runtimes beyond just Docker. If a container is detected via any supported method, remediation is skipped regardless of the specific container engine. The report should indicate which runtime was detected or, if the runtime is unrecognized, state "container environment detected" generically.

- What happens with nested containers (Docker-in-Docker)? The agent detects the innermost container it is running in and skips remediation. The report should note the container environment but does not need to distinguish nesting depth.

- What happens when the agent is running as a privileged container with access to the host's init system? The agent MUST still skip remediation. Privileged containers remain ephemeral and container-based; the presence of Docker evidence overrides any access-level considerations. This is the safest default.

- What happens when the central platform receives a remediation report with "skipped" status for a target the operator expected to be remediated? The report clearly documents the skip reason. The operator can see in the remediation history that the target was skipped due to container detection. No alert is triggered for skipped remediations (they are intentional, not failures).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The agent MUST inspect its runtime environment before executing any remediation step to determine whether it is running inside a container (Docker or compatible container runtime).

- **FR-002**: The agent MUST detect Docker containers using at least the following standard indicators: presence of `/.dockerenv` file, Docker-specific entries in `/proc/1/cgroup`, and absence of a full init system (PID 1 not being systemd or init).

- **FR-003**: When the agent conclusively detects it is running inside a container, it MUST skip the remediation step entirely — no fix commands, service restarts, or configuration changes are executed.

- **FR-004**: When the agent cannot conclusively determine its runtime environment (detection is inconclusive), it MUST default to skipping remediation as a safety precaution.

- **FR-005**: The agent MUST generate a remediation report record with status "skipped" and a `skipReason` field that documents why remediation was skipped. The reason must be human-readable and include the specific evidence used for detection.

- **FR-006**: The skip reason in the remediation report MUST distinguish between: (a) "Docker container detected" (conclusive detection), (b) "Container environment detected" (non-Docker container runtime detected), and (c) "Container detection inconclusive — remediation skipped as precaution" (unable to determine).

- **FR-007**: After skipping a remediation step, the agent MUST proceed to execute the next step in its execution plan (if any). Skipping remediation MUST NOT abort the entire plan execution.

- **FR-008**: When a remediation step is skipped and no further steps exist in the plan, the plan execution MUST complete with a PARTIAL status and the skip reason MUST be reported to the central platform.

- **FR-009**: The remediation report with "skipped" status MUST be persisted on the central platform and visible to security operators through the existing remediation history and detail views.

- **FR-010**: System MUST NOT trigger alerts or notifications for remediation records with "skipped" status — skipped remediations are intentional safety decisions, not failures requiring operator attention.

- **FR-011**: The Docker container detection MUST execute quickly (no network calls, no external dependencies) and MUST NOT add more than 1 second of overhead to the remediation step execution time.

### Cross-Cutting Requirements

- **Internationalization**: All skip reason messages must be authored in English, following the existing convention. Human-readable reason strings must be localizable.

- **Validation and Error Handling**: If the detection mechanism encounters an error (e.g., cannot read `/proc` filesystem due to permissions), the agent must treat this as inconclusive detection and skip remediation safely. The error must be logged at DEBUG level for diagnostics but the user-facing report must show the precautionary skip reason.

- **Security Constraints**: The detection mechanism reads only local filesystem artifacts (`/.dockerenv`, `/proc/1/cgroup`, `/proc/1/cmdline`). It must not execute any commands beyond reading these files. It must not make network requests for detection purposes.

- **Observability**: The detection result (which indicators were found/not found) must be logged at INFO level for audit purposes. The detection mechanism and its outcome must be visible in agent execution logs.

### Key Entities

- **Remediation Report (extended)**: The existing remediation report entity gains a new `skipReason` field. When remediation is skipped, `status` is "skipped" and `skipReason` contains the human-readable explanation along with detection evidence. Existing fields (`target`, `agent`, `cveId`, `timestamps`) remain unchanged.

- **Container Detection Result**: An internal-only structure produced by the detection mechanism before remediation. Contains: a boolean indicating whether a container was detected, the detection method(s) that triggered, and a confidence level (conclusive, inconclusive). This is used to decide whether to skip and to populate the report's skip reason.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Agents running inside Docker containers skip 100% of remediation steps without executing any fix commands — zero remediation actions are performed in containerized environments.

- **SC-002**: Remediation reports for skipped steps include a clear, human-readable skip reason in 100% of cases, visible to security operators through the existing remediation history and detail views.

- **SC-003**: Container detection completes in under 1 second and does not cause any measurable delay in plan execution beyond the remediation step.

- **SC-004**: When remediation is skipped, the agent proceeds to the next plan step (or completes the plan with PARTIAL status) in 100% of cases — no plan aborts due to Docker detection.

- **SC-005**: Agents running on bare-metal hosts or virtual machines (non-container environments) continue to execute remediation normally — the Docker detection check causes zero false-positive skips on real hosts.

- **SC-006**: Security operators can distinguish between "Docker detected", "container detected (non-Docker)", and "detection inconclusive" skip reasons in the remediation report within 5 seconds of viewing the detail page.

## Assumptions

- The agent has read access to `/.dockerenv`, `/proc/1/cgroup`, and `/proc/1/cmdline` on the target system. In the rare case these are inaccessible, the agent defaults to the safe behavior (skip remediation).

- Docker container detection via `/.dockerenv` and `/proc/1/cgroup` covers the vast majority of Docker deployments. Detection of other container runtimes (Podman, containerd) is a best-effort enhancement and not required for the initial implementation.

- The existing remediation report entity (`RemediationRecord`) can accommodate a new `skipReason` field without breaking existing consumers. The "skipped" status value is already defined or can be added to the existing status enumeration.

- The agent execution engine already supports conditional step execution or step skipping — or can be extended minimally to support it. The Docker detection is performed as a pre-condition check before the remediation step executes.

- Privileged containers are treated identically to unprivileged containers for remediation purposes. Even with elevated access, the container remains an ephemeral environment where remediation is ineffective.

- The detection mechanism is intentionally simple and filesystem-based. More sophisticated detection (e.g., analyzing kernel parameters, checking hypervisor signatures) is out of scope for this feature.

- No additional infrastructure or external services are required — detection uses only local filesystem artifacts already present on containerized Linux systems.

## Constitution Notes

- This feature spans the `agents/unix/` module (where the detection logic and skip behavior reside) and, to a lesser extent, the `api/` module (where the remediation report entity may need a new field). The `ui/` module may require minor updates to display the skip reason in existing remediation views.

- The agent execution engine (`agents/unix/`) already supports step-based plan execution with context propagation. The Docker detection is a pre-condition check that gates the remediation step — it fits naturally into the existing step pipeline.

- All detection logic must be GraalVM-native compatible (no reflection-based container detection libraries). Reading files via `java.nio.file.Files` is the recommended approach.

- The remediation report entity in `api/` uses MongoDB. Adding a `skipReason` field to the existing document is backward-compatible — existing records without the field remain valid.

- Open question: Should the Docker detection result be cached per agent session, or should it be re-evaluated before each remediation step? Caching per session is reasonable since the container environment does not change during an agent's lifetime, but re-evaluation adds negligible overhead and guards against edge cases.
