package com.spulido.agent.heartbeat;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spulido.agent.worker.http.AgentHttpClient;
import com.spulido.agent.worker.http.dto.HeartbeatResponse;

@ExtendWith(MockitoExtension.class)
class HeartbeatSenderTest {

    @Mock
    private AgentHttpClient httpClient;

    @InjectMocks
    private HeartbeatSender heartbeatSender;

    @Test
    void sendHeartbeat_callsHttpClient() {
        HeartbeatResponse response = new HeartbeatResponse();
        response.setAgentId("agent-1");
        response.setStatus("ACTIVE");
        response.setLastConnection("2026-06-26T14:30:15");
        when(httpClient.sendHeartbeat()).thenReturn(response);

        heartbeatSender.sendHeartbeat();

        verify(httpClient).sendHeartbeat();
    }

    @Test
    void sendHeartbeat_handlesExceptionGracefully() {
        when(httpClient.sendHeartbeat()).thenThrow(new RuntimeException("Connection refused"));

        // Should not throw — scheduler continues on next cycle
        heartbeatSender.sendHeartbeat();

        verify(httpClient).sendHeartbeat();
    }

    @Test
    void sendHeartbeat_handlesNullResponse() {
        when(httpClient.sendHeartbeat()).thenReturn(null);

        // Should not throw
        heartbeatSender.sendHeartbeat();

        verify(httpClient).sendHeartbeat();
    }
}
