package com.spulido.tfg.domain.remediation.model;

/**
 * Status lifecycle of a remediation attempt.
 * <ul>
 *   <li>PENDING: Created but not yet started</li>
 *   <li>IN_PROGRESS: Agent is executing the remediation</li>
 *   <li>SUCCESS: Fix applied and verified</li>
 *   <li>FAILED: Fix failed or verification failed</li>
 *   <li>PENDING_REBOOT: Fix applied but requires system reboot</li>
 *   <li>SKIPPED: Kernel update required, manual action needed</li>
 * </ul>
 */
public enum RemediationStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    PENDING_REBOOT,
    SKIPPED
}
