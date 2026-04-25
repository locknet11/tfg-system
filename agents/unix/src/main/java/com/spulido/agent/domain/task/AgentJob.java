package com.spulido.agent.domain.task;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class AgentJob {

    private final String jobId;
    private final List<AgentTask> tasks;
    private JobStatus status;
    private final Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String failureReason;

    public AgentJob(String jobId, List<AgentTask> tasks) {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("jobId must not be blank");
        }
        if (tasks == null || tasks.isEmpty()) {
            throw new IllegalArgumentException("Job must contain at least one task");
        }
        this.jobId = jobId;
        this.tasks = Collections.unmodifiableList(tasks);
        this.status = JobStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void start() {
        if (this.status != JobStatus.PENDING) {
            throw new IllegalStateException("Job can only start from PENDING state, current: " + this.status);
        }
        this.status = JobStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void complete() {
        if (this.status != JobStatus.RUNNING) {
            throw new IllegalStateException("Job can only complete from RUNNING state, current: " + this.status);
        }
        this.status = JobStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void fail(String reason) {
        if (this.status != JobStatus.RUNNING) {
            throw new IllegalStateException("Job can only fail from RUNNING state, current: " + this.status);
        }
        this.status = JobStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = Instant.now();
    }

    public void invalidate(String reason) {
        if (this.status != JobStatus.PENDING) {
            throw new IllegalStateException("Job can only be invalidated from PENDING state, current: " + this.status);
        }
        this.status = JobStatus.INVALID;
        this.failureReason = reason;
    }

    public void interrupt() {
        if (this.status != JobStatus.RUNNING) {
            throw new IllegalStateException("Job can only be interrupted from RUNNING state, current: " + this.status);
        }
        this.status = JobStatus.INTERRUPTED;
        this.completedAt = Instant.now();
    }

    public String getJobId() { return jobId; }
    public List<AgentTask> getTasks() { return tasks; }
    public JobStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getFailureReason() { return failureReason; }
}
