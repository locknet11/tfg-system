package com.spulido.agent.heartbeat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import com.spulido.agent.config.AgentConfig;
import com.spulido.agent.teardown.TeardownService;
import com.spulido.agent.teardown.TeardownTrigger;
import com.spulido.agent.worker.http.AgentHttpClient;
import com.spulido.agent.worker.http.dto.HeartbeatResponse;

@Component
public class HeartbeatSender {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatSender.class);

    /** Consecutive authenticated-rejection heartbeats before implicit self-destruct. */
    static final int AUTH_REJECTION_THRESHOLD = 3;

    private final AgentHttpClient httpClient;
    private final AgentConfig config;
    private final TeardownService teardownService;

    private int consecutiveAuthRejections = 0;

    public HeartbeatSender(AgentHttpClient httpClient, AgentConfig config, TeardownService teardownService) {
        this.httpClient = httpClient;
        this.config = config;
        this.teardownService = teardownService;
    }

    @Scheduled(fixedDelayString = "${agent.heartbeat.interval-ms:30000}")
    public void sendHeartbeat() {
        HeartbeatResponse response;
        try {
            response = httpClient.sendHeartbeat();
        } catch (HttpClientErrorException e) {
            handleAuthRejection(e);
            return;
        } catch (Exception e) {
            // Transport failure (timeout, connection refused, 5xx, DNS). Not an
            // authenticated rejection: never triggers teardown, never advances the
            // rejection counter.
            log.warn("Failed to send heartbeat to central platform: {}", e.getMessage());
            return;
        }

        // A successful, authenticated heartbeat resets the rejection counter.
        consecutiveAuthRejections = 0;

        if (response == null) {
            return;
        }
        log.debug("Heartbeat acknowledged - agentId: {}, status: {}, lastConnection: {}",
                response.getAgentId(), response.getStatus(), response.getLastConnection());

        if (response.isDeprovision() && isForThisAgent(response)) {
            log.info("Platform de-provision signal received (reason: {}); initiating self-destruction",
                    response.getDeprovisionReason());
            teardownService.selfDestruct(TeardownTrigger.PLATFORM_DEPROVISION);
        }
    }

    private void handleAuthRejection(HttpClientErrorException e) {
        int statusCode = e.getStatusCode().value();
        if (statusCode == 401 || statusCode == 403 || statusCode == 404) {
            consecutiveAuthRejections++;
            log.warn("Heartbeat authenticated-rejection {} ({} consecutive)", statusCode, consecutiveAuthRejections);
            if (consecutiveAuthRejections >= AUTH_REJECTION_THRESHOLD) {
                log.info("Registration appears revoked after {} rejections; initiating self-destruction",
                        consecutiveAuthRejections);
                teardownService.selfDestruct(TeardownTrigger.AUTH_REVOKED);
            }
        } else {
            // Other 4xx are treated as transport-like: do not count toward revocation.
            log.warn("Heartbeat client error {}: {}", statusCode, e.getMessage());
        }
    }

    private boolean isForThisAgent(HeartbeatResponse response) {
        String configuredId = config.getAgentId();
        return configuredId == null || configuredId.isBlank()
                || configuredId.equals(response.getAgentId());
    }
}
