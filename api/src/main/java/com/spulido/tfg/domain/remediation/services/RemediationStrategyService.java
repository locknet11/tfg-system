package com.spulido.tfg.domain.remediation.services;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.spulido.tfg.domain.remediation.model.RemediationStrategy;

/**
 * Service for looking up remediation strategies from the knowledge base.
 */
public interface RemediationStrategyService {

    /**
     * Resolve a remediation strategy for a specific CVE and operating system.
     *
     * @param cveId the CVE identifier
     * @param operatingSystem the target operating system
     * @return Optional containing the strategy if found, empty otherwise
     */
    Optional<RemediationStrategy> resolveStrategy(String cveId, String operatingSystem);

    /**
     * Check if a strategy exists for a given CVE and OS.
     */
    boolean hasStrategy(String cveId, String operatingSystem);

    /**
     * Get total count of strategies in the knowledge base.
     */
    long countStrategies();

    /**
     * Find a strategy by its document ID.
     */
    Optional<RemediationStrategy> findById(String id);

    /**
     * List strategies with pagination and optional filters.
     */
    Page<RemediationStrategy> findAll(String cveId, String operatingSystem,
            String packageName, String remediationType, String action, Pageable pageable);
}
