package com.spulido.tfg.domain.alerts.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.spulido.tfg.domain.BaseEntity;
import com.spulido.tfg.domain.ScopedEntity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Entity representing an alert configuration for a project.
 * Alerts are triggered when specified conditions are met during system operations.
 */
@Document(collection = "alertConfigurations")
@CompoundIndex(name = "alert_scope_idx", def = "{ 'organizationId': 1, 'projectId': 1 }")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AlertConfiguration extends BaseEntity implements ScopedEntity {

    @Field
    @Email(message = "Invalid email format")
    @NotNull(message = "Email is required")
    private String sendTo;

    @Field
    @NotEmpty(message = "At least one condition is required")
    private List<WhenCondition> conditions;

    @Field
    @NotNull(message = "Organization ID is required")
    private String organizationId;

    @Field
    @NotNull(message = "Project ID is required")
    private String projectId;

    @Field
    private boolean enabled = true;

    @Field
    private Instant lastTriggeredAt;
}
