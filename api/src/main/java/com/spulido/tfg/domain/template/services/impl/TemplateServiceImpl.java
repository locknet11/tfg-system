package com.spulido.tfg.domain.template.services.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.spulido.tfg.domain.plan.model.Plan;
import com.spulido.tfg.domain.plan.model.Step;
import com.spulido.tfg.domain.plan.model.StepExecutionStatus;
import com.spulido.tfg.domain.template.db.TemplateRepository;
import com.spulido.tfg.domain.template.exception.TemplateException;
import com.spulido.tfg.domain.template.model.Template;
import com.spulido.tfg.domain.template.model.dto.TemplatesList;
import com.spulido.tfg.domain.template.services.TemplateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository repository;

    @Override
    public Template createTemplate(Template template) throws TemplateException {
        validateTemplate(template);
        template.setId(null);
        template.setPlan(normalizePlan(template.getPlan()));
        return repository.save(template);
    }

    @Override
    public Template updateTemplate(String templateId, Template template) throws TemplateException {
        Template existing = getTemplate(templateId);
        validateTemplate(template);
        template.setId(existing.getId());
        template.setCreatedAt(existing.getCreatedAt());
        template.setOrganizationId(existing.getOrganizationId());
        template.setProjectId(existing.getProjectId());
        template.setPlan(normalizePlan(template.getPlan()));
        return repository.save(template);
    }

    @Override
    public Template getTemplate(String templateId) throws TemplateException {
        return repository.findByIdScoped(templateId)
                .orElseThrow(() -> new TemplateException("template.error.notfound"));
    }

    @Override
    public TemplatesList listTemplates(PageRequest pageRequest) {
        Page<Template> page = repository.findAllScoped(pageRequest);
        return new TemplatesList(page.getContent(), pageRequest, page.getTotalElements());
    }

    @Override
    public void deleteTemplate(String templateId) throws TemplateException {
        Template existing = getTemplate(templateId);
        repository.delete(existing);
    }

    private void validateTemplate(Template template) throws TemplateException {
        if (template.getPlan() == null || template.getPlan().getSteps() == null
                || template.getPlan().getSteps().isEmpty()) {
            throw new TemplateException("template.error.plan.steps.required");
        }
    }

    private Plan normalizePlan(Plan plan) {
        if (plan == null)
            return null;
        List<Step> normalizedSteps = plan.getSteps().stream()
                .map(step -> Step.builder()
                        .action(step.getAction())
                        .status(step.getStatus() != null ? step.getStatus() : StepExecutionStatus.PENDING)
                        .logs(step.getLogs())
                        .build())
                .collect(Collectors.toList());
        return Plan.builder()
                .notes(plan.getNotes())
                .allowTemplating(plan.isAllowTemplating())
                .steps(normalizedSteps)
                .build();
    }
}
