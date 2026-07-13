package com.spulido.tfg.domain.agent.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.db.AgentTeardownRepository;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.AgentTeardownRecord;
import com.spulido.tfg.domain.agent.model.dto.TeardownReportRequest;

@ExtendWith(MockitoExtension.class)
class AgentCommunicationServiceTeardownTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private AgentTeardownRepository agentTeardownRepository;

    @InjectMocks
    private AgentCommunicationServiceImpl service;

    private TeardownReportRequest request(String trigger) {
        TeardownReportRequest req = new TeardownReportRequest();
        req.setAgentId("agent-1");
        req.setTrigger(trigger);
        req.setBinaryRemoval("PENDING_DETACHED");
        TeardownReportRequest.ArtifactResult r = new TeardownReportRequest.ArtifactResult();
        r.setType("AGENT_CONFIG");
        r.setPath("/tmp/agent.properties");
        r.setStatus("REMOVED");
        req.setResults(List.of(r));
        return req;
    }

    @Test
    void recordTeardown_persistsRecord_andReapsAgent() {
        Agent agent = new Agent().setOrganizationId("org1").setProjectId("proj1");
        agent.setId("agent-1");
        when(agentTeardownRepository.findFirstByAgentIdAndTrigger("agent-1", "PLAN_COMPLETION"))
                .thenReturn(Optional.empty());
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(agentTeardownRepository.save(any(AgentTeardownRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AgentTeardownRecord record = service.recordTeardown("agent-1", request("PLAN_COMPLETION"));

        assertThat(record.getAgentId()).isEqualTo("agent-1");
        assertThat(record.getOrganizationId()).isEqualTo("org1");
        assertThat(record.getTrigger()).isEqualTo("PLAN_COMPLETION");
        assertThat(record.getResults()).hasSize(1);
        verify(agentRepository).delete(agent);
        verify(agentTeardownRepository).save(any(AgentTeardownRecord.class));
    }

    @Test
    void recordTeardown_isIdempotentForSameAgentAndTrigger() {
        AgentTeardownRecord existing = new AgentTeardownRecord().setAgentId("agent-1").setTrigger("AUTH_REVOKED");
        when(agentTeardownRepository.findFirstByAgentIdAndTrigger("agent-1", "AUTH_REVOKED"))
                .thenReturn(Optional.of(existing));

        AgentTeardownRecord record = service.recordTeardown("agent-1", request("AUTH_REVOKED"));

        assertThat(record).isSameAs(existing);
        verify(agentTeardownRepository, never()).save(any());
        verify(agentRepository, never()).delete(any());
    }
}
