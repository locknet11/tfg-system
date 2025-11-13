package com.spulido.tfg.domain.target.model.dto;

import com.spulido.tfg.domain.target.model.OperatingSystem;
import com.spulido.tfg.domain.target.model.TargetStatus;

import lombok.Data;

@Data
public class TargetInfo {
    String id;
    String systemName;
    String description;
    OperatingSystem os;
    String uniqueId;
    String organizationId;
    String projectId;
    String ipOrDomain;
    TargetStatus status;
    String assignedAgent;
}