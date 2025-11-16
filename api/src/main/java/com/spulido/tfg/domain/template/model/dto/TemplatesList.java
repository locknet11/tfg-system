package com.spulido.tfg.domain.template.model.dto;

import java.util.List;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.spulido.tfg.domain.template.model.Template;

public class TemplatesList extends PageImpl<Template> {

    public TemplatesList(List<Template> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }
}
