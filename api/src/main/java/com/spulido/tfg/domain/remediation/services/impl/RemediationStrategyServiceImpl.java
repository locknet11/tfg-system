package com.spulido.tfg.domain.remediation.services.impl;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.spulido.tfg.domain.remediation.db.RemediationStrategyRepository;
import com.spulido.tfg.domain.remediation.model.RemediationStrategy;
import com.spulido.tfg.domain.remediation.services.RemediationStrategyService;

@Service
public class RemediationStrategyServiceImpl implements RemediationStrategyService {

    private static final Logger log = LoggerFactory.getLogger(RemediationStrategyServiceImpl.class);

    private final RemediationStrategyRepository repository;

    public RemediationStrategyServiceImpl(RemediationStrategyRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Optional<RemediationStrategy> resolveStrategy(String cveId, String operatingSystem) {
        if (cveId == null || cveId.isBlank()) {
            log.warn("Cannot resolve strategy: cveId is null or blank");
            return Optional.empty();
        }
        if (operatingSystem == null || operatingSystem.isBlank()) {
            log.warn("Cannot resolve strategy: operatingSystem is null or blank");
            return Optional.empty();
        }

        log.debug("Resolving remediation strategy for CVE: {}, OS: {}", cveId, operatingSystem);

        Optional<RemediationStrategy> strategy = repository.findByCveIdAndOperatingSystem(cveId, operatingSystem);

        if (strategy.isPresent()) {
            log.info("Found strategy for CVE: {}, OS: {}, package: {}",
                    cveId, operatingSystem, strategy.get().getPackageName());
        } else {
            log.info("No strategy found for CVE: {}, OS: {}", cveId, operatingSystem);
        }

        return strategy;
    }

    @Override
    public boolean hasStrategy(String cveId, String operatingSystem) {
        if (cveId == null || operatingSystem == null) {
            return false;
        }
        return repository.existsByCveIdAndOperatingSystem(cveId, operatingSystem);
    }

    @Override
    public long countStrategies() {
        return repository.count();
    }

    @Override
    public Optional<RemediationStrategy> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return repository.findById(id);
    }

    @Override
    public Page<RemediationStrategy> findAll(String cveId, String operatingSystem,
            String packageName, String remediationType, String action, Pageable pageable) {

        return repository.search(cveId, operatingSystem, packageName, remediationType, action, pageable);
    }
}
