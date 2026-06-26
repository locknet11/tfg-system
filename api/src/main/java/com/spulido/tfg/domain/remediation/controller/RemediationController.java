package com.spulido.tfg.domain.remediation.controller;

import java.util.Optional;

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

import com.spulido.tfg.domain.remediation.model.RemediationRecord;
import com.spulido.tfg.domain.remediation.model.RemediationStatus;
import com.spulido.tfg.domain.remediation.model.dto.RemediationInfo;
import com.spulido.tfg.domain.remediation.model.dto.RemediationStatistics;
import com.spulido.tfg.domain.remediation.services.RemediationService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for remediation history and statistics.
 * Authenticated via JWT session (ADMIN, OPERATOR roles).
 */
@RestController
@RequestMapping("/api/remediations")
@RequiredArgsConstructor
public class RemediationController {

    private final RemediationService remediationService;

    /**
     * List remediation records with pagination and optional filters.
     */
    @GetMapping
    public ResponseEntity<Page<RemediationInfo>> listRemediations(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "targetId", required = false) String targetId) {

        int pageSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        RemediationStatus statusFilter = parseStatus(status);

        Page<RemediationRecord> records = remediationService.findAll(statusFilter, targetId, pageable);

        Page<RemediationInfo> infos = records.map(remediationService::toInfo);

        return ResponseEntity.ok(infos);
    }

    /**
     * Get a single remediation record with full details and logs.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RemediationInfo> getRemediation(@PathVariable String id) {
        RemediationRecord record = remediationService.findById(id);
        return ResponseEntity.ok(remediationService.toInfo(record));
    }

    /**
     * Get remediation statistics for the dashboard widget.
     */
    @GetMapping("/statistics")
    public ResponseEntity<RemediationStatistics> getStatistics() {
        return ResponseEntity.ok(remediationService.getStatistics());
    }

    private RemediationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return RemediationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
