package com.spulido.tfg.domain.agent.model.dto;

import java.util.List;

import com.spulido.tfg.domain.dashboard.model.dto.VulnerabilityTrendPoint;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMetricsResponse {

    private long activeAgents;
    private long totalAgents;
    private long detectedVulnerabilities;
    private long appliedRemediations;
    private double uptimePercentage;
    private List<VulnerabilityTrendPoint> vulnerabilityTrend;
}
