package com.spulido.tfg.domain.agent.db;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.spulido.tfg.domain.agent.model.AgentTeardownRecord;

public interface AgentTeardownRepository extends MongoRepository<AgentTeardownRecord, String> {

    List<AgentTeardownRecord> findByAgentId(String agentId);

    Optional<AgentTeardownRecord> findFirstByAgentIdAndTrigger(String agentId, String trigger);
}
