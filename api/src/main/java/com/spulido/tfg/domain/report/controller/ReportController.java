package com.spulido.tfg.domain.report.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.report.model.GenerationType;
import com.spulido.tfg.domain.report.model.Report;
import com.spulido.tfg.domain.report.model.dto.ReportGenerateRequest;
import com.spulido.tfg.domain.report.model.dto.ReportInfo;
import com.spulido.tfg.domain.report.services.ReportService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for report generation and history. All endpoints are
 * authenticated and scoped to the active organization/project.
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Generate and persist a report for the active org/project from the filter
     * body. Returns 201 with the snapshot, or 422 when no data matches
     * ({@code REPORT_EMPTY_RESULT}) or no project is selected.
     */
    @PostMapping
    public ResponseEntity<Report> generate(@Valid @RequestBody(required = false) ReportGenerateRequest request) {
        Report report = reportService.generate(request, GenerationType.ON_DEMAND);
        return ResponseEntity.status(HttpStatus.CREATED).body(report);
    }

    /**
     * Paged history for the active org/project, newest first.
     */
    @GetMapping
    public ResponseEntity<Page<ReportInfo>> history(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {

        int pageSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(reportService.findHistory(pageable));
    }

    /**
     * Full stored snapshot by id (tenant-scoped; 404 if not in the caller's scope).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Report> getReport(@PathVariable String id) {
        return ResponseEntity.ok(reportService.findById(id));
    }
}
