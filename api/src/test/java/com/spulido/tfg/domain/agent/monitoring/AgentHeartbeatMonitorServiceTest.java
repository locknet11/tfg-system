package com.spulido.tfg.domain.agent.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.AgentStatus;
import com.spulido.tfg.domain.target.db.TargetRepository;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.target.model.TargetStatus;

@ExtendWith(MockitoExtension.class)
class AgentHeartbeatMonitorServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private TargetRepository targetRepository;

    @Mock
    private HeartbeatConfig heartbeatConfig;

    @InjectMocks
    private AgentHeartbeatMonitorService service;

    @Test
    void evaluateAgentHeartbeats_marksStaleAgentAsUnresponsiveAndTargetOffline() {
        when(heartbeatConfig.getTimeoutSeconds()).thenReturn(120);

        Agent agent = createAgent("agent-1", "TestAgent", AgentStatus.ACTIVE);
        agent.setLastConnection(LocalDateTime.now().minusMinutes(5));
        when(agentRepository.findByStatusInAndLastConnectionBefore(
                any(), any())).thenReturn(List.of(agent));
        when(agentRepository.save(any())).thenReturn(agent);

        Target target = new Target();
        target.setId("target-1");
        target.setStatus(TargetStatus.ONLINE);
        when(targetRepository.findByAssignedAgent("agent-1")).thenReturn(Optional.of(target));
        when(targetRepository.save(any(Target.class))).thenReturn(target);

        service.evaluateAgentHeartbeats();

        assertThat(agent.getStatus()).isEqualTo(AgentStatus.UNRESPONSIVE);
        verify(targetRepository).findByAssignedAgent("agent-1");
        verify(targetRepository).save(argThat(t -> t.getStatus() == TargetStatus.OFFLINE));
    }

    @Test
    void evaluateAgentHeartbeats_handlesMultipleStaleAgentsAcrossOrgs() {
        when(heartbeatConfig.getTimeoutSeconds()).thenReturn(120);

        Agent agent1 = createAgent("agent-1", "Agent-1", AgentStatus.ACTIVE);
        agent1.setLastConnection(LocalDateTime.now().minusMinutes(5));
        agent1.setOrganizationId("org-1");
        agent1.setProjectId("proj-1");

        Agent agent2 = createAgent("agent-2", "Agent-2", AgentStatus.CREATED);
        agent2.setLastConnection(LocalDateTime.now().minusMinutes(3));
        agent2.setOrganizationId("org-2");
        agent2.setProjectId("proj-2");

        when(agentRepository.findByStatusInAndLastConnectionBefore(any(), any()))
                .thenReturn(List.of(agent1, agent2));
        when(agentRepository.save(any())).thenReturn(agent1, agent2);

        Target target1 = new Target();
        target1.setId("t1");
        target1.setStatus(TargetStatus.ONLINE);
        when(targetRepository.findByAssignedAgent("agent-1")).thenReturn(Optional.of(target1));
        when(targetRepository.save(any(Target.class))).thenReturn(target1);

        Target target2 = new Target();
        target2.setId("t2");
        target2.setStatus(TargetStatus.ONLINE);
        when(targetRepository.findByAssignedAgent("agent-2")).thenReturn(Optional.of(target2));

        service.evaluateAgentHeartbeats();

        assertThat(agent1.getStatus()).isEqualTo(AgentStatus.UNRESPONSIVE);
        assertThat(agent2.getStatus()).isEqualTo(AgentStatus.UNRESPONSIVE);
        verify(targetRepository, times(2)).save(any(Target.class));
    }

    @Test
    void evaluateAgentHeartbeats_doesNotAffectRecentHeartbeatAgents() {
        when(heartbeatConfig.getTimeoutSeconds()).thenReturn(120);
        when(agentRepository.findByStatusInAndLastConnectionBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        service.evaluateAgentHeartbeats();

        verify(agentRepository, never()).save(any());
        verify(targetRepository, never()).findByAssignedAgent(any());
    }

    @Test
    void evaluateAgentHeartbeats_excludesKilledAndInCreationAgents() {
        when(heartbeatConfig.getTimeoutSeconds()).thenReturn(120);
        when(agentRepository.findByStatusInAndLastConnectionBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        service.evaluateAgentHeartbeats();

        // Verify the query uses only ACTIVE and CREATED statuses (KILLED and IN_CREATION excluded)
        verify(agentRepository).findByStatusInAndLastConnectionBefore(
                argThat(statuses -> statuses.containsAll(Arrays.asList(AgentStatus.ACTIVE, AgentStatus.CREATED))
                        && !statuses.contains(AgentStatus.KILLED)
                        && !statuses.contains(AgentStatus.IN_CREATION)
                        && !statuses.contains(AgentStatus.UNRESPONSIVE)),
                any());
    }

    @Test
    void evaluateAgentHeartbeats_skipsTargetUpdateWhenAlreadyOffline() {
        when(heartbeatConfig.getTimeoutSeconds()).thenReturn(120);

        Agent agent = createAgent("agent-1", "TestAgent", AgentStatus.ACTIVE);
        agent.setLastConnection(LocalDateTime.now().minusMinutes(5));
        when(agentRepository.findByStatusInAndLastConnectionBefore(any(), any()))
                .thenReturn(List.of(agent));
        when(agentRepository.save(any())).thenReturn(agent);

        Target target = new Target();
        target.setId("target-1");
        target.setStatus(TargetStatus.OFFLINE); // Already offline
        when(targetRepository.findByAssignedAgent("agent-1")).thenReturn(Optional.of(target));

        service.evaluateAgentHeartbeats();

        assertThat(agent.getStatus()).isEqualTo(AgentStatus.UNRESPONSIVE);
        // Should not save target since it's already offline
        verify(targetRepository, never()).save(any());
    }

    @Test
    void evaluateAgentHeartbeats_skipsInReviewTarget() {
        when(heartbeatConfig.getTimeoutSeconds()).thenReturn(120);

        Agent agent = createAgent("agent-1", "TestAgent", AgentStatus.ACTIVE);
        agent.setLastConnection(LocalDateTime.now().minusMinutes(5));
        when(agentRepository.findByStatusInAndLastConnectionBefore(any(), any()))
                .thenReturn(List.of(agent));
        when(agentRepository.save(any())).thenReturn(agent);

        Target target = new Target();
        target.setId("target-1");
        target.setStatus(TargetStatus.IN_REVIEW);
        when(targetRepository.findByAssignedAgent("agent-1")).thenReturn(Optional.of(target));

        service.evaluateAgentHeartbeats();

        assertThat(agent.getStatus()).isEqualTo(AgentStatus.UNRESPONSIVE);
        // IN_REVIEW should not be changed to OFFLINE
        verify(targetRepository, never()).save(any());
    }

    @Test
    void evaluateAgentHeartbeats_handlesAgentWithoutAssignedTarget() {
        when(heartbeatConfig.getTimeoutSeconds()).thenReturn(120);

        Agent agent = createAgent("agent-1", "OrphanAgent", AgentStatus.ACTIVE);
        agent.setLastConnection(LocalDateTime.now().minusMinutes(5));
        when(agentRepository.findByStatusInAndLastConnectionBefore(any(), any()))
                .thenReturn(List.of(agent));
        when(agentRepository.save(any())).thenReturn(agent);
        when(targetRepository.findByAssignedAgent("agent-1")).thenReturn(Optional.empty());

        service.evaluateAgentHeartbeats();

        assertThat(agent.getStatus()).isEqualTo(AgentStatus.UNRESPONSIVE);
        verify(targetRepository, never()).save(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void evaluateAgentHeartbeats_passesCutoffBasedOnConfig() {
        when(heartbeatConfig.getTimeoutSeconds()).thenReturn(60);
        when(agentRepository.findByStatusInAndLastConnectionBefore(any(), any()))
                .thenReturn(Collections.emptyList());

        service.evaluateAgentHeartbeats();

        verify(agentRepository).findByStatusInAndLastConnectionBefore(any(),
                argThat(cutoff -> cutoff.isBefore(LocalDateTime.now().minusSeconds(50))));
    }

    private Agent createAgent(String id, String name, AgentStatus status) {
        Agent agent = new Agent();
        agent.setId(id);
        agent.setName(name);
        agent.setStatus(status);
        return agent;
    }
}
