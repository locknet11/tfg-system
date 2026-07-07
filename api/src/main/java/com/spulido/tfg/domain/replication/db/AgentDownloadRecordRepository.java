package com.spulido.tfg.domain.replication.db;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.spulido.tfg.domain.replication.model.AgentDownloadRecord;

@Repository
public interface AgentDownloadRecordRepository extends MongoRepository<AgentDownloadRecord, String> {

    @Query("{ 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()} }")
    Page<AgentDownloadRecord> findAllScoped(Pageable pageable);

    @Query("{ 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()}, 'platform': ?0 }")
    Page<AgentDownloadRecord> findAllScopedByPlatform(String platform, Pageable pageable);

    @Query("{ 'organizationId': ?#{T(com.spulido.tfg.common.context.ProjectContext).getOrganizationId()}, 'userId': ?0 }")
    Page<AgentDownloadRecord> findAllScopedByUserId(String userId, Pageable pageable);
}
