package com.spulido.agent.container;

/**
 * Which detection method provided the primary indicator of a container runtime.
 */
public enum DetectionMethod {

    /** /.dockerenv file exists at root — classic Docker indicator */
    DOCKERENV_FILE,

    /** /proc/1/cgroup contains /docker/, /kubepods/, /containerd/, or /lxc/ — cgroup v1 */
    CGROUP_V1,

    /** /proc/self/mountinfo contains /docker/containers/ — cgroup v2 */
    MOUNTINFO_V2,

    /** PID 1 in /proc/1/sched is not init or systemd — generic container indicator */
    PID1_SCHED,

    /** /run/.containerenv exists — Podman indicator */
    CONTAINERENV_FILE,

    /** No detection was performed or all checks were negative */
    NONE
}
