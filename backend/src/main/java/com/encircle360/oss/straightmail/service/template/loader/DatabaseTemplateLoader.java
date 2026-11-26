package com.encircle360.oss.straightmail.service.template.loader;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.template.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * {@link TemplateLoader} implementation active when the {@code database} Spring profile is enabled.
 *
 * <p>Attempts to load a template from the database via {@link TemplateService}. If no database
 * entry is found, it falls back to loading from the classpath or filesystem using
 * {@link AbstractTemplateLoader#loadFromFiles}.
 */
@Service
@RequiredArgsConstructor
@Profile(DatabaseConfig.PROFILE)
public class DatabaseTemplateLoader extends AbstractTemplateLoader {

    private final TemplateService templateService;

    /**
     * Loads a template by ID, first from the database, then from the filesystem/classpath.
     *
     * @param templateId the template identifier; {@code null} returns {@code null}
     * @return the resolved {@link com.encircle360.oss.straightmail.model.Template},
     * or {@code null} if not found in either source
     */
    @Override
    public Template loadTemplate(String templateId) {
        if (templateId == null) {
            return null;
        }

        Template template = templateService.get(templateId);
        if (template == null) {
            return super.loadFromFiles(templateId);
        }
        return template;
    }
}
