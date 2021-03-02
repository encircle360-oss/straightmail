package com.encircle360.oss.straightmail.service.template;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.encircle360.oss.straightmail.model.Template;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractTemplateLoader implements TemplateLoader {

    protected String getFileContent(String path) {
        Resource resource = new ClassPathResource("templates/" + path);
        if(resource.exists()) {
            try {
                return new String(resource.getInputStream().readAllBytes());
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return null;
    }

    protected Template loadFromFiles(String templateId) {
        String baseTemplatePath = templateId + ".ftl";
        String subjectTemplatePath = templateId + "_subject.ftl";
        String plainTemplatePath = templateId + "_plain.ftl";

        return Template
            .builder()
            .id(templateId)
            .name(templateId)
            .subject(getFileContent(subjectTemplatePath))
            .plain(getFileContent(plainTemplatePath))
            .html(getFileContent(baseTemplatePath))
            .build();
    }
}
