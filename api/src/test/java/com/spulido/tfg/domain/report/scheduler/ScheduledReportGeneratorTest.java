package com.spulido.tfg.domain.report.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.spulido.tfg.common.context.ProjectContext;
import com.spulido.tfg.common.exception.ErrorCode;
import com.spulido.tfg.domain.remediation.db.RemediationRecordRepository;
import com.spulido.tfg.domain.remediation.model.RemediationRecord;
import com.spulido.tfg.domain.report.exception.ReportException;
import com.spulido.tfg.domain.report.model.GenerationType;
import com.spulido.tfg.domain.report.services.ReportService;

@ExtendWith(MockitoExtension.class)
class ScheduledReportGeneratorTest {

    @Mock
    private RemediationRecordRepository remediationRepository;
    @Mock
    private ReportService reportService;

    private RemediationRecord record(String org, String project) {
        RemediationRecord r = new RemediationRecord();
        r.setOrganizationId(org);
        r.setProjectId(project);
        return r;
    }

    @Test
    void generateScheduledReports_generatesOncePerDistinctProjectWithData() {
        when(remediationRepository.findAll()).thenReturn(List.of(
                record("org-1", "proj-1"),
                record("org-1", "proj-1"),
                record("org-2", "proj-2")));

        List<String> contexts = new ArrayList<>();
        when(reportService.generate(any(), eq(GenerationType.SCHEDULED))).thenAnswer(inv -> {
            contexts.add(ProjectContext.getOrganizationId() + "/" + ProjectContext.getProjectId());
            return null;
        });

        new ScheduledReportGenerator(remediationRepository, reportService).generateScheduledReports();

        verify(reportService, times(2)).generate(any(), eq(GenerationType.SCHEDULED));
        assertThat(contexts).containsExactlyInAnyOrder("org-1/proj-1", "org-2/proj-2");
        // Context is cleared after the run.
        assertThat(ProjectContext.hasContext()).isFalse();
    }

    @Test
    void generateScheduledReports_skipsProjectsWithoutQualifyingData() {
        when(remediationRepository.findAll()).thenReturn(List.of(
                record("org-1", "proj-1"),
                record("org-2", "proj-2")));
        when(reportService.generate(any(), eq(GenerationType.SCHEDULED)))
                .thenThrow(new ReportException("empty", ErrorCode.REPORT_EMPTY_RESULT))
                .thenReturn(null);

        new ScheduledReportGenerator(remediationRepository, reportService).generateScheduledReports();

        // Both attempted; the empty one is skipped without propagating.
        verify(reportService, times(2)).generate(any(), eq(GenerationType.SCHEDULED));
        assertThat(ProjectContext.hasContext()).isFalse();
    }
}
