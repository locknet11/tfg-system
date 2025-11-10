package com.spulido.tfg.domain.agent.services.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.dto.AgentsList;
import com.spulido.tfg.domain.agent.services.AgentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentRepository repository;

    @Override
    public AgentsList listAgents(PageRequest pageRequest) {
        Page<Agent> page = repository.findAll(pageRequest);
        return new AgentsList(page.getContent(), pageRequest, page.getTotalElements());
    }

    @Override
    public Agent getById(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public void deleteAgent(String id) {
        repository.deleteById(id);
    }
}
