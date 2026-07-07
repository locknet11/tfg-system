package com.spulido.tfg.domain.replication.model.dto;

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
public class AgentDownloadInfo {

    private String platform;

    private String agentVersion;

    private long fileSizeBytes;

    private String blake3Hash;

    private String downloadUrl;
}
