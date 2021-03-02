package com.encircle360.oss.straightmail.service.template;

import java.io.IOException;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.encircle360.oss.straightmail.config.MongoDbConfig;
import com.encircle360.oss.straightmail.model.Template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("!" + MongoDbConfig.PROFILE)
public class FileTemplateLoader implements TemplateLoader {

    @Override
    public Template loadTemplate(String templateId) {
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

    private String getFileContent(String path) {
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
}
