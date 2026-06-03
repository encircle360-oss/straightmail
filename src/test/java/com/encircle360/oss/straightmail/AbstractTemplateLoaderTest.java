package com.encircle360.oss.straightmail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.template.FileTemplateLoader;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
public class AbstractTemplateLoaderTest {

    @Autowired FileTemplateLoader fileTemplateLoader;

    @Test
    public void load_template_with_html_and_subject_returns_template_with_null_plain() {
        // test.ftl + test_subject.ftl exist as resources, test_plain.ftl does not.
        Template template = fileTemplateLoader.loadTemplate("test");

        assertNotNull(template);
        assertNotNull(template.getHtml());
        assertNotNull(template.getSubject());
        assertNull(template.getPlain(),
                "Plain template is optional - absence must be silently null");
    }

    @Test
    public void load_template_with_missing_html_and_subject_yields_null_fields() {
        Template template = fileTemplateLoader.loadTemplate("__definitely_missing__");

        assertNotNull(template);
        assertNull(template.getHtml());
        assertNull(template.getSubject());
        assertNull(template.getPlain());
    }
}
