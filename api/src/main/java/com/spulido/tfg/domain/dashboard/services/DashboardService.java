package com.spulido.tfg.domain.dashboard.services;

import java.util.List;

import com.spulido.tfg.domain.dashboard.model.dto.CriticalVulnerabilityInfo;
import com.spulido.tfg.domain.dashboard.model.dto.DashboardKpis;
import com.spulido.tfg.domain.dashboard.model.dto.VulnerabilityTrendPoint;

public interface DashboardService {

    DashboardKpis getKpis();

    List<CriticalVulnerabilityInfo> getCriticalVulnerabilities();

    List<VulnerabilityTrendPoint> getVulnerabilityTrend(int months);
}
