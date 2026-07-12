package com.spulido.tfg.domain.report.model;

import java.time.Instant;

import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One backing detail row of a report snapshot, derived from a single remediation
 * record and enriched with CVE metadata and the target name at generation time.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ReportItem {

    @Field
    private String cveId;

    @Field
    private String severity;

    @Field
    private Double cvssScore;

    @Field
    private String targetId;

    @Field
    private String targetName;

    @Field
    private String remediationStatus;

    @Field
    private Instant startedAt;

    @Field
    private Instant completedAt;
}
