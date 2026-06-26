package com.spulido.tfg.domain.remediation.db;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.spulido.tfg.domain.remediation.model.RemediationRecord;
import com.spulido.tfg.domain.remediation.model.RemediationStatus;

@Repository
public interface RemediationRecordRepository extends MongoRepository<RemediationRecord, String> {

    Page<RemediationRecord> findByOrganizationIdAndProjectId(String organizationId, String projectId, Pageable pageable);

    Page<RemediationRecord> findByOrganizationIdAndProjectIdAndStatus(
            String organizationId, String projectId, RemediationStatus status, Pageable pageable);

    Page<RemediationRecord> findByOrganizationIdAndProjectIdAndTargetId(
            String organizationId, String projectId, String targetId, Pageable pageable);

    Page<RemediationRecord> findByOrganizationIdAndProjectIdAndCveId(
            String organizationId, String projectId, String cveId, Pageable pageable);

    List<RemediationRecord> findByOrganizationIdAndProjectIdAndStatusIn(
            String organizationId, String projectId, List<RemediationStatus> statuses);

    long countByOrganizationIdAndProjectIdAndStatus(String organizationId, String projectId, RemediationStatus status);

    List<RemediationRecord> findTop5ByOrganizationIdAndProjectIdOrderByCompletedAtDesc(
            String organizationId, String projectId);

    List<RemediationRecord> findByOrganizationIdAndProjectIdAndCompletedAtBetween(
            String organizationId, String projectId, Instant from, Instant to);
}
