package com.spulido.tfg.domain.replication.services;

public interface BinaryIntegrityService {

    String computeBlake3Hash(byte[] binary);

    String signHash(String hash);

    String getAlgorithm();
}
