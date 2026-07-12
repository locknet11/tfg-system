package com.spulido.tfg.domain.remediation.util;

import java.time.Duration;
import java.util.Collection;

import com.spulido.tfg.domain.remediation.model.RemediationRecord;
import com.spulido.tfg.domain.remediation.model.RemediationStatus;

/**
 * Shared remediation metric helpers. Single source of truth so the reports view
 * and the dashboard statistics widget never disagree.
 */
public final class RemediationMetrics {

    private RemediationMetrics() {
    }

    /**
     * Mean time to remediate, in seconds, over successful remediations that have
     * both a start and an end time. Records lacking either timestamp, or not in
     * {@link RemediationStatus#SUCCESS}, are excluded. Returns {@code 0} when no
     * qualifying record exists.
     */
    public static long meanTimeToRemediateSeconds(Collection<RemediationRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0L;
        }

        long total = 0L;
        long count = 0L;
        for (RemediationRecord record : records) {
            if (record == null || record.getStatus() != RemediationStatus.SUCCESS) {
                continue;
            }
            if (record.getStartedAt() == null || record.getCompletedAt() == null) {
                continue;
            }
            long seconds = Duration.between(record.getStartedAt(), record.getCompletedAt()).getSeconds();
            if (seconds < 0) {
                continue;
            }
            total += seconds;
            count++;
        }

        return count == 0 ? 0L : total / count;
    }
}
