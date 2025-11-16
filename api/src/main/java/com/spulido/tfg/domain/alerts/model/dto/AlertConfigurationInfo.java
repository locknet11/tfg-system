package com.spulido.tfg.domain.alerts.model.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.spulido.tfg.domain.alerts.model.WhenCondition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfigurationInfo {

    private String id;
    private String sendTo;
    private List<WhenCondition> conditions;
    private boolean enabled;
    private Instant lastTriggeredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
