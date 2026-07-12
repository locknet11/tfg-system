package com.spulido.agent.worker.http.dto;

/**
 * Request body sent by an exploited agent to self-register with Central when it has no
 * Target ID. Central creates a new target from the reported hostname and links this agent.
 */
public class RegisterReplicatedRequest {

    private String preauthCode;
    private String hostname;
    private String os;

    public RegisterReplicatedRequest() {}

    public RegisterReplicatedRequest(String preauthCode, String hostname, String os) {
        this.preauthCode = preauthCode;
        this.hostname = hostname;
        this.os = os;
    }

    public String getPreauthCode() { return preauthCode; }
    public void setPreauthCode(String preauthCode) { this.preauthCode = preauthCode; }
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public String getOs() { return os; }
    public void setOs(String os) { this.os = os; }
}
