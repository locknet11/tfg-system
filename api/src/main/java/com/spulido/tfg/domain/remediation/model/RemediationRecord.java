package com.spulido.tfg.domain.remediation.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.spulido.tfg.domain.BaseEntity;
import com.spulido.tfg.domain.ScopedEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a single remediation attempt for a specific CVE on a specific target.
 * Each record tracks the full lifecycle: from pending to success/failure/skipped,
 * including execution logs and verification results.
 */
@Document(collection = "remediation_records")
@CompoundIndex(name = "org_proj_idx", def = "{ 'organizationId': 1, 'projectId': 1 }")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RemediationRecord extends BaseEntity implements ScopedEntity {

    @Field
    private String vulnerabilityRecordId;

    @Field
    @Indexed
    private String cveId;

    @Field
    @Indexed
    private String targetId;

    @Field
    @Indexed
    private String agentId;

    @Field
    private String planId;

    @Field
    private RemediationType remediationType;

    @Field
    @Indexed
    private RemediationStatus status;

    @Field
    private String packageName;

    @Field
    private String packageVersionBefore;

    @Field
    private String packageVersionAfter;

    @Field
    private String actionDescription;

    @Field
    private List<String> preCheckLogs;

    @Field
    private List<String> executionLogs;

    @Field
    private List<String> postCheckLogs;

    @Field
    private Instant startedAt;

    @Field
    private Instant completedAt;

    @Field
    private String errorMessage;

    @Field
    private String rollbackHint;

    @Field
    private String skipReason;

    @Field
    private String organizationId;

    @Field
    private String projectId;
}
