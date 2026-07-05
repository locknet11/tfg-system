package com.spulido.agent.container;

/**
 * Confidence level of the container detection result.
 */
public enum DetectionConfidence {

    /** At least one known container indicator was positively matched */
    CONFIRMED,

    /** Unable to read detection files — result is a safety default (treat as container) */
    INCONCLUSIVE
}
