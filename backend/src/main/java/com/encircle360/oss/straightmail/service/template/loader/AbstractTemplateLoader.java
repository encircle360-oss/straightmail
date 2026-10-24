package com.encircle360.oss.straightmail.service.template.loader;

import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.util.LogSanitizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Abstract base class for {@link TemplateLoader} implementations that load FreeMarker templates
 * from the filesystem.
 *
 * <p>Provides shared helper methods for reading template file content from either the classpath
 * (under {@code classpath:templates/}) or the filesystem (under {@code /resources/templates/}),
 * and for constructing a {@link com.encircle360.oss.straightmail.model.Template} from the
 * three conventional template files ({@code .ftl}, {@code _subject.ftl}, {@code _plain.ftl}).
 */
@Slf4j
public abstract class AbstractTemplateLoader implements TemplateLoader {

    /**
     * Reads a template file that must exist. Logs ERROR when the file cannot be
     * resolved because the missing file will break the surrounding render/send call.
     *
     * @param path template path relative to the {@code templates/} root (e.g. {@code "welcome.ftl"})
     * @return file content, or {@code null} if the file is missing
     */
    protected String getRequiredFileContent(String path) {
        String content = readTemplateFile(path);
        if (content == null) {
            log.error("Couldn't find required template {} in classpath or filesystem.", LogSanitizer.sanitize(path));
        }
        return content;
    }

    /**
     * Reads an optional template file. Missing files are not logged because the
     * caller is expected to have a fallback (e.g. deriving plain text from HTML).
     * I/O errors while reading an existing file are still logged at ERROR.
     *
     * @param path template path relative to the {@code templates/} root
     * @return file content, or {@code null} if the file does not exist
     */
    protected String getOptionalFileContent(String path) {
        return readTemplateFile(path);
    }

    private String readTemplateFile(String path) {
        if (escapesTemplateRoot(path)) {
            log.warn("Rejected template path '{}': it would leave the templates root.", LogSanitizer.sanitize(path));
            return null;
        }

        String fullClassPath = "templates/" + path;
        Resource resource = new ClassPathResource(fullClassPath);
        if (resource.exists()) {
            try {
                return new String(resource.getInputStream().readAllBytes());
            } catch (IOException e) {
                log.error("Error while reading template {} from classpath.", LogSanitizer.sanitize(fullClassPath), e);
                return null;
            }
        }

        Path templateRoot = Paths.get("/resources/templates").toAbsolutePath().normalize();
        Path filePath = templateRoot.resolve(path).normalize();
        if (filePath.startsWith(templateRoot) && Files.exists(filePath)) {
            try {
                return Files.readString(filePath);
            } catch (Exception e) {
                log.error("Error while reading template {} from filesystem.", LogSanitizer.sanitize(filePath), e);
                return null;
            }
        }

        return null;
    }

    /**
     * Builds a {@link com.encircle360.oss.straightmail.model.Template} by reading the three
     * conventional files for the given template ID:
     * <ul>
     *   <li>{@code <templateId>.ftl} — HTML body (required)</li>
     *   <li>{@code <templateId>_subject.ftl} — subject line (required)</li>
     *   <li>{@code <templateId>_plain.ftl} — plain-text body (optional)</li>
     * </ul>
     *
     * @param templateId the template identifier (without suffix)
     * @return a {@link com.encircle360.oss.straightmail.model.Template} with content read from files;
     * fields are {@code null} if the corresponding file does not exist
     */
    protected Template loadFromFiles(String templateId) {
        String baseTemplatePath = templateId + ".ftl";
        String subjectTemplatePath = templateId + "_subject.ftl";
        String plainTemplatePath = templateId + "_plain.ftl";

        // The HTML body and the subject line are required to build a valid mail;
        // the plain-text body is optional because EmailService derives one from
        // the rendered HTML when no dedicated template is present.
        return Template.builder()
                .name(templateId)
                .subject(getRequiredFileContent(subjectTemplatePath))
                .plain(getOptionalFileContent(plainTemplatePath))
                .html(getRequiredFileContent(baseTemplatePath))
                .build();
    }

    /**
     * Returns {@code true} when a template path would resolve outside the templates root.
     *
     * <p>Template ids reach the loaders from caller-supplied input, so a path containing
     * {@code ..} or an absolute path must not be read. The classpath lookup is checked here
     * because {@link ClassPathResource} normalises {@code ..} away rather than refusing it.
     *
     * @param path template path relative to the {@code templates/} root
     * @return {@code true} if the path is absolute or escapes the root
     */
    private boolean escapesTemplateRoot(String path) {
        if (path == null || path.isBlank()) {
            return true;
        }
        Path root = Paths.get("templates").normalize();
        Path candidate = root.resolve(path).normalize();
        return !candidate.startsWith(root);
    }
}
