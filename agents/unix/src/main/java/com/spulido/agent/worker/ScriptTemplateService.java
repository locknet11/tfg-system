package com.spulido.agent.worker;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ScriptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ScriptTemplateService.class);

    private static final String TEMPLATES_BASE_PATH = "scripts/";

    public String renderTemplate(String templateName, java.util.Map<String, String> replacements) {
        String templatePath = TEMPLATES_BASE_PATH + templateName;
        try {
            ClassPathResource resource = new ClassPathResource(templatePath);
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            for (java.util.Map.Entry<String, String> entry : replacements.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                String value = entry.getValue() != null ? entry.getValue() : "";
                content = content.replace(placeholder, value);
            }

            return content;
        } catch (IOException e) {
            log.error("Failed to load template: {}", templatePath, e);
            throw new RuntimeException("Failed to load template: " + templatePath, e);
        }
    }
}
