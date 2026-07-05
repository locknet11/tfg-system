# Data Model: Docker Container Remediation Skip

**Date**: 2026-07-05
**Feature**: [spec.md](spec.md) | [plan.md](plan.md)

## Overview

This feature introduces one new internal data structure (`ContainerDetectionResult`) and adds a single `skipReason` field to four existing classes across both modules. No new collections, no new endpoints, no new entities.

## New Entity: ContainerDetectionResult (agent-internal, not persisted)

```text
┌──────────────────────────────────────────────────────┐
│ ContainerDetectionResult                             │
├──────────────────────────────────────────────────────┤
│ container: boolean                                   │
│   true  → container runtime detected                 │
│   false → running on real host / VM                  │
│                                                      │
│ confidence: DetectionConfidence                      │
│   CONFIRMED   → at least one known indicator matched │
│   INCONCLUSIVE → unable to read detection files;     │
│                  default to safe (treat as container)│
│                                                      │
│ detectionMethod: DetectionMethod                     │
│   DOCKERENV_FILE     → /.dockerenv exists            │
│   CGROUP_V1          → /proc/1/cgroup has /docker/,  │
│                        /kubepods/, /containerd/,     │
│                        or /lxc/                      │
│   MOUNTINFO_V2       → /proc/self/mountinfo has      │
│                        /docker/containers/           │
│   PID1_SCHED         → PID 1 is not init/systemd    │
│   CONTAINERENV_FILE  → /run/.containerenv exists     │
│                        (Podman indicator)            │
│   NONE               → no detection performed        │
│                                                      │
│ matchedIndicators: Set<DetectionMethod>              │
│   which specific indicators triggered detection      │
│   empty set if container=false                       │
│                                                      │
│ runtimeName: String (nullable)                       │
│   "docker", "podman", "containerd", "lxc", or null   │
│   derived from the specific indicator that matched   │
│   null if container=false or inconclusive             │
└──────────────────────────────────────────────────────┘
```

**State transitions**: `ContainerDetectionResult` is an immutable value object created once per detection call. No state machine.

**Construction logic**:

```
1. Check /.dockerenv → if exists: container=true, CONFIRMED, runtime="docker"
2. Check /proc/1/cgroup → if lines contain /docker/, /kubepods/, /containerd/, or /lxc/
   → container=true, CONFIRMED, runtime extracted from path
3. Check /proc/self/mountinfo → if lines contain /docker/containers/
   → container=true, CONFIRMED, runtime="docker"
4. Check /run/.containerenv → if exists: container=true, CONFIRMED, runtime="podman"
5. Check /proc/1/sched pid name → if NOT "init" or "systemd"
   → container=true, CONFIRMED, runtime="container" (generic)
6. If any I/O error occurs reading above files
   → container=true, INCONCLUSIVE (fail-safe)
7. If no indicators matched
   → container=false, CONFIRMED (host confirmed)
```

## Modified Entity: RemediationRecord (api/)

```text
┌──────────────────────────────────────────────────────┐
│ RemediationRecord (existing + NEW field)              │
├──────────────────────────────────────────────────────┤
│ ... all existing fields unchanged ...                 │
│                                                      │
│ skipReason: String  ←── NEW (nullable)               │
│   Populated only when status = SKIPPED                │
│   Contains human-readable skip explanation            │
│   Examples:                                          │
│   • "Docker container detected — remediation skipped  │
│      to avoid ineffective changes in an ephemeral     │
│      environment"                                    │
│   • "Container detection inconclusive — remediation   │
│      skipped as precaution"                          │
│   • null (for non-skipped remediations)               │
└──────────────────────────────────────────────────────┘
```

## Modified DTO: RemediationReportRequest (both modules)

### Agent side (agents/unix/)

```text
┌──────────────────────────────────────────────────────┐
│ RemediationReportRequest (existing + NEW field)       │
├──────────────────────────────────────────────────────┤
│ cveId: String                                        │
│ targetId: String                                     │
│ remediationType: String                              │
│ status: String                                       │
│ packageName: String                                  │
│ packageVersionBefore: String                         │
│ packageVersionAfter: String                          │
│ actionDescription: String                            │
│ preCheckLogs: List<String>                           │
│ executionLogs: List<String>                          │
│ postCheckLogs: List<String>                          │
│ errorMessage: String                                 │
│ rollbackHint: String                                 │
│ skipReason: String  ←── NEW (nullable)               │
└──────────────────────────────────────────────────────┘
```

### API side (api/)

Same field additions. The `@Valid` annotation on `AgentCommunicationController.reportRemediationResult()` automatically validates the new field when present.

## Modified DTO: RemediationInfo (api/)

```text
┌──────────────────────────────────────────────────────┐
│ RemediationInfo (existing + NEW field)                │
├──────────────────────────────────────────────────────┤
│ ... all existing fields unchanged ...                 │
│                                                      │
│ skipReason: String  ←── NEW (nullable)               │
│   Mirrors RemediationRecord.skipReason               │
│   Available to UI via existing GET endpoints          │
└──────────────────────────────────────────────────────┘
```

## Relationships

```text
ContainerDetector (agents/unix/)
        │
        │ called by
        ▼
RemediationStepHandler (agents/unix/)
        │
        │ builds RemediationReportRequest with skipReason
        ▼
AgentHttpClient.reportRemediationResult()
        │
        │ HTTP POST /api/agent/comm/remediation/report
        ▼
AgentCommunicationController.reportRemediationResult() (api/)
        │
        │ creates RemediationRecord with skipReason
        ▼
RemediationRecord (MongoDB remediation_records collection)
        │
        │ mapped to
        ▼
RemediationInfo (returned by GET /api/remediations)
```

## Validation Rules

| Entity | Field | Rule |
|--------|-------|------|
| RemediationReportRequest (agent) | skipReason | Nullable; populated when container detection skips remediation |
| RemediationReportRequest (agent) | status | MUST be "SKIPPED" when skipReason is non-null |
| RemediationReportRequest (agent) | cveId | When docker-skip: set to "CONTAINER-DETECTED" (placeholder — no specific CVE is being remediated) |
| RemediationRecord | skipReason | Nullable; only present when status = SKIPPED |
| RemediationInfo | skipReason | Nullable; mirrors RemediationRecord |

## Container-Skip vs Kernel-Skip Disambiguation

Both result in `status = SKIPPED`. The `skipReason` field distinguishes them:

| Scenario | status | skipReason |
|---|---|---|
| Kernel update detected | SKIPPED | null (legacy — no skipReason set) |
| Docker container detected | SKIPPED | "Docker container detected — remediation skipped..." |
| Detection inconclusive | SKIPPED | "Container detection inconclusive — remediation skipped as precaution" |
| Normal remediation success | SUCCESS | null |
