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

    @Query("{ '$and': [" +
        "{ '$or': [" +
            "{ 'systemName': { '$regex': ?0, '$options': 'i' } }, " +
            "{ 'description': { '$regex': ?0, '$options': 'i' } }" +
        "] }, " +
        "{ 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()}, " +
        "'projectId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getProjectId()} }" +
        "] }")
    Page<Target> findByQueryScoped(String query, Pageable pageable);
    
    @Query("{ '_id': ?0, 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()}, 'projectId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getProjectId()} }")
    Optional<Target> findByIdScoped(String id);
    
    @Query("{ 'uniqueId': ?0, 'projectId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getProjectId()} }")
    Optional<Target> findByUniqueIdScoped(String uniqueId);
    
    // Unscoped queries - for internal use (e.g., agent registration)
    @Query("{ 'ipOrDomain' : ?0 }")
    Optional<Target> findByIpOrDomain(String ipOrDomain);
    
    Optional<Target> findByUniqueId(String uniqueId);

    // Machine-identity lookup scoped to org/project (used to de-duplicate exploited targets)
    Optional<Target> findByIpOrDomainAndOrganizationIdAndProjectId(String ipOrDomain, String organizationId,
            String projectId);

    @Query("{ 'assignedAgent' : ?0 }")
    Optional<Target> findByAssignedAgent(String agentId);

    Optional<Target> findByPreauthCode(String preauthCode);
}
