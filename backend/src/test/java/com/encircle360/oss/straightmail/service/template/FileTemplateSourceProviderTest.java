package com.encircle360.oss.straightmail.service.template;

import com.encircle360.oss.straightmail.dto.template.TemplateView;
import com.encircle360.oss.straightmail.service.template.provider.FileTemplateSourceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FileTemplateSourceProviderTest {

    @TempDir
    Path tempDir;

    private FileTemplateSourceProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        provider = new FileTemplateSourceProvider();
        ReflectionTestUtils.setField(provider, "basePath", tempDir.toString());

        // flat template: default/welcome.ftl
        Path defaultDir = tempDir.resolve("default");
        Files.createDirectories(defaultDir);
        Files.writeString(defaultDir.resolve("welcome.ftl"), "<h1>Welcome</h1>");

        // subdirectory template: default/emails/order.ftl + _subject
        Path emailsDir = defaultDir.resolve("emails");
        Files.createDirectories(emailsDir);
        Files.writeString(emailsDir.resolve("order.ftl"), "<p>Your order</p>");
        Files.writeString(emailsDir.resolve("order_subject.ftl"), "Order Confirmation");
    }

    @Test
    void findTemplate_does_not_escape_the_tenant_directory_into_another_tenant() throws IOException {
        // a second tenant with a template of its own
        Path otherTenant = tempDir.resolve("acme-2");
        Files.createDirectories(otherTenant);
        Files.writeString(otherTenant.resolve("secret.ftl"), "<h1>acme-2 only</h1>");

        Optional<TemplateView> result = provider.findTemplate("default", "../acme-2/secret");

        assertTrue(result.isEmpty(), "a template name must not resolve outside its tenant directory");
    }

    @Test
    void findTemplate_does_not_escape_the_base_directory() throws IOException {
        Path outside = tempDir.getParent().resolve("outside-" + tempDir.getFileName());
        Files.createDirectories(outside);
        Files.writeString(outside.resolve("leak.ftl"), "<h1>outside</h1>");

        String escape = "../" + outside.getFileName() + "/leak";
        Optional<TemplateView> result = provider.findTemplate("default", escape);

        assertTrue(result.isEmpty(), "a template name must not resolve outside the configured base path");
    }

    @Test
    void findTemplate_rejects_an_absolute_name_pointing_outside_the_tenant() throws IOException {
        // an absolute name would make Path.resolve discard the tenant directory entirely
        Path otherTenant = tempDir.resolve("acme-3");
        Files.createDirectories(otherTenant);
        Files.writeString(otherTenant.resolve("secret.ftl"), "<h1>acme-3 only</h1>");

        String absolute = otherTenant.resolve("secret").toString();
        Optional<TemplateView> result = provider.findTemplate("default", absolute);

        assertTrue(result.isEmpty(), "an absolute name must not escape the tenant directory");
    }

    @Test
    void listTemplates_includes_subdirectory_path_in_id() {
        List<TemplateView> views = provider.listTemplates("default", null);

        assertEquals(2, views.size());
        Set<String> names = views.stream()
                .map(v -> v.template().getName())
                .collect(Collectors.toSet());
        assertTrue(names.contains("welcome"), "Expected flat template 'welcome'");
        assertTrue(names.contains("emails/order"), "Expected subdirectory template 'emails/order'");
    }

    @Test
    void listTemplates_excludes_companion_subject_and_plain_files() {
        List<TemplateView> views = provider.listTemplates("default", null);

        boolean hasSubjectAsEntry = views.stream()
                .anyMatch(v -> v.template().getName().contains("_subject") || v.template().getName().contains("_plain"));
        assertFalse(hasSubjectAsEntry, "Companion _subject/_plain files must not appear as standalone templates");
    }

    @Test
    void findTemplate_resolves_subdirectory_path_correctly() {
        Optional<TemplateView> result = provider.findTemplate("default", "emails/order");

        assertTrue(result.isPresent(), "Expected to find template 'emails/order'");
        assertEquals("<p>Your order</p>", result.get().template().getHtml());
        assertEquals("Order Confirmation", result.get().template().getSubject());
    }

    @Test
    void findTemplate_resolves_flat_template() {
        Optional<TemplateView> result = provider.findTemplate("default", "welcome");

        assertTrue(result.isPresent(), "Expected to find template 'welcome'");
        assertEquals("<h1>Welcome</h1>", result.get().template().getHtml());
        assertNull(result.get().template().getSubject());
    }

    @Test
    void listTemplates_returns_empty_for_unknown_tenant() {
        List<TemplateView> views = provider.listTemplates("unknown-tenant", null);
        assertTrue(views.isEmpty());
    }

    // --- source-tag filtering (delegates to TemplateSourceProvider default methods) ---

    @Test
    void listTemplates_withConflictingSourceTag_returnsEmpty() {
        // Explicitly request database source — file provider must not return anything
        List<TemplateView> views = provider.listTemplates("default", List.of("source:database"));
        assertTrue(views.isEmpty(), "Expected empty list when conflicting source tag is requested");
    }

    @Test
    void listTemplates_withOwnSourceTag_returnsAll() {
        // source:file is a virtual tag — it should be stripped and all templates returned
        List<TemplateView> views = provider.listTemplates("default", List.of("source:file"));
        assertEquals(2, views.size(), "Expected all templates when own source tag is the only filter");
    }

    @Test
    void listTemplates_withUnmatchedUserTag_returnsEmpty() {
        // Tag the welcome template and filter for a tag that does not exist
        List<TemplateView> views = provider.listTemplates("default", List.of("no-such-tag"));
        assertTrue(views.isEmpty(), "Expected empty list when user tag matches no template");
    }
}
