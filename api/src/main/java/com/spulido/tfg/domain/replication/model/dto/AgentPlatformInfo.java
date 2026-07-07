package com.spulido.tfg.domain.replication.model.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentPlatformInfo {

    private String platform;

    private String label;

    private String agentVersion;

    private long fileSizeBytes;

    private String blake3Hash;

    private LocalDateTime lastBuilt;
}
