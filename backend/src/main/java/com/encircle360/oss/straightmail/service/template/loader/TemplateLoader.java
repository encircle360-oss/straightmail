package com.encircle360.oss.straightmail.service.template.loader;

import com.encircle360.oss.straightmail.model.Template;

/**
 * Strategy interface for loading a {@link com.encircle360.oss.straightmail.model.Template}
 * by its identifier in file-based (non-database) mode.
 *
 * <p>Implementations are used when the {@code database} Spring profile is <em>not</em> active
 * and templates are served from classpath or filesystem resources.
 */
public interface TemplateLoader {

    /**
     * Loads and returns the template for the given identifier.
     *
     * @param templateId the unique template identifier (typically a file path without extension)
     * @return the loaded {@link com.encircle360.oss.straightmail.model.Template},
     * or {@code null} if no template with the given ID exists
     */
    Template loadTemplate(String templateId);
}
