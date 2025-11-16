package com.spulido.tfg.domain.alerts.model.dto;

import java.util.List;

import com.spulido.tfg.domain.alerts.model.WhenCondition;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfigurationRequest {

    @Email(message = "Invalid email format")
    @NotNull(message = "Email is required")
    private String sendTo;

    @NotEmpty(message = "At least one condition is required")
    private List<WhenCondition> conditions;

    private boolean enabled = true;
}
