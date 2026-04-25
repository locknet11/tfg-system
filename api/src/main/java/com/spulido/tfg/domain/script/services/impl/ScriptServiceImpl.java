package com.spulido.tfg.domain.script.services.impl;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.spulido.tfg.domain.script.services.ScriptService;
import com.spulido.tfg.domain.target.model.OperatingSystem;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScriptServiceImpl implements ScriptService {

    private final Configuration freemarkerConfiguration;

    @Override
    public String generateInstallScript(
            OperatingSystem os,
            String apiUrl,
            String organizationIdentifier,
            String projectIdentifier,
            String targetUniqueId,
            String preauthCode) {

        String templateName = getTemplateNameForOs(os);

        Map<String, Object> model = new HashMap<>();
        model.put("apiUrl", apiUrl);
        model.put("organizationIdentifier", organizationIdentifier);
        model.put("projectIdentifier", projectIdentifier);
        model.put("targetUniqueId", targetUniqueId);
        model.put("preauthCode", preauthCode);

        try {
            Template template = freemarkerConfiguration.getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("Error generating install script for OS: {}", os, e);
            throw new RuntimeException("Failed to generate installation script", e);
        }
    }

    private String getTemplateNameForOs(OperatingSystem os) {
        return switch (os) {
            case LINUX -> "unix.sh.ftl";
        };
    }

    private String getErrorTemplateNameForOs(OperatingSystem os) {
        return switch (os) {
            case LINUX -> "unix-error.sh.ftl";
        };
    }

    @Override
    public String generateInstallErrorScript(OperatingSystem os, String targetUniqueId, String errorMessage) {
        String templateName = getErrorTemplateNameForOs(os);

        Map<String, Object> model = new HashMap<>();
        model.put("targetUniqueId", targetUniqueId);
        model.put("errorMessage", errorMessage);

        try {
            Template template = freemarkerConfiguration.getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (Exception e) {
            log.error("Error generating install error script for OS: {}", os, e);
            throw new RuntimeException("Failed to generate installation error script", e);
        }
    }
}
