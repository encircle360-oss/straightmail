package com.encircle360.oss.straightmail.service.template;

import com.encircle360.oss.straightmail.model.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public abstract class AbstractTemplateLoader implements TemplateLoader {

    /**
     * Reads a template file that must exist. If the file is not found in either
     * the classpath or the filesystem, the absence is logged at ERROR level
     * because the missing file will break the surrounding render / send call.
     *
     * @param path template path relative to the {@code templates/} root (for
     *             example {@code auth/password-change-verification.ftl})
     * @return file content, or {@code null} if the file is missing
     */
    protected String getRequiredFileContent(String path) {
        String content = readTemplateFile(path);
        if (content == null) {
            log.error("Couldn't find required template {} in classpath or filesystem.", path);
        }
        return content;
    }

    /**
     * Reads an optional template file. Missing files are not logged because the
     * caller is expected to have a fallback (for example deriving the plain
     * text body from the HTML body). I/O errors while reading an existing file
     * are still logged at ERROR level since they indicate a real defect.
     *
     * @param path template path relative to the {@code templates/} root
     * @return file content, or {@code null} if the file does not exist
     */
    protected String getOptionalFileContent(String path) {
        return readTemplateFile(path);
    }

    /**
     * @deprecated Use {@link #getRequiredFileContent(String)} for mandatory
     * files or {@link #getOptionalFileContent(String)} for files with a
     * documented fallback. Kept temporarily for binary compatibility.
     */
    @Deprecated(forRemoval = true)
    protected String getFileContent(String path) {
        return getRequiredFileContent(path);
    }

    private String readTemplateFile(String path) {
        String fullClassPath = "templates/" + path;
        Resource resource = new ClassPathResource(fullClassPath);
        if (resource.exists()) {
            try {
                return new String(resource.getInputStream().readAllBytes());
            } catch (IOException e) {
                log.error("Error while reading template {} from classpath.", fullClassPath, e);
                return null;
            }
        }

        String fullFsPath = "/resources/templates/" + path;
        Path filePath = Paths.get(fullFsPath);
        if (Files.exists(filePath)) {
            try {
                return Files.readString(filePath);
            } catch (Exception e) {
                log.error("Error while reading template {} from filesystem.", fullFsPath, e);
                return null;
            }
        }

        return null;
    }

    protected Template loadFromFiles(String templateId) {
        String baseTemplatePath = templateId + ".ftl";
        String subjectTemplatePath = templateId + "_subject.ftl";
        String plainTemplatePath = templateId + "_plain.ftl";

        // The HTML body and the subject line are required to build a valid mail;
        // the plain-text body is optional because EmailService derives one from
        // the rendered HTML when no dedicated template is present.
        return Template.builder()
                .id(templateId)
                .name(templateId)
                .subject(getRequiredFileContent(subjectTemplatePath))
                .plain(getOptionalFileContent(plainTemplatePath))
                .html(getRequiredFileContent(baseTemplatePath))
                .build();
    }
}
