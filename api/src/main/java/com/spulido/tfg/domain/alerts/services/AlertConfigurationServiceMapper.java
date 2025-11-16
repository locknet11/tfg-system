package com.spulido.tfg.domain.alerts.services;

import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.spulido.tfg.domain.alerts.model.AlertConfiguration;
import com.spulido.tfg.domain.alerts.model.dto.AlertConfigurationInfo;
import com.spulido.tfg.domain.alerts.model.dto.AlertConfigurationRequest;
import com.spulido.tfg.domain.alerts.model.dto.AlertConfigurationsList;
import com.spulido.tfg.domain.shared.ResponseList;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AlertConfigurationServiceMapper {

    private final ModelMapper modelMapper;

    public AlertConfiguration requestToAlertConfiguration(AlertConfigurationRequest request) {
        return modelMapper.map(request, AlertConfiguration.class);
    }

    public AlertConfigurationInfo alertConfigurationToInfo(AlertConfiguration alertConfiguration) {
        return modelMapper.map(alertConfiguration, AlertConfigurationInfo.class);
    }

    public ResponseList<AlertConfigurationInfo> alertConfigurationsListToResponseList(
            AlertConfigurationsList list) {
        ResponseList<AlertConfigurationInfo> response = new ResponseList<>();
        response.setContent(list.getContent().stream()
                .map(this::alertConfigurationToInfo)
                .collect(Collectors.toList()));
        response.setTotalElements(list.getTotalElements());
        response.setTotalPages(list.getTotalPages());
        response.setPage(list.getPageable().getPageNumber());
        response.setSize(list.getPageable().getPageSize());
        return response;
    }
}
