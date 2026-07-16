package com.spulido.tfg.domain.agent.services.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.db.AgentTeardownRepository;
import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.AgentStatus;
import com.spulido.tfg.domain.agent.model.AgentTeardownRecord;
import com.spulido.tfg.domain.agent.model.dto.TeardownReportRequest;
import com.spulido.tfg.domain.agent.model.dto.UpdateStepRequest;
import com.spulido.tfg.common.context.ProjectContext;
import com.spulido.tfg.domain.agent.services.AgentCommunicationService;
import com.spulido.tfg.domain.alerts.model.AlertEvent;
import com.spulido.tfg.domain.alerts.services.AlertTriggerService;
import com.spulido.tfg.domain.plan.model.Plan;
import com.spulido.tfg.domain.plan.model.Step;
import com.spulido.tfg.domain.vulnerability.model.CveEntry;
import com.spulido.tfg.domain.exploitation.model.dto.ExploitationKnowledgeRequest;
import com.spulido.tfg.domain.exploitation.model.dto.ExploitationKnowledgeResponse;
import com.spulido.tfg.domain.exploitation.services.ExploitationKnowledgeService;
import com.spulido.tfg.domain.target.db.TargetRepository;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.target.model.TargetStatus;
import com.spulido.tfg.domain.vulnerability.model.ServiceVulnerabilityRecord;
import com.spulido.tfg.domain.vulnerability.services.VulnerabilityLookupService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentCommunicationServiceImpl implements AgentCommunicationService {

    private final AgentRepository agentRepository;
    private final AgentTeardownRepository agentTeardownRepository;
    private final TargetRepository targetRepository;
    private final VulnerabilityLookupService vulnerabilityLookupService;
    private final ExploitationKnowledgeService exploitationKnowledgeService;
    private final AlertTriggerService alertTriggerService;

    @Override
    public Agent updateHeartbeat(String agentId) throws AgentException {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentException("agent.error.agentNotFound"));

        agent.setLastConnection(LocalDateTime.now());

        // If agent was unresponsive, set it back to active and restore target to ONLINE
        if (agent.getStatus() == AgentStatus.UNRESPONSIVE) {
            agent.setStatus(AgentStatus.ACTIVE);
            targetRepository.findByAssignedAgent(agent.getId()).ifPresent(target -> {
                if (target.getStatus() == TargetStatus.OFFLINE) {
                    target.setStatus(TargetStatus.ONLINE);
                    targetRepository.save(target);
                    log.info("Target {} restored to ONLINE (agent {} recovered)", target.getId(), agent.getName());
                }
            });
            log.info("Agent {} is now active again", agent.getName());
        }

        return agentRepository.save(agent);
    }

    @Override
    public AgentTeardownRecord recordTeardown(String agentId, TeardownReportRequest request) {
        // Idempotent: a duplicate report for the same agent + trigger returns the
        // existing record rather than creating a second one.
        return agentTeardownRepository.findFirstByAgentIdAndTrigger(agentId, request.getTrigger())
                .orElseGet(() -> {
                    AgentTeardownRecord record = new AgentTeardownRecord()
                            .setAgentId(agentId)
                            .setTrigger(request.getTrigger())
                            .setAgentTimestamp(request.getTimestamp())
                            .setBinaryRemoval(request.getBinaryRemoval())
                            .setReportedAt(Instant.now());
                    if (request.getResults() != null) {
                        record.setResults(request.getResults().stream()
                                .map(r -> new AgentTeardownRecord.ArtifactResult()
                                        .setType(r.getType())
                                        .setPath(r.getPath())
                                        .setStatus(r.getStatus())
                                        .setDetail(r.getDetail()))
                                .collect(Collectors.toList()));
                    }

                    agentRepository.findById(agentId).ifPresent(agent -> {
                        record.setOrganizationId(agent.getOrganizationId());
                        record.setProjectId(agent.getProjectId());
                        // Release the target before reaping the agent. Otherwise the target
                        // stays pinned to an agent that no longer exists, and re-registration
                        // is rejected with "target already has an agent" while the Agents view
                        // shows nothing (the agent was hard-deleted). Mirrors deleteAgent().
                        targetRepository.findByAssignedAgent(agent.getId()).ifPresent(target -> {
                            target.setAssignedAgent(null);
                            target.setStatus(TargetStatus.OFFLINE);
                            targetRepository.save(target);
                            log.info("Target {} released after agent {} teardown", target.getId(), agentId);
                        });
                        // Reap the torn-down agent now that its outcome is recorded.
                        agentRepository.delete(agent);
                        log.info("Agent {} reaped after teardown report ({})", agentId, request.getTrigger());
                    });

                    return agentTeardownRepository.save(record);
                });
    }

    @Override
    public Plan getAgentPlan(String agentId) throws AgentException {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentException("agent.error.agentNotFound"));

        return agent.getPlan();
    }

    @Override
    public Plan updateStepStatus(String agentId, int stepIndex, UpdateStepRequest request) throws AgentException {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentException("agent.error.agentNotFound"));

        Plan plan = agent.getPlan();
        if (plan == null) {
            throw new AgentException("agent.error.noPlanAssigned");
        }

        List<Step> steps = plan.getSteps();
        if (steps == null || stepIndex < 0 || stepIndex >= steps.size()) {
            throw new AgentException("agent.error.invalidStepIndex");
        }

        Step step = steps.get(stepIndex);
        step.setStatus(request.getStatus());

        // Append new logs to existing logs
        if (request.getLogs() != null && !request.getLogs().isEmpty()) {
            List<String> existingLogs = step.getLogs();
            if (existingLogs == null) {
                existingLogs = new ArrayList<>();
            } else {
                existingLogs = new ArrayList<>(existingLogs);
            }
            existingLogs.addAll(request.getLogs());
            step.setLogs(existingLogs);
        }

        agent.setUpdatedAt(LocalDateTime.now());
        agent.setLastConnection(LocalDateTime.now());

        agentRepository.save(agent);

        log.info("Agent {} updated step {} to status {}", agent.getName(), stepIndex, request.getStatus());

        return plan;
    }

    @Override
    public ServiceVulnerabilityRecord lookupVulnerabilities(
            String serviceName, String serviceVersion) throws Exception {
        ServiceVulnerabilityRecord record = vulnerabilityLookupService.lookup(serviceName, serviceVersion);

        // Emit a VULNERABILITY_DETECTED alert when an agent-driven lookup returns CVEs,
        // so the "vulnerability found" alert conditions (ON_VULNERABILITY_DETECTED /
        // ON_HIGH_SEVERITY_VULNERABILITY) actually fire. Without this, only remediation
        // completion was producing alerts — detected vulnerabilities were silent.
        if (record != null && record.getCves() != null && !record.getCves().isEmpty()) {
            triggerVulnerabilityDetectedAlert(serviceName, serviceVersion, record);
        }

        return record;
    }

    private void triggerVulnerabilityDetectedAlert(
            String serviceName, String serviceVersion, ServiceVulnerabilityRecord record) {
        try {
            String maxSeverity = highestSeverity(record.getCves());
            java.util.Map<String, Object> payload = java.util.Map.of(
                    "serviceKey", record.getServiceKey() != null ? record.getServiceKey() : "",
                    "serviceName", serviceName != null ? serviceName : "",
                    "serviceVersion", serviceVersion != null ? serviceVersion : "",
                    "cveCount", record.getTotalCves(),
                    "maxSeverity", maxSeverity != null ? maxSeverity : "");

            AlertEvent event = AlertEvent.builder()
                    .type(AlertEvent.AlertEventType.VULNERABILITY_DETECTED)
                    .severity(maxSeverity)
                    .payload(payload)
                    .timestamp(Instant.now())
                    .organizationId(ProjectContext.getOrganizationId())
                    .projectId(ProjectContext.getProjectId())
                    .build();

            alertTriggerService.checkAndTrigger(event);
            log.info("Triggered VULNERABILITY_DETECTED alert for {}@{} ({} CVEs, max {})",
                    serviceName, serviceVersion, record.getTotalCves(), maxSeverity);
        } catch (Exception e) {
            log.warn("Failed to trigger VULNERABILITY_DETECTED alert for {}@{}: {}",
                    serviceName, serviceVersion, e.getMessage());
        }
    }

    private static String highestSeverity(java.util.List<CveEntry> cves) {
        java.util.List<String> order = java.util.List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");
        String best = null;
        if (cves == null) {
            return null;
        }
        for (CveEntry cve : cves) {
            String sev = cve.getSeverity();
            if (sev == null || sev.isBlank()) {
                continue;
            }
            if (best == null || order.indexOf(sev.toUpperCase()) > order.indexOf(best.toUpperCase())) {
                best = sev.toUpperCase();
            }
        }
        return best;
    }

    @Override
    public ExploitationKnowledgeResponse requestExploitationKnowledge(
            String agentId, ExploitationKnowledgeRequest request) throws Exception {
        return exploitationKnowledgeService.requestExploitationKnowledge(agentId, request);
    }
}
