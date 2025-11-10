package com.spulido.tfg.domain.agent.services;

import org.springframework.data.domain.PageRequest;

import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.dto.AgentsList;

public interface AgentService {
    AgentsList listAgents(PageRequest pageRequest);

    Agent getById(String id);

    void deleteAgent(String id);
}
