package com.spulido.tfg.domain.agent.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.dto.AgentRegistrationResponse;
import com.spulido.tfg.domain.agent.model.dto.RegisterReplicatedAgentRequest;
import com.spulido.tfg.domain.organization.services.OrganizationService;
import com.spulido.tfg.domain.project.services.ProjectService;
import com.spulido.tfg.domain.replication.db.ReplicationRequestRepository;
import com.spulido.tfg.domain.replication.model.ReplicationRequest;
import com.spulido.tfg.domain.replication.model.ReplicationRequestStatus;
import com.spulido.tfg.domain.script.services.ScriptService;
import com.spulido.tfg.domain.target.db.TargetRepository;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.target.model.TargetStatus;
import com.spulido.tfg.domain.target.services.TargetService;
import com.spulido.tfg.domain.template.services.TemplateService;

@ExtendWith(MockitoExtension.class)
class AgentServiceImplRegisterReplicatedAgentTest {

    @Mock private AgentRepository agentRepository;
    @Mock private OrganizationService organizationService;
    @Mock private ProjectService projectService;
    @Mock private TargetService targetService;
    @Mock private TargetRepository targetRepository;
    @Mock private TemplateService templateService;
    @Mock private ScriptService scriptService;
    @Mock private ReplicationRequestRepository replicationRequestRepository;

    @InjectMocks
    private AgentServiceImpl agentService;

    private ReplicationRequest approvedReplication() {
        ReplicationRequest replication = new ReplicationRequest();
        replication.setStatus(ReplicationRequestStatus.APPROVED);
        replication.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        replication.setOrganizationId("org-id");
        replication.setProjectId("proj-id");
        replication.setParentAgentId("parent-agent-id");
        replication.setCveId("CVE-2025-0001");
        replication.setExploitId("exploit-1");
        replication.setPreauthCode("preauth-abc123");
        return replication;
    }

    private RegisterReplicatedAgentRequest request(String hostname) {
        RegisterReplicatedAgentRequest request = new RegisterReplicatedAgentRequest();
        request.setPreauthCode("preauth-abc123");
        request.setHostname(hostname);
        request.setClientIp("172.31.128.4");
        return request;
    }

    private void stubHappyPath() throws Exception {
        when(replicationRequestRepository.findByPreauthCode("preauth-abc123"))
                .thenReturn(Optional.of(approvedReplication()));
        when(targetRepository.findByIpOrDomainAndOrganizationIdAndProjectId(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(targetService.createTarget(any(Target.class))).thenAnswer(inv -> {
            Target t = inv.getArgument(0);
            t.setId("new-target-id");
            return t;
        });
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> {
            Agent a = inv.getArgument(0);
            a.setId("new-agent-id");
            return a;
        });
    }

    @Test
    void createsTargetFromHostnameLinksAgentAndReturnsCredentials() throws Exception {
        stubHappyPath();

        AgentRegistrationResponse response = agentService.registerReplicatedAgent(request("web-node-07"));

        assertThat(response.getAgentId()).isEqualTo("new-agent-id");
        assertThat(response.getTargetId()).isEqualTo("new-target-id");
        assertThat(response.getApiKey()).isNotEmpty();

        ArgumentCaptor<Target> targetCaptor = ArgumentCaptor.forClass(Target.class);
        verify(targetService).createTarget(targetCaptor.capture());
        assertThat(targetCaptor.getValue().getSystemName()).isEqualTo("web-node-07");

        ArgumentCaptor<Target> linkedTarget = ArgumentCaptor.forClass(Target.class);
        verify(targetService).updateTarget(linkedTarget.capture());
        assertThat(linkedTarget.getValue().getAssignedAgent()).isEqualTo("new-agent-id");
        assertThat(linkedTarget.getValue().getStatus()).isEqualTo(TargetStatus.ONLINE);
        assertThat(linkedTarget.getValue().getIpOrDomain()).isEqualTo("172.31.128.4");
    }

    @Test
    void registersAgentWithoutAnyPlan() throws Exception {
        stubHappyPath();

        agentService.registerReplicatedAgent(request("web-node-07"));

        ArgumentCaptor<Agent> agentCaptor = ArgumentCaptor.forClass(Agent.class);
        verify(agentRepository).save(agentCaptor.capture());
        assertThat(agentCaptor.getValue().getPlan()).isNull();
        assertThat(agentCaptor.getValue().getReplicatedFrom()).isEqualTo("parent-agent-id");
        // The exploited flow must never consult templates for auto-plan assignment.
        verify(templateService, never()).getTemplate(anyString());
    }

    @Test
    void sanitizesHostnameForTargetName() throws Exception {
        stubHappyPath();

        agentService.registerReplicatedAgent(request("  evil name; rm -rf /  "));

        ArgumentCaptor<Target> targetCaptor = ArgumentCaptor.forClass(Target.class);
        verify(targetService).createTarget(targetCaptor.capture());
        String name = targetCaptor.getValue().getSystemName();
        assertThat(name).doesNotContain(" ").doesNotContain(";").doesNotContain("/");
        assertThat(name).isNotEmpty();
    }

    @Test
    void fallsBackToClientIpWhenHostnameBlank() throws Exception {
        stubHappyPath();

        agentService.registerReplicatedAgent(request("   "));

        ArgumentCaptor<Target> targetCaptor = ArgumentCaptor.forClass(Target.class);
        verify(targetService).createTarget(targetCaptor.capture());
        assertThat(targetCaptor.getValue().getSystemName()).isEqualTo("172.31.128.4");
    }

    @Test
    void reusesExistingUnassignedTargetForSameHost() throws Exception {
        Target existing = new Target();
        existing.setId("existing-target-id");
        existing.setSystemName("old-name");
        when(replicationRequestRepository.findByPreauthCode("preauth-abc123"))
                .thenReturn(Optional.of(approvedReplication()));
        when(targetRepository.findByIpOrDomainAndOrganizationIdAndProjectId(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(existing));
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> {
            Agent a = inv.getArgument(0);
            a.setId("new-agent-id");
            return a;
        });

        AgentRegistrationResponse response = agentService.registerReplicatedAgent(request("web-node-07"));

        assertThat(response.getTargetId()).isEqualTo("existing-target-id");
        verify(targetService, never()).createTarget(any(Target.class));
    }

    @Test
    void rejectsWhenHostAlreadyHasAgent() {
        Target existing = new Target();
        existing.setId("existing-target-id");
        existing.setAssignedAgent("already-here");
        when(replicationRequestRepository.findByPreauthCode("preauth-abc123"))
                .thenReturn(Optional.of(approvedReplication()));
        when(targetRepository.findByIpOrDomainAndOrganizationIdAndProjectId(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> agentService.registerReplicatedAgent(request("web-node-07")))
                .isInstanceOf(AgentException.class);
    }

    @Test
    void rejectsUnknownPreauthCode() {
        when(replicationRequestRepository.findByPreauthCode("preauth-abc123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentService.registerReplicatedAgent(request("web-node-07")))
                .isInstanceOf(AgentException.class);
    }

    @Test
    void rejectsBlankPreauthCode() {
        RegisterReplicatedAgentRequest req = request("web-node-07");
        req.setPreauthCode("  ");

        assertThatThrownBy(() -> agentService.registerReplicatedAgent(req))
                .isInstanceOf(AgentException.class);
    }

    @Test
    void rejectsNonApprovedReplication() {
        ReplicationRequest pending = approvedReplication();
        pending.setStatus(ReplicationRequestStatus.PENDING);
        when(replicationRequestRepository.findByPreauthCode("preauth-abc123")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> agentService.registerReplicatedAgent(request("web-node-07")))
                .isInstanceOf(AgentException.class);
    }

    @Test
    void rejectsExpiredReplication() {
        ReplicationRequest expired = approvedReplication();
        expired.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(replicationRequestRepository.findByPreauthCode("preauth-abc123")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> agentService.registerReplicatedAgent(request("web-node-07")))
                .isInstanceOf(AgentException.class);
    }
}
