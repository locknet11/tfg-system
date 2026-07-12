package com.spulido.tfg.domain.report.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
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
 * An immutable, point-in-time security report generated for a single
 * organization/project. Stores the filters used, the computed summary, and the
 * backing detail items. Snapshots keep computed values so history never drifts
 * when the underlying remediation/vulnerability/target records change.
 *
 * <p>Written only on creation; there is no update path.
 */
@Document(collection = "reports")
@CompoundIndex(name = "org_proj_idx", def = "{ 'organizationId': 1, 'projectId': 1 }")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Report extends BaseEntity implements ScopedEntity {

    @Field
    private String organizationId;

    @Field
    private String projectId;

    @Field
    private String title;

    @Field
    private GenerationType generationType;

    @Field
    private Instant generatedAt;

    @Field
    private String generatedBy;

    @Field
    private ReportFilters filters;

    @Field
    private ReportSummary summary;

    @Field
    private List<ReportItem> items;
}
