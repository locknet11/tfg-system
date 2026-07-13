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
    private String preauthCode;

    private String exploitDefaultTargetUser = "root";
    private String exploitTransferMethod = "auto";
    private int exploitTransferMethodRetries = 3;
    private int exploitTransferFileMaxSizeMb = 100;
    private int heartbeatIntervalMs = 30000;
    private boolean selfDestructOnPlanCompletion = true;

    public boolean isSelfDestructOnPlanCompletion() {
        return selfDestructOnPlanCompletion;
    }

    public void setSelfDestructOnPlanCompletion(boolean selfDestructOnPlanCompletion) {
        this.selfDestructOnPlanCompletion = selfDestructOnPlanCompletion;
    }

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

    public String getPreauthCode() {
        return preauthCode;
    }

    public void setPreauthCode(String preauthCode) {
        this.preauthCode = preauthCode;
    }

    public String getExploitDefaultTargetUser() {
        return exploitDefaultTargetUser;
    }

    public void setExploitDefaultTargetUser(String exploitDefaultTargetUser) {
        this.exploitDefaultTargetUser = exploitDefaultTargetUser;
    }

    public String getExploitTransferMethod() {
        return exploitTransferMethod;
    }

    public void setExploitTransferMethod(String exploitTransferMethod) {
        this.exploitTransferMethod = exploitTransferMethod;
    }

    public int getExploitTransferMethodRetries() {
        return exploitTransferMethodRetries;
    }

    public void setExploitTransferMethodRetries(int exploitTransferMethodRetries) {
        this.exploitTransferMethodRetries = exploitTransferMethodRetries;
    }

    public int getExploitTransferFileMaxSizeMb() {
        return exploitTransferFileMaxSizeMb;
    }

    public void setExploitTransferFileMaxSizeMb(int exploitTransferFileMaxSizeMb) {
        this.exploitTransferFileMaxSizeMb = exploitTransferFileMaxSizeMb;
    }

    public int getHeartbeatIntervalMs() {
        return heartbeatIntervalMs;
    }

    public void setHeartbeatIntervalMs(int heartbeatIntervalMs) {
        this.heartbeatIntervalMs = heartbeatIntervalMs;
    }
}
