# Feature Specification: Agent Tool Bundling

**Feature Branch**: `020-agent-tool-bundling`
**Created**: 2026-07-12
**Status**: Draft
**Input**: User description: "The unix agent needs tools to achieve the several possible plan steps, such as network scan, port scan, service scan, etc., as well as wget/curl for file transfer. Since the agent is delivered as a single compiled binary, it is necessary to bundle these tool binaries into the agent binary so it is self-contained and does not depend on tools being pre-installed on the target host. The goal of this feature is to have a fully functional unix agent that can successfully complete the steps of a remediation/exploitation plan end-to-end, without failing due to missing external tools on the target system."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reconnaissance steps produce real results on any target host (Priority: P1)

An operator assigns a plan to an agent that includes network discovery and port/service identification steps. Today these steps are placeholders that report success without doing any real work. With this feature, the agent performs the actual scan using tooling it carries with it, and reports real findings (live hosts, open ports, identified services) back to the platform — even on a freshly provisioned target host that has no scanning tools installed.

**Why this priority**: Reconnaissance steps are the foundation the rest of a plan (exploitation knowledge lookup, remediation) builds on. Without real results here, everything downstream operates on fabricated data.

**Independent Test**: Deploy an agent to a bare target host (no scanning tools pre-installed), assign a plan containing a network scan and a service/port scan step, run it, and confirm the reported step results contain real discovered hosts/ports/services rather than placeholder data.

**Acceptance Scenarios**:

1. **Given** an agent running on a target host with no scanning tools installed, **When** a plan step of type network scan executes, **Then** the step completes and reports the reachable hosts/addresses it actually found.
2. **Given** an agent running on a target host with no scanning tools installed, **When** a plan step of type port/service scan executes against a specified target, **Then** the step completes and reports the open ports and identified services it actually found.
3. **Given** a scan step targeting a host that is unreachable, **When** the step executes, **Then** the step completes with a result reflecting no reachable hosts/ports rather than crashing the agent or the plan run.

---

### User Story 2 - Steps that need to fetch a remote resource work without host tooling (Priority: P2)

A plan step (for example, part of a remediation or exploitation action) needs to retrieve a file from a remote location, the way `wget` or `curl` would. The agent performs this retrieval using its own bundled capability, without assuming the target host has any such tool installed.

**Why this priority**: Several remediation and exploitation actions depend on fetching a patch, payload, or reference file. Without a self-contained retrieval capability, these steps fail on minimal/hardened hosts, which is exactly the environment this agent is meant to operate in.

**Independent Test**: Assign a plan whose step(s) require fetching a remote file to an agent running on a host with no `wget`/`curl`/equivalent installed, run it, and confirm the file is retrieved successfully and the step reports success with the expected content available for subsequent steps.

**Acceptance Scenarios**:

1. **Given** a target host without `wget`, `curl`, or any other file-retrieval tool installed, **When** a plan step needs to fetch a remote file, **Then** the agent retrieves it successfully using its own bundled capability.
2. **Given** a remote resource that is unreachable or returns an error, **When** the fetch is attempted, **Then** the step is reported as failed with a descriptive error instead of the plan silently continuing as if the fetch succeeded.

---

### User Story 3 - Missing/incompatible tooling fails loudly instead of silently (Priority: P3)

An operator needs to trust that a plan step which reports success actually did the work. If a bundled tool cannot run on a given target (wrong CPU architecture, execution failure, timeout), the affected step must report failure with a clear reason, so the operator (or downstream automation) does not mistake a no-op for a completed action.

**Why this priority**: This is what makes Stories 1 and 2 trustworthy in production — without honest failure reporting, silent placeholder-like behavior could resurface in edge cases and go unnoticed.

**Independent Test**: Force a bundled tool invocation to fail (e.g., simulate an unsupported platform or a process timeout) and confirm the plan step is reported as failed with an explanatory message, and the overall plan run reflects that failure rather than a false success.

**Acceptance Scenarios**:

1. **Given** a bundled tool that fails to execute on the current host, **When** the plan step that depends on it runs, **Then** the step result is reported as failed with a descriptive error.
2. **Given** a bundled tool invocation that exceeds an expected time budget, **When** the step is executing, **Then** the step is terminated and reported as failed rather than hanging indefinitely.

### Edge Cases

- What happens when the target host's CPU architecture or OS variant has no matching bundled tool binary?
- How does the system handle a bundled tool process that hangs or never terminates?
- What happens when a bundled tool needs privileges the agent process does not have (e.g., raw-socket scanning requiring elevated rights)?
- What happens when two plan steps that both need the same bundled tool run concurrently on the same agent instance?
- What happens when the scan/fetch target parameter is malformed or missing?
- How does the system behave when the destination for a fetched file is not writable?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The agent MUST be able to execute network discovery plan steps that identify reachable hosts/addresses on a specified target range, using tooling bundled with the agent itself.
- **FR-002**: The agent MUST be able to execute port/service identification plan steps that identify open ports and, where determinable, the services running on them, using tooling bundled with the agent itself.
- **FR-003**: The agent MUST be able to retrieve a remote file/resource as part of a plan step, using tooling bundled with the agent itself, equivalent in capability to common command-line file-retrieval tools.
- **FR-004**: The agent's distribution MUST be self-contained: completing any of the plan step types covered by this feature MUST NOT require any tool to be pre-installed on the target host beyond what the agent itself carries.
- **FR-005**: When a plan step covered by this feature cannot be completed (required tool unavailable for the current platform, execution failure, timeout, unreachable target), the agent MUST report that step as failed with a descriptive, actionable error rather than reporting a false success.
- **FR-006**: The placeholder (no-op) behavior currently used for network/service/system scan plan steps MUST be replaced with real, tool-backed execution that produces genuine findings.
- **FR-007**: Results produced by these plan steps (discovered hosts, ports, services, retrieved file metadata, or failure reasons) MUST be captured and reported back to the centralized platform using the same structured step-reporting mechanism already used by other plan step types.
- **FR-008**: The agent MUST enforce that scan and fetch actions only operate against the target(s)/resource(s) specified by the plan step's own parameters, and MUST NOT expand scope to hosts or resources not specified in the step.
- **FR-009**: A bundled tool invocation that runs beyond an expected time budget MUST be terminated by the agent and the corresponding step reported as failed, rather than left running indefinitely.

### Cross-Cutting Requirements

- **Validation and Error Handling**: Plan step parameters (targets, addresses, URLs) MUST be validated before a bundled tool process is invoked; invalid or missing parameters MUST fail the step with a clear error rather than being passed through to the underlying tool.
- **Security Constraints**: Bundled tool execution MUST be confined to the target(s) explicitly declared in the plan step, consistent with the platform's existing authorization model for what a given plan is allowed to act upon. Tool output MUST NOT be trusted as executable input (e.g., no unsanitized invocation of scan output as further commands).

### Key Entities

- **Bundled Tool**: A capability (network discovery, port/service identification, remote file retrieval) shipped as part of the agent's own distribution and invoked locally by the agent; identified by the capability it provides rather than by a specific product name.
- **Plan Step**: An existing unit of work assigned to the agent as part of a plan; this feature changes what happens *inside* the scan-type and any fetch-dependent step types so they perform real work instead of placeholder behavior.
- **Step Result**: The structured outcome of executing a plan step (success/failure, findings or retrieved-resource metadata, error message when applicable) reported back to the centralized platform.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An agent deployed to a freshly provisioned target host, with no additional software installed on that host, completes a plan containing network discovery, port/service identification, and file-retrieval steps end-to-end with zero steps failing due to a missing tool.
- **SC-002**: 100% of network discovery and port/service identification plan steps return genuine findings (real discovered hosts/ports/services or an honest empty result) instead of placeholder/no-op responses.
- **SC-003**: When a required tool cannot complete its task on a given host, 100% of the affected plan steps are reported as failed with an actionable error message rather than a false success.
- **SC-004**: Deploying an agent to a new target host requires zero manual tool-installation steps before it can execute a full plan.

## Assumptions

- Target hosts are unix-like systems within the OS/architecture scope the agent already supports; broadening that platform/architecture scope is out of scope for this feature.
- The specific tool implementations used to satisfy each capability (network discovery, port/service identification, file retrieval) are an implementation decision made during planning, as long as the functional capability and self-containment requirements above are met.
- This feature replaces the current placeholder (no-op) behavior behind the existing network/service/system scan plan step types, and adds file-retrieval capability for use by any plan step type that needs to fetch a remote resource (e.g., as part of a remediation or exploitation action) — it does not introduce a new operator-facing plan step type of its own.
- Bundled tools run with the same privileges as the agent process; capabilities that strictly require elevated privileges the agent does not have are a known limitation, not something this feature solves via privilege escalation.
- Existing plan step reporting, retry, and plan-execution orchestration mechanisms are reused as-is; this feature only changes what happens inside the affected step executions.

## Constitution Notes

- Confirm which repository guidance from `AGENTS.md` applies: the `agents/unix` module builds as a Spring Boot + GraalVM native binary, and per `AGENTS.md`, any scripts/templates this feature introduces must live in resource template files accessed via `ClassPathResource`, never built inline via string concatenation.
- Confirm whether `.agents/skills/` introduces stack-specific constraints for this work — none identified beyond the native-image bundling implications, to be resolved during planning (e.g., how non-JVM binaries are packaged into and extracted from a GraalVM native image).
- Open question deferred to planning rather than guessed: which specific tool(s) satisfy each capability (network discovery, port/service identification, file retrieval) and how their binaries are packaged into/extracted from the native image distribution.
