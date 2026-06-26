package com.spulido.tfg.domain.remediation.db;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.spulido.tfg.domain.remediation.model.RemediationStrategy;

@Repository
public interface RemediationStrategyRepository extends MongoRepository<RemediationStrategy, String> {

    Optional<RemediationStrategy> findByCveIdAndOperatingSystem(String cveId, String operatingSystem);

    Optional<RemediationStrategy> findByCveId(String cveId);

    boolean existsByCveIdAndOperatingSystem(String cveId, String operatingSystem);

    long count();
}
