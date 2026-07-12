package com.spulido.tfg.domain.dashboard.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.common.context.ProjectContext;
import com.spulido.tfg.domain.dashboard.model.dto.CriticalVulnerabilityInfo;
import com.spulido.tfg.domain.dashboard.model.dto.DashboardKpis;
import com.spulido.tfg.domain.dashboard.model.dto.VulnerabilityTrendPoint;
import com.spulido.tfg.domain.dashboard.services.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("isAuthenticated()")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/kpis")
    public ResponseEntity<DashboardKpis> getKpis() {
        if (!ProjectContext.hasContext()) {
            return ResponseEntity.unprocessableEntity().build();
        }
        return ResponseEntity.ok(dashboardService.getKpis());
    }

    @GetMapping("/critical-vulnerabilities")
    public ResponseEntity<List<CriticalVulnerabilityInfo>> getCriticalVulnerabilities() {
        if (!ProjectContext.hasContext()) {
            return ResponseEntity.unprocessableEntity().build();
        }
        return ResponseEntity.ok(dashboardService.getCriticalVulnerabilities());
    }

    @GetMapping("/vulnerability-trend")
    public ResponseEntity<List<VulnerabilityTrendPoint>> getVulnerabilityTrend(
            @RequestParam(defaultValue = "6") int months) {
        if (!ProjectContext.hasContext()) {
            return ResponseEntity.unprocessableEntity().build();
        }
        if (months < 1 || months > 24) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(dashboardService.getVulnerabilityTrend(months));
    }
}
