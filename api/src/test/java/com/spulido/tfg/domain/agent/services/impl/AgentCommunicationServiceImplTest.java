package com.spulido.tfg.domain.agent.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.AgentStatus;
import com.spulido.tfg.domain.exploitation.services.ExploitationKnowledgeService;
import com.spulido.tfg.domain.target.db.TargetRepository;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.target.model.TargetStatus;
import com.spulido.tfg.domain.vulnerability.services.VulnerabilityLookupService;

@ExtendWith(MockitoExtension.class)
class AgentCommunicationServiceImplTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private TargetRepository targetRepository;

    @Mock
    private VulnerabilityLookupService vulnerabilityLookupService;

    @Mock
    private ExploitationKnowledgeService exploitationKnowledgeService;

    @InjectMocks
    private AgentCommunicationServiceImpl service;

    @Test
    void updateHeartbeat_updatesLastConnectionForActiveAgent() throws AgentException {
        Agent agent = createAgent("agent-1", "TestAgent", AgentStatus.ACTIVE);
        agent.setLastConnection(LocalDateTime.now().minusHours(1));
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        Agent result = service.updateHeartbeat("agent-1");

        assertThat(result.getLastConnection()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AgentStatus.ACTIVE);
        verify(targetRepository, never()).findByAssignedAgent(any());
    }

    @Test
    void updateHeartbeat_restoresUnresponsiveAgentAndTarget() throws AgentException {
        Agent agent = createAgent("agent-1", "TestAgent", AgentStatus.UNRESPONSIVE);
        agent.setLastConnection(LocalDateTime.now().minusMinutes(5));
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        Target target = new Target();
        target.setId("target-1");
        target.setStatus(TargetStatus.OFFLINE);
        when(targetRepository.findByAssignedAgent("agent-1")).thenReturn(Optional.of(target));
        when(targetRepository.save(any(Target.class))).thenReturn(target);

        Agent result = service.updateHeartbeat("agent-1");

        assertThat(result.getStatus()).isEqualTo(AgentStatus.ACTIVE);
        assertThat(result.getLastConnection()).isNotNull();
        verify(targetRepository).findByAssignedAgent("agent-1");
        verify(targetRepository).save(argThat(t -> t.getStatus() == TargetStatus.ONLINE));
    }

    @Test
    void updateHeartbeat_doesNotRestoreInReviewTarget() throws AgentException {
        Agent agent = createAgent("agent-1", "TestAgent", AgentStatus.UNRESPONSIVE);
        agent.setLastConnection(LocalDateTime.now().minusMinutes(5));
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        Target target = new Target();
        target.setId("target-1");
        target.setStatus(TargetStatus.IN_REVIEW);
        when(targetRepository.findByAssignedAgent("agent-1")).thenReturn(Optional.of(target));

        service.updateHeartbeat("agent-1");

        assertThat(agent.getStatus()).isEqualTo(AgentStatus.ACTIVE);
        verify(targetRepository, never()).save(any());
    }

    @Test
    void updateHeartbeat_throwsWhenAgentNotFound() {
        when(agentRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateHeartbeat("nonexistent"))
                .isInstanceOf(AgentException.class);
    }

    private Agent createAgent(String id, String name, AgentStatus status) {
        Agent agent = new Agent();
        agent.setId(id);
        agent.setName(name);
        agent.setStatus(status);
        return agent;
    }
}
