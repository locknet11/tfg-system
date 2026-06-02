package com.spulido.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "agent")
public class AgentConfig {

    private String centralUrl;
    private String apiKey;
    private String agentId;
    private String centralPublicKey;

    public String getCentralUrl() {
        return centralUrl;
    }

    public void setCentralUrl(String centralUrl) {
        this.centralUrl = centralUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getCentralPublicKey() {
        return centralPublicKey;
    }

    public void setCentralPublicKey(String centralPublicKey) {
        this.centralPublicKey = centralPublicKey;
    }
}
