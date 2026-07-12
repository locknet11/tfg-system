package com.spulido.tfg.domain.report.model.dto;

import java.time.Instant;

import com.spulido.tfg.domain.report.model.GenerationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * History-row projection of a stored report. Carries headline counts for the
 * list table without shipping the full item list.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ReportInfo {

    private String id;

    private String title;

    private GenerationType generationType;

    private Instant generatedAt;

    private String generatedBy;

    private long totalVulnerabilities;

    private long totalRemediations;

    private int targetsCovered;
}
