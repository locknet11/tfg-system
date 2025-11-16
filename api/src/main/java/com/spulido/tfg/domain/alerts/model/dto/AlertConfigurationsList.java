package com.spulido.tfg.domain.alerts.model.dto;

import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.spulido.tfg.domain.alerts.model.AlertConfiguration;

public class AlertConfigurationsList extends PageImpl<AlertConfiguration> {

    public AlertConfigurationsList(List<AlertConfiguration> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }
}
