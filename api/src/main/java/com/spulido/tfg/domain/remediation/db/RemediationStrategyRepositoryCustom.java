package com.spulido.tfg.domain.remediation.db;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.spulido.tfg.domain.remediation.model.RemediationStrategy;

/**
 * Dynamic search over the strategy catalog. Derived queries cannot express an arbitrary
 * combination of optional filters, so the criteria are assembled by hand.
 */
public interface RemediationStrategyRepositoryCustom {

    /**
     * Search strategies matching every non-blank filter (AND semantics).
     * {@code cveId} and {@code packageName} match as case-insensitive substrings;
     * {@code operatingSystem}, {@code remediationType} and {@code action} match exactly.
     * A filter whose value is not a valid enum constant matches nothing.
     */
    Page<RemediationStrategy> search(String cveId, String operatingSystem, String packageName,
            String remediationType, String action, Pageable pageable);
}
