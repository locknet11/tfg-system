package com.spulido.agent.worker.http.dto;

import java.util.List;

public class PlanResponse {

    private String notes;
    private boolean allowTemplating;
    private String targetId;
    private String targetIp;
    private List<PlanStepResponse> steps;

    public PlanResponse() {}

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isAllowTemplating() {
        return allowTemplating;
    }

    public void setAllowTemplating(boolean allowTemplating) {
        this.allowTemplating = allowTemplating;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public List<PlanStepResponse> getSteps() {
        return steps;
    }

    public void setSteps(List<PlanStepResponse> steps) {
        this.steps = steps;
    }
}
