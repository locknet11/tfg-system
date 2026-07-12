package com.spulido.tfg.domain.remediation.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spulido.tfg.domain.remediation.db.RemediationStrategyRepository;
import com.spulido.tfg.domain.remediation.model.RemediationAction;
import com.spulido.tfg.domain.remediation.model.RemediationStrategy;

/**
 * Loads remediation strategies from a JSON resource file into MongoDB on application startup.
 * Uses incremental seeding: existing entries are preserved, only new strategies are added.
 * Invalid entries are skipped with a logged error rather than aborting the entire seed.
 */
@Component
public class RemediationStrategyLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RemediationStrategyLoader.class);
    private static final String STRATEGIES_RESOURCE_PATH = "remediation/strategies.json";
    private static final Pattern CVE_ID_PATTERN = Pattern.compile("CVE-\\d{4}-\\d{4,}");

    private final RemediationStrategyRepository repository;
    private final ObjectMapper objectMapper;

    public RemediationStrategyLoader(RemediationStrategyRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Loading remediation strategies from {}", STRATEGIES_RESOURCE_PATH);

        try {
            List<RemediationStrategy> strategies = loadStrategies();

            if (strategies == null || strategies.isEmpty()) {
                log.warn("No strategies found in resource file: {}", STRATEGIES_RESOURCE_PATH);
                return;
            }

            int added = 0;
            int skipped = 0;
            int invalid = 0;
            int duplicated = 0;

            Set<String> seenInFile = new HashSet<>();

            for (int i = 0; i < strategies.size(); i++) {
                RemediationStrategy strategy = strategies.get(i);

                String validationError = validateStrategy(strategy, i);
                if (validationError != null) {
                    log.error("Skipping invalid strategy at index {}: {}", i, validationError);
                    invalid++;
                    continue;
                }

                // A CVE + OS pair is unique by index (cve_os_idx). A repeated pair inside the file
                // means one of the two entries can never reach the database, so it is a content bug.
                String key = strategy.getCveId() + "@" + strategy.getOperatingSystem();
                if (!seenInFile.add(key)) {
                    log.error("Skipping duplicate strategy at index {}: CVE {} on {} is already defined "
                            + "earlier in {} (only one strategy per CVE + operating system is allowed)",
                            i, strategy.getCveId(), strategy.getOperatingSystem(), STRATEGIES_RESOURCE_PATH);
                    duplicated++;
                    continue;
                }

                boolean exists = repository.existsByCveIdAndOperatingSystem(
                        strategy.getCveId(), strategy.getOperatingSystem());

                if (exists) {
                    log.debug("Strategy already exists for CVE {} on {}, skipping",
                            strategy.getCveId(), strategy.getOperatingSystem());
                    skipped++;
                    continue;
                }

                repository.save(strategy);
                added++;
                log.debug("Added strategy: CVE {} on {} for {}",
                        strategy.getCveId(), strategy.getOperatingSystem(), strategy.getPackageName());
            }

            log.info("Seed complete: {} added, {} skipped (already exist), {} invalid, {} duplicated in file",
                    added, skipped, invalid, duplicated);
        } catch (IOException e) {
            log.error("Failed to load remediation strategies from {}: {}",
                    STRATEGIES_RESOURCE_PATH, e.getMessage());
        }
    }

    /**
     * Validates a single strategy entry. Returns an error message if invalid, null if valid.
     */
    String validateStrategy(RemediationStrategy strategy, int index) {
        if (strategy.getCveId() == null || !CVE_ID_PATTERN.matcher(strategy.getCveId()).matches()) {
            return "Invalid or missing cveId: '" + strategy.getCveId() + "' (expected pattern CVE-YYYY-NNNN+)";
        }
        if (strategy.getOperatingSystem() == null || strategy.getOperatingSystem().isBlank()) {
            return "Missing operatingSystem";
        }
        if (strategy.getPackageName() == null || strategy.getPackageName().isBlank()) {
            return "Missing packageName";
        }
        if (strategy.getRemediationType() == null) {
            return "Missing remediationType";
        }
        if (strategy.getAction() == null) {
            return "Missing action";
        }
        if (strategy.getAction() != RemediationAction.MANUAL
                && (strategy.getTargetVersion() == null || strategy.getTargetVersion().isBlank())) {
            return "Missing targetVersion (required for non-MANUAL actions)";
        }
        if (strategy.getAction() != RemediationAction.MANUAL
                && (strategy.getFixCommands() == null || strategy.getFixCommands().isEmpty())) {
            return "fixCommands is missing or empty for non-MANUAL action";
        }
        if (strategy.getNotes() == null || strategy.getNotes().isBlank()) {
            return "Missing notes";
        }
        return null;
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
