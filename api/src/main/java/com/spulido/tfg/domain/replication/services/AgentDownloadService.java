package com.spulido.tfg.domain.replication.services;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.spulido.tfg.domain.replication.model.AgentDownloadRecord;
import com.spulido.tfg.domain.replication.model.dto.AgentDownloadInfo;
import com.spulido.tfg.domain.replication.model.dto.AgentPlatformInfo;

import jakarta.servlet.http.HttpServletRequest;

public interface AgentDownloadService {

    List<AgentPlatformInfo> listPlatforms();

    byte[] getBinaryForPlatform(String platform, HttpServletRequest request);

    String getManifestForPlatform(String platform);

    Page<AgentDownloadRecord> listDownloadRecords(Pageable pageable, String platform, String userId);
}
