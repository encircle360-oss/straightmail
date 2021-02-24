package com.encircle360.oss.straightmail.service;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Locale;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.ui.ModelMap;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.servlet.support.RequestContext;

import com.encircle360.oss.straightmail.dto.FakeLocaleHttpServletRequest;
import com.encircle360.oss.straightmail.wrapper.JsonNodeObjectWrapper;
import com.fasterxml.jackson.databind.JsonNode;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FreemarkerService {

    @Value("${spring.mail.default-template}")
    private final String DEFAULT_TEMPLATE = null;

    private final String DEFAULT_LOCALE = Locale.getDefault().getLanguage();

    private final JsonNodeObjectWrapper jsonNodeObjectWrapper;

    private final Configuration freemarkerConfiguration;

    private final ServletContext context;

    public String parseTemplateFromString(String templateContent, String locale, HashMap<String, JsonNode> model) throws IOException, TemplateException {
        if(templateContent == null) {
            return null;
        }

        ModelMap modelMap = toModelMap(model);

        if (locale == null) {
            locale = DEFAULT_LOCALE;
        }

        freemarkerConfiguration.setObjectWrapper(jsonNodeObjectWrapper);
        Template template = new Template("email", new StringReader(templateContent), freemarkerConfiguration);
        return processTemplate(template, locale, modelMap);
    }

    public ModelMap toModelMap(HashMap<String, JsonNode> model) {
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

        // add macro request context, otherwise spring import will not work
        modelMap.addAttribute("springMacroRequestContext",
            new RequestContext(new FakeLocaleHttpServletRequest(locale), context));

        // process to string and return
        return FreeMarkerTemplateUtils.processTemplateIntoString(template, modelMap);
    }

    public boolean templateExists(String templateId) {
        String templatePath = templateId + ".ftl";
        return new ClassPathResource("templates/" + templatePath).exists();
    }

    public String parseTemplateFromFile(String emailTemplateFileId, String locale, HashMap<String, JsonNode> model) throws IOException, TemplateException {
        ModelMap modelMap = toModelMap(model);

        if (emailTemplateFileId == null) {
            emailTemplateFileId = DEFAULT_TEMPLATE;
        }

        if (locale == null) {
            locale = DEFAULT_LOCALE;
        }

        String templatePath = emailTemplateFileId + ".ftl";

        freemarkerConfiguration.setObjectWrapper(jsonNodeObjectWrapper);
        Template template = freemarkerConfiguration.getTemplate(templatePath);

        return processTemplate(template, locale, modelMap);
    }

}
