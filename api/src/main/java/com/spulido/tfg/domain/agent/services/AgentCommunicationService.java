package com.spulido.tfg.domain.agent.services;

import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.dto.UpdateStepRequest;
import com.spulido.tfg.domain.plan.model.Plan;

/**
 * Service interface for agent-to-platform communication operations.
 */
public interface AgentCommunicationService {

    /**
     * Updates the agent's heartbeat (lastConnection timestamp).
     *
     * @param agentId the agent's ID
     * @return the updated agent
     * @throws AgentException if agent not found
     */
    Agent updateHeartbeat(String agentId) throws AgentException;

    /**
     * Gets the current plan assigned to the agent.
     *
     * @param agentId the agent's ID
     * @return the plan or null if no plan assigned
     * @throws AgentException if agent not found
     */
    Plan getAgentPlan(String agentId) throws AgentException;

    /**
     * Updates the status and logs of a specific step.
     *
     * @param agentId    the agent's ID
     * @param stepIndex  the index of the step to update
     * @param request    the update request containing new status and logs
     * @return the updated plan
     * @throws AgentException if agent not found or step index invalid
     */
    Plan updateStepStatus(String agentId, int stepIndex, UpdateStepRequest request) throws AgentException;
}
