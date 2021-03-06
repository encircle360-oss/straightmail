package com.encircle360.oss.straightmail.service.template;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.encircle360.oss.straightmail.config.MongoDbConfig;
import com.encircle360.oss.straightmail.model.Template;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Profile(MongoDbConfig.PROFILE)
public class MongoDbTemplateLoader extends AbstractTemplateLoader {

    private final TemplateService templateService;

    @Override
    public Template loadTemplate(String templateId) {
        if (templateId == null) {
            return null;
        }

        Template template = templateService.get(templateId);
        if (template == null) {
            return loadFromFiles(templateId);
        }
        return template;
    }
}
