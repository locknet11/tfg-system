package com.spulido.tfg.domain.alerts.controller;

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spulido.tfg.domain.alerts.exception.AlertException;
import com.spulido.tfg.domain.alerts.model.AlertConfiguration;
import com.spulido.tfg.domain.alerts.model.dto.AlertConfigurationInfo;
import com.spulido.tfg.domain.alerts.model.dto.AlertConfigurationRequest;
import com.spulido.tfg.domain.alerts.services.AlertConfigurationService;
import com.spulido.tfg.domain.alerts.services.AlertConfigurationServiceMapper;
import com.spulido.tfg.domain.shared.ResponseList;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AlertConfigurationController {

    private final AlertConfigurationService alertConfigurationService;
    private final AlertConfigurationServiceMapper mapper;

    @GetMapping
    public ResponseEntity<ResponseList<AlertConfigurationInfo>> listAlertConfigurations(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        var alertConfigurations = alertConfigurationService.listAlertConfigurations(PageRequest.of(page, size));
        return ResponseEntity.ok(mapper.alertConfigurationsListToResponseList(alertConfigurations));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertConfigurationInfo> getAlertConfiguration(@PathVariable("id") String id)
            throws AlertException {
        AlertConfiguration alertConfiguration = alertConfigurationService.getAlertConfiguration(id);
        return ResponseEntity.ok(mapper.alertConfigurationToInfo(alertConfiguration));
    }

    @PostMapping
    public ResponseEntity<AlertConfigurationInfo> createAlertConfiguration(
            @RequestBody @Valid AlertConfigurationRequest request) throws AlertException, URISyntaxException {
        AlertConfiguration toCreate = mapper.requestToAlertConfiguration(request);
        AlertConfiguration created = alertConfigurationService.createAlertConfiguration(toCreate);
        AlertConfigurationInfo dto = mapper.alertConfigurationToInfo(created);
        return ResponseEntity.created(new URI(String.format("/api/alerts/%s", dto.getId()))).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlertConfigurationInfo> updateAlertConfiguration(
            @PathVariable("id") String id,
            @RequestBody @Valid AlertConfigurationRequest request) throws AlertException {
        AlertConfiguration toUpdate = mapper.requestToAlertConfiguration(request);
        AlertConfiguration updated = alertConfigurationService.updateAlertConfiguration(id, toUpdate);
        return ResponseEntity.ok(mapper.alertConfigurationToInfo(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlertConfiguration(@PathVariable("id") String id) throws AlertException {
        alertConfigurationService.deleteAlertConfiguration(id);
        return ResponseEntity.noContent().build();
    }
}
