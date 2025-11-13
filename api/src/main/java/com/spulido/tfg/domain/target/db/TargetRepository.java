package com.spulido.tfg.domain.target.db;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.spulido.tfg.domain.target.model.Target;

public interface TargetRepository extends MongoRepository<Target, String> {
    
    // Scoped queries - use ProjectContext to filter by org/project
    @Query("{ 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()}, 'projectId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getProjectId()} }")
    Page<Target> findAllScoped(Pageable pageable);
    
    @Query("{ '_id': ?0, 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()}, 'projectId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getProjectId()} }")
    Optional<Target> findByIdScoped(String id);
    
    @Query("{ 'uniqueId': ?0, 'projectId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getProjectId()} }")
    Optional<Target> findByUniqueIdScoped(String uniqueId);
    
    // Unscoped queries - for internal use (e.g., agent registration)
    @Query("{ 'ipOrDomain' : ?0 }")
    Optional<Target> findByIpOrDomain(String ipOrDomain);
    
    Optional<Target> findByUniqueId(String uniqueId);

    @Query("{ 'assignedAgent' : ?0 }")
    Optional<Target> findByAssignedAgent(String agentId);
}
