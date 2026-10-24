package com.encircle360.oss.straightmail.service.template.loader;

import com.encircle360.oss.straightmail.model.Template;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * {@link TemplateLoader} implementation that loads templates from the filesystem or classpath.
 *
 * <p>Reads from the classpath ({@code classpath:templates/}) or the filesystem
 * ({@code /resources/templates/}). Used by {@link com.encircle360.oss.straightmail.service.EmailService}
 * as a final fallback after {@link com.encircle360.oss.straightmail.service.template.provider.CompositeTemplateProvider}
 * returns no match. Logs all discovered filesystem template files at startup.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileTemplateLoader extends AbstractTemplateLoader {

    /**
     * Loads a template by delegating to {@link AbstractTemplateLoader#loadFromFiles}.
     *
     * @param templateId the template identifier (without {@code .ftl} suffix)
     * @return the loaded {@link com.encircle360.oss.straightmail.model.Template}
     */
    @Override
    public Template loadTemplate(String templateId) {
        return super.loadFromFiles(templateId);
    }

    /**
     * Logs all {@code .ftl} files found under {@code /resources/templates/} at startup for diagnostics.
     * Emits an info message if the directory does not exist.
     *
     * @throws IOException if directory traversal fails
     */
    @PostConstruct
    public void logTemplates() throws IOException {
        log.info("FileTemplateLoader initialisation started...");
        Resource folder = new FileSystemResource("/resources/templates/");
        String fileName = folder.getFile().getPath();
        Path path = Path.of(fileName);

        if (!path.toFile().exists()) {
            log.info("/resources/templates/ does not exist. Service will not load any templates from filesystem.");
            return;
        }

        try (Stream<Path> files = Files.walk(path)) {
            files.filter(file -> file.getFileName().toString().contains("."))
                    .forEach(file -> log.info("Found filesystem template file {}", file.toAbsolutePath()));
        }
    }
}
