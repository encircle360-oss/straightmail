package com.encircle360.oss.straightmail.dto.template;

import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.service.template.provider.TemplateSource;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.encircle360.oss.straightmail.service.template.provider.TemplateSource.*;

/**
 * Internal projection that wraps a {@link Template} with its {@link TemplateSource}, editability flag,
 * and — for Git-backed templates — the branch the template was loaded from.
 *
 * <p>Used by {@link com.encircle360.oss.straightmail.service.template.provider.TemplateSourceProvider} implementations
 * to pass source-annotated templates to the {@link com.encircle360.oss.straightmail.service.template.provider.CompositeTemplateProvider}
 * and subsequently to the controller for mapping to {@link TemplateDTO}.
 */
public record TemplateView(Template template, TemplateSource source, boolean editable, @Nullable String gitBranch) {

    /**
     * Creates a {@link TemplateView} for a database-backed template (always editable).
     *
     * @param t the database template
     * @return a view with source {@link TemplateSource#DATABASE} and {@code editable=true}
     */
    public static TemplateView ofDatabase(Template t) {
        return new TemplateView(t, DATABASE, true, null);
    }

    /**
     * Creates a {@link TemplateView} for a Git-backed template (read-only).
     *
     * @param t      the Git template
     * @param branch the Git branch the template was loaded from
     * @return a view with source {@link TemplateSource#GIT} and {@code editable=false}
     */
    public static TemplateView ofGit(Template t, String branch) {
        return new TemplateView(t, GIT, false, branch);
    }

    /**
     * Creates a {@link TemplateView} for a filesystem-backed template (read-only).
     *
     * @param t the file template
     * @return a view with source {@link TemplateSource#FILE} and {@code editable=false}
     */
    public static TemplateView ofFile(Template t) {
        return new TemplateView(t, FILE, false, null);
    }

    /**
     * Returns the effective tag list for this view, including an implicit source tag of the form
     * {@code "source:<sourceName>"} (e.g. {@code "source:database"}, {@code "source:file"}, {@code "source:git"}).
     *
     * <p>Use this instead of {@code template().getTags()} wherever tag filtering or tag display
     * is required, so that the source tag participates in both operations consistently.
     *
     * @return mutable list of tags including the implicit source tag; never {@code null}
     */
    public List<String> effectiveTags() {
        List<String> base = template.getTags() == null ? new ArrayList<>() : new ArrayList<>(template.getTags());
        String sourceTag = "source:" + source.name().toLowerCase();
        if (!base.contains(sourceTag)) {
            base.add(sourceTag);
        }
        return base;
    }
}
