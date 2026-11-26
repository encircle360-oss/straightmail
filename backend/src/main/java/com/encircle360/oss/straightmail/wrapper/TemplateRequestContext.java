package com.encircle360.oss.straightmail.wrapper;

import org.springframework.web.servlet.support.RequestContext;

import java.util.List;
import java.util.Map;

/**
 * The only view of Spring's {@link RequestContext} that a template gets, exposed as
 * {@code springMacroRequestContext}.
 *
 * <p>The macros in Spring's {@code spring.ftl} read that variable. Handing them the full
 * {@link RequestContext} would also hand every template {@code getWebApplicationContext()}, and with
 * it every bean and every configuration property, including {@code encryption.key}. Tenants author
 * their own templates, so this facade exposes the message and URL methods those macros call and
 * nothing else. The form-binding macros ({@code getBindStatus}) have no use in a mail and stay
 * unavailable.
 */
public class TemplateRequestContext {

    private final RequestContext requestContext;

    /**
     * Wraps the given request context.
     *
     * @param requestContext the request context to delegate to; never exposed to templates
     */
    public TemplateRequestContext(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    /** Used by {@code <@spring.message code />}. */
    public String getMessage(String code) {
        return requestContext.getMessage(code);
    }

    /** Used by {@code <@spring.messageText code, text />}. */
    public String getMessage(String code, String defaultMessage) {
        return requestContext.getMessage(code, defaultMessage);
    }

    /** Used by {@code <@spring.messageArgs code, args />}. */
    public String getMessage(String code, List<?> args) {
        return requestContext.getMessage(code, args);
    }

    /** Used by {@code <@spring.messageArgsText code, args, text />}. */
    public String getMessage(String code, List<?> args, String defaultMessage) {
        return requestContext.getMessage(code, args, defaultMessage);
    }

    /** Used by {@code <@spring.url relativeUrl />}. */
    public String getContextUrl(String relativeUrl) {
        return requestContext.getContextUrl(relativeUrl);
    }

    /** Used by {@code <@spring.url relativeUrl, params... />}. */
    public String getContextUrl(String relativeUrl, Map<String, ?> params) {
        return requestContext.getContextUrl(relativeUrl, params);
    }
}
