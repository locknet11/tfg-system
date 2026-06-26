package com.spulido.tfg.domain.remediation.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spulido.tfg.domain.remediation.db.RemediationStrategyRepository;
import com.spulido.tfg.domain.remediation.model.RemediationStrategy;

/**
 * Loads remediation strategies from a JSON resource file into MongoDB on application startup.
 * Only seeds if the collection is empty to avoid overwriting manually curated strategies.
 */
@Component
public class RemediationStrategyLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RemediationStrategyLoader.class);
    private static final String STRATEGIES_RESOURCE_PATH = "remediation/strategies.json";

    private final RemediationStrategyRepository repository;
    private final ObjectMapper objectMapper;

    public RemediationStrategyLoader(RemediationStrategyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        long existingCount = repository.count();

        if (existingCount > 0) {
            log.info("Remediation strategies collection already has {} entries, skipping seed", existingCount);
            return;
        }

        log.info("Remediation strategies collection is empty, seeding from {}", STRATEGIES_RESOURCE_PATH);

        try {
            List<RemediationStrategy> strategies = loadStrategies();

            if (strategies == null || strategies.isEmpty()) {
                log.warn("No strategies found in resource file: {}", STRATEGIES_RESOURCE_PATH);
                return;
            }

            repository.saveAll(strategies);
            log.info("Successfully seeded {} remediation strategies", strategies.size());
        } catch (IOException e) {
            log.error("Failed to load remediation strategies from {}: {}",
                    STRATEGIES_RESOURCE_PATH, e.getMessage());
        }
    }

    private List<RemediationStrategy> loadStrategies() throws IOException {
        ClassPathResource resource = new ClassPathResource(STRATEGIES_RESOURCE_PATH);

        if (!resource.exists()) {
            log.warn("Resource file not found: {}", STRATEGIES_RESOURCE_PATH);
            return List.of();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            RemediationStrategy[] strategies = objectMapper.readValue(json, RemediationStrategy[].class);
            return Arrays.asList(strategies);
        }
    }
}
