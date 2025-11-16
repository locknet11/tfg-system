package com.spulido.tfg.domain.template.services;

import org.springframework.data.domain.PageRequest;

import com.spulido.tfg.domain.template.exception.TemplateException;
import com.spulido.tfg.domain.template.model.Template;
import com.spulido.tfg.domain.template.model.dto.TemplatesList;

public interface TemplateService {

    Template createTemplate(Template template) throws TemplateException;

    Template updateTemplate(String templateId, Template template) throws TemplateException;

    Template getTemplate(String templateId) throws TemplateException;

    TemplatesList listTemplates(PageRequest pageRequest);

    void deleteTemplate(String templateId) throws TemplateException;
}
