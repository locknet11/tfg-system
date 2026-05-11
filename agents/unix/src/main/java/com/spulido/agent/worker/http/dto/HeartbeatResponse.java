package com.spulido.agent.worker.http.dto;

public class HeartbeatResponse {

    private String agentId;
    private String status;
    private String lastConnection;
    private boolean hasPlan;

    public HeartbeatResponse() {}

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLastConnection() {
        return lastConnection;
    }

    public void setLastConnection(String lastConnection) {
        this.lastConnection = lastConnection;
    }

    public boolean isHasPlan() {
        return hasPlan;
    }

    public void setHasPlan(boolean hasPlan) {
        this.hasPlan = hasPlan;
    }
}
