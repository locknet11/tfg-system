package com.spulido.tfg.domain.remediation.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.spulido.tfg.domain.remediation.model.RemediationRecord;
import com.spulido.tfg.domain.remediation.model.RemediationStatus;

class RemediationMetricsTest {

    private RemediationRecord record(RemediationStatus status, Instant started, Instant completed) {
        RemediationRecord r = new RemediationRecord();
        r.setStatus(status);
        r.setStartedAt(started);
        r.setCompletedAt(completed);
        return r;
    }

    @Test
    void meanTimeToRemediateSeconds_averagesSuccessfulRecordsWithBothTimestamps() {
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        List<RemediationRecord> records = List.of(
                record(RemediationStatus.SUCCESS, base, base.plusSeconds(100)),
                record(RemediationStatus.SUCCESS, base, base.plusSeconds(300)));

        assertThat(RemediationMetrics.meanTimeToRemediateSeconds(records)).isEqualTo(200L);
    }

    @Test
    void meanTimeToRemediateSeconds_excludesNonSuccessAndMissingTimestamps() {
        Instant base = Instant.parse("2026-06-01T00:00:00Z");
        List<RemediationRecord> records = List.of(
                record(RemediationStatus.SUCCESS, base, base.plusSeconds(60)),
                record(RemediationStatus.FAILED, base, base.plusSeconds(9999)),
                record(RemediationStatus.SUCCESS, base, null),
                record(RemediationStatus.SUCCESS, null, base.plusSeconds(50)));

        assertThat(RemediationMetrics.meanTimeToRemediateSeconds(records)).isEqualTo(60L);
    }

    @Test
    void meanTimeToRemediateSeconds_returnsZeroWhenNoQualifyingRecords() {
        assertThat(RemediationMetrics.meanTimeToRemediateSeconds(List.of())).isZero();
        assertThat(RemediationMetrics.meanTimeToRemediateSeconds(null)).isZero();
        assertThat(RemediationMetrics.meanTimeToRemediateSeconds(
                List.of(record(RemediationStatus.FAILED, Instant.now(), Instant.now())))).isZero();
    }
}
