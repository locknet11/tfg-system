package com.spulido.tfg.domain.agent.services.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.AgentStatus;
import com.spulido.tfg.domain.agent.model.dto.AgentRegistrationResponse;
import com.spulido.tfg.domain.agent.model.dto.AgentsList;
import com.spulido.tfg.domain.agent.model.dto.RegisterAgentRequest;
import com.spulido.tfg.domain.agent.services.AgentService;
import com.spulido.tfg.domain.organization.exception.OrganizationException;
import com.spulido.tfg.domain.organization.model.Organization;
import com.spulido.tfg.domain.organization.services.OrganizationService;
import com.spulido.tfg.domain.project.exception.ProjectException;
import com.spulido.tfg.domain.project.model.Project;
import com.spulido.tfg.domain.project.services.ProjectService;
import com.spulido.tfg.domain.target.exception.TargetException;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.target.model.TargetStatus;
import com.spulido.tfg.domain.target.services.TargetService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentRepository repository;
    private final OrganizationService organizationService;
    private final ProjectService projectService;
    private final TargetService targetService;

    @Override
    public AgentsList listAgents(PageRequest pageRequest) {
        Page<Agent> page = repository.findAllScoped(pageRequest);
        return new AgentsList(page.getContent(), pageRequest, page.getTotalElements());
    }

    @Override
    public Agent getById(String id) {
        return repository.findByIdScoped(id).orElse(null);
    }

    @Override
    public void deleteAgent(String id) {
        // Delete using scoped query to ensure user can only delete their own agents
        repository.findByIdScoped(id).ifPresent(repository::delete);
    }

    @Override
    public AgentRegistrationResponse registerAgent(RegisterAgentRequest request) throws AgentException {
        try {
            // Load organization by identifier
            Organization organization = organizationService.getByOrganizationIdentifier(request.getOrganizationIdentifier());

            // Load project by identifier
            Project project = projectService.getByProjectIdentifier(request.getProjectIdentifier());

            // Validate project belongs to organization
            if (!project.getOrganizationId().equals(organization.getId())) {
                throw new AgentException("agent.error.projectDoesNotBelongToOrganization");
            }

            // Load target by uniqueId
            Target target = targetService.getByUniqueId(request.getTargetUniqueId());

            // Validate target belongs to project
            if (!target.getProjectId().equals(project.getId())) {
                throw new AgentException("agent.error.targetDoesNotBelongToProject");
            }

            // Check if target already has an assigned agent
            if (target.getAssignedAgent() != null && !target.getAssignedAgent().isEmpty()) {
                throw new AgentException("agent.error.targetAlreadyHasAgent");
            }

            // Create new Agent with targetUniqueId in name
            Agent agent = new Agent();
            agent.setName("Agent-" + request.getTargetUniqueId());
            agent.setStatus(AgentStatus.ACTIVE);
            agent.setLastConnection(LocalDateTime.now());
            agent.setVersion("1.0.0");
            agent.setOrganizationId(organization.getId());
            agent.setProjectId(project.getId());

            Agent savedAgent = repository.save(agent);

            // Update Target with IP and assigned agent
            target.setIpOrDomain(request.getClientIp());
            target.setAssignedAgent(savedAgent.getId());
            target.setStatus(TargetStatus.ONLINE);

            targetService.updateTarget(target);

            // Build response
            AgentRegistrationResponse response = new AgentRegistrationResponse();
            response.setAgentId(savedAgent.getId());
            response.setTargetId(target.getId());
            response.setIpAddress(request.getClientIp());
            response.setStatus(savedAgent.getStatus().toString());

            return response;
            
        } catch (OrganizationException e) {
            throw new AgentException("agent.error.organizationNotFound");
        } catch (ProjectException e) {
            throw new AgentException("agent.error.projectNotFound");
        } catch (TargetException e) {
            throw new AgentException("agent.error.targetNotFound");
        }
    }
}
