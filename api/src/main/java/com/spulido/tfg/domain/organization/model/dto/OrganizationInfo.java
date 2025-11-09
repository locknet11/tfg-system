package com.spulido.tfg.domain.organization.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class OrganizationInfo {
    private String id;
    private String name;
    private String organizationIdentifier;
    private String description;
    private String ownerId;
    private List<String> projectIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}