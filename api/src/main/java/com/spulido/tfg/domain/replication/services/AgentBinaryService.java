package com.spulido.tfg.domain.replication.services;

public interface AgentBinaryService {

    byte[] getBinaryBytes();

    String getBlake3Hash();

    String getSignedManifest();
}
