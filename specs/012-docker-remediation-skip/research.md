# Research: Docker Container Remediation Skip

**Date**: 2026-07-05
**Feature**: [spec.md](spec.md) | [plan.md](plan.md)

## Research Task 1: Container Detection Methods

**Decision**: Use a multi-method allowlist approach combining three filesystem checks, ordered by reliability:

1. **`/.dockerenv` file** (primary Docker indicator) — Quick file existence check. Docker creates this sentinel file at the container root. Reliable for Docker Engine containers but may be absent in newer Docker versions and is Docker-specific.
2. **`/proc/1/cgroup` inspection** (cgroup v1 primary) — Read PID 1's control group file and check for `/docker/`, `/kubepods/`, `/containerd/`, or `/lxc/` path segments. Works on cgroup v1 (default on most Linux distributions). The init process (PID 1) is always inside the container's cgroup namespace.
3. **`/proc/self/mountinfo` inspection** (cgroup v2 primary) — Read the current process mountinfo and check for `/docker/containers/` paths. This is the primary detection method on cgroup v2 systems (modern distributions). The mountinfo exposes container-specific mount points that are absent on bare-metal/VMs.

**Additional indicators used for confidence scoring**:
- **`/proc/1/sched`** — PID 1 process name: `bash`, `sh`, or other non-init names indicate a container. Bare-metal and VMs have `init` or `systemd` as PID 1.
- **`/run/.containerenv`** — Podman-specific sentinel file. Detection of this file indicates a Podman container environment.

**Rationale**: The three-method approach covers cgroup v1, cgroup v2, and Docker's own sentinel file. This provides robust detection across different Linux distributions and container runtimes without relying on any single indicator. The filesystem-only approach is GraalVM-native safe (uses `java.nio.file.Files`), requires no external commands, and completes in well under 1 second.

**Recommendation from Baeldung (2024)**: cgroup v1 uses `/proc/1/cgroup` (look for `/docker/` or `/lxc/`), cgroup v2 uses `/proc/self/mountinfo` (look for `/docker/containers/`). The `/.dockerenv` file is a legacy indicator that may be removed in future Docker versions but remains present in current stable releases.

**Allowlist strategy**: Only declare "inside container" when at least one known indicator matches. This prevents false positives on VMs where cgroup v2 paths may coincidentally contain patterns that look container-like. If no indicators match, the environment is considered a real host and remediation proceeds normally.

**Alternatives considered**:
- **Single-indicator detection** (e.g., only `/.dockerenv`): Rejected — fragile. The file may not exist in all Docker setups or container runtimes.
- **Executing shell commands** (`cat /proc/1/cgroup`): Rejected — violates the spec requirement of filesystem-only detection (no external commands) and is slower. Java can read `/proc` files directly.
- **Using a container detection library** (e.g., Quarkus `ContainerRuntimeUtil`): Rejected — adds an external dependency, may not be GraalVM-native compatible, and is overkill for the three simple filesystem checks needed.
- **Environment variable approach** (`OS_ENV=container`): Rejected — requires container images to be built with a custom environment variable, which is not guaranteed for agent deployment. This is useful as an additional confidence booster but cannot be relied upon as the sole indicator.

## Research Task 2: Integration Point in Step Execution Pipeline

**Decision**: Add a pre-condition guard clause at the top of `RemediationStepHandler.handle()`, before any vulnerability lookup or remediation commands are executed. The `ContainerDetector` is injected as a constructor dependency.

**Rationale**: The `RemediationStepHandler` is the single entry point for all remediation execution. Adding the guard at the very top of `handle()` ensures:
- Detection runs exactly once per remediation step, not once per CVE
- If skipped, ALL CVEs for that step are skipped (not processed individually)
- No HTTP calls to Central are made (no vulnerability lookup, no strategy request)
- A single `SKIPPED` report is generated covering the entire step

The `ContainerDetector` is a standalone class (not a `@Component` bean) to avoid Spring context issues in GraalVM native images. It is instantiated in `WorkerCoordinator` and passed to the `RemediationStepHandler` constructor alongside existing dependencies (`AgentHttpClient`, `CommandExecutor`).

**ContainerDetector API**:

```java
public class ContainerDetector {
    /**
     * @return detection result with: isContainer (boolean),
     *         detectionMethod (which indicator triggered),
     *         confidence (CONFIRMED / INCONCLUSIVE)
     */
    public ContainerDetectionResult detect();
}
```

**Alternatives considered**:
- **Detection in `TaskExecutionService.executeJob()`**: Rejected — too generic. The execution service doesn't know about remediation semantics. Would require step-type awareness that violates single responsibility.
- **Detection as a separate step before remediation in the plan template**: Rejected — requires plan template changes and couples detection to plan authoring. Detection should be automatic, not configurable.
- **Detection in `WorkerCoordinator`**: Rejected — too far upstream. The coordinator manages plan polling and execution lifecycle, not per-step logic.

## Research Task 3: Handling SKIPPED Status in StepResult

**Decision**: Extend `StepResult` with a `skipped` flag and a static factory method `StepResult.skipped(action, reason, logs)`. The `TaskExecutionService` must treat a skipped result as non-failure (continue to next step).

**Current behavior**: `StepResult.failure()` returns `success=false`. `TaskExecutionService` checks `!result.isSuccess()` and aborts the job. This must change so that a skipped step does NOT abort the plan.

**Required changes**:
- `StepResult`: add `boolean skipped` field + `static StepResult skipped(action, logs)` factory
- `TaskExecutionService.executeJob()`: after handler returns, check if result is skipped; if so, do NOT abort the job, just record the result in context and continue

**Rationale**: A skipped remediation is a deliberate, safe decision — not an error. The job should continue executing subsequent steps. This aligns with User Story 3 from the spec.

**Alternatives considered**:
- **Using `StepResult.success()` with a special log message**: Rejected — semantically incorrect. The step didn't succeed; it was skipped. Calling it "success" would mislead consumers of the result.
- **Using a separate `StepOutcome` enum**: Rejected — adds complexity. A boolean flag achieves the same with minimal change.
- **Handling skip entirely within `RemediationStepHandler` by returning success with skip logs**: Rejected — the distinction between "success" and "skipped" must be preserved for plan-level status reporting (PARTIAL vs SUCCESS).

## Research Task 4: skipReason Field in Data Model

**Decision**: Add a single `skipReason` field (nullable `String`) to three locations:
1. `agents/unix/` — `RemediationReportRequest` DTO (sent from agent to central)
2. `api/` — `RemediationReportRequest` DTO (received by central endpoint)
3. `api/` — `RemediationRecord` entity (persisted in MongoDB)
4. `api/` — `RemediationInfo` DTO (returned to UI)

The `RemediationStatus` enum already has a `SKIPPED` value (defined for kernel updates). The `skipReason` field provides the human-readable explanation that distinguishes *why* it was skipped.

**skipReason values per spec FR-006**:
| Detection Result | skipReason Value |
|---|---|
| Docker conclusively detected | `"Docker container detected — remediation skipped to avoid ineffective or destructive changes in an ephemeral environment"` |
| Non-Docker container detected | `"Container environment detected (runtime: {name}) — remediation skipped to avoid ineffective changes in an ephemeral environment"` |
| Detection inconclusive | `"Container detection inconclusive — remediation skipped as precaution"` |

**Rationale**: Adding a single nullable field to existing document-oriented storage (MongoDB) is backward-compatible — existing records without the field remain valid. The field is only populated when `status == SKIPPED`. The `RemediationStatus.SKIPPED` enum value already exists and is used by the kernel update case; the `skipReason` field disambiguates kernel-skip from container-skip.

**Alternatives considered**:
- **New `RemediationSkipReason` sub-entity**: Rejected — over-engineered. A simple string with structured content (detection method + evidence) is sufficient.
- **Overloading the `errorMessage` field**: Rejected — semantically incorrect. A skip is not an error.
- **Using a separate `RemediationSkipRecord` collection**: Rejected — adds schema complexity for a single field addition.
