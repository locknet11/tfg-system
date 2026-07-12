package com.spulido.agent.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spulido.agent.config.AgentConfig;
import com.spulido.agent.worker.http.AgentHttpClient;
import com.spulido.agent.worker.http.dto.RegisterReplicatedResponse;

@ExtendWith(MockitoExtension.class)
class ReplicatedAgentRegistrarTest {

    @Mock
    private AgentHttpClient httpClient;

    /** Registrar variant with instant backoff so retry loops don't sleep in tests. */
    private ReplicatedAgentRegistrar registrar(AgentConfig config) {
        return new ReplicatedAgentRegistrar(config, httpClient) {
            @Override
            protected boolean backoff(int attempt) {
                return true;
            }
        };
    }

    private AgentConfig exploitedInstallConfig() {
        AgentConfig config = new AgentConfig();
        config.setCentralUrl("http://central:8080");
        config.setPreauthCode("preauth-abc123");
        // agentId + apiKey intentionally left null (exploited install)
        return config;
    }

    @Test
    void selfRegistersAndStoresCredentialsWhenNoIdentity() {
        AgentConfig config = exploitedInstallConfig();
        RegisterReplicatedResponse response = new RegisterReplicatedResponse();
        response.setAgentId("agent-1");
        response.setApiKey("key-1");
        response.setTargetId("target-1");
        when(httpClient.registerReplicated(any(), anyString())).thenReturn(response);

        registrar(config).run(null);

        verify(httpClient).registerReplicated(any(), anyString());
        assertThat(config.getAgentId()).isEqualTo("agent-1");
        assertThat(config.getApiKey()).isEqualTo("key-1");
    }

    @Test
    void skipsWhenAgentAlreadyProvisioned() {
        AgentConfig config = exploitedInstallConfig();
        config.setAgentId("existing-agent");
        config.setApiKey("existing-key");

        registrar(config).run(null);

        verify(httpClient, never()).registerReplicated(any(), anyString());
    }

    @Test
    void skipsWhenNoPreauthCode() {
        AgentConfig config = new AgentConfig();
        config.setCentralUrl("http://central:8080");
        // no preauth code, no identity

        registrar(config).run(null);

        verify(httpClient, never()).registerReplicated(any(), anyString());
    }

    @Test
    void retriesAndRemainsUnauthenticatedOnPersistentFailure() {
        AgentConfig config = exploitedInstallConfig();
        when(httpClient.registerReplicated(any(), anyString()))
                .thenThrow(new RuntimeException("Connection refused"));

        registrar(config).run(null);

        verify(httpClient, times(5)).registerReplicated(any(), anyString());
        assertThat(config.getAgentId()).isNull();
        assertThat(config.getApiKey()).isNull();
    }
}
