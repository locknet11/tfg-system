package com.spulido.tfg.domain.alerts.services.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.spulido.tfg.common.context.ProjectContext;
import com.spulido.tfg.domain.alerts.db.AlertConfigurationRepository;
import com.spulido.tfg.domain.alerts.model.AlertConfiguration;
import com.spulido.tfg.domain.alerts.model.AlertEvent;
import com.spulido.tfg.domain.alerts.model.WhenCondition;
import com.spulido.tfg.domain.alerts.services.AlertTriggerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertTriggerServiceImpl implements AlertTriggerService {

    private final AlertConfigurationRepository repository;
    private final JavaMailSender mailSender;

    @Override
    @Async
    public void checkAndTrigger(AlertEvent event) {
        try {
            // Set context for scoped queries
            if (event.getOrganizationId() != null && event.getProjectId() != null) {
                ProjectContext.set(event.getOrganizationId(), event.getProjectId());
            }

            List<AlertConfiguration> activeConfigs = repository.findAllActiveScoped();

            for (AlertConfiguration config : activeConfigs) {
                if (shouldTrigger(config, event)) {
                    sendAlert(config, event);
                    updateLastTriggeredAt(config);
                }
            }
        } catch (Exception e) {
            log.error("Error checking and triggering alerts for event: {}", event, e);
        } finally {
            ProjectContext.clear();
        }
    }

    private boolean shouldTrigger(AlertConfiguration config, AlertEvent event) {
        for (WhenCondition condition : config.getConditions()) {
            if (matchesCondition(condition, event)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCondition(WhenCondition condition, AlertEvent event) {
        switch (condition) {
            case ON_VULNERABILITY_DETECTED:
                return event.getType() == AlertEvent.AlertEventType.VULNERABILITY_DETECTED;

            case ON_HIGH_SEVERITY_VULNERABILITY:
                return event.getType() == AlertEvent.AlertEventType.VULNERABILITY_DETECTED
                        && isHighSeverity(event.getSeverity());

            case ON_REMEDIATION_SUCCESS:
                return event.getType() == AlertEvent.AlertEventType.REMEDIATION_COMPLETED
                        && "SUCCESS".equalsIgnoreCase(
                                event.getPayload() != null ? String.valueOf(event.getPayload().get("status")) : "");

            case ON_REMEDIATION_FAILURE:
                return event.getType() == AlertEvent.AlertEventType.REMEDIATION_COMPLETED
                        && "FAILURE".equalsIgnoreCase(
                                event.getPayload() != null ? String.valueOf(event.getPayload().get("status")) : "");

            case ON_SCAN_COMPLETED:
                return event.getType() == AlertEvent.AlertEventType.SCAN_COMPLETED;

            default:
                return false;
        }
    }

    private boolean isHighSeverity(String severity) {
        if (severity == null) {
            return false;
        }
        return "HIGH".equalsIgnoreCase(severity) || "CRITICAL".equalsIgnoreCase(severity);
    }

    private void sendAlert(AlertConfiguration config, AlertEvent event) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(config.getSendTo());
            message.setSubject("Security Alert: " + event.getType());
            message.setText(buildEmailBody(event));

            mailSender.send(message);
            log.info("Alert sent to {} for event type {}", config.getSendTo(), event.getType());
        } catch (Exception e) {
            log.error("Failed to send alert to {}: {}", config.getSendTo(), e.getMessage(), e);
        }
    }

    private String buildEmailBody(AlertEvent event) {
        StringBuilder body = new StringBuilder();
        body.append("Security Alert\n\n");
        body.append("Event Type: ").append(event.getType()).append("\n");
        body.append("Timestamp: ").append(event.getTimestamp()).append("\n");

        if (event.getSeverity() != null) {
            body.append("Severity: ").append(event.getSeverity()).append("\n");
        }

        if (event.getPayload() != null && !event.getPayload().isEmpty()) {
            body.append("\nDetails:\n");
            event.getPayload().forEach((key, value) -> body.append("  ").append(key).append(": ").append(value)
                    .append("\n"));
        }

        body.append("\n---\n");
        body.append("This is an automated alert from the TFG Security System.\n");

        return body.toString();
    }

    private void updateLastTriggeredAt(AlertConfiguration config) {
        try {
            config.setLastTriggeredAt(Instant.now());
            repository.save(config);
        } catch (Exception e) {
            log.error("Failed to update lastTriggeredAt for alert config {}: {}", config.getId(), e.getMessage());
        }
    }
}
