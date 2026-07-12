package com.spulido.tfg.domain.remediation.model.dto;

import java.util.Map;

/**
 * API response DTO for aggregated strategy counts.
 */
public record StrategyCountResponse(
        long total,
        Map<String, Long> byType,
        Map<String, Long> byOs) {
}
