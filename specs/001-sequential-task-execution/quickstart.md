# Quickstart: Sequential Task Execution

## Goal

Validate that the unix agent can decompose a job into ordered tasks, execute each task in
sequence, and stop remaining tasks when one task fails.

## Prerequisites

- Java 17 available locally
- Maven wrapper executable permissions available in `agents/unix/`

## Validation Steps

1. Open the unix agent module:

   ```bash
   cd agents/unix
   ```

2. Run the test suite for the task execution feature:

   ```bash
   ./mvnw test
   ```

3. Start the agent locally if manual verification is needed:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Trigger or simulate a multi-step job with at least three ordered steps.

5. Verify the following outcomes:
   - Each step is represented as a separate task.
   - Tasks move through `PENDING`, `RUNNING`, and `COMPLETED` in order.
   - If one task fails, later tasks remain unstarted or are marked skipped.
   - The job is only marked complete when all tasks complete successfully.

## Expected Result

The unix agent reports task-level progress for each step and preserves fail-fast execution
for invalid or unsuccessful jobs.
