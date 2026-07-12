package com.spulido.tfg.domain.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.spulido.tfg.domain.agent.db.AgentRepository;
import com.spulido.tfg.domain.agent.model.Agent;
import com.spulido.tfg.domain.agent.model.AgentStatus;
import com.spulido.tfg.domain.dashboard.model.dto.CriticalVulnerabilityInfo;
import com.spulido.tfg.domain.dashboard.services.impl.DashboardServiceImpl;
import com.spulido.tfg.domain.remediation.db.RemediationRecordRepository;
import com.spulido.tfg.domain.remediation.model.RemediationStatus;
import com.spulido.tfg.domain.target.db.TargetRepository;
import com.spulido.tfg.domain.vulnerability.db.ServiceVulnerabilityRepository;
import com.spulido.tfg.domain.vulnerability.model.CveEntry;
import com.spulido.tfg.domain.vulnerability.model.ServiceVulnerabilityRecord;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private TargetRepository targetRepository;

    @Mock
    private RemediationRecordRepository remediationRecordRepository;

    @Mock
    private ServiceVulnerabilityRepository serviceVulnerabilityRepository;

    private DashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DashboardServiceImpl(
                agentRepository, targetRepository,
                remediationRecordRepository, serviceVulnerabilityRepository);
    }

    @Test
    void getKpis_returnsCorrectCounts() {
        Page<Agent> agentPage = new PageImpl<>(List.of(
                activeAgent(),
                activeAgent(),
                inactiveAgent()));
        when(agentRepository.findAllScoped(any(Pageable.class))).thenReturn(agentPage);

        Page<?> targetPage = new PageImpl<>(List.of(new Object(), new Object(), new Object(), new Object()));
        when(targetRepository.findAllScoped(any(Pageable.class))).thenReturn((Page) targetPage);

        when(remediationRecordRepository.countByOrganizationIdAndProjectIdAndStatus(
                eq(""), eq(""), eq(RemediationStatus.SUCCESS))).thenReturn(15L);

        var kpis = service.getKpis();

        assertThat(kpis.getTargetsCount()).isEqualTo(4L);
        assertThat(kpis.getActiveAgentsCount()).isEqualTo(2L);
        assertThat(kpis.getFixedVulnerabilitiesCount()).isEqualTo(15L);
    }

    @Test
    void getCriticalVulnerabilities_returnsMax10Flattened() {
        ServiceVulnerabilityRecord record = new ServiceVulnerabilityRecord();
        record.setServiceKey("svc:1");
        record.setServiceName("test-service");
        record.setFetchedAt(Instant.now());
        record.setCves(List.of(
                criticalCve("CVE-2025-001", "High severity bug", 9.8),
                criticalCve("CVE-2025-002", "Critical vuln", 10.0)));

        when(serviceVulnerabilityRepository.findByCveSeverity(eq("CRITICAL"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        List<CriticalVulnerabilityInfo> result = service.getCriticalVulnerabilities();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCveId()).isEqualTo("CVE-2025-001");
        assertThat(result.get(0).getServiceName()).isEqualTo("test-service");
        assertThat(result.get(1).getCveId()).isEqualTo("CVE-2025-002");
    }

    @Test
    void getCriticalVulnerabilities_excludesNonCriticalCves() {
        ServiceVulnerabilityRecord record = new ServiceVulnerabilityRecord();
        record.setServiceKey("svc:2");
        record.setServiceName("other-service");
        record.setFetchedAt(Instant.now());
        record.setCves(List.of(
                highCve("CVE-2025-003", "High vuln", 7.5),
                criticalCve("CVE-2025-004", "Critical vuln", 9.0)));

        when(serviceVulnerabilityRepository.findByCveSeverity(eq("CRITICAL"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        List<CriticalVulnerabilityInfo> result = service.getCriticalVulnerabilities();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCveId()).isEqualTo("CVE-2025-004");
    }

    @Test
    void getVulnerabilityTrend_returnsCorrectMonthCounts() {
        Instant now = Instant.now();
        ServiceVulnerabilityRecord record = new ServiceVulnerabilityRecord();
        record.setFetchedAt(now);
        record.setCves(List.of());

        when(serviceVulnerabilityRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        var trend = service.getVulnerabilityTrend(6);

        assertThat(trend).hasSize(6);
        assertThat(trend).allMatch(p -> p.getCount() >= 0);
        assertThat(trend.stream().filter(p -> p.getCount() > 0).count()).isGreaterThan(0);
    }

    @Test
    void getVulnerabilityTrend_clampsMonthsToRange() {
        when(serviceVulnerabilityRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var trendLow = service.getVulnerabilityTrend(0);
        assertThat(trendLow).hasSize(6);

        var trendHigh = service.getVulnerabilityTrend(25);
        assertThat(trendHigh).hasSize(24);
    }

    private Agent activeAgent() {
        Agent agent = new Agent();
        agent.setStatus(AgentStatus.ACTIVE);
        return agent;
    }

    private Agent inactiveAgent() {
        Agent agent = new Agent();
        agent.setStatus(AgentStatus.UNRESPONSIVE);
        return agent;
    }

    private CveEntry criticalCve(String cveId, String description, double cvss) {
        CveEntry cve = new CveEntry();
        cve.setCveId(cveId);
        cve.setDescription(description);
        cve.setCvssScore(cvss);
        cve.setSeverity("CRITICAL");
        return cve;
    }

    private CveEntry highCve(String cveId, String description, double cvss) {
        CveEntry cve = new CveEntry();
        cve.setCveId(cveId);
        cve.setDescription(description);
        cve.setCvssScore(cvss);
        cve.setSeverity("HIGH");
        return cve;
    }
}
