package com.spulido.tfg.domain.dashboard.services.impl;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.spulido.tfg.common.context.ProjectContext;
import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.model.AgentStatus;
import com.spulido.tfg.domain.dashboard.model.dto.CriticalVulnerabilityInfo;
import com.spulido.tfg.domain.dashboard.model.dto.DashboardKpis;
import com.spulido.tfg.domain.dashboard.model.dto.VulnerabilityTrendPoint;
import com.spulido.tfg.domain.dashboard.services.DashboardService;
import com.spulido.tfg.domain.remediation.db.RemediationRecordRepository;
import com.spulido.tfg.domain.remediation.model.RemediationStatus;
import com.spulido.tfg.domain.target.db.TargetRepository;
import com.spulido.tfg.domain.vulnerability.db.ServiceVulnerabilityRepository;
import com.spulido.tfg.domain.vulnerability.model.CveEntry;
import com.spulido.tfg.domain.vulnerability.model.ServiceVulnerabilityRecord;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final AgentRepository agentRepository;
    private final TargetRepository targetRepository;
    private final RemediationRecordRepository remediationRecordRepository;
    private final ServiceVulnerabilityRepository serviceVulnerabilityRepository;

    public DashboardServiceImpl(AgentRepository agentRepository,
            TargetRepository targetRepository,
            RemediationRecordRepository remediationRecordRepository,
            ServiceVulnerabilityRepository serviceVulnerabilityRepository) {
        this.agentRepository = Objects.requireNonNull(agentRepository, "agentRepository must not be null");
        this.targetRepository = Objects.requireNonNull(targetRepository, "targetRepository must not be null");
        this.remediationRecordRepository = Objects.requireNonNull(remediationRecordRepository,
                "remediationRecordRepository must not be null");
        this.serviceVulnerabilityRepository = Objects.requireNonNull(serviceVulnerabilityRepository,
                "serviceVulnerabilityRepository must not be null");
    }

    @Override
    public DashboardKpis getKpis() {
        String orgId = getContextOrgId();
        String projectId = getContextProjectId();

        long targetsCount = targetRepository.findAllScoped(PageRequest.of(0, 1)).getTotalElements();

        long activeAgentsCount = agentRepository.findAllScoped(PageRequest.of(0, Integer.MAX_VALUE))
                .stream()
                .filter(agent -> agent.getStatus() == AgentStatus.ACTIVE)
                .count();

        long fixedVulnerabilitiesCount = remediationRecordRepository
                .countByOrganizationIdAndProjectIdAndStatus(orgId, projectId, RemediationStatus.SUCCESS);

        return DashboardKpis.builder()
                .targetsCount(targetsCount)
                .activeAgentsCount(activeAgentsCount)
                .fixedVulnerabilitiesCount(fixedVulnerabilitiesCount)
                .build();
    }

    @Override
    public List<CriticalVulnerabilityInfo> getCriticalVulnerabilities() {
        var pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "fetchedAt"));
        var records = serviceVulnerabilityRepository.findByCveSeverity("CRITICAL", pageable);

        return records.stream()
                .flatMap(record -> record.getCves().stream()
                        .filter(cve -> "CRITICAL".equalsIgnoreCase(cve.getSeverity()))
                        .map(cve -> CriticalVulnerabilityInfo.builder()
                                .serviceKey(record.getServiceKey())
                                .cveId(cve.getCveId())
                                .description(cve.getDescription())
                                .serviceName(record.getServiceName())
                                .cvssScore(cve.getCvssScore())
                                .reportedDate(record.getFetchedAt())
                                .build()))
                .sorted(Comparator.comparing(CriticalVulnerabilityInfo::getReportedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(10)
                .collect(Collectors.toList());
    }

    @Override
    public List<VulnerabilityTrendPoint> getVulnerabilityTrend(int months) {
        if (months < 1) {
            months = 6;
        }
        if (months > 24) {
            months = 24;
        }

        Instant now = Instant.now();
        Instant cutoff = now.minus(months * 30L, ChronoUnit.DAYS);
        var pageable = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "fetchedAt"));
        var records = serviceVulnerabilityRepository.findAll(pageable);

        // Group counts by month (yyyy-MM) for records within the cutoff window
        Map<String, Long> monthCounts = new LinkedHashMap<>();

        // Initialize all months in range with zero (chronological order)
        for (int i = months - 1; i >= 0; i--) {
            String monthKey = LocalDate.ofInstant(now.minus(i * 30L, ChronoUnit.DAYS), ZoneOffset.UTC)
                    .withDayOfMonth(1)
                    .format(MONTH_FORMATTER);
            monthCounts.put(monthKey, 0L);
        }

        // Count records per month for records within the window
        for (ServiceVulnerabilityRecord record : records) {
            if (record.getFetchedAt() != null && record.getFetchedAt().isAfter(cutoff)) {
                String monthKey = LocalDate.ofInstant(record.getFetchedAt(), ZoneOffset.UTC)
                        .withDayOfMonth(1)
                        .format(MONTH_FORMATTER);
                monthCounts.merge(monthKey, 1L, Long::sum);
            }
        }

        return monthCounts.entrySet().stream()
                .map(entry -> VulnerabilityTrendPoint.builder()
                        .period(entry.getKey())
                        .count(entry.getValue())
                        .build())
                .collect(Collectors.toList());
    }

    private String getContextOrgId() {
        String orgId = ProjectContext.getOrganizationId();
        return orgId != null ? orgId : "";
    }

    private String getContextProjectId() {
        String projectId = ProjectContext.getProjectId();
        return projectId != null ? projectId : "";
    }
}
