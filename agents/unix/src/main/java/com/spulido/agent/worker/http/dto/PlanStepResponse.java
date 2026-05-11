package com.spulido.agent.worker.http.dto;

import java.util.List;

public class PlanStepResponse {

    private String action;
    private String status;
    private List<String> logs;

    public PlanStepResponse() {}

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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
