package com.spulido.agent.worker.http.dto;

public class StepStatusResponse {

    private String notes;
    private boolean allowTemplating;

    public StepStatusResponse() {}

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
}
