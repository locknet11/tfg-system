package com.spulido.tfg.domain.agent.services;

import org.springframework.data.domain.PageRequest;

import com.spulido.tfg.domain.agent.exception.AgentException;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.dto.AgentRegistrationResponse;
import com.spulido.tfg.domain.agent.model.dto.AgentsList;
import com.spulido.tfg.domain.agent.model.dto.RegisterAgentRequest;

public interface AgentService {
    AgentsList listAgents(PageRequest pageRequest);

    Agent getById(String id);

    void deleteAgent(String id);

    AgentRegistrationResponse registerAgent(RegisterAgentRequest request) throws AgentException;
}
