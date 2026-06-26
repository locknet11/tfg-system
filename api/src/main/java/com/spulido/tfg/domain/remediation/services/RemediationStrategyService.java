package com.spulido.tfg.domain.remediation.services;

import java.util.Optional;

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
}
