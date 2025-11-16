package com.spulido.tfg.domain.template.model.dto;

import java.time.LocalDateTime;

import com.spulido.tfg.domain.plan.model.dto.PlanInfo;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TemplateInfo {

    private String id;
    private String name;
    private String description;
    private PlanInfo plan;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
