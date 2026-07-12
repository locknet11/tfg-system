package com.spulido.agent.worker.http.dto;

/**
 * Response returned by Central after a successful exploited-agent self-registration. Carries
 * the credentials the agent uses for subsequent authenticated communication.
 */
public class RegisterReplicatedResponse {

    private String agentId;
    private String apiKey;
    private String targetId;

    public RegisterReplicatedResponse() {}

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
}
