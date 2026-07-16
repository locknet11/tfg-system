package com.spulido.tfg.domain.agent.services.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.spulido.tfg.domain.agent.model.dto.RegisterReplicatedAgentRequest;
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
import com.spulido.tfg.domain.replication.db.ReplicationRequestRepository;
import com.spulido.tfg.domain.replication.model.ReplicationExploitInfo;
import com.spulido.tfg.domain.replication.model.ReplicationPolicy;
import com.spulido.tfg.domain.replication.model.ReplicationRequest;
import com.spulido.tfg.domain.replication.model.ReplicationRequestStatus;
import com.spulido.tfg.domain.target.exception.TargetException;
import com.spulido.tfg.domain.target.model.OperatingSystem;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.target.db.TargetRepository;
import com.spulido.tfg.domain.target.model.TargetStatus;
import com.spulido.tfg.domain.target.services.TargetService;
import com.spulido.tfg.domain.template.exception.TemplateException;
import com.spulido.tfg.domain.script.services.ScriptService;
import com.spulido.tfg.domain.template.model.Template;
import com.spulido.tfg.domain.template.services.TemplateService;
import com.spulido.tfg.domain.remediation.db.RemediationRecordRepository;
import com.spulido.tfg.domain.remediation.model.RemediationStatus;
import com.spulido.tfg.domain.vulnerability.db.ServiceVulnerabilityRepository;
import com.spulido.tfg.domain.vulnerability.model.ServiceVulnerabilityRecord;
import com.spulido.tfg.domain.dashboard.model.dto.VulnerabilityTrendPoint;
import com.spulido.tfg.domain.agent.model.dto.AgentMetricsResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int API_KEY_LENGTH = 32;

    private final AgentRepository repository;
    private final OrganizationService organizationService;
    private final ProjectService projectService;
    private final TargetService targetService;
    private final TargetRepository targetRepository;
    private final TemplateService templateService;
    private final ScriptService scriptService;
    private final ReplicationRequestRepository replicationRequestRepository;
    private final RemediationRecordRepository remediationRecordRepository;
    private final ServiceVulnerabilityRepository serviceVulnerabilityRepository;

    private static final DateTimeFormatter WEEK_FORMATTER = DateTimeFormatter.ofPattern("yyyy-'W'ww");

    @Value("${api.base-url:http://localhost:8080}")
    private String apiBaseUrl;

    @Value("${central.public-key:}")
    private String centralPublicKey;

    @Override
    public AgentsList listAgents(PageRequest pageRequest, String query) {
        Page<Agent> page;
        if (query != null && !query.isBlank()) {
            page = repository.findByQueryScoped(query, pageRequest);
        } else {
            page = repository.findAllScoped(pageRequest);
        }
        return new AgentsList(page.getContent(), pageRequest, page.getTotalElements());
    }

    @Override
    public Agent getById(String id) {
        return repository.findByIdScoped(id).orElse(null);
    }

    @Override
    public void deleteAgent(String id) {
        // Scoped query ensures a user can only delete their own agents.
        repository.findByIdScoped(id).ifPresent(agent -> {
            // Clear the assignedAgent from the target so it can be re-registered.
            targetRepository.findByAssignedAgent(agent.getId()).ifPresent(target -> {
                target.setAssignedAgent(null);
                target.setStatus(TargetStatus.OFFLINE);
                targetRepository.save(target);
                log.info("Cleared agent assignment from target {}", target.getId());
            });

            // Soft-mark de-provisioned so the agent's next heartbeat receives the
            // self-destruction signal. The record is hard-deleted when the agent
            // reports its teardown outcome, or reaped by the offline monitor.
            agent.setDeprovisioned(true);
            agent.setDeprovisionReason("Deleted by operator");
            agent.setStatus(AgentStatus.KILLED);
            repository.save(agent);
            log.info("Agent {} marked de-provisioned; awaiting self-destruction", agent.getId());
        });
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

            // Validate preauthCode matches the target's stored preauthCode
            if (request.getPreauthCode() == null || !request.getPreauthCode().equals(target.getPreauthCode())) {
                throw new AgentException("agent.error.invalidPreauthCode");
            }

            // Check if target already has an assigned agent
            if (target.getAssignedAgent() != null && !target.getAssignedAgent().isEmpty()) {
                throw new AgentException("agent.error.targetAlreadyHasAgent");
            }

            // Generate unique API key for agent authentication
            String apiKey = generateApiKey();

            // Generate one-time install token for binary download
            String installToken = generateInstallToken();
            String downloadUrl = apiBaseUrl + "/api/agent/binary/download/" + installToken;

            // Create new Agent with targetUniqueId in name
            Agent agent = new Agent();
            agent.setName("Agent-" + request.getTargetUniqueId());
            agent.setStatus(AgentStatus.ACTIVE);
            agent.setLastConnection(LocalDateTime.now());
            agent.setVersion("1.0.0");
            agent.setOrganizationId(organization.getId());
            agent.setProjectId(project.getId());
            agent.setApiKey(apiKey);
            agent.setInstallToken(installToken);
            agent.setInstallTokenExpiresAt(java.time.Instant.now().plusSeconds(300));

            Agent savedAgent = repository.save(agent);

            // Update Target with IP and assigned agent.
            // Only adopt the caller's IP if the target has no address configured; otherwise
            // respect the operator-set ipOrDomain (e.g. 127.0.0.1 for a local self-scan).
            if (target.getIpOrDomain() == null || target.getIpOrDomain().isBlank()) {
                target.setIpOrDomain(request.getClientIp());
            }
            target.setAssignedAgent(savedAgent.getId());
            target.setStatus(TargetStatus.ONLINE);

            // since this request is not from platform we need to scope the update
            ProjectContext.set(organization.getId(), project.getId());

            targetService.updateTarget(target);

            // Auto-assign plan from default template if configured in replication policy
            autoAssignPlanFromPolicy(savedAgent, project);

            // Generate install script with all required variables
            String installScript = scriptService.generateInstallScript(
                    target.getOs(),
                    apiBaseUrl,
                    organization.getOrganizationIdentifier(),
                    project.getProjectIdentifier(),
                    request.getTargetUniqueId(),
                    target.getPreauthCode(),
                    downloadUrl,
                    savedAgent.getId(),
                    apiKey,
                    centralPublicKey);

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
    public AgentRegistrationResponse registerReplicatedAgent(RegisterReplicatedAgentRequest request)
            throws AgentException {

        // Authorize using the replication preauth code, which resolves to the approving
        // replication request (source of org/project/parent-agent scope).
        if (request.getPreauthCode() == null || request.getPreauthCode().isBlank()) {
            throw new AgentException("agent.error.invalidPreauthCode");
        }

        ReplicationRequest replication = replicationRequestRepository
                .findByPreauthCode(request.getPreauthCode())
                .orElseThrow(() -> new AgentException("agent.error.replicationNotAuthorized"));

        if (replication.getStatus() != ReplicationRequestStatus.APPROVED) {
            throw new AgentException("agent.error.replicationNotAuthorized");
        }
        if (replication.getExpiresAt() != null && replication.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AgentException("agent.error.replicationExpired");
        }

        String organizationId = replication.getOrganizationId();
        String projectId = replication.getProjectId();

        // Scope subsequent persistence to the replication's org/project.
        ProjectContext.set(organizationId, projectId);

        String clientIp = request.getClientIp();
        String targetName = sanitizeHostname(request.getHostname());
        if (targetName == null) {
            targetName = (clientIp != null && !clientIp.isBlank()) ? clientIp : "exploited-host";
        }

        OperatingSystem os = request.getOs() != null ? request.getOs() : OperatingSystem.LINUX;

        // De-duplicate by machine identity: same client IP within the same org/project.
        Target existing = null;
        if (clientIp != null && !clientIp.isBlank()) {
            existing = targetRepository
                    .findByIpOrDomainAndOrganizationIdAndProjectId(clientIp, organizationId, projectId)
                    .orElse(null);
        }
        if (existing != null && existing.getAssignedAgent() != null && !existing.getAssignedAgent().isEmpty()) {
            // Host is already covered by an agent — do not create a second one.
            throw new AgentException("agent.error.targetAlreadyHasAgent");
        }

        boolean createdNewTarget = false;
        Target target;
        if (existing != null) {
            target = existing;
        } else {
            Target newTarget = new Target();
            newTarget.setSystemName(targetName);
            newTarget.setOs(os);
            newTarget.setOrganizationId(organizationId);
            newTarget.setProjectId(projectId);
            try {
                target = targetService.createTarget(newTarget);
            } catch (TargetException e) {
                throw new AgentException("agent.error.replicatedTargetCreationFailed");
            }
            createdNewTarget = true;
        }

        // Create the replicated agent WITHOUT a plan — the administrator assigns one later
        // from the panel (this deliberately skips the auto-plan behavior of standard registration).
        String apiKey = generateApiKey();
        Agent agent = new Agent();
        agent.setName("Agent-" + targetName);
        agent.setStatus(AgentStatus.ACTIVE);
        agent.setLastConnection(LocalDateTime.now());
        agent.setVersion("1.0.0");
        agent.setOrganizationId(organizationId);
        agent.setProjectId(projectId);
        agent.setApiKey(apiKey);
        agent.setReplicatedFrom(replication.getParentAgentId());
        agent.setReplicatedAt(LocalDateTime.now());
        agent.setReplicationExploit(new ReplicationExploitInfo(replication.getCveId(), replication.getExploitId()));

        Agent savedAgent;
        try {
            savedAgent = repository.save(agent);

            // Establish the bidirectional link and bring the target online.
            // Prefer the discovered/exploited host address captured in the replication
            // request (the sibling reached over the private network) so the child agent —
            // running on that same host — can scan/remediate itself. Fall back to the
            // caller's public IP (clientIp) only when the replication didn't record one.
            String childTargetAddress = (replication.getTargetIp() != null && !replication.getTargetIp().isBlank())
                    ? replication.getTargetIp() : clientIp;
            target.setIpOrDomain(childTargetAddress);
            target.setAssignedAgent(savedAgent.getId());
            target.setStatus(TargetStatus.ONLINE);
            targetService.updateTarget(target);
        } catch (Exception e) {
            // Atomicity: never leave an orphaned target (that we created) or an orphaned agent.
            if (agent.getId() != null) {
                try {
                    repository.delete(agent);
                } catch (Exception ignored) {
                    // best-effort rollback
                }
            }
            if (createdNewTarget) {
                try {
                    targetRepository.delete(target);
                } catch (Exception ignored) {
                    // best-effort rollback
                }
            }
            log.error("Replicated agent registration failed for host {}: {}", targetName, e.getMessage());
            throw new AgentException("agent.error.replicatedRegistrationFailed");
        }

        log.info("Registered replicated agent {} on new target {} (host: {})",
                savedAgent.getId(), target.getId(), targetName);

        AgentRegistrationResponse response = new AgentRegistrationResponse();
        response.setAgentId(savedAgent.getId());
        response.setTargetId(target.getId());
        response.setIpAddress(clientIp);
        response.setStatus(savedAgent.getStatus().toString());
        response.setApiKey(apiKey);
        return response;
    }

    /**
     * Sanitizes an untrusted hostname reported by an exploited host so it can be safely stored
     * and displayed as a target name. Returns {@code null} when nothing usable remains.
     */
    private String sanitizeHostname(String hostname) {
        if (hostname == null) {
            return null;
        }
        String cleaned = hostname.trim().replaceAll("[^A-Za-z0-9._-]", "-");
        cleaned = cleaned.replaceAll("-{2,}", "-").replaceAll("^[-.]+", "").replaceAll("[-.]+$", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        if (cleaned.length() > 253) {
            cleaned = cleaned.substring(0, 253);
        }
        return cleaned;
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

    @Override
    public AgentMetricsResponse getMetrics() {
        String orgId = getContextOrgId();
        String projectId = getContextProjectId();

        // Agent counts
        var allAgents = repository.findAllScoped(PageRequest.of(0, Integer.MAX_VALUE));
        long totalAgents = allAgents.getTotalElements();
        long activeAgents = allAgents.stream()
                .filter(a -> a.getStatus() == AgentStatus.ACTIVE)
                .count();

        double uptimePct = totalAgents > 0 ? (activeAgents * 100.0) / totalAgents : 0.0;

        // Vulnerability count
        var vulnPage = serviceVulnerabilityRepository.findAll(PageRequest.of(0, 1));
        long detectedVulnerabilities = vulnPage.getTotalElements();

        // Successful remediations
        long appliedRemediations = remediationRecordRepository
                .countByOrganizationIdAndProjectIdAndStatus(orgId, projectId, RemediationStatus.SUCCESS);

        // Weekly vulnerability trend (last 4 weeks)
        var allVulns = serviceVulnerabilityRepository.findAll(
                PageRequest.of(0, Integer.MAX_VALUE,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "fetchedAt")));
        Instant now = Instant.now();
        Instant cutoff = now.minus(28, ChronoUnit.DAYS);

        Map<String, Long> weekCounts = new LinkedHashMap<>();
        for (int i = 3; i >= 0; i--) {
            String weekKey = LocalDate.ofInstant(now.minus(i * 7L, ChronoUnit.DAYS), ZoneOffset.UTC)
                    .format(WEEK_FORMATTER);
            weekCounts.put(weekKey, 0L);
        }

        for (ServiceVulnerabilityRecord record : allVulns) {
            if (record.getFetchedAt() != null && record.getFetchedAt().isAfter(cutoff)) {
                String weekKey = LocalDate.ofInstant(record.getFetchedAt(), ZoneOffset.UTC)
                        .format(WEEK_FORMATTER);
                weekCounts.merge(weekKey, 1L, Long::sum);
            }
        }

        List<VulnerabilityTrendPoint> trend = weekCounts.entrySet().stream()
                .map(e -> VulnerabilityTrendPoint.builder()
                        .period(e.getKey())
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return AgentMetricsResponse.builder()
                .activeAgents(activeAgents)
                .totalAgents(totalAgents)
                .detectedVulnerabilities(detectedVulnerabilities)
                .appliedRemediations(appliedRemediations)
                .uptimePercentage(Math.round(uptimePct * 10.0) / 10.0)
                .vulnerabilityTrend(trend)
                .build();
    }

    private String getContextOrgId() {
        String orgId = ProjectContext.getOrganizationId();
        return orgId != null ? orgId : "";
    }

    private String getContextProjectId() {
        String projectId = ProjectContext.getProjectId();
        return projectId != null ? projectId : "";
    }

    private String generateApiKey() {
        byte[] randomBytes = new byte[API_KEY_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Generates a one-time install token for binary download during agent registration.
     * Token is valid for 5 minutes and consumed on first use.
     */
    private String generateInstallToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Automatically assigns a plan to the agent if the project has a replication policy
     * with a default template configured. This enables newly replicated agents to
     * immediately start executing remediation workflows.
     *
     * @param agent the newly registered agent
     * @param project the project the agent belongs to
     */
    private void autoAssignPlanFromPolicy(Agent agent, Project project) {
        if (project.getReplicationPolicy() == null) {
            log.debug("Project {} has no replication policy, skipping auto-plan assignment",
                    project.getId());
            return;
        }

        ReplicationPolicy policy = project.getReplicationPolicy();
        String templateId = policy.getDefaultTemplateId();

        if (templateId == null || templateId.isBlank()) {
            log.debug("Project {} has no default template configured, skipping auto-plan assignment",
                    project.getId());
            return;
        }

        try {
            Template template = templateService.getTemplate(templateId);
            Plan plan = deepCopyPlan(template.getPlan());

            if (plan == null) {
                log.warn("Template {} has no plan defined, skipping auto-plan assignment", templateId);
                return;
            }

            agent.setPlan(plan);
            agent.setUpdatedAt(LocalDateTime.now());
            repository.save(agent);

            log.info("Auto-assigned plan from template {} to agent {}",
                    templateId, agent.getId());

        } catch (TemplateException e) {
            log.warn("Failed to auto-assign plan from template {}: {}. " +
                    "Agent {} will need manual plan assignment.",
                    templateId, e.getMessage(), agent.getId());
        }
    }
}
