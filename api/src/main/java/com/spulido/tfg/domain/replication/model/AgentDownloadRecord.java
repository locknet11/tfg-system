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

@Document(collection = "agent_download_records")
@CompoundIndex(name = "org_time_idx", def = "{ 'organizationId': 1, 'downloadedAt': -1 }")
@CompoundIndex(name = "user_time_idx", def = "{ 'userId': 1, 'downloadedAt': -1 }")
@NoArgsConstructor
@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor
public class AgentDownloadRecord extends BaseEntity implements ScopedEntity {

    @Field
    private String userId;

    @Field
    private String userEmail;

    @Field
    @Indexed
    private String organizationId;

    @Field
    @Indexed
    private String projectId;

    @Field
    private String platform;

    @Field
    private String agentVersion;

    @Field
    private long fileSizeBytes;

    @Field
    private String blake3Hash;

    @Field
    private String clientIp;

    @Field
    private String userAgent;

    @Field
    @Indexed
    private LocalDateTime downloadedAt;

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
