package com.spulido.tfg.domain.remediation.model.dto;

import java.util.List;

import com.spulido.tfg.domain.remediation.model.RemediationStrategy;

/**
 * API response DTO for a single entry in the remediation strategy catalog.
 */
public record StrategyCatalogEntry(
        String id,
        String cveId,
        String operatingSystem,
        String packageName,
        String remediationType,
        String action,
        String targetVersion,
        List<String> preCheckCommands,
        List<String> fixCommands,
        List<String> postCheckCommands,
        String serviceName,
        boolean requiresReboot,
        String notes) {

    public static StrategyCatalogEntry from(RemediationStrategy strategy) {
        return new StrategyCatalogEntry(
                strategy.getId(),
                strategy.getCveId(),
                strategy.getOperatingSystem(),
                strategy.getPackageName(),
                strategy.getRemediationType() != null ? strategy.getRemediationType().name() : null,
                strategy.getAction() != null ? strategy.getAction().name() : null,
                strategy.getTargetVersion(),
                strategy.getPreCheckCommands(),
                strategy.getFixCommands(),
                strategy.getPostCheckCommands(),
                strategy.getServiceName(),
                strategy.isRequiresReboot(),
                strategy.getNotes());
    }
}
