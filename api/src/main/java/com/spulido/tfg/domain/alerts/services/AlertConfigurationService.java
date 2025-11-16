package com.spulido.tfg.domain.alerts.services;

import org.springframework.data.domain.PageRequest;

import com.spulido.tfg.domain.alerts.exception.AlertException;
import com.spulido.tfg.domain.alerts.model.AlertConfiguration;
import com.spulido.tfg.domain.alerts.model.dto.AlertConfigurationsList;

public interface AlertConfigurationService {

    AlertConfiguration createAlertConfiguration(AlertConfiguration alertConfiguration) throws AlertException;

    AlertConfiguration updateAlertConfiguration(String id, AlertConfiguration alertConfiguration) throws AlertException;

    AlertConfiguration getAlertConfiguration(String id) throws AlertException;

    AlertConfigurationsList listAlertConfigurations(PageRequest pageRequest);

    void deleteAlertConfiguration(String id) throws AlertException;
}
