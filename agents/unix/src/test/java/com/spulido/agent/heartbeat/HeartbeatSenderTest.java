package com.spulido.agent.heartbeat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.spulido.agent.config.AgentConfig;
import com.spulido.agent.teardown.TeardownService;
import com.spulido.agent.teardown.TeardownTrigger;
import com.spulido.agent.worker.http.AgentHttpClient;
import com.spulido.agent.worker.http.dto.HeartbeatResponse;

@ExtendWith(MockitoExtension.class)
class HeartbeatSenderTest {

    @Mock
    private AgentHttpClient httpClient;

    @Mock
    private AgentConfig config;

    @Mock
    private TeardownService teardownService;

    @InjectMocks
    private HeartbeatSender heartbeatSender;

    @Test
    void sendHeartbeat_callsHttpClient() {
        HeartbeatResponse response = new HeartbeatResponse();
        response.setAgentId("agent-1");
        response.setStatus("ACTIVE");
        when(httpClient.sendHeartbeat()).thenReturn(response);

        heartbeatSender.sendHeartbeat();

        verify(httpClient).sendHeartbeat();
        verify(teardownService, never()).selfDestruct(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sendHeartbeat_handlesTransportFailureGracefully_noTeardown() {
        when(httpClient.sendHeartbeat()).thenThrow(new ResourceAccessException("Connection refused"));

        heartbeatSender.sendHeartbeat();

        verify(teardownService, never()).selfDestruct(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void sendHeartbeat_handlesNullResponse() {
        when(httpClient.sendHeartbeat()).thenReturn(null);

        heartbeatSender.sendHeartbeat();

        verify(httpClient).sendHeartbeat();
    }

    @Test
    void sendHeartbeat_deprovisionSignal_triggersPlatformDeprovisionTeardown() {
        HeartbeatResponse response = new HeartbeatResponse();
        response.setAgentId("agent-1");
        response.setDeprovision(true);
        response.setDeprovisionReason("Deleted by operator");
        when(httpClient.sendHeartbeat()).thenReturn(response);
        when(config.getAgentId()).thenReturn("agent-1");

        heartbeatSender.sendHeartbeat();

        verify(teardownService).selfDestruct(TeardownTrigger.PLATFORM_DEPROVISION);
    }

    @Test
    void sendHeartbeat_threeConsecutiveAuthRejections_triggerAuthRevokedTeardown() {
        when(httpClient.sendHeartbeat())
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        heartbeatSender.sendHeartbeat();
        heartbeatSender.sendHeartbeat();
        verify(teardownService, never()).selfDestruct(org.mockito.ArgumentMatchers.any());

        heartbeatSender.sendHeartbeat();
        verify(teardownService, times(1)).selfDestruct(TeardownTrigger.AUTH_REVOKED);
    }

    @Test
    void sendHeartbeat_successResetsAuthRejectionCounter() {
        HeartbeatResponse ok = new HeartbeatResponse();
        ok.setAgentId("agent-1");
        when(httpClient.sendHeartbeat())
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN))
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN))
                .thenReturn(ok)
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN))
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        // 2 rejections, then success (resets), then 2 more rejections = never 3 in a row.
        for (int i = 0; i < 5; i++) {
            heartbeatSender.sendHeartbeat();
        }

        verify(teardownService, never()).selfDestruct(org.mockito.ArgumentMatchers.any());
    }
}
