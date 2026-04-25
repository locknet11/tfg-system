# Data Model: Sequential Task Execution

## Job

- **Purpose**: Represents a single unit of work received by the unix agent.
- **Fields**:
  - `jobId`: unique identifier for the job
  - `tasks`: ordered list of task records
  - `status`: overall job status
  - `createdAt`: time the job entered the agent
  - `startedAt`: time execution began
  - `completedAt`: time execution finished or stopped
  - `failureReason`: summary reason when the job does not complete successfully
- **Relationships**:
  - One job has many tasks.
- **Validation Rules**:
  - A job must contain at least one executable task.
  - Task order values within a job must be unique and contiguous for execution.
- **State Transitions**:
  - `PENDING -> RUNNING -> COMPLETED`
  - `PENDING -> INVALID`
  - `RUNNING -> FAILED`
  - `RUNNING -> INTERRUPTED`

## Task

- **Purpose**: Represents one ordered executable step within a job.
- **Fields**:
  - `taskId`: unique identifier for the task
  - `jobId`: owning job identifier
  - `sequence`: zero-based or one-based order position within the job
  - `command`: executable step definition from the job
  - `status`: task execution state
  - `startedAt`: execution start time
  - `completedAt`: execution end time
  - `timeoutAt`: optional cutoff time for execution timeout handling
  - `result`: task result payload
- **Relationships**:
  - Many tasks belong to one job.
  - One task has zero or one task result.
- **Validation Rules**:
  - A task cannot start unless all previous tasks completed successfully.
  - A task cannot transition from `FAILED` or `COMPLETED` back to `RUNNING` in the same
    execution flow.
- **State Transitions**:
  - `PENDING -> RUNNING -> COMPLETED`
  - `PENDING -> RUNNING -> FAILED`
  - `PENDING -> SKIPPED` when an earlier task fails
  - `RUNNING -> INTERRUPTED`

## Task Result

- **Purpose**: Captures the outcome of a task execution attempt.
- **Fields**:
  - `taskId`: owning task identifier
  - `outcome`: success or failure indicator
  - `message`: human-readable outcome summary
  - `failureReason`: detailed reason when execution fails
  - `recordedAt`: timestamp when the result was captured
- **Validation Rules**:
  - Failed outcomes must include a failure reason.
  - Completed tasks must have exactly one recorded result.

## Status Enumerations

- **JobStatus**: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `INVALID`, `INTERRUPTED`
- **TaskStatus**: `PENDING`, `RUNNING`, `COMPLETED`, `FAILED`, `SKIPPED`, `INTERRUPTED`
