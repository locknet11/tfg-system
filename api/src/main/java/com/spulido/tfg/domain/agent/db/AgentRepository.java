package com.spulido.tfg.domain.agent.db;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.AgentStatus;

public interface AgentRepository extends MongoRepository<Agent, String> {
    
    // Scoped queries - use ProjectContext to filter by org/project
    @Query("{ 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()}, 'projectId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getProjectId()} }")
    Page<Agent> findAllScoped(Pageable pageable);

    @Query("{ '$and': [" +
        "{ 'name': { '$regex': ?0, '$options': 'i' } }, " +
        "{ 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()}, " +
        "'projectId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getProjectId()} }" +
        "] }")
    Page<Agent> findByQueryScoped(String query, Pageable pageable);
    
    @Query("{ '_id': ?0, 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()}, 'projectId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getProjectId()} }")
    Optional<Agent> findByIdScoped(String id);

    // Find agent by API key (for agent authentication)
    Optional<Agent> findByApiKey(String apiKey);

    // Unscoped query — crosses all org/project boundaries (used by heartbeat monitor scheduler)
    // Finds agents in candidate statuses with stale heartbeats
    List<Agent> findByStatusInAndLastConnectionBefore(List<AgentStatus> statuses, LocalDateTime cutoff);
}
