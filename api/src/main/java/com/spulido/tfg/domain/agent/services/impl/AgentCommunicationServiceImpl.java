package com.spulido.tfg.domain.agent.services.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.AgentStatus;
import com.spulido.tfg.domain.agent.model.dto.UpdateStepRequest;
import com.spulido.tfg.domain.agent.services.AgentCommunicationService;
import com.spulido.tfg.domain.plan.model.Plan;
import com.spulido.tfg.domain.plan.model.Step;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentCommunicationServiceImpl implements AgentCommunicationService {

    private final AgentRepository agentRepository;

    @Override
    public Agent updateHeartbeat(String agentId) throws AgentException {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new AgentException("agent.error.agentNotFound"));

        agent.setLastConnection(LocalDateTime.now());

        // If agent was unresponsive, set it back to active
        if (agent.getStatus() == AgentStatus.UNRESPONSIVE) {
            agent.setStatus(AgentStatus.ACTIVE);
            log.info("Agent {} is now active again", agent.getName());
        }

        return agentRepository.save(agent);
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
}
