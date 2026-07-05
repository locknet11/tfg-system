package com.spulido.agent.domain.task;

import java.util.Collections;
import java.util.List;

public class StepResult {

    private final StepAction action;
    private final List<ServiceInfo> services;
    private final List<String> scripts;
    private final List<String> logs;
    private final boolean success;
    private final boolean skipped;

    public StepResult(StepAction action, List<ServiceInfo> services, List<String> scripts,
                      List<String> logs, boolean success, boolean skipped) {
        this.action = action;
        this.services = services != null ? Collections.unmodifiableList(services) : List.of();
        this.scripts = scripts != null ? Collections.unmodifiableList(scripts) : List.of();
        this.logs = logs != null ? Collections.unmodifiableList(logs) : List.of();
        this.success = success;
        this.skipped = skipped;
    }

    public StepAction getAction() {
        return action;
    }

    public List<ServiceInfo> getServices() {
        return services;
    }

    public List<String> getScripts() {
        return scripts;
    }

    public List<String> getLogs() {
        return logs;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public static StepResult success(StepAction action, List<ServiceInfo> services,
                                     List<String> scripts, List<String> logs) {
        return new StepResult(action, services, scripts, logs, true, false);
    }

    public static StepResult failure(StepAction action, List<String> logs) {
        return new StepResult(action, List.of(), List.of(), logs, false, false);
    }

    public static StepResult skipped(StepAction action, List<String> logs) {
        return new StepResult(action, List.of(), List.of(), logs, false, true);
    }
}
