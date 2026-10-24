package com.encircle360.oss.straightmail.service;

import com.encircle360.oss.straightmail.util.FakeLocaleHttpServletRequest;
import com.encircle360.oss.straightmail.wrapper.TemplateRequestContext;
import tools.jackson.databind.JsonNode;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.servlet.ServletContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.support.RequestContext;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Map;

/**
 * Service for rendering FreeMarker templates into strings.
 *
 * <p>Supports two rendering modes:
 * <ul>
 *   <li><b>Inline rendering</b> ({@link #renderTemplateToString}): parses and renders a raw
 *       FreeMarker template string passed directly as content.</li>
 *   <li><b>File-based rendering</b> ({@link #parseTemplateFromFile}): loads a {@code .ftl} file
 *       from the classpath by template ID and renders it.</li>
 * </ul>
 *
 * <p>All templates have access to Spring message macros via {@code <@spring.messages 'key' />},
 * enabled by injecting a {@code springMacroRequestContext} built from a synthetic
 * {@link FakeLocaleHttpServletRequest} using the requested locale.
 *
 * <p>JSON model data is made available to templates through a custom
 * {@link com.encircle360.oss.straightmail.wrapper.JsonNodeObjectWrapper}, which is installed on the
 * shared {@link Configuration} at startup by
 * {@link com.encircle360.oss.straightmail.config.FreemarkerSecurityConfig} together with the
 * template sandbox.
 */
@Service
@RequiredArgsConstructor
public class FreemarkerService {

    @Value("${spring.mail.default-template}")
    private String DEFAULT_TEMPLATE = null;

    private final String DEFAULT_LOCALE = Locale.getDefault().getLanguage();

    private final Configuration freemarkerConfiguration;

    private final ServletContext context;

    /**
     * Renders a raw FreeMarker template string with the given model.
     *
     * @param templateContent the FreeMarker template source as a string; {@code null} returns {@code null}
     * @param locale          BCP 47 locale tag (e.g. {@code "de"}, {@code "en-US"}); falls back to JVM default if {@code null}
     * @param model           key-value pairs passed as template variables; may be {@code null}
     * @return the rendered output string
     * @throws IOException       if the template cannot be parsed
     * @throws TemplateException if rendering fails due to a template error
     */
    public String renderTemplateToString(String templateContent, String locale, Map<String, JsonNode> model) throws IOException, TemplateException {
        if (templateContent == null) {
            return null;
        }

        ModelMap modelMap = this.toModelMap(model);

        if (locale == null) {
            locale = DEFAULT_LOCALE;
        }

        Template template = new Template("email", new StringReader(templateContent), freemarkerConfiguration);
        return this.processTemplate(template, locale, modelMap);
    }

    /**
     * Converts a {@code Map<String, JsonNode>} model into a Spring {@link ModelMap}.
     *
     * @param model the raw model map; {@code null} returns an empty {@link ModelMap}
     * @return a {@link ModelMap} containing all entries from the input map
     */
    public ModelMap toModelMap(Map<String, JsonNode> model) {
        ModelMap modelMap = new ModelMap();
        if (model == null) {
            return modelMap;
        }
        modelMap.addAllAttributes(model);
        return modelMap;
    }

    private String processTemplate(Template template, String locale, ModelMap modelMap) throws IOException, TemplateException {

        template.setLocale(Locale.forLanguageTag(locale));

        // add import of spring macros, so we can use <@spring.messages 'x' /> in our templates
        template.addAutoImport("spring", "spring.ftl");

        // add macro request context, otherwise spring import will not work; only the narrow facade
        // reaches the template, never the RequestContext with its application context
        modelMap.addAttribute("springMacroRequestContext", new TemplateRequestContext(
                new RequestContext(new FakeLocaleHttpServletRequest(locale), context)));

        // process to string and return
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, modelMap);
    }

    /**
     * Checks whether a FreeMarker template file exists on the classpath.
     *
     * @param templateId the template identifier (without {@code .ftl} suffix)
     * @return {@code true} if the corresponding {@code .ftl} file exists under {@code classpath:templates/}
     */
    public boolean templateExists(String templateId) {
        String templatePath = templateId + ".ftl";
        return new ClassPathResource("templates/" + templatePath).exists();
    }

    /**
     * Loads a FreeMarker template from the classpath by ID and renders it with the given model.
     *
     * @param emailTemplateFileId the template ID (without {@code .ftl} suffix); falls back to the
     *                            configured default template if {@code null}
     * @param locale              BCP 47 locale tag; falls back to JVM default if {@code null}
     * @param model               key-value pairs passed as template variables; may be {@code null}
     * @return the rendered output string
     * @throws IOException       if the template file cannot be read
     * @throws TemplateException if rendering fails due to a template error
     */
    public String parseTemplateFromFile(String emailTemplateFileId, String locale, Map<String, JsonNode> model) throws IOException, TemplateException {
        ModelMap modelMap = this.toModelMap(model);

        if (emailTemplateFileId == null) {
            emailTemplateFileId = DEFAULT_TEMPLATE;
        }

        if (locale == null) {
            locale = DEFAULT_LOCALE;
        }

        String templatePath = emailTemplateFileId + ".ftl";

        Template template = freemarkerConfiguration.getTemplate(templatePath);

        return this.processTemplate(template, locale, modelMap);
    }

}
