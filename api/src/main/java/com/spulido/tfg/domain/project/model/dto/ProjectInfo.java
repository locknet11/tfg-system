package com.spulido.tfg.domain.project.model.dto;

import com.spulido.tfg.domain.project.model.ProjectStatus;
import com.spulido.tfg.domain.replication.model.ReplicationPolicy;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ProjectInfo {
    private String id;
    private String name;
    private String projectIdentifier;
    private String description;
    private String organizationId;
    private ProjectStatus status;
    private ReplicationPolicy replicationPolicy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}