package com.spulido.tfg.domain.replication.db;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.spulido.tfg.domain.replication.model.ReplicationRequest;
import com.spulido.tfg.domain.replication.model.ReplicationRequestStatus;

@Repository
public interface ReplicationRequestRepository extends MongoRepository<ReplicationRequest, String> {

    @Query("{ 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()}, 'projectId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getProjectId()} }")
    Page<ReplicationRequest> findAllScoped(Pageable pageable);

    @Query("{ 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()}, 'projectId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getProjectId()}, 'status': ?0 }")
    Page<ReplicationRequest> findAllScopedByStatus(ReplicationRequestStatus status, Pageable pageable);

    @Query("{ 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()}, 'projectId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getProjectId()}, 'severity': ?0 }")
    Page<ReplicationRequest> findAllScopedBySeverity(String severity, Pageable pageable);

    @Query("{ 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()}, 'projectId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getProjectId()}, 'status': ?0, 'severity': ?1 }")
    Page<ReplicationRequest> findAllScopedByStatusAndSeverity(ReplicationRequestStatus status, String severity, Pageable pageable);

    @Query("{ 'id': ?0, 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()}, 'projectId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getProjectId()} }")
    Optional<ReplicationRequest> findByIdScoped(String id);

    Optional<ReplicationRequest> findByReplicationToken(String replicationToken);

    Optional<ReplicationRequest> findByPreauthCode(String preauthCode);

    List<ReplicationRequest> findByStatusAndExpiresAtBefore(ReplicationRequestStatus status, LocalDateTime now);

    List<ReplicationRequest> findByParentAgentId(String parentAgentId);

    @Query("{ 'targetIp': ?0, 'exploitId': ?1, 'status': { $in: ['PENDING', 'APPROVED'] } }")
    List<ReplicationRequest> findDuplicateRequests(String targetIp, String exploitId);
}
