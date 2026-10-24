package com.encircle360.oss.straightmail.service.template.provider;

import com.encircle360.oss.straightmail.dto.template.TemplateView;
import com.encircle360.oss.straightmail.model.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link TemplateSourceProvider} that serves templates from a configurable base directory on the filesystem.
 *
 * <p>Only active when {@code templates.file.enabled=true}. Templates are expected to be organised
 * per-tenant in sub-directories: {@code <base-path>/<tenantId>/<templateName>.ftl}.
 * Subdirectories are supported; a file at {@code <tenantId>/emails/welcome.ftl} gets the
 * template ID {@code emails/welcome}. Companion files for subject and plain-text follow the
 * naming convention {@code <templateName>_subject.ftl} and {@code <templateName>_plain.ftl}.
 *
 * <p>The base path defaults to {@code /resources/templates} and can be overridden via
 * the {@code templates.file.base-path} configuration property.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "templates.file.enabled", havingValue = "true")
public class FileTemplateSourceProvider implements TemplateSourceProvider {

    @Value("${templates.file.base-path:/resources/templates}")
    private String basePath;

    @Override
    public TemplateSource getSource() {
        return TemplateSource.FILE;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public List<TemplateView> listTemplates(String tenantId, List<String> tags) {
        Path tenantDir = Paths.get(basePath, tenantId);
        if (!Files.exists(tenantDir)) {
            return List.of();
        }

        List<TemplateView> views = new ArrayList<>();
        try (var stream = Files.walk(tenantDir)) {
            stream.filter(p -> p.toString().endsWith(".ftl"))
                    .filter(p -> !p.getFileName().toString().contains("_subject") && !p.getFileName().toString().contains("_plain"))
                    .forEach(p -> {
                        String relativePath = tenantDir.relativize(p).toString();
                        String templateName = relativePath.substring(0, relativePath.length() - 4); // remove .ftl
                        Template t = this.buildTemplate(tenantId, templateName, tenantDir);
                        if (t != null) {
                            views.add(TemplateView.ofFile(t));
                        }
                    });
        } catch (IOException e) {
            log.error("Error listing file templates for tenant '{}'", tenantId, e);
        }

        // If the caller requests a different source explicitly, no file templates can match.
        if (hasConflictingSource(tags)) {
            return List.of();
        }
        // "source:file" is a virtual tag — strip it before applying user-defined tag filters.
        return applyTagFilter(views, stripSourceTag(tags));
    }

    /**
     * Checks whether the configured file-template base directory exists and is readable.
     *
     * @return {@code true} if the base directory is accessible; {@code false} otherwise
     */
    public boolean isBaseDirAccessible() {
        Path base = Paths.get(basePath);
        return Files.exists(base) && Files.isReadable(base);
    }

    /**
     * Counts all main template files (i.e. non-companion {@code .ftl} files) across every
     * tenant sub-directory under the configured base path.
     *
     * @return total number of main templates found; {@code 0} if the base directory does not
     * exist or is not readable
     */
    public long countAllTemplates() {
        Path base = Paths.get(basePath);
        if (!Files.exists(base) || !Files.isReadable(base)) {
            return 0L;
        }
        try (var stream = Files.walk(base)) {
            return stream
                    .filter(p -> p.toString().endsWith(".ftl"))
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        return !name.contains("_subject") && !name.contains("_plain");
                    })
                    .count();
        } catch (IOException e) {
            log.error("Error counting file templates under '{}'", basePath, e);
            return 0L;
        }
    }

    @Override
    public Optional<TemplateView> findTemplate(String tenantId, String name) {
        Path tenantDir = Paths.get(basePath, tenantId);
        Template t = this.buildTemplate(tenantId, name, tenantDir);
        return Optional.ofNullable(t).map(TemplateView::ofFile);
    }

    private Template buildTemplate(String tenantId, String name, Path tenantDir) {
        Path base = tenantDir.toAbsolutePath().normalize();

        Path htmlPath = this.resolveWithin(base, name + ".ftl");
        if (htmlPath == null || !Files.exists(htmlPath)) {
            return null;
        }

        String html = this.readFile(htmlPath);
        String subject = this.readFile(this.resolveWithin(base, name + "_subject.ftl"));
        String plain = this.readFile(this.resolveWithin(base, name + "_plain.ftl"));

        return Template.builder()
                .name(name)
                .tenantId(tenantId)
                .html(html)
                .subject(subject)
                .plain(plain)
                .build();
    }

    /**
     * Resolves {@code relative} against {@code base} and confines the result to that directory.
     *
     * <p>Template names reach this provider from caller-supplied input (the {@code ::}-encoded
     * template id on the read, render and send endpoints). Without confinement a name containing
     * {@code ..} escapes the tenant directory and reads another tenant's templates, so a name that
     * does not stay under {@code base} resolves to {@code null} rather than to a file. An absolute
     * name is rejected by the same check, because {@link Path#resolve} would otherwise discard the
     * base entirely.
     *
     * @param base     the tenant directory, already absolute and normalised
     * @param relative the file name to resolve, may contain path separators
     * @return the confined path, or {@code null} if it would leave {@code base}
     */
    private Path resolveWithin(Path base, String relative) {
        Path candidate = base.resolve(relative).normalize();
        return candidate.startsWith(base) ? candidate : null;
    }

    private String readFile(Path path) {
        if (path == null || !Files.exists(path)) {
            return null;
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            log.error("Error reading template file {}", path, e);
            return null;
        }
    }
}
