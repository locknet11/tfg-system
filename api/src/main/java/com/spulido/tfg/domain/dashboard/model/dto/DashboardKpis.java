package com.spulido.tfg.domain.dashboard.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpis {

    private long targetsCount;
    private long activeAgentsCount;
    private long fixedVulnerabilitiesCount;
}
