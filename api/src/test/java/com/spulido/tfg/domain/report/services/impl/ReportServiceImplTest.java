package com.spulido.tfg.domain.report.services.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.spulido.tfg.common.context.ProjectContext;
import com.spulido.tfg.common.exception.ErrorCode;
import com.spulido.tfg.domain.remediation.db.RemediationRecordRepository;
import com.spulido.tfg.domain.remediation.model.RemediationRecord;
import com.spulido.tfg.domain.remediation.model.RemediationStatus;
import com.spulido.tfg.domain.report.exception.ReportException;
import com.spulido.tfg.domain.report.model.GenerationType;
import com.spulido.tfg.domain.report.model.Report;
import com.spulido.tfg.domain.report.model.dto.ReportGenerateRequest;
import com.spulido.tfg.domain.target.db.TargetRepository;
import com.spulido.tfg.domain.target.model.Target;
import com.spulido.tfg.domain.vulnerability.db.ServiceVulnerabilityRepository;
import com.spulido.tfg.domain.vulnerability.model.CveEntry;
import com.spulido.tfg.domain.vulnerability.model.ServiceVulnerabilityRecord;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    private static final String ORG = "org-1";
    private static final String PROJECT = "proj-1";

    @Mock
    private com.spulido.tfg.domain.report.db.ReportRepository reportRepository;
    @Mock
    private RemediationRecordRepository remediationRepository;
    @Mock
    private ServiceVulnerabilityRepository vulnerabilityRepository;
    @Mock
    private TargetRepository targetRepository;

    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReportServiceImpl(reportRepository, remediationRepository, vulnerabilityRepository,
                targetRepository);
        ProjectContext.set(ORG, PROJECT);
    }

    @AfterEach
    void tearDown() {
        ProjectContext.clear();
    }

    private RemediationRecord record(String cveId, String targetId, RemediationStatus status,
            Instant started, Instant completed) {
        RemediationRecord r = new RemediationRecord();
        r.setCveId(cveId);
        r.setTargetId(targetId);
        r.setStatus(status);
        r.setStartedAt(started);
        r.setCompletedAt(completed);
        r.setOrganizationId(ORG);
        r.setProjectId(PROJECT);
        return r;
    }

    private Target target(String id, String systemName, String ipOrDomain) {
        Target t = new Target();
        t.setId(id);
        t.setSystemName(systemName);
        t.setIpOrDomain(ipOrDomain);
        return t;
    }

    private void stubTargets(Target... targets) {
        when(targetRepository.findAllScoped(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(targets)));
    }

    private void stubSaveEcho() {
        when(reportRepository.save(any(Report.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void generate_buildsSummaryWithSeverityStatusMttrAndTargets() {
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        RemediationRecord r1 = record("CVE-1", "t-1", RemediationStatus.SUCCESS, base, base.plusSeconds(100));
        RemediationRecord r2 = record("CVE-2", "t-2", RemediationStatus.FAILED, base, base.plusSeconds(300));
        when(remediationRepository.findByOrganizationIdAndProjectId(ORG, PROJECT)).thenReturn(List.of(r1, r2));

        CveEntry cve1 = CveEntry.builder().cveId("CVE-1").severity("critical").cvssScore(9.8).build();
        CveEntry cve2 = CveEntry.builder().cveId("CVE-2").severity("MEDIUM").cvssScore(5.0).build();
        ServiceVulnerabilityRecord svr = new ServiceVulnerabilityRecord();
        svr.setCves(List.of(cve1, cve2));
        when(vulnerabilityRepository.findAll()).thenReturn(List.of(svr));

        stubTargets(target("t-1", "web-01", "10.0.0.1"), target("t-2", "db-01", "10.0.0.2"));
        stubSaveEcho();

        Report report = service.generate(new ReportGenerateRequest(), GenerationType.ON_DEMAND);

        assertThat(report.getOrganizationId()).isEqualTo(ORG);
        assertThat(report.getProjectId()).isEqualTo(PROJECT);
        assertThat(report.getGenerationType()).isEqualTo(GenerationType.ON_DEMAND);
        assertThat(report.getItems()).hasSize(2);
        assertThat(report.getSummary().getTotalRemediations()).isEqualTo(2);
        assertThat(report.getSummary().getTotalVulnerabilities()).isEqualTo(2);
        assertThat(report.getSummary().getTargetsCovered()).isEqualTo(2);
        assertThat(report.getSummary().getVulnerabilitiesBySeverity().get("CRITICAL")).isEqualTo(1L);
        assertThat(report.getSummary().getVulnerabilitiesBySeverity().get("MEDIUM")).isEqualTo(1L);
        assertThat(report.getSummary().getRemediationsByStatus().get("SUCCESS")).isEqualTo(1L);
        assertThat(report.getSummary().getRemediationsByStatus().get("FAILED")).isEqualTo(1L);
        // MTTR only over the single SUCCESS record with both timestamps: 100s.
        assertThat(report.getSummary().getMeanTimeToRemediateSeconds()).isEqualTo(100L);
        assertThat(report.getItems().get(0).getTargetName()).isEqualTo("web-01");
    }

    @Test
    void generate_unknownSeverityWhenCveNotCached() {
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        RemediationRecord r1 = record("CVE-X", "t-1", RemediationStatus.SUCCESS, base, base.plusSeconds(10));
        when(remediationRepository.findByOrganizationIdAndProjectId(ORG, PROJECT)).thenReturn(List.of(r1));
        when(vulnerabilityRepository.findAll()).thenReturn(List.of());
        stubTargets();
        stubSaveEcho();

        Report report = service.generate(new ReportGenerateRequest(), GenerationType.ON_DEMAND);

        assertThat(report.getItems().get(0).getSeverity()).isEqualTo("UNKNOWN");
        assertThat(report.getSummary().getVulnerabilitiesBySeverity().get("UNKNOWN")).isEqualTo(1L);
    }

    @Test
    void generate_emptyResultThrowsAndPersistsNothing() {
        when(remediationRepository.findByOrganizationIdAndProjectId(ORG, PROJECT)).thenReturn(List.of());
        stubTargets();

        assertThatThrownBy(() -> service.generate(new ReportGenerateRequest(), GenerationType.ON_DEMAND))
                .isInstanceOf(ReportException.class)
                .satisfies(ex -> assertThat(((ReportException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.REPORT_EMPTY_RESULT));

        verify(reportRepository, never()).save(any());
    }

    @Test
    void generate_missingProjectContextThrows() {
        ProjectContext.clear();

        assertThatThrownBy(() -> service.generate(new ReportGenerateRequest(), GenerationType.ON_DEMAND))
                .isInstanceOf(ReportException.class)
                .satisfies(ex -> assertThat(((ReportException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.REPORT_NO_PROJECT_CONTEXT));

        verify(reportRepository, never()).save(any());
    }

    @Test
    void generate_severityFilterKeepsOnlyMatchingItems() {
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        RemediationRecord crit = record("CVE-1", "t-1", RemediationStatus.SUCCESS, base, base.plusSeconds(10));
        RemediationRecord med = record("CVE-2", "t-2", RemediationStatus.SUCCESS, base, base.plusSeconds(20));
        when(remediationRepository.findByOrganizationIdAndProjectId(ORG, PROJECT)).thenReturn(List.of(crit, med));

        ServiceVulnerabilityRecord svr = new ServiceVulnerabilityRecord();
        svr.setCves(List.of(
                CveEntry.builder().cveId("CVE-1").severity("CRITICAL").build(),
                CveEntry.builder().cveId("CVE-2").severity("MEDIUM").build()));
        when(vulnerabilityRepository.findAll()).thenReturn(List.of(svr));
        stubTargets();
        stubSaveEcho();

        ReportGenerateRequest request = ReportGenerateRequest.builder()
                .severities(List.of("CRITICAL"))
                .build();
        Report report = service.generate(request, GenerationType.ON_DEMAND);

        assertThat(report.getItems()).hasSize(1);
        assertThat(report.getItems().get(0).getSeverity()).isEqualTo("CRITICAL");
        assertThat(report.getSummary().getTotalRemediations()).isEqualTo(1);
    }

    @Test
    void findById_missingThrowsReportNotFound() {
        when(reportRepository.findByIdAndOrganizationIdAndProjectId("r-x", ORG, PROJECT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById("r-x"))
                .isInstanceOf(ReportException.class)
                .satisfies(ex -> assertThat(((ReportException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.REPORT_NOT_FOUND));
    }

    @Test
    void generate_scheduledMarksGeneratedByAsSystem() {
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        RemediationRecord r1 = record("CVE-1", "t-1", RemediationStatus.SUCCESS, base, base.plusSeconds(10));
        when(remediationRepository.findByOrganizationIdAndProjectId(ORG, PROJECT)).thenReturn(List.of(r1));
        when(vulnerabilityRepository.findAll()).thenReturn(List.of());
        stubTargets();
        stubSaveEcho();

        Report report = service.generate(null, GenerationType.SCHEDULED);

        assertThat(report.getGeneratedBy()).isEqualTo("system");
        assertThat(report.getGenerationType()).isEqualTo(GenerationType.SCHEDULED);
    }

    // US1 — target-scoped report matches records by the selected target's ipOrDomain.
    @Test
    void generate_targetFilterMatchesRecordsByTargetIpOrDomain() {
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        RemediationRecord r = record("CVE-1", "127.0.0.1", RemediationStatus.SUCCESS, base, base.plusSeconds(10));
        when(remediationRepository.findByOrganizationIdAndProjectId(ORG, PROJECT)).thenReturn(List.of(r));
        when(vulnerabilityRepository.findAll()).thenReturn(List.of());
        stubTargets(target("t-1", "vm-ubuntu", "127.0.0.1"));
        stubSaveEcho();

        ReportGenerateRequest request = ReportGenerateRequest.builder().targetId("t-1").build();
        Report report = service.generate(request, GenerationType.ON_DEMAND);

        assertThat(report.getItems()).hasSize(1);
        assertThat(report.getItems().get(0).getTargetId()).isEqualTo("127.0.0.1");
        assertThat(report.getItems().get(0).getTargetName()).isEqualTo("vm-ubuntu");
    }

    // US1 — target-scoped report still matches records whose stored targetId equals the target id.
    @Test
    void generate_targetFilterMatchesRecordsByTargetId() {
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        RemediationRecord r = record("CVE-1", "t-1", RemediationStatus.SUCCESS, base, base.plusSeconds(10));
        when(remediationRepository.findByOrganizationIdAndProjectId(ORG, PROJECT)).thenReturn(List.of(r));
        when(vulnerabilityRepository.findAll()).thenReturn(List.of());
        stubTargets(target("t-1", "vm-ubuntu", "10.0.0.5"));
        stubSaveEcho();

        ReportGenerateRequest request = ReportGenerateRequest.builder().targetId("t-1").build();
        Report report = service.generate(request, GenerationType.ON_DEMAND);

        assertThat(report.getItems()).hasSize(1);
        assertThat(report.getItems().get(0).getTargetName()).isEqualTo("vm-ubuntu");
    }

    // US1 — a target filter that matches nothing still yields the empty-result error.
    @Test
    void generate_targetFilterWithNoMatchThrowsEmptyResult() {
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        RemediationRecord r = record("CVE-1", "127.0.0.1", RemediationStatus.SUCCESS, base, base.plusSeconds(10));
        when(remediationRepository.findByOrganizationIdAndProjectId(ORG, PROJECT)).thenReturn(List.of(r));
        stubTargets(target("t-other", "other-host", "192.168.1.1"));

        ReportGenerateRequest request = ReportGenerateRequest.builder().targetId("t-other").build();

        assertThatThrownBy(() -> service.generate(request, GenerationType.ON_DEMAND))
                .isInstanceOf(ReportException.class)
                .satisfies(ex -> assertThat(((ReportException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.REPORT_EMPTY_RESULT));

        verify(reportRepository, never()).save(any());
    }

    // US1 — a record's IPv4 loopback matches a target registered with the IPv6 loopback (and vice versa).
    @Test
    void generate_targetFilterMatchesRecordsByLoopbackEquivalence() {
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        RemediationRecord r = record("CVE-1", "127.0.0.1", RemediationStatus.SUCCESS, base, base.plusSeconds(10));
        when(remediationRepository.findByOrganizationIdAndProjectId(ORG, PROJECT)).thenReturn(List.of(r));
        when(vulnerabilityRepository.findAll()).thenReturn(List.of());
        stubTargets(target("t-1", "vm-ubuntu", "0:0:0:0:0:0:0:1"));
        stubSaveEcho();

        ReportGenerateRequest request = ReportGenerateRequest.builder().targetId("t-1").build();
        Report report = service.generate(request, GenerationType.ON_DEMAND);

        assertThat(report.getItems()).hasSize(1);
        assertThat(report.getItems().get(0).getTargetName()).isEqualTo("vm-ubuntu");
    }

    // US2 — a record's IPv4 loopback resolves to the name of a target registered with the IPv6 loopback.
    @Test
    void generate_resolvesTargetNameAcrossLoopbackForms() {
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        RemediationRecord r = record("CVE-1", "127.0.0.1", RemediationStatus.SUCCESS, base, base.plusSeconds(10));
        when(remediationRepository.findByOrganizationIdAndProjectId(ORG, PROJECT)).thenReturn(List.of(r));
        when(vulnerabilityRepository.findAll()).thenReturn(List.of());
        stubTargets(target("t-1", "vm-ubuntu", "::1"));
        stubSaveEcho();

        Report report = service.generate(new ReportGenerateRequest(), GenerationType.ON_DEMAND);

        assertThat(report.getItems().get(0).getTargetName()).isEqualTo("vm-ubuntu");
    }

    // US2 — target names resolve by id first, then by ipOrDomain, else null.
    @Test
    void generate_resolvesTargetNamesByIdThenIpOrDomainElseNull() {
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        RemediationRecord byId = record("CVE-1", "t-1", RemediationStatus.SUCCESS, base, base.plusSeconds(5));
        RemediationRecord byIp = record("CVE-2", "127.0.0.1", RemediationStatus.SUCCESS, base, base.plusSeconds(5));
        RemediationRecord unknown = record("CVE-3", "no-such", RemediationStatus.SUCCESS, base, base.plusSeconds(5));
        when(remediationRepository.findByOrganizationIdAndProjectId(ORG, PROJECT))
                .thenReturn(List.of(byId, byIp, unknown));
        when(vulnerabilityRepository.findAll()).thenReturn(List.of());
        stubTargets(target("t-1", "host-by-id", "10.0.0.1"), target("t-2", "host-by-ip", "127.0.0.1"));
        stubSaveEcho();

        Report report = service.generate(new ReportGenerateRequest(), GenerationType.ON_DEMAND);

        assertThat(report.getItems()).hasSize(3);
        assertThat(report.getItems().get(0).getTargetName()).isEqualTo("host-by-id");
        assertThat(report.getItems().get(1).getTargetName()).isEqualTo("host-by-ip");
        assertThat(report.getItems().get(2).getTargetName()).isNull();
    }

    // US3 — without a target filter, all records are returned regardless of targetId shape.
    @Test
    void generate_withoutTargetFilterReturnsAllRecords() {
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        RemediationRecord r1 = record("CVE-1", "t-1", RemediationStatus.SUCCESS, base, base.plusSeconds(10));
        RemediationRecord r2 = record("CVE-2", "127.0.0.1", RemediationStatus.SUCCESS, base, base.plusSeconds(10));
        when(remediationRepository.findByOrganizationIdAndProjectId(ORG, PROJECT)).thenReturn(List.of(r1, r2));
        when(vulnerabilityRepository.findAll()).thenReturn(List.of());
        stubTargets(target("t-1", "a", "10.0.0.1"), target("t-2", "b", "127.0.0.1"));
        stubSaveEcho();

        Report report = service.generate(new ReportGenerateRequest(), GenerationType.ON_DEMAND);

        assertThat(report.getItems()).hasSize(2);
    }
}
