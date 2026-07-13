package com.spulido.agent.teardown;

/**
 * Outcome of attempting to remove a single artifact.
 */
public enum RemovalStatus {
    REMOVED,
    FAILED,
    NOT_PRESENT
}
