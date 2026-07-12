package com.spulido.tfg.domain.report.model.dto;

import java.time.Instant;
import java.util.List;

import com.spulido.tfg.domain.remediation.model.RemediationStatus;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filter body for generating a report. All fields are optional; an absent or
 * empty value applies no narrowing.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ReportGenerateRequest {

    private String targetId;

    private Instant from;

    private Instant to;

    private List<String> severities;

    private List<RemediationStatus> statuses;

    /**
     * When both bounds are present, {@code from} must not be after {@code to}.
     */
    @AssertTrue(message = "exception.invalidFieldValue")
    public boolean isDateRangeValid() {
        if (from == null || to == null) {
            return true;
        }
        return !from.isAfter(to);
    }
}
