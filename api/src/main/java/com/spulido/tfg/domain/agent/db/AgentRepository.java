package com.spulido.tfg.domain.agent.db;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spulido.tfg.domain.agent.model.Agent;

public interface AgentRepository extends MongoRepository<Agent, String> {
}
