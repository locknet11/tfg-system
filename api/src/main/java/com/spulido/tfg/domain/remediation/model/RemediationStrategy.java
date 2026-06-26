package com.spulido.tfg.domain.remediation.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a remediation strategy from the knowledge base.
 * Maps a CVE + operating system combination to specific remediation commands.
 * This is a global entity (not scoped to org/project) — shared knowledge.
 */
@Document(collection = "remediation_strategies")
@CompoundIndex(name = "cve_os_idx", def = "{ 'cveId': 1, 'operatingSystem': 1 }", unique = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RemediationStrategy {

    @Id
    private String id;

    @Field
    @Indexed
    private String cveId;

    @Field
    private String operatingSystem;

    @Field
    @Indexed
    private String packageName;

    @Field
    private RemediationType remediationType;

    @Field
    private RemediationAction action;

    @Field
    private String targetVersion;

    @Field
    private List<String> preCheckCommands;

    @Field
    private List<String> fixCommands;

    @Field
    private List<String> postCheckCommands;

    @Field
    private String serviceName;

    @Field
    private boolean requiresReboot;

    @Field
    private String notes;
}
