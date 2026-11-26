package com.encircle360.oss.straightmail.repository;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.model.Tenant;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Tenant} entities.
 *
 * <p>Only active when the {@code database} Spring profile is enabled (see {@link DatabaseConfig#PROFILE}).
 * The primary key is the tenant {@code slug} (a URL-safe string identifier).
 */
@Repository
@Profile(DatabaseConfig.PROFILE)
public interface TenantRepository extends JpaRepository<Tenant, String> {

    /**
     * Checks whether a tenant with the given slug exists.
     *
     * @param slug the tenant slug to check
     * @return {@code true} if a tenant with this slug is present in the database
     */
    boolean existsBySlug(String slug);

    /**
     * Finds a tenant by its slug.
     *
     * @param slug the tenant slug
     * @return an {@link Optional} containing the tenant, or empty if not found
     */
    Optional<Tenant> findBySlug(String slug);

    /**
     * Finds an active tenant whose API key hash matches the given hash.
     *
     * <p>Used by {@link com.encircle360.oss.straightmail.tenant.filter.ApiKeyAuthenticationFilter}
     * to authenticate per-tenant API keys.
     *
     * @param apiKeyHash the SHA-256 hex hash of the incoming API key
     * @return an {@link Optional} containing the matching active tenant, or empty if not found
     */
    Optional<Tenant> findByApiKeyHashAndActiveTrue(String apiKeyHash);

    /**
     * Returns all active tenants that have a Git repository URL configured.
     *
     * <p>Used by {@link com.encircle360.oss.straightmail.scheduler.GitSyncScheduler} to determine
     * which tenants should have their Git templates synchronised.
     *
     * @return list of active tenants with a non-null Git repository URL
     */
    List<Tenant> findAllByGitRepoUrlNotNullAndActiveTrue();
}
