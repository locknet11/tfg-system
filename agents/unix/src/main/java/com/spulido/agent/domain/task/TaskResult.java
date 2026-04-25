package com.spulido.agent.domain.task;

import java.time.Instant;

public class TaskResult {

    private final String taskId;
    private final boolean success;
    private final String message;
    private final String failureReason;
    private final Instant recordedAt;

    private TaskResult(String taskId, boolean success, String message, String failureReason) {
        this.taskId = taskId;
        this.success = success;
        this.message = message;
        this.failureReason = failureReason;
        this.recordedAt = Instant.now();
    }

    public static TaskResult success(String taskId, String message) {
        return new TaskResult(taskId, true, message, null);
    }

    public static TaskResult failure(String taskId, String message, String failureReason) {
        if (failureReason == null || failureReason.isBlank()) {
            throw new IllegalArgumentException("Failure reason must be provided for failed results");
        }
        return new TaskResult(taskId, false, message, failureReason);
    }

    public String getTaskId() {
        return taskId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
