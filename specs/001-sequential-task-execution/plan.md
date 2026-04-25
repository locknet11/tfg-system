# Implementation Plan: Sequential Task Execution

**Branch**: `001-sequential-task-execution` | **Date**: 2026-04-25 | **Spec**: `/specs/001-sequential-task-execution/spec.md`
**Input**: Feature specification from `/specs/001-sequential-task-execution/spec.md`

## Summary

Add a task-based execution model to the unix agent so each received job is decomposed into
ordered tasks, each task is tracked independently, and execution stops when any task fails.
The implementation will extend the current polling and worker coordination flow in
`agents/unix/` with explicit job, task, and result state objects while preserving the
agent's single-process Spring Boot architecture.

## Technical Context

**Language/Version**: Java 17  
**Primary Dependencies**: Spring Boot 3.5.7, Spring Scheduling, Spring Web, Lombok  
**Storage**: In-memory agent state plus existing reporting flow to external platform  
**Testing**: JUnit 5 via `spring-boot-starter-test`  
**Target Platform**: macOS/Linux unix agent process and GraalVM native build target  
**Project Type**: Background worker / agent service  
**Performance Goals**: Execute job steps in declared order with no duplicate execution in a
single polling cycle and surface task state changes immediately in agent logs or reporting  
**Constraints**: Preserve sequential execution per job, stop on first task failure, keep the
design compatible with Spring Boot native-image packaging, and avoid introducing external
orchestration services for v1  
**Scale/Scope**: Single agent instance processing one job execution flow at a time, with each
job containing a small ordered list of executable steps

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Repository guidance reviewed: `AGENTS.md` and `.agents/skills/java-springboot/SKILL.md`
- English-only rule satisfied for plan artifacts, contract text, and quickstart steps
- Proposed design is the smallest correct change: extend current worker coordination rather
  than introducing a distributed task runner
- Stack rules captured for affected module `agents/unix/`: Spring Boot, constructor
  injection, DTO-like boundaries, explicit validation, and deterministic tests
- Verification steps identified: `cd agents/unix && ./mvnw test`
- Git actions identified: branch creation required explicit user approval and was approved;
  no further git action is required for planning
- Unknown or ambiguous requirements resolved: current behavior uses scheduled polling plus a
  placeholder `runJob` execution path, so planning assumes task orchestration is currently
  absent

Post-design gate review: PASS. The design keeps changes local to the unix agent, remains
English-only, avoids unnecessary abstraction, and defines verification paths without
violating repository rules.

## Project Structure

### Documentation (this feature)

```text
specs/001-sequential-task-execution/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── task-execution-contract.md
└── tasks.md
```

### Source Code (repository root)

```text
agents/unix/
├── pom.xml
├── src/
│   ├── main/java/com/spulido/agent/
│   │   ├── AgentApplication.java
│   │   ├── config/
│   │   │   └── WorkerPoolConfig.java
│   │   ├── domain/
│   │   │   └── datasources/
│   │   │       └── Datasource.java
│   │   ├── utils/
│   │   │   └── AgentLifecycle.java
│   │   └── worker/
│   │       └── WorkerCoordinator.java
│   └── test/java/com/spulido/agent/
│       └── AgentApplicationTests.java
```

**Structure Decision**: Keep implementation inside `agents/unix/` and add feature-focused
domain and worker classes alongside the existing coordinator. Tests remain under
`agents/unix/src/test/java` and cover task sequencing behavior without external network or
filesystem dependencies.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |
