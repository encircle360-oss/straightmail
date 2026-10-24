package com.encircle360.oss.straightmail.config;

import com.encircle360.oss.straightmail.wrapper.JsonNodeObjectWrapper;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Hardens the auto-configured FreeMarker {@link Configuration} once at startup.
 *
 * <p>Tenants author their own email templates, so template source is untrusted input. FreeMarker
 * defaults to {@link TemplateClassResolver#UNRESTRICTED_RESOLVER}, which lets the {@code ?new}
 * built-in instantiate any class on the classpath — including
 * {@code freemarker.template.utility.Execute}, which runs OS commands. Rendering a template is
 * scoped to a tenant's own mail; it must never reach the host or the Spring context.
 *
 * <p>The object wrapper is set here as well rather than per request: {@link Configuration} is a
 * shared singleton, so mutating it while requests are in flight is both a race and a needless
 * repetition of startup work.
 */
@org.springframework.context.annotation.Configuration
@RequiredArgsConstructor
@Slf4j
public class FreemarkerSecurityConfig {

    private final Configuration freemarkerConfiguration;

    private final JsonNodeObjectWrapper jsonNodeObjectWrapper;

    @PostConstruct
    void hardenFreemarkerConfiguration() {
        freemarkerConfiguration.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        freemarkerConfiguration.setAPIBuiltinEnabled(false);
        freemarkerConfiguration.setObjectWrapper(jsonNodeObjectWrapper);
        log.info("FreeMarker sandbox active: ?new resolves nothing, ?api disabled");
    }
}
