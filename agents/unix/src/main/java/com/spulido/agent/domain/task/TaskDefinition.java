package com.spulido.agent.domain.task;

public class TaskDefinition {

    private final String stepId;
    private final int order;
    private final String command;
    private final long timeoutSeconds;

    public TaskDefinition(String stepId, int order, String command, long timeoutSeconds) {
        if (stepId == null || stepId.isBlank()) {
            throw new IllegalArgumentException("stepId must not be blank");
        }
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank");
        }
        if (order < 0) {
            throw new IllegalArgumentException("order must be non-negative");
        }
        this.stepId = stepId;
        this.order = order;
        this.command = command;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getStepId() {
        return stepId;
    }

    public int getOrder() {
        return order;
    }

    public String getCommand() {
        return command;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
