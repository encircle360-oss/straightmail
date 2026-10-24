package com.encircle360.oss.straightmail.repository;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.model.Template;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Template} entities.
 *
 * <p>Only active when the {@code database} Spring profile is enabled (see {@link DatabaseConfig#PROFILE}).
 * All queries are tenant-scoped; callers must always pass the correct tenant ID to prevent
 * cross-tenant data access.
 */
@Repository
@Profile(DatabaseConfig.PROFILE)
public interface TemplateRepository extends JpaRepository<Template, UUID> {

    /**
     * Returns a paginated list of all templates belonging to the given tenant.
     *
     * @param tenantId the tenant slug
     * @param pageable pagination and sorting parameters
     * @return a page of matching {@link Template} entities
     */
    Page<Template> findAllByTenantId(String tenantId, Pageable pageable);

    /**
     * Finds a template by tenant and name.
     *
     * @param tenantId the tenant slug
     * @param name     the template name
     * @return an {@link Optional} containing the matching template, or empty if not found
     */
    Optional<Template> findByTenantIdAndName(String tenantId, String name);

    /**
     * Finds a template by UUID and tenant, ensuring cross-tenant isolation.
     *
     * @param id       the template UUID
     * @param tenantId the tenant slug
     * @return an {@link Optional} containing the matching template, or empty if not found
     */
    Optional<Template> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Finds all templates for a tenant where ALL specified tags are present.
     */
    @Query(
            value = """
                    SELECT t FROM Template t
                    WHERE t.tenantId = :tenantId
                    AND (
                        SELECT COUNT(tag) FROM Template t2
                        JOIN t2.tags tag
                        WHERE t2.id = t.id
                          AND tag IN :tags
                    ) = :#{#tags.size()}
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM Template t
                    WHERE t.tenantId = :tenantId
                    AND (
                        SELECT COUNT(tag) FROM Template t2
                        JOIN t2.tags tag
                        WHERE t2.id = t.id
                          AND tag IN :tags
                    ) = :#{#tags.size()}
                    """
    )
    Page<Template> findAllByTenantIdAndTagsContains(String tenantId, List<String> tags, Pageable pageable);
}
