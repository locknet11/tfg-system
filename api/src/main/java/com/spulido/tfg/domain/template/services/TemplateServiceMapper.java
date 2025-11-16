package com.spulido.tfg.domain.template.services;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.spulido.tfg.domain.plan.model.Plan;
import com.spulido.tfg.domain.plan.model.Step;
import com.spulido.tfg.domain.plan.model.StepExecutionStatus;
import com.spulido.tfg.domain.plan.model.dto.PlanInfo;
import com.spulido.tfg.domain.plan.services.PlanMapper;
import com.spulido.tfg.domain.shared.ResponseList;
import com.spulido.tfg.domain.template.model.Template;
import com.spulido.tfg.domain.template.model.dto.TemplateInfo;
import com.spulido.tfg.domain.template.model.dto.TemplateRequest;
import com.spulido.tfg.domain.template.model.dto.TemplatesList;

@Component
public class TemplateServiceMapper {

        private final PlanMapper planMapper;

        public TemplateServiceMapper(PlanMapper planMapper) {
                this.planMapper = planMapper;
        }

        public Template requestToTemplate(TemplateRequest request) {
                Plan plan = Plan.builder()
                                .notes(request.getPlan().getNotes())
                                .allowTemplating(request.getPlan().isAllowTemplating())
                                .steps(request.getPlan().getSteps().stream()
                                                .map(stepRequest -> Step.builder()
                                                                .action(stepRequest.getAction())
                                                                .status(StepExecutionStatus.PENDING)
                                                                .build())
                                                .collect(Collectors.toList()))
                                .build();

                Template template = new Template();
                template.setName(request.getName())
                                .setDescription(request.getDescription())
                                .setPlan(plan)
                                .setUpdatedAt(LocalDateTime.now())
                                .setCreatedAt(LocalDateTime.now());

                return template;
        }

        public TemplateInfo templateToInfo(Template template) {
                PlanInfo planInfo = planMapper.planToInfo(template.getPlan());
                return TemplateInfo.builder()
                                .id(template.getId())
                                .name(template.getName())
                                .description(template.getDescription())
                                .plan(planInfo)
                                .createdAt(template.getCreatedAt())
                                .updatedAt(template.getUpdatedAt())
                                .build();
        }

        public ResponseList<TemplateInfo> templatesListToResponseList(TemplatesList list) {
                ResponseList<TemplateInfo> response = new ResponseList<>();
                response.setContent(list.getContent().stream()
                                .map(this::templateToInfo)
                                .collect(Collectors.toList()));
                response.setTotalElements(list.getTotalElements());
                response.setTotalPages(list.getTotalPages());
                response.setPage(list.getPageable().getPageNumber());
                response.setSize(list.getPageable().getPageSize());
                return response;
        }

        public Plan templateToPlan(Template template) {
                if (template.getPlan() == null) {
                        return null;
                }
                return Plan.builder()
                                .notes(template.getPlan().getNotes())
                                .allowTemplating(template.getPlan().isAllowTemplating())
                                .steps(template.getPlan().getSteps().stream()
                                                .map(step -> Step.builder()
                                                                .action(step.getAction())
                                                                .status(StepExecutionStatus.PENDING)
                                                                .logs(null)
                                                                .build())
                                                .collect(Collectors.toList()))
                                .build();
        }
}
