package com.spulido.tfg.domain.remediation.controller;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.remediation.model.RemediationStrategy;
import com.spulido.tfg.domain.remediation.model.dto.StrategyCatalogEntry;
import com.spulido.tfg.domain.remediation.model.dto.StrategyCountResponse;
import com.spulido.tfg.domain.remediation.services.RemediationStrategyService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for remediation strategy catalog.
 * Authenticated via JWT session (ADMIN, OPERATOR roles).
 */
@RestController
@RequestMapping("/api/remediation-strategies")
@RequiredArgsConstructor
public class RemediationStrategyController {

    private final RemediationStrategyService strategyService;

    /**
     * List remediation strategies with pagination and optional filters.
     */
    @GetMapping
    public ResponseEntity<Page<StrategyCatalogEntry>> listStrategies(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "cveId", required = false) String cveId,
            @RequestParam(value = "operatingSystem", required = false) String operatingSystem,
            @RequestParam(value = "packageName", required = false) String packageName,
            @RequestParam(value = "remediationType", required = false) String remediationType,
            @RequestParam(value = "action", required = false) String action) {

        int pageSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "cveId"));

        Page<RemediationStrategy> strategies = strategyService.findAll(
                cveId, operatingSystem, packageName, remediationType, action, pageable);

        Page<StrategyCatalogEntry> response = strategies.map(StrategyCatalogEntry::from);

        return ResponseEntity.ok(response);
    }

    /**
     * Get a single strategy by its document ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<StrategyCatalogEntry> getStrategy(@PathVariable String id) {
        return strategyService.findById(id)
                .map(StrategyCatalogEntry::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get aggregate counts for strategy catalog dashboard metrics.
     */
    @GetMapping("/count")
    public ResponseEntity<StrategyCountResponse> getCount() {
        long total = strategyService.countStrategies();

        // Aggregate counts would ideally use MongoDB aggregation,
        // but for simplicity we iterate the small strategy catalog
        Page<RemediationStrategy> allPage = strategyService.findAll(null, null, null, null, null,
                PageRequest.of(0, 500));

        Map<String, Long> byType = allPage.getContent().stream()
                .collect(Collectors.groupingBy(
                        s -> s.getRemediationType() != null ? s.getRemediationType().name() : "UNKNOWN",
                        Collectors.counting()));

        Map<String, Long> byOs = allPage.getContent().stream()
                .collect(Collectors.groupingBy(
                        s -> s.getOperatingSystem() != null ? s.getOperatingSystem() : "unknown",
                        Collectors.counting()));

        return ResponseEntity.ok(new StrategyCountResponse(total, byType, byOs));
    }
}
