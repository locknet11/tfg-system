package com.spulido.tfg.domain.agent.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Acknowledgement of a persisted teardown-outcome report.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeardownReportResponse {

    private boolean received;
    private String recordId;
}
