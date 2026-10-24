package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.template.loader.FileTemplateLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = TestApplication.class)
public class AbstractTemplateLoaderTest {

    @Autowired
    FileTemplateLoader fileTemplateLoader;

    @Test
    void load_template_with_html_and_subject_returns_null_plain_when_plain_file_is_absent() {
        // test_json_node.ftl + test_json_node_subject.ftl exist; test_json_node_plain.ftl does not.
        Template template = fileTemplateLoader.loadTemplate("test_json_node");

        assertNotNull(template);
        assertNotNull(template.getHtml());
        assertNotNull(template.getSubject());
        assertNull(template.getPlain(), "Plain template is optional — absence must be silently null");
    }

    @Test
    void load_template_rejects_an_id_that_leaves_the_templates_root() {
        // application.yml sits one level above templates/ on the classpath
        Template template = fileTemplateLoader.loadTemplate("../application");

        assertNull(template.getHtml(), "a traversing template id must not resolve to a file");
        assertNull(template.getSubject());
    }

    @Test
    void load_template_allows_an_id_that_normalises_back_into_the_root() {
        // emails/../test normalises to test, which is a legitimate template — confinement must not
        // reject a path merely because it contains ".."
        Template template = fileTemplateLoader.loadTemplate("emails/../test");

        assertNotNull(template.getHtml());
    }

    @Test
    void load_template_rejects_an_absolute_id() {
        Template template = fileTemplateLoader.loadTemplate("/etc/passwd");

        assertNull(template.getHtml());
        assertNull(template.getSubject());
    }

    @Test
    void load_template_with_missing_id_yields_null_fields() {
        Template template = fileTemplateLoader.loadTemplate("__definitely_missing__");

        assertNotNull(template);
        assertNull(template.getHtml());
        assertNull(template.getSubject());
        assertNull(template.getPlain());
    }
}
