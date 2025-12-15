package com.spulido.tfg.domain.agent.services.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.spulido.tfg.common.context.ProjectContext;
import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.AgentStatus;
import com.spulido.tfg.domain.agent.model.dto.AgentRegistrationResponse;
import com.spulido.tfg.domain.agent.model.dto.AgentsList;
import com.spulido.tfg.domain.agent.model.dto.AssignPlanRequest;
import com.spulido.tfg.domain.agent.model.dto.RegisterAgentRequest;
import com.spulido.tfg.domain.agent.services.AgentService;
import com.spulido.tfg.domain.organization.exception.OrganizationException;
import com.spulido.tfg.domain.organization.model.Organization;
import com.spulido.tfg.domain.organization.services.OrganizationService;
import com.spulido.tfg.domain.plan.model.Plan;
import com.spulido.tfg.domain.plan.model.Step;
import com.spulido.tfg.domain.plan.model.StepExecutionStatus;
import com.spulido.tfg.domain.plan.model.dto.PlanRequest;
import com.spulido.tfg.domain.project.exception.ProjectException;
import com.spulido.tfg.domain.project.model.Project;
import com.spulido.tfg.domain.project.services.ProjectService;
import com.spulido.tfg.domain.target.exception.TargetException;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.target.model.TargetStatus;
import com.spulido.tfg.domain.target.services.TargetService;
import com.spulido.tfg.domain.template.exception.TemplateException;
import com.spulido.tfg.domain.script.services.ScriptService;
import com.spulido.tfg.domain.template.model.Template;
import com.spulido.tfg.domain.template.services.TemplateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int API_KEY_LENGTH = 32;

    private final AgentRepository repository;
    private final OrganizationService organizationService;
    private final ProjectService projectService;
    private final TargetService targetService;
    private final TemplateService templateService;
    private final ScriptService scriptService;

    @Value("${api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

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
            Organization organization = organizationService
                    .getByOrganizationIdentifier(request.getOrganizationIdentifier());

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

            // Generate unique API key for agent authentication
            String apiKey = generateApiKey();

            // Create new Agent with targetUniqueId in name
            Agent agent = new Agent();
            agent.setName("Agent-" + request.getTargetUniqueId());
            agent.setStatus(AgentStatus.ACTIVE);
            agent.setLastConnection(LocalDateTime.now());
            agent.setVersion("1.0.0");
            agent.setOrganizationId(organization.getId());
            agent.setProjectId(project.getId());
            agent.setApiKey(apiKey);

            Agent savedAgent = repository.save(agent);

            // Update Target with IP and assigned agent
            target.setIpOrDomain(request.getClientIp());
            target.setAssignedAgent(savedAgent.getId());
            target.setStatus(TargetStatus.ONLINE);

            // since this request is not from platform we need to scope the update
            ProjectContext.set(organization.getId(), project.getId());

            targetService.updateTarget(target);

            // Generate install script
            String installScript = scriptService.generateInstallScript(
                    target.getOs(),
                    apiBaseUrl,
                    organization.getOrganizationIdentifier(),
                    project.getProjectIdentifier(),
                    request.getTargetUniqueId(),
                    target.getPreauthCode());

            // Build response
            AgentRegistrationResponse response = new AgentRegistrationResponse();
            response.setAgentId(savedAgent.getId());
            response.setTargetId(target.getId());
            response.setIpAddress(request.getClientIp());
            response.setStatus(savedAgent.getStatus().toString());
            response.setApiKey(apiKey);
            response.setInstallScript(installScript);

            return response;

        } catch (OrganizationException e) {
            throw new AgentException("agent.error.organizationNotFound");
        } catch (ProjectException e) {
            throw new AgentException("agent.error.projectNotFound");
        } catch (TargetException e) {
            throw new AgentException("agent.error.targetNotFound");
        }
    }

    @Override
    public Agent assignPlan(String agentId, AssignPlanRequest request) throws AgentException {
        // Find agent
        Agent agent = repository.findByIdScoped(agentId)
                .orElseThrow(() -> new AgentException("agent.error.agentNotFound"));

        Plan plan;
        if (request.getUseTemplate()) {
            // Get template and deep copy plan
            try {
                Template template = templateService.getTemplate(request.getTemplateId());
                plan = deepCopyPlan(template.getPlan());
            } catch (TemplateException e) {
                throw new AgentException("agent.error.templateNotFound");
            }
        } else {
            // Create plan from DTO
            plan = convertDtoToPlan(request.getPlan());
        }

        // Assign plan to agent
        agent.setPlan(plan);
        agent.setUpdatedAt(LocalDateTime.now());

        return repository.save(agent);
    }

    private Plan deepCopyPlan(Plan original) {
        if (original == null)
            return null;
        Plan copy = new Plan();
        copy.setNotes(original.getNotes());
        copy.setSteps(original.getSteps().stream()
                .map(this::deepCopyStep)
                .toList());
        return copy;
    }

    private Step deepCopyStep(Step original) {
        Step copy = new Step();
        copy.setStatus(StepExecutionStatus.PENDING);
        copy.setAction(original.getAction());
        copy.setLogs(List.of()); // Empty list
        return copy;
    }

    private Plan convertDtoToPlan(PlanRequest dto) {
        Plan plan = new Plan();
        plan.setNotes(dto.getNotes());
        plan.setSteps(dto.getSteps().stream()
                .map(stepDto -> {
                    Step step = new Step();
                    step.setAction(stepDto.getAction());
                    step.setStatus(StepExecutionStatus.PENDING);
                    step.setLogs(List.of());
                    return step;
                })
                .toList());
        return plan;
    }

    private String generateApiKey() {
        byte[] randomBytes = new byte[API_KEY_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
