package com.spulido.tfg.domain.report.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.mapping.Field;

import com.spulido.tfg.domain.remediation.model.RemediationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The optional narrowing criteria captured with a report snapshot.
 * Empty severities/statuses mean "all".
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ReportFilters {

    @Field
    private String targetId;

    @Field
    private Instant from;

    @Field
    private Instant to;

    @Field
    private List<String> severities;

    @Field
    private List<RemediationStatus> statuses;
}
