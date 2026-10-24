package com.encircle360.oss.straightmail.service.template;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.repository.TemplateRepository;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing {@link Template} entities in the database.
 *
 * <p>Only active when the {@code database} Spring profile is enabled (see {@link DatabaseConfig#PROFILE}).
 * All read and write operations are automatically scoped to the current tenant, determined
 * by reading {@link TenantContext#getTenantId()} at runtime.
 */
@Service
@RequiredArgsConstructor
@Profile(DatabaseConfig.PROFILE)
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TenantContext tenantContext;

    /**
     * Returns all templates for the current tenant, optionally filtered by tags.
     *
     * @param tags optional list of tags to filter by; all provided tags must be present on the template;
     *             {@code null} or empty means no filter
     * @return list of matching {@link Template} entities
     */
    public List<Template> findAll(List<String> tags) {
        String tenantId = tenantContext.getTenantId();
        if (tags != null && !tags.isEmpty()) {
            return templateRepository
                    .findAllByTenantIdAndTagsContains(tenantId, tags, Pageable.unpaged())
                    .getContent();
        }
        return templateRepository.findAllByTenantId(tenantId, Pageable.unpaged()).getContent();
    }

    /**
     * Persists the given template, automatically associating it with the current tenant.
     *
     * @param template the template to save
     * @return the saved {@link Template} entity with generated ID
     */
    public Template save(Template template) {
        template.setTenantId(tenantContext.getTenantId());
        return templateRepository.save(template);
    }

    /**
     * Retrieves a template by its UUID, scoped to the current tenant.
     *
     * @param id the template UUID as a string
     * @return the matching {@link Template}, or {@code null} if not found or if {@code id} is not a valid UUID
     */
    public Template get(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            return templateRepository.findByIdAndTenantId(uuid, tenantContext.getTenantId()).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Looks up a single template by UUID, scoped to the current tenant.
     *
     * @param id the template UUID
     * @return an {@link Optional} containing the matching template, or empty if not found
     */
    public Optional<Template> findById(UUID id) {
        return templateRepository.findByIdAndTenantId(id, tenantContext.getTenantId());
    }

    /**
     * Looks up a single template by name, scoped to the current tenant.
     *
     * @param name the template name
     * @return an {@link Optional} containing the matching template, or empty if not found
     */
    public Optional<Template> findByName(String name) {
        return templateRepository.findByTenantIdAndName(tenantContext.getTenantId(), name);
    }

    /**
     * Deletes the given template, enforcing that it belongs to the current tenant.
     *
     * @param template the template to delete
     * @throws org.springframework.web.server.ResponseStatusException with {@code 403 FORBIDDEN} if the template belongs to a different tenant
     */
    public void delete(Template template) {
        if (!template.getTenantId().equals(tenantContext.getTenantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-tenant access denied");
        }
        templateRepository.delete(template);
    }
}
