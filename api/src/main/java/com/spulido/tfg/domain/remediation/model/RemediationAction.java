package com.spulido.tfg.domain.remediation.model;

/**
 * Specific remediation action to be executed on the target system.
 */
public enum RemediationAction {
    APT_UPGRADE,
    APT_INSTALL,
    CONFIG_UPDATE,
    SYSTEMCTL_RESTART,
    MANUAL
}
