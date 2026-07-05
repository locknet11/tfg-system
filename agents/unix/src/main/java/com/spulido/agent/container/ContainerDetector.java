package com.spulido.agent.container;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects whether the current process is running inside a container runtime
 * (Docker, Podman, containerd, LXC) using filesystem-based checks only.
 *
 * <p>Uses an allowlist strategy: only declares "inside container" when at least
 * one known indicator is positively matched. All detection uses
 * {@link java.nio.file.Files} for GraalVM native-image compatibility.</p>
 *
 * <h3>Detection methods (checked in order):</h3>
 * <ol>
 *   <li>{@code /.dockerenv} — Docker sentinel file</li>
 *   <li>{@code /proc/1/cgroup} — cgroup v1 paths containing /docker/, /kubepods/, /containerd/, or /lxc/</li>
 *   <li>{@code /proc/self/mountinfo} — cgroup v2 paths containing /docker/containers/</li>
 *   <li>{@code /proc/1/sched} — PID 1 process name (not init or systemd → container)</li>
 *   <li>{@code /run/.containerenv} — Podman sentinel file</li>
 * </ol>
 */
public class ContainerDetector {

    private static final Logger log = LoggerFactory.getLogger(ContainerDetector.class);

    private static final Path DOCKERENV = Path.of("/.dockerenv");
    private static final Path CGROUP_V1 = Path.of("/proc/1/cgroup");
    private static final Path MOUNTINFO = Path.of("/proc/self/mountinfo");
    private static final Path PID1_SCHED = Path.of("/proc/1/sched");
    private static final Path CONTAINERENV = Path.of("/run/.containerenv");

    /**
     * Inspects the runtime environment and returns a detection result.
     *
     * @return detection result indicating whether a container runtime was found
     */
    public ContainerDetectionResult detect() {
        log.debug("Starting container runtime detection");

        // Check 1: /.dockerenv sentinel file
        if (checkDockerenv()) {
            log.info("Container detected: /.dockerenv exists");
            return confirmed(DetectionMethod.DOCKERENV_FILE, "docker",
                    EnumSet.of(DetectionMethod.DOCKERENV_FILE));
        }

        // Check 2: /proc/1/cgroup for Docker/container paths (cgroup v1)
        String cgroupContent = readFile(CGROUP_V1);
        if (cgroupContent != null) {
            String cgroupRuntime = detectFromCgroupV1(cgroupContent);
            if (cgroupRuntime != null) {
                log.info("Container detected via cgroup v1: runtime={}", cgroupRuntime);
                return confirmed(DetectionMethod.CGROUP_V1, cgroupRuntime,
                        EnumSet.of(DetectionMethod.CGROUP_V1));
            }
        }

        // Check 3: /proc/self/mountinfo for Docker paths (cgroup v2)
        String mountinfoContent = readFile(MOUNTINFO);
        if (mountinfoContent != null) {
            if (mountinfoContent.contains("/docker/containers/")) {
                log.info("Container detected via mountinfo (cgroup v2): docker");
                return confirmed(DetectionMethod.MOUNTINFO_V2, "docker",
                        EnumSet.of(DetectionMethod.MOUNTINFO_V2));
            }
        }

        // Check 4: /proc/1/sched — PID 1 process name
        String schedContent = readFile(PID1_SCHED);
        if (schedContent != null) {
            DetectionMethod scheduleResult = detectFromSched(schedContent);
            if (scheduleResult != null) {
                log.info("Container detected via PID 1 sched: process is not init/systemd");
                return confirmed(DetectionMethod.PID1_SCHED, "container",
                        EnumSet.of(DetectionMethod.PID1_SCHED));
            }
        }

        // Check 5: /run/.containerenv — Podman sentinel
        if (checkContainerenv()) {
            log.info("Container detected: /run/.containerenv exists (Podman)");
            return confirmed(DetectionMethod.CONTAINERENV_FILE, "podman",
                    EnumSet.of(DetectionMethod.CONTAINERENV_FILE));
        }

        // If any file was unreadable (inconclusive), fail-safe: treat as container
        boolean anyFileUnreadable = cgroupContent == null || mountinfoContent == null
                || schedContent == null;
        if (anyFileUnreadable) {
            log.warn("Container detection inconclusive: some /proc files unreadable; "
                    + "defaulting to skip remediation as precaution");
            return inconclusive();
        }

        // No indicators matched — running on a real host
        log.info("No container indicators detected — running on host/VM");
        return notContainer();
    }

    private boolean checkDockerenv() {
        return Files.exists(DOCKERENV);
    }

    private boolean checkContainerenv() {
        return Files.exists(CONTAINERENV);
    }

    /**
     * Reads all lines from a file, returning the content as a single string.
     * Returns null if the file cannot be read (inconclusive result).
     */
    private String readFile(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            return String.join("\n", lines);
        } catch (IOException e) {
            log.debug("Unable to read {}: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * Checks cgroup v1 content for Docker, Kubernetes, containerd, or LXC paths.
     * Returns the detected runtime name, or null if no container path is found.
     */
    private String detectFromCgroupV1(String content) {
        if (content.contains("/docker/")) {
            return "docker";
        }
        if (content.contains("/kubepods/")) {
            return "kubernetes";
        }
        if (content.contains("/containerd/")) {
            return "containerd";
        }
        if (content.contains("/lxc/")) {
            return "lxc";
        }
        return null;
    }

    /**
     * Checks /proc/1/sched to see if PID 1 is a known init system.
     * Returns DETECTION if PID 1 is NOT init or systemd (indicating a container),
     * or null if PID 1 looks like a host init process.
     */
    private DetectionMethod detectFromSched(String content) {
        // First line of /proc/1/sched: "processname (1, #threads: 1)"
        String firstLine = content.lines().findFirst().orElse("");
        String lowerLine = firstLine.toLowerCase();

        // Known host init systems
        if (lowerLine.startsWith("init ") || lowerLine.startsWith("systemd")) {
            return null; // Looks like a real host
        }

        // If PID 1 is something else (bash, sh, java, etc.) → container
        if (!firstLine.isEmpty()) {
            return DetectionMethod.PID1_SCHED;
        }

        return null;
    }

    private ContainerDetectionResult confirmed(DetectionMethod method, String runtime,
                                                Set<DetectionMethod> indicators) {
        return new ContainerDetectionResult(true, DetectionConfidence.CONFIRMED,
                method, indicators, runtime);
    }

    private ContainerDetectionResult notContainer() {
        return new ContainerDetectionResult(false, DetectionConfidence.CONFIRMED,
                DetectionMethod.NONE, Set.of(), null);
    }

    private ContainerDetectionResult inconclusive() {
        return new ContainerDetectionResult(true, DetectionConfidence.INCONCLUSIVE,
                DetectionMethod.NONE, Set.of(), null);
    }
}
