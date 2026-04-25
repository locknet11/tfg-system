package com.spulido.agent.domain.task;

import java.time.Instant;

public class AgentTask {

    private final String taskId;
    private final String jobId;
    private final int sequence;
    private final String command;
    private TaskStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private Instant timeoutAt;
    private TaskResult result;

    public AgentTask(String taskId, String jobId, int sequence, String command) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("jobId must not be blank");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must be non-negative");
        }
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank");
        }
        this.taskId = taskId;
        this.jobId = jobId;
        this.sequence = sequence;
        this.command = command;
        this.status = TaskStatus.PENDING;
    }

    public void start() {
        if (this.status != TaskStatus.PENDING) {
            throw new IllegalStateException("Task can only start from PENDING state, current: " + this.status);
        }
        this.status = TaskStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void complete(TaskResult taskResult) {
        if (this.status != TaskStatus.RUNNING) {
            throw new IllegalStateException("Task can only complete from RUNNING state, current: " + this.status);
        }
        this.result = taskResult;
        this.completedAt = Instant.now();
        this.status = taskResult.isSuccess() ? TaskStatus.COMPLETED : TaskStatus.FAILED;
    }

    public void skip() {
        if (this.status != TaskStatus.PENDING) {
            throw new IllegalStateException("Task can only be skipped from PENDING state, current: " + this.status);
        }
        this.status = TaskStatus.SKIPPED;
    }

    public void interrupt() {
        if (this.status != TaskStatus.RUNNING) {
            throw new IllegalStateException("Task can only be interrupted from RUNNING state, current: " + this.status);
        }
        this.status = TaskStatus.INTERRUPTED;
        this.completedAt = Instant.now();
    }

    public String getTaskId() { return taskId; }
    public String getJobId() { return jobId; }
    public int getSequence() { return sequence; }
    public String getCommand() { return command; }
    public TaskStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getTimeoutAt() { return timeoutAt; }
    public TaskResult getResult() { return result; }

    public void setTimeoutAt(Instant timeoutAt) {
        this.timeoutAt = timeoutAt;
    }
}
