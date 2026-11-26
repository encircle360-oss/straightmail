package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.config.MessageSourceConfig;
import com.encircle360.oss.straightmail.service.FreemarkerService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that message bundles in the external {@code i18n.base-path} directory reach templates
 * through {@code <@spring.message>}, override built-in keys, and leave the built-in bundles as the
 * fallback.
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ExternalMessageBundleTest {

    private static final Path EXTERNAL_DIR = createExternalBundles();

    @Autowired
    private FreemarkerService freemarkerService;

    @DynamicPropertySource
    static void externalBundleDirectory(DynamicPropertyRegistry registry) {
        registry.add("i18n.base-path", EXTERNAL_DIR::toString);
    }

    @AfterAll
    static void deleteExternalBundles() throws IOException {
        FileSystemUtils.deleteRecursively(EXTERNAL_DIR);
    }

    @Test
    void keyOnlyInExternalBundleRenders() throws Exception {
        assertEquals("Grüße aus dem Verzeichnis",
                freemarkerService.renderTemplateToString("<@spring.message 'external.greeting'/>", "de", null));
        assertEquals("Grüße aus dem Verzeichnis",
                freemarkerService.renderTemplateToString("<@spring.message 'external.greeting'/>", "en", null));
    }

    @Test
    void externalValueWinsOverBuiltInValue() throws Exception {
        assertEquals("Externer Text",
                freemarkerService.renderTemplateToString("<@spring.message 'email.text'/>", "de", null));
    }

    @Test
    void builtInKeysStillResolve() throws Exception {
        assertEquals("Betreff",
                freemarkerService.renderTemplateToString("<@spring.message 'email.subject'/>", "de", null));
        assertEquals("Subject",
                freemarkerService.renderTemplateToString("<@spring.message 'email.subject'/>", "en", null));
    }

    @Test
    void missingExternalDirectoryFallsBackToBuiltInBundles() {
        MessageSource messageSource = new MessageSourceConfig()
                .messageSource(EXTERNAL_DIR.resolve("does-not-exist").toString());

        assertEquals("Betreff", messageSource.getMessage("email.subject", null, Locale.GERMAN));
        assertEquals("Some text here ...", messageSource.getMessage("email.text", null, Locale.ENGLISH));
    }

    private static Path createExternalBundles() {
        try {
            Path dir = Files.createTempDirectory("straightmail-i18n");
            Files.writeString(dir.resolve("messages.properties"),
                    "external.greeting=Grüße aus dem Verzeichnis\n", StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("messages_de.properties"),
                    "email.text=Externer Text\n", StandardCharsets.UTF_8);
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
