package com.spulido.tfg.domain.remediation.model;

/**
 * Types of remediation actions based on the nature of the fix required.
 * <ul>
 *   <li>SERVICE_UPDATE: Package upgrade or configuration change, service restart only</li>
 *   <li>REBOOT_REQUIRED: Fix applied but requires system reboot to take effect</li>
 *   <li>KERNEL_UPDATE: Kernel-level vulnerability, manual intervention required</li>
 *   <li>CONTAINER_DETECTED: Agent is running inside a container — remediation skipped</li>
 *   <li>UNKNOWN: Cannot determine remediation type</li>
 * </ul>
 */
public enum RemediationType {
    SERVICE_UPDATE,
    REBOOT_REQUIRED,
    KERNEL_UPDATE,
    CONTAINER_DETECTED,
    UNKNOWN
}
