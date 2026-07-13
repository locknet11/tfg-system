package com.spulido.agent.teardown;

/**
 * The cause that initiated agent self-destruction.
 */
public enum TeardownTrigger {
    PLAN_COMPLETION,
    PLATFORM_DEPROVISION,
    AUTH_REVOKED,
    SELF_DESTRUCT_STEP
}
