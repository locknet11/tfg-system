package com.spulido.tfg.domain.alerts.services;

import com.spulido.tfg.domain.alerts.model.AlertEvent;

public interface AlertTriggerService {

    /**
     * Checks alert configurations and triggers notifications if conditions match the event.
     * 
     * @param event The event to check against alert configurations
     */
    void checkAndTrigger(AlertEvent event);
}
