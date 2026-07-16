package com.spulido.tfg.domain.alerts.services.impl;

import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.spulido.tfg.common.context.ProjectContext;
import com.spulido.tfg.domain.alerts.db.AlertConfigurationRepository;
import com.spulido.tfg.domain.alerts.model.AlertConfiguration;
import com.spulido.tfg.domain.alerts.model.AlertEvent;
import com.spulido.tfg.domain.alerts.model.WhenCondition;
import com.spulido.tfg.domain.alerts.services.AlertTriggerService;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertTriggerServiceImpl implements AlertTriggerService {

    private final AlertConfigurationRepository repository;
    private final Resend resendClient;
    private final Configuration freemarkerConfiguration;

    @Value("${resend.from-address}")
    private String fromAddress;

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
                // RemediationStatus enum emits "FAILED" (not "FAILURE"); accept both spellings
                // so a failed remediation reliably matches this condition.
                return event.getType() == AlertEvent.AlertEventType.REMEDIATION_COMPLETED
                        && isFailureStatus(
                                event.getPayload() != null ? String.valueOf(event.getPayload().get("status")) : "");

            case ON_SCAN_COMPLETED:
                return event.getType() == AlertEvent.AlertEventType.SCAN_COMPLETED;

            case ON_REPLICATION_REQUESTED:
                return event.getType() == AlertEvent.AlertEventType.REPLICATION_REQUESTED;

            case ON_AGENT_REPLICATED:
                return event.getType() == AlertEvent.AlertEventType.AGENT_REPLICATED;

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

    private boolean isFailureStatus(String status) {
        if (status == null) {
            return false;
        }
        return "FAILURE".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status);
    }

    private void sendAlert(AlertConfiguration config, AlertEvent event) {
        try {
            String body = buildEmailBody(event);

            CreateEmailOptions options = CreateEmailOptions.builder()
                    .from(fromAddress)
                    .to(config.getSendTo())
                    .subject("Security Alert: " + event.getType())
                    .html(body)
                    .text(body)
                    .build();

            CreateEmailResponse response = resendClient.emails().send(options);
            log.info("Alert sent to {} for event type {} (id: {})", config.getSendTo(), event.getType(), response.getId());
        } catch (ResendException e) {
            log.error("Failed to send alert to {}: {}", config.getSendTo(), e.getMessage(), e);
        }
    }

    private String buildEmailBody(AlertEvent event) {
        Map<String, Object> model = new HashMap<>();
        model.put("eventType", event.getType() != null ? event.getType().toString() : "UNKNOWN");
        model.put("timestamp", event.getTimestamp() != null ? event.getTimestamp().toString() : "");

        if (event.getSeverity() != null) {
            model.put("severity", event.getSeverity());
        }

        if (event.getPayload() != null && !event.getPayload().isEmpty()) {
            List<Map<String, String>> entries = event.getPayload().entrySet().stream()
                    .map(e -> {
                        Map<String, String> entry = new HashMap<>();
                        entry.put("key", e.getKey());
                        entry.put("value", String.valueOf(e.getValue()));
                        return entry;
                    })
                    .collect(Collectors.toList());
            model.put("payloadEntries", entries);
        }

        try {
            Template template = freemarkerConfiguration.getTemplate("alert-email.ftl");
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("Failed to render email template", e);
            return "Security Alert\n\n"
                    + "Event Type: " + event.getType() + "\n"
                    + "Timestamp: " + event.getTimestamp() + "\n"
                    + "\n---\n"
                    + "This is an automated alert from the TFG Security System.\n";
        }
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
