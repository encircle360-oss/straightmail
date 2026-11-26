package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.service.FreemarkerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Pins the FreeMarker sandbox.
 *
 * <p>Tenants author their own templates, so template source is untrusted input. FreeMarker's
 * default {@code TemplateClassResolver.UNRESTRICTED_RESOLVER} lets {@code ?new} instantiate any
 * class on the classpath — including {@code freemarker.template.utility.Execute}, which runs OS
 * commands. These tests fail if the resolver is ever reset to the library default.
 */
@SpringBootTest(classes = TestApplication.class)
class FreemarkerSandboxTest {

    @Autowired
    FreemarkerService freemarkerService;

    @Test
    void new_builtin_cannot_instantiate_the_execute_utility() {
        String payload = "<#assign e=\"freemarker.template.utility.Execute\"?new()>${e(\"echo pwned\")}";

        Exception thrown = Assertions.assertThrows(Exception.class,
            () -> freemarkerService.renderTemplateToString(payload, "en", null));

        Assertions.assertFalse(thrown.getMessage() != null && thrown.getMessage().contains("pwned"),
            "command output must never reach the rendered result");
    }

    @Test
    void new_builtin_is_blocked_by_the_class_resolver_not_by_type_checks() {
        // Execute implements TemplateMethodModel, so ?new would accept it on type grounds alone.
        // Only the class resolver stops it — assert the message says so, otherwise this test would
        // pass for the wrong reason.
        String payload = "<#assign e=\"freemarker.template.utility.Execute\"?new()>${e(\"echo x\")}";

        Exception thrown = Assertions.assertThrows(Exception.class,
            () -> freemarkerService.renderTemplateToString(payload, "en", null));

        String message = String.valueOf(thrown.getMessage()).toLowerCase();
        Assertions.assertTrue(message.contains("security") || message.contains("not allowed"),
            "expected the class resolver to refuse instantiation, got: " + thrown.getMessage());
    }

    @Test
    void request_context_does_not_expose_the_application_context() {
        // RequestContext.getWebApplicationContext() would hand the template every bean and every
        // property, encryption.key included.
        String payload = "${springMacroRequestContext.webApplicationContext.environment"
            + ".getProperty('encryption.key')}";

        Exception thrown = Assertions.assertThrows(Exception.class,
            () -> freemarkerService.renderTemplateToString(payload, "en", null));

        Assertions.assertFalse(String.valueOf(thrown.getMessage()).contains("AAAAAAAA"),
            "the encryption key must never reach the template");
    }

    @Test
    void templates_cannot_reflect_via_get_class() {
        String payload = "${springMacroRequestContext.class.classLoader}";

        Assertions.assertThrows(Exception.class,
            () -> freemarkerService.renderTemplateToString(payload, "en", null));
    }

    @Test
    void spring_message_macros_still_render() {
        String rendered = Assertions.assertDoesNotThrow(() -> freemarkerService.renderTemplateToString(
            "<@spring.messageText 'no.such.key', 'fallback' />", "en", null));

        Assertions.assertEquals("fallback", rendered);
    }

    @Test
    void ordinary_templates_still_render() {
        String rendered = Assertions.assertDoesNotThrow(
            () -> freemarkerService.renderTemplateToString("Hello ${'world'}", "en", null));

        Assertions.assertEquals("Hello world", rendered);
    }
}
