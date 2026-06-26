package com.spulido.agent.heartbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.spulido.agent.worker.http.AgentHttpClient;
import com.spulido.agent.worker.http.dto.HeartbeatResponse;

@Component
public class HeartbeatSender {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatSender.class);

    private final AgentHttpClient httpClient;

    public HeartbeatSender(AgentHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Scheduled(fixedDelayString = "${agent.heartbeat.interval-ms:30000}")
    public void sendHeartbeat() {
        try {
            HeartbeatResponse response = httpClient.sendHeartbeat();
            if (response != null) {
                log.debug("Heartbeat acknowledged - agentId: {}, status: {}, lastConnection: {}",
                        response.getAgentId(), response.getStatus(), response.getLastConnection());
            }
        } catch (Exception e) {
            log.warn("Failed to send heartbeat to central platform: {}", e.getMessage());
        }
    }
}
