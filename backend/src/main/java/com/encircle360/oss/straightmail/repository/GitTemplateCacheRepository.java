package com.encircle360.oss.straightmail.repository;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.model.GitTemplateCache;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link GitTemplateCache} entities.
 *
 * <p>Only active when the {@code database} Spring profile is enabled.
 */
@Repository
@Profile(DatabaseConfig.PROFILE)
public interface GitTemplateCacheRepository extends JpaRepository<GitTemplateCache, String> {

    List<GitTemplateCache> findAllByTenantIdOrderByBranchOrderAscTemplateNameAsc(String tenantId);

    Optional<GitTemplateCache> findByTenantIdAndBranchAndTemplateName(
            String tenantId, String branch, String templateName);

    void deleteAllByTenantId(String tenantId);

    void deleteAllByTenantIdAndBranch(String tenantId, String branch);
}
