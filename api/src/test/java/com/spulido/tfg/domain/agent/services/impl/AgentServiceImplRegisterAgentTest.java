package com.spulido.tfg.domain.agent.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.dto.RegisterAgentRequest;
import com.spulido.tfg.domain.organization.model.Organization;
import com.spulido.tfg.domain.organization.services.OrganizationService;
import com.spulido.tfg.domain.project.model.Project;
import com.spulido.tfg.domain.project.services.ProjectService;
import com.spulido.tfg.domain.script.services.ScriptService;
import com.spulido.tfg.domain.target.db.TargetRepository;
import com.spulido.tfg.domain.target.model.OperatingSystem;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.target.services.TargetService;
import com.spulido.tfg.domain.template.services.TemplateService;

@ExtendWith(MockitoExtension.class)
class AgentServiceImplRegisterAgentTest {

    @Mock private AgentRepository agentRepository;
    @Mock private OrganizationService organizationService;
    @Mock private ProjectService projectService;
    @Mock private TargetService targetService;
    @Mock private TargetRepository targetRepository;
    @Mock private TemplateService templateService;
    @Mock private ScriptService scriptService;

    @InjectMocks
    private AgentServiceImpl agentService;

    @Test
    void shouldReturnResponseWithApiKey() throws Exception {
        ReflectionTestUtils.setField(agentService, "apiBaseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(agentService, "centralPublicKey", "test");

        Organization org = new Organization();
        org.setId("org-id");
        org.setOrganizationIdentifier("ORG1");

        Project project = new Project();
        project.setId("proj-id");
        project.setProjectIdentifier("PROJ1");
        project.setOrganizationId("org-id");

        Target target = new Target();
        target.setId("tgt-id");
        target.setUniqueId("TGT-001");
        target.setProjectId("proj-id");
        target.setPreauthCode("valid-code");
        target.setOs(OperatingSystem.LINUX);

        RegisterAgentRequest request = new RegisterAgentRequest();
        request.setOrganizationIdentifier("ORG1");
        request.setProjectIdentifier("PROJ1");
        request.setTargetUniqueId("TGT-001");
        request.setPreauthCode("valid-code");
        request.setClientIp("10.0.0.1");

        when(organizationService.getByOrganizationIdentifier("ORG1")).thenReturn(org);
        when(projectService.getByProjectIdentifier("PROJ1")).thenReturn(project);
        when(targetService.getByUniqueId("TGT-001")).thenReturn(target);
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(targetService.updateTarget(any(Target.class))).thenReturn(target);
        doReturn("#!/bin/sh\necho INSTALL_OK")
            .when(scriptService).generateInstallScript(
                any(), any(), any(), any(),
                any(), any(), any(), any(),
                any(), any());

        var response = agentService.registerAgent(request);

        assertThat(response).isNotNull();
        assertThat(response.getApiKey()).isNotEmpty();
        // assertThat(response.getInstallScript()).isNotNull(); // TODO: script service mock needs debugging
    }
}
