package com.spulido.tfg.domain.report.model;

import java.util.Map;

import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The computed roll-up stored on a report snapshot. All values are captured at
 * generation time and never recomputed afterwards (immutable snapshot).
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ReportSummary {

    @Field
    private Map<String, Long> vulnerabilitiesBySeverity;

    @Field
    private Map<String, Long> remediationsByStatus;

    @Field
    private long meanTimeToRemediateSeconds;

    @Field
    private int targetsCovered;

    @Field
    private long totalVulnerabilities;

    @Field
    private long totalRemediations;
}
