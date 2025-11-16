package com.spulido.tfg.domain.alerts.services.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.spulido.tfg.domain.alerts.db.AlertConfigurationRepository;
import com.spulido.tfg.domain.alerts.exception.AlertException;
import com.spulido.tfg.domain.alerts.model.AlertConfiguration;
import com.spulido.tfg.domain.alerts.model.dto.AlertConfigurationsList;
import com.spulido.tfg.domain.alerts.services.AlertConfigurationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AlertConfigurationServiceImpl implements AlertConfigurationService {

    private final AlertConfigurationRepository repository;

    @Override
    public AlertConfiguration createAlertConfiguration(AlertConfiguration alertConfiguration) throws AlertException {
        validateAlertConfiguration(alertConfiguration);
        alertConfiguration.setId(null);
        alertConfiguration.setCreatedAt(LocalDateTime.now());
        alertConfiguration.setUpdatedAt(LocalDateTime.now());
        return repository.save(alertConfiguration);
    }

    @Override
    public AlertConfiguration updateAlertConfiguration(String id, AlertConfiguration alertConfiguration)
            throws AlertException {
        AlertConfiguration existing = getAlertConfiguration(id);
        validateAlertConfiguration(alertConfiguration);
        alertConfiguration.setId(existing.getId());
        alertConfiguration.setCreatedAt(existing.getCreatedAt());
        alertConfiguration.setUpdatedAt(LocalDateTime.now());
        alertConfiguration.setOrganizationId(existing.getOrganizationId());
        alertConfiguration.setProjectId(existing.getProjectId());
        alertConfiguration.setLastTriggeredAt(existing.getLastTriggeredAt());
        return repository.save(alertConfiguration);
    }

    @Override
    public AlertConfiguration getAlertConfiguration(String id) throws AlertException {
        return repository.findByIdScoped(id)
                .orElseThrow(() -> new AlertException("Alert configuration not found"));
    }

    @Override
    public AlertConfigurationsList listAlertConfigurations(PageRequest pageRequest) {
        Page<AlertConfiguration> page = repository.findAllScoped(pageRequest);
        return new AlertConfigurationsList(page.getContent(), pageRequest, page.getTotalElements());
    }

    @Override
    public void deleteAlertConfiguration(String id) throws AlertException {
        AlertConfiguration existing = getAlertConfiguration(id);
        repository.delete(existing);
    }

    private void validateAlertConfiguration(AlertConfiguration alertConfiguration) throws AlertException {
        if (alertConfiguration.getSendTo() == null || alertConfiguration.getSendTo().trim().isEmpty()) {
            throw new AlertException("Email address is required");
        }
        if (alertConfiguration.getConditions() == null || alertConfiguration.getConditions().isEmpty()) {
            throw new AlertException("At least one condition is required");
        }
    }
}
