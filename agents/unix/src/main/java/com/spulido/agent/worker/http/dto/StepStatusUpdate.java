package com.spulido.agent.worker.http.dto;

import java.util.List;

public class StepStatusUpdate {

    private String status;
    private List<String> logs;

    public StepStatusUpdate() {}

    public StepStatusUpdate(String status, List<String> logs) {
        this.status = status;
        this.logs = logs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getLogs() {
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }
}
