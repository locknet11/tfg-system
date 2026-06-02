package com.spulido.tfg.domain.replication.model;

import java.time.LocalDateTime;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.spulido.tfg.domain.BaseEntity;
import com.spulido.tfg.domain.ScopedEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Document(collection = "replication_requests")
@CompoundIndex(name = "org_proj_idx", def = "{ 'organizationId': 1, 'projectId': 1 }")
@CompoundIndex(name = "target_exploit_status_idx", def = "{ 'targetIp': 1, 'exploitId': 1, 'status': 1 }")
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor
public class ReplicationRequest extends BaseEntity implements ScopedEntity {

    @Field
    private String parentAgentId;

    @Field
    private String targetIp;

    @Field
    private Integer targetPort;

    @Field
    private String exploitId;

    @Field
    private String cveId;

    @Field
    private String serviceName;

    @Field
    private String serviceVersion;

    @Field
    private String severity;

    @Field
    @Indexed
    private ReplicationRequestStatus status;

    @Field
    @Indexed(unique = true)
    private String replicationToken;

    @Field
    private String downloadUrl;

    @Field
    private ReplicationApprovalMode policy;

    @Field
    private String approvedBy;

    @Field
    private String preauthCode;

    @Field
    @Indexed
    private String organizationId;

    @Field
    @Indexed
    private String projectId;

    @Field
    private LocalDateTime expiresAt;

    @Field
    private LocalDateTime resolvedAt;

    @Override
    public String getOrganizationId() {
        return organizationId;
    }

    @Override
    public String getProjectId() {
        return projectId;
    }

    @Override
    public void setOrganizationIdValue(String organizationId) {
        this.organizationId = organizationId;
    }

    @Override
    public void setProjectIdValue(String projectId) {
        this.projectId = projectId;
    }
}
