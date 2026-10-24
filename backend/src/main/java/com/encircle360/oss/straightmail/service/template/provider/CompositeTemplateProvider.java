package com.encircle360.oss.straightmail.service.template.provider;

import com.encircle360.oss.straightmail.dto.template.TemplateView;
import com.encircle360.oss.straightmail.model.Template;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Aggregates multiple {@link TemplateSourceProvider} implementations into a single unified provider.
 *
 * <p>Delegates template listing and resolution to all enabled providers in their natural injection order.
 * When resolving a single template by name, the first matching result across all providers wins.
 */
@Service
@RequiredArgsConstructor
public class CompositeTemplateProvider {

    private final List<TemplateSourceProvider> providers;

    /**
     * Returns a page of templates visible to the given tenant across all enabled providers, optionally filtered by tags.
     *
     * <p>All provider results are aggregated in-memory first, then the requested page is sliced out.
     * This is necessary because results may come from heterogeneous sources (database, file, Git) that
     * cannot be jointly paginated at the query level.
     *
     * @param tenantId the tenant slug whose templates should be listed
     * @param tags     optional list of tags to filter by; {@code null} or empty means no tag filter
     * @param pageable pagination and sorting parameters
     * @return a {@link Page} of {@link TemplateView} entries
     */
    public Page<TemplateView> listAll(String tenantId, List<String> tags, Pageable pageable) {
        List<TemplateView> all = providers.stream()
                .filter(TemplateSourceProvider::isEnabled)
                .flatMap(p -> p.listTemplates(tenantId, tags).stream())
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<TemplateView> pageContent = start < all.size() ? all.subList(start, end) : List.of();

        return new PageImpl<>(pageContent, pageable, all.size());
    }

    /**
     * Resolves a single template by name for the given tenant, searching all enabled providers in order.
     *
     * <p>Returns the first match found. Provider priority is determined by their injection order in the
     * Spring application context.
     *
     * @param tenantId the tenant slug
     * @param name     the template name / identifier
     * @return an {@link Optional} containing the resolved {@link Template}, or empty if not found in any provider
     */
    public Optional<Template> resolve(String tenantId, String name) {
        return this.resolveView(tenantId, name).map(TemplateView::template);
    }

    /**
     * Resolves a single template by name across all enabled providers and returns the full
     * {@link TemplateView} including source and editability metadata.
     *
     * <p>Returns the first match found. Provider priority is determined by their injection order in the
     * Spring application context.
     *
     * @param tenantId the tenant slug
     * @param name     the template name / identifier
     * @return an {@link Optional} containing the resolved {@link TemplateView}, or empty if not found in any provider
     */
    public Optional<TemplateView> resolveView(String tenantId, String name) {
        return providers.stream()
                .filter(TemplateSourceProvider::isEnabled)
                .map(p -> p.findTemplate(tenantId, name))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }
}
