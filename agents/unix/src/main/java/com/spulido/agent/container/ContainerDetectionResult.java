package com.spulido.agent.container;

import java.util.Collections;
import java.util.Set;

/**
 * Result of container runtime detection.
 * Immutable value object — created once per detection call.
 */
public class ContainerDetectionResult {

    private final boolean container;
    private final DetectionConfidence confidence;
    private final DetectionMethod detectionMethod;
    private final Set<DetectionMethod> matchedIndicators;
    private final String runtimeName;

    public ContainerDetectionResult(boolean container, DetectionConfidence confidence,
                                     DetectionMethod detectionMethod,
                                     Set<DetectionMethod> matchedIndicators,
                                     String runtimeName) {
        this.container = container;
        this.confidence = confidence;
        this.detectionMethod = detectionMethod;
        this.matchedIndicators = matchedIndicators != null
                ? Collections.unmodifiableSet(matchedIndicators) : Set.of();
        this.runtimeName = runtimeName;
    }

    public boolean isContainer() {
        return container;
    }

    public DetectionConfidence getConfidence() {
        return confidence;
    }

    public DetectionMethod getDetectionMethod() {
        return detectionMethod;
    }

    public Set<DetectionMethod> getMatchedIndicators() {
        return matchedIndicators;
    }

    public String getRuntimeName() {
        return runtimeName;
    }

    @Override
    public String toString() {
        return "ContainerDetectionResult{container=" + container
                + ", confidence=" + confidence
                + ", runtime=" + (runtimeName != null ? runtimeName : "none")
                + ", indicators=" + matchedIndicators + "}";
    }
}
