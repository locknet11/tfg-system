package com.spulido.tfg.domain.agent.controller;

import java.time.LocalDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.config.security.AgentApiKeyFilter.AgentAuthenticationToken;
import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.dto.HeartbeatResponse;
import com.spulido.tfg.domain.agent.model.dto.UpdateStepRequest;
import com.spulido.tfg.domain.agent.services.AgentCommunicationService;
import com.spulido.tfg.domain.plan.model.Plan;
import com.spulido.tfg.domain.plan.model.dto.PlanInfo;
import com.spulido.tfg.domain.plan.services.PlanMapper;
import com.spulido.tfg.domain.vulnerability.model.ServiceVulnerabilityRecord;
import com.spulido.tfg.domain.vulnerability.model.dto.VulnerabilityLookupRequest;
import com.spulido.tfg.domain.vulnerability.model.dto.VulnerabilityLookupResponse;
import com.spulido.tfg.domain.vulnerability.services.VulnerabilityMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controller for agent-to-platform communication endpoints.
 * These endpoints are authenticated via API key (X-Agent-Api-Key header).
 */
@RestController
@RequestMapping("/api/agent/comm")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENT')")
public class AgentCommunicationController {

    private final AgentCommunicationService communicationService;
    private final PlanMapper planMapper;
    private final VulnerabilityMapper vulnerabilityMapper;

    /**
     * Heartbeat endpoint - updates the agent's lastConnection timestamp.
     */
    @PutMapping("/heartbeat")
    public ResponseEntity<HeartbeatResponse> heartbeat(
            @AuthenticationPrincipal String agentId) throws AgentException {

        Agent agent = communicationService.updateHeartbeat(agentId);

        HeartbeatResponse response = HeartbeatResponse.builder()
                .agentId(agent.getId())
                .status(agent.getStatus().toString())
                .lastConnection(agent.getLastConnection())
                .hasPlan(agent.getPlan() != null)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Get the current plan assigned to the agent.
     */
    @GetMapping("/plan")
    public ResponseEntity<PlanInfo> getPlan(
            @AuthenticationPrincipal String agentId) throws AgentException {

        Plan plan = communicationService.getAgentPlan(agentId);

        if (plan == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(planMapper.planToInfo(plan));
    }

    /**
     * Update the status and logs of a specific step in the agent's plan.
     */
    @PutMapping("/plan/step/{stepIndex}")
    public ResponseEntity<PlanInfo> updateStep(
            @AuthenticationPrincipal String agentId,
            @PathVariable("stepIndex") int stepIndex,
            @RequestBody @Valid UpdateStepRequest request) throws AgentException {

        Plan plan = communicationService.updateStepStatus(agentId, stepIndex, request);

        return ResponseEntity.ok(planMapper.planToInfo(plan));
    }

    /**
     * Lookup vulnerability and exploit data for a service+version.
     * Uses lazy-loading: returns cached data if available, otherwise queries NVD.
     */
    @PostMapping("/vulnerabilities/lookup")
    public ResponseEntity<VulnerabilityLookupResponse> lookupVulnerabilities(
            @RequestBody @Valid VulnerabilityLookupRequest request) throws Exception {

        ServiceVulnerabilityRecord record = communicationService.lookupVulnerabilities(
                request.getServiceName(), request.getServiceVersion());

        return ResponseEntity.ok(vulnerabilityMapper.recordToResponse(record));
    }
}
