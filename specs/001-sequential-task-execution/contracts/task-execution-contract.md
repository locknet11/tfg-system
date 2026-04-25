# Task Execution Contract

## Purpose

Define the internal execution contract for multi-step jobs handled by the unix agent.

## Job Input Contract

A job supplied to the task execution flow must provide:

- `jobId`: unique job identifier
- `steps`: ordered collection of executable steps

Each step must provide:

- `stepId`: unique step identifier inside the job
- `order`: execution position
- `command`: executable command definition
- `timeoutSeconds`: optional execution timeout

## Execution Rules

1. The agent converts each input step into one internal task.
2. Tasks start in ascending `order`.
3. The next task starts only after the current task finishes successfully.
4. If a task fails, times out, or is interrupted, later tasks do not start.
5. The job is marked complete only when every task completes successfully.

## Observable Task Output

For every task, the execution flow must make available:

- `taskId`
- `jobId`
- `order`
- `status`
- `startedAt`
- `completedAt`
- `message`
- `failureReason`

## Error Contract

- Invalid job: returned when the job has no executable steps or invalid ordering.
- Task failure: returned when a command step does not complete successfully.
- Task timeout: returned when execution exceeds the allowed timeout.
- Execution interrupted: returned when the agent stops before the task completes.
