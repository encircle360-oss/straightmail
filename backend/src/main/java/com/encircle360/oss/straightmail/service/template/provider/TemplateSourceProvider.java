package com.encircle360.oss.straightmail.service.template.provider;

import com.encircle360.oss.straightmail.dto.template.TemplateView;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Strategy interface for a single template source (database, Git, or filesystem) used in database mode.
 *
 * <p>Implementations are aggregated by {@link CompositeTemplateProvider}, which delegates
 * template listing and resolution to all enabled providers in injection order.
 *
 * <p>Default methods centralise the source-tag filtering logic that is common to all providers:
 * {@link #hasConflictingSource}, {@link #stripSourceTag}, and {@link #applyTagFilter}.
 * Implementations call these in {@link #listTemplates} to avoid duplicating the filtering pattern.
 */
public interface TemplateSourceProvider {

    /**
     * Returns the {@link TemplateSource} type this provider represents.
     *
     * @return the template source type
     */
    TemplateSource getSource();

    /**
     * Indicates whether this provider is currently active and should be queried.
     *
     * @return {@code true} if this provider is enabled and should participate in template resolution
     */
    boolean isEnabled();

    /**
     * Lists all templates available from this source for the given tenant, optionally filtered by tags.
     *
     * @param tenantId the tenant slug whose templates should be listed
     * @param tags     optional list of tags to filter by; {@code null} or empty means no filter
     * @return list of matching {@link TemplateView} entries; never {@code null}
     */
    List<TemplateView> listTemplates(String tenantId, List<String> tags);

    /**
     * Looks up a specific template by name for the given tenant.
     *
     * @param tenantId the tenant slug
     * @param name     the template name / identifier
     * @return an {@link Optional} containing the matching {@link TemplateView}, or empty if not found
     */
    Optional<TemplateView> findTemplate(String tenantId, String name);

    /**
     * Returns {@code true} when the tag list explicitly requests a different source than this provider.
     *
     * <p>For example, a {@code FileTemplateSourceProvider} returns {@code true} if the tags contain
     * {@code source:database} or {@code source:git}, because those requests cannot be satisfied here.
     *
     * @param tags the caller-supplied tag filter; may be {@code null}
     * @return {@code true} if a conflicting {@code source:*} tag is present
     */
    default boolean hasConflictingSource(List<String> tags) {
        String ownSourceTag = "source:" + getSource().name().toLowerCase();
        return tags != null && tags.stream()
                .anyMatch(t -> t.startsWith("source:") && !t.equals(ownSourceTag));
    }

    /**
     * Returns a copy of the tag list with this provider's virtual source tag removed.
     *
     * <p>Source tags (e.g. {@code source:file}) are implicit — they are never stored on templates
     * themselves. Stripping them before forwarding to the underlying data store prevents spurious
     * "no match" results.
     *
     * @param tags the caller-supplied tag filter; may be {@code null}
     * @return filtered tag list without the own source tag, or {@code null} if input was {@code null}
     */
    default List<String> stripSourceTag(List<String> tags) {
        String ownSourceTag = "source:" + getSource().name().toLowerCase();
        return tags == null ? null : tags.stream().filter(t -> !t.equals(ownSourceTag)).toList();
    }

    /**
     * Filters the given view list to only those whose effective tags contain all requested tags.
     *
     * <p>When {@code userTags} is {@code null} or empty, the full list is returned unchanged.
     *
     * @param views    the candidate template views
     * @param userTags the tags to require; may be {@code null} or empty
     * @return views whose effective tag set is a superset of {@code userTags}
     */
    default List<TemplateView> applyTagFilter(List<TemplateView> views, List<String> userTags) {
        if (userTags == null || userTags.isEmpty()) {
            return views;
        }
        Set<String> required = new HashSet<>(userTags);
        return views.stream()
                .filter(v -> new HashSet<>(v.effectiveTags()).containsAll(required))
                .toList();
    }
}
