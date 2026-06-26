package com.spulido.tfg.domain.remediation.model.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemediationStatistics {

    private long totalCount;
    private Map<String, Long> byStatus;
    private long meanTimeToRemediateSeconds;
    private List<RecentActivity> recentActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String id;
        private String cveId;
        private String targetName;
        private String status;
        private String completedAt;
    }
}
