package com.encircle360.oss.straightmail.service.template;

import com.encircle360.oss.straightmail.model.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public abstract class AbstractTemplateLoader implements TemplateLoader {

    protected String getFileContent(String path) {
        String fullClassPath = "templates/" + path;
        Resource resource = new ClassPathResource(fullClassPath);
        if (resource.exists()) {
            try {
                return new String(resource.getInputStream().readAllBytes());
            } catch (IOException e) {
                log.error("Error while getting file content from classpath {}.", fullClassPath, e);
            }
        }

        String fullFsPath = "/resources/templates/" + path;
        Path filePath = Paths.get(fullFsPath);
        if (Files.exists(filePath)) {
            try {
                return Files.readString(filePath);
            } catch (Exception e) {
                log.error("Error while getting file content {} from filesystem.", fullFsPath, e);
            }
        }

        log.error("Couldn't find template {} in classpath or filesystem.", path);
        return null;
    }

    protected Template loadFromFiles(String templateId) {
        String baseTemplatePath = templateId + ".ftl";
        String subjectTemplatePath = templateId + "_subject.ftl";
        String plainTemplatePath = templateId + "_plain.ftl";

        return Template.builder()
                .id(templateId)
                .name(templateId)
                .subject(getFileContent(subjectTemplatePath))
                .plain(getFileContent(plainTemplatePath))
                .html(getFileContent(baseTemplatePath))
                .build();
    }
}
