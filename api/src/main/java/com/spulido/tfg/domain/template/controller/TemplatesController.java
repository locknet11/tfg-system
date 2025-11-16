package com.spulido.tfg.domain.template.controller;

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

import com.spulido.tfg.domain.shared.ResponseList;
import com.spulido.tfg.domain.template.exception.TemplateException;
import com.spulido.tfg.domain.template.model.Template;
import com.spulido.tfg.domain.template.model.dto.TemplateInfo;
import com.spulido.tfg.domain.template.model.dto.TemplateRequest;
import com.spulido.tfg.domain.template.services.TemplateService;
import com.spulido.tfg.domain.template.services.TemplateServiceMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TemplatesController {

    private final TemplateService templateService;
    private final TemplateServiceMapper mapper;

    @GetMapping
    public ResponseEntity<ResponseList<TemplateInfo>> listTemplates(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        var templates = templateService.listTemplates(PageRequest.of(page, size));
        return ResponseEntity.ok(mapper.templatesListToResponseList(templates));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateInfo> getTemplate(@PathVariable("id") String id) throws TemplateException {
        Template template = templateService.getTemplate(id);
        return ResponseEntity.ok(mapper.templateToInfo(template));
    }

    @PostMapping
    public ResponseEntity<TemplateInfo> createTemplate(@RequestBody @Valid TemplateRequest request)
            throws TemplateException, URISyntaxException {
        Template toCreate = mapper.requestToTemplate(request);
        Template created = templateService.createTemplate(toCreate);
        TemplateInfo dto = mapper.templateToInfo(created);
        return ResponseEntity.created(new URI(String.format("/api/templates/%s", dto.getId()))).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateInfo> updateTemplate(
            @PathVariable("id") String id,
            @RequestBody @Valid TemplateRequest request) throws TemplateException {
        Template toUpdate = mapper.requestToTemplate(request);
        Template updated = templateService.updateTemplate(id, toUpdate);
        return ResponseEntity.ok(mapper.templateToInfo(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable("id") String id) throws TemplateException {
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
