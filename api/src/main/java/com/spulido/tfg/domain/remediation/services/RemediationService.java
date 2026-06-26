package com.spulido.tfg.domain.remediation.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.spulido.tfg.domain.remediation.model.RemediationRecord;
import com.spulido.tfg.domain.remediation.model.RemediationStatus;
import com.spulido.tfg.domain.remediation.model.dto.RemediationInfo;
import com.spulido.tfg.domain.remediation.model.dto.RemediationReportRequest;
import com.spulido.tfg.domain.remediation.model.dto.RemediationStatistics;

/**
 * Service for managing remediation records and operations.
 */
public interface RemediationService {

    /**
     * Create a new remediation record from an agent report.
     *
     * @param request the remediation report from the agent
     * @param agentId the ID of the agent performing the remediation
     * @return the created remediation record
     */
    RemediationRecord createRemediation(RemediationReportRequest request, String agentId);

    /**
     * Find a remediation record by ID.
     *
     * @param id the remediation record ID
     * @return the remediation record
     * @throws com.spulido.tfg.domain.remediation.exception.RemediationException if not found
     */
    RemediationRecord findById(String id);

    /**
     * List remediation records with pagination and filtering.
     *
     * @param status optional status filter
     * @param targetId optional target filter
     * @param pageable pagination parameters
     * @return page of remediation records
     */
    Page<RemediationRecord> findAll(RemediationStatus status, String targetId, Pageable pageable);

    /**
     * Get statistics for remediation operations.
     *
     * @return statistics including counts by status and recent activity
     */
    RemediationStatistics getStatistics();

    /**
     * Convert a remediation record to a DTO for API responses.
     */
    RemediationInfo toInfo(RemediationRecord record);
}
