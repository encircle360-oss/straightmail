package com.encircle360.oss.straightmail.service.template.provider;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.dto.template.TemplateView;
import com.encircle360.oss.straightmail.service.template.TemplateService;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link TemplateSourceProvider} that serves templates stored in the relational database.
 *
 * <p>Only active when the {@code database} Spring profile is enabled (see {@link DatabaseConfig#PROFILE}).
 * Always reports itself as enabled. Resolves templates by UUID or by name for the current tenant.
 * Delegates all data access to {@link TemplateService}; tenant scoping is handled there via
 * {@link TenantContext}.
 */
@Service
@RequiredArgsConstructor
@Profile(DatabaseConfig.PROFILE)
public class DatabaseTemplateSourceProvider implements TemplateSourceProvider {

    private final TemplateService templateService;

    @Override
    public TemplateSource getSource() {
        return TemplateSource.DATABASE;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public List<TemplateView> listTemplates(String tenantId, List<String> tags) {
        // If the caller requests a different source explicitly, no database templates can match.
        if (hasConflictingSource(tags)) {
            return List.of();
        }
        // "source:database" is a virtual tag — strip it before querying the DB so only real
        // stored tags are passed to the repository filter.
        List<String> storedTags = stripSourceTag(tags);
        return templateService.findAll(storedTags == null || storedTags.isEmpty() ? null : storedTags)
                .stream()
                .map(TemplateView::ofDatabase)
                .toList();
    }

    @Override
    public Optional<TemplateView> findTemplate(String tenantId, String name) {
        try {
            UUID id = UUID.fromString(name);
            return templateService.findById(id)
                    .map(TemplateView::ofDatabase);
        } catch (IllegalArgumentException e) {
            return templateService.findByName(name)
                    .map(TemplateView::ofDatabase);
        }
    }
}
