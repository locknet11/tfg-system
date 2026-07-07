package com.spulido.tfg.domain.replication.services;

import java.util.List;

import com.spulido.tfg.domain.replication.model.dto.AgentPlatformInfo;

public interface AgentBinaryService {

    byte[] getBinaryBytes();

    String getBlake3Hash();

    String getSignedManifest();

    List<AgentPlatformInfo> getAvailablePlatforms();

    byte[] getBinaryForPlatform(String platform);

    String getSignedManifestForPlatform(String platform);
}
