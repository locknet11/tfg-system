# Quickstart: Docker Container Remediation Skip

**Date**: 2026-07-05
**Feature**: [spec.md](spec.md) | [plan.md](plan.md)

## Prerequisites

- Agent module (`agents/unix/`) build environment with Java 17 and Maven
- API module (`api/`) build environment with Java 17, Maven, and MongoDB
- Docker Desktop (or compatible container runtime) for integration testing

## Quick Verification

### 1. Unit Test: ContainerDetector

```bash
cd agents/unix
./mvnw test -Dtest=ContainerDetectorTest
```

This runs the `ContainerDetector` unit tests that mock the filesystem to verify:
- Docker detection via `/.dockerenv` existence
- Docker detection via `/proc/1/cgroup` with `/docker/` path
- Docker detection via `/proc/self/mountinfo` with `/docker/containers/` path
- Podman detection via `/run/.containerenv`
- No container detected (none of the indicators match)
- Inconclusive detection (files unreadable due to permissions)
- Detection under 1 second

### 2. Unit Test: RemediationStepHandler (Container Skip)

```bash
cd agents/unix
./mvnw test -Dtest=RemediationStepHandlerTest#shouldSkipRemediationWhenDockerDetected
```

Verifies that when `ContainerDetector.detect()` returns `container=true`, the handler:
- Does NOT call `httpClient.lookupVulnerabilities()`
- Does NOT call `httpClient.requestRemediationStrategy()`
- Does NOT call `commandExecutor.execute()`
- Returns a `StepResult` with `skipped=true`
- Calls `httpClient.reportRemediationResult()` with `status=SKIPPED` and a non-null `skipReason`

### 3. Unit Test: RemediationStepHandler (Host — Normal Flow)

```bash
cd agents/unix
./mvnw test -Dtest=RemediationStepHandlerTest#shouldProceedNormallyWhenNotInContainer
```

Verifies that when `ContainerDetector.detect()` returns `container=false`, the handler proceeds with normal remediation (no regression).

### 4. Unit Test: TaskExecutionService (Skip Doesn't Abort)

```bash
cd agents/unix
./mvnw test -Dtest=TaskExecutionServiceTest#shouldContinueAfterSkippedStep
```

Verifies that when a step handler returns a skipped result, the job continues to the next step instead of aborting.

### 5. Integration Test: Docker Container

```bash
# Build agent
cd agents/unix && ./mvnw clean package -DskipTests

# Run agent in Docker
docker run --rm -v $(pwd)/target/agent.jar:/agent.jar \
  -e CENTRAL_URL=http://host.docker.internal:8080 \
  openjdk:17-jdk-slim \
  java -jar /agent.jar

# Observe: agent detects Docker, skips remediation, logs skip reason
```

### 6. API: Verify skipReason Persistence

```bash
cd api
./mvnw test -Dtest=RemediationServiceTest#shouldPersistSkipReason
```

Verifies that `RemediationService.createRemediation()` stores `skipReason` in MongoDB when provided.

### 7. API: Verify skipReason in Response

```bash
cd api
./mvnw test -Dtest=RemediationControllerTest#shouldIncludeSkipReasonInResponse
```

Verifies that `GET /api/remediations` and `GET /api/remediations/{id}` include `skipReason` in the response when present.

## Build Commands (Full)

```bash
# Agent module
cd agents/unix
./mvnw clean package

# API module
cd api
./mvnw clean package

# UI module (no changes needed for this feature)
cd ui
npm ci && npm run build
```

## Key Files Changed

| File | Module | Change |
|------|--------|--------|
| `container/ContainerDetector.java` | agents/unix | NEW — detection logic |
| `container/ContainerDetectionResult.java` | agents/unix | NEW — detection result value object |
| `worker/step/RemediationStepHandler.java` | agents/unix | MODIFIED — add pre-check guard |
| `worker/http/dto/RemediationReportRequest.java` | agents/unix | MODIFIED — add skipReason |
| `domain/task/StepResult.java` | agents/unix | MODIFIED — add skipped flag + factory |
| `worker/TaskExecutionService.java` | agents/unix | MODIFIED — handle skipped results |
| `remediation/model/RemediationRecord.java` | api | MODIFIED — add skipReason |
| `remediation/model/dto/RemediationReportRequest.java` | api | MODIFIED — add skipReason |
| `remediation/model/dto/RemediationInfo.java` | api | MODIFIED — add skipReason |
