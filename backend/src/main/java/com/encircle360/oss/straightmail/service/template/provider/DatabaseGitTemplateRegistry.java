package com.encircle360.oss.straightmail.service.template.provider;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.model.GitTemplateCache;
import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.repository.GitTemplateCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Database-backed {@link GitTemplateRegistry} implementation active when the {@code database} profile is set.
 *
 * <p>Persists synced templates in the {@code git_template_cache} table so that all pods in a cluster
 * share a consistent template view after the sync pod (protected by ShedLock) completes its work.
 *
 * <p>{@link #updateBranch} atomically replaces all rows for a given {@code (tenantSlug, branch)} pair
 * within a single transaction: existing rows are deleted first, then the new set is inserted.
 * Branch priority is preserved via the {@code branchOrder} column, which mirrors the index of the
 * branch in the tenant's configured {@code gitBranches} list.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "templates.git-sync.enabled", havingValue = "true")
@Profile(DatabaseConfig.PROFILE)
public class DatabaseGitTemplateRegistry implements GitTemplateRegistry {

    private final GitTemplateCacheRepository cacheRepository;

    @Override
    @Transactional
    public void updateBranch(String tenantSlug, String branch, int branchOrder, Map<String, Template> templates) {
        cacheRepository.deleteAllByTenantIdAndBranch(tenantSlug, branch);
        // Flush so that DELETE statements are sent to the DB before the INSERTs below.
        // Hibernate's action queue processes inserts before deletes by default, which would
        // violate the unique constraint on (tenant_id, branch, template_name).
        cacheRepository.flush();
        templates.forEach((name, t) -> {
            GitTemplateCache entity = GitTemplateCache.builder()
                    .id(UUID.randomUUID().toString())
                    .tenantId(tenantSlug)
                    .branch(branch)
                    .branchOrder(branchOrder)
                    .templateName(name)
                    .html(t.getHtml())
                    .subject(t.getSubject())
                    .plain(t.getPlain())
                    .build();
            cacheRepository.save(entity);
        });
    }

    @Override
    public Map<String, Map<String, Template>> getBranchTemplates(String tenantSlug) {
        List<GitTemplateCache> rows = cacheRepository
                .findAllByTenantIdOrderByBranchOrderAscTemplateNameAsc(tenantSlug);
        // Preserve branch insertion order (driven by branchOrder) using LinkedHashMap.
        Map<String, Map<String, Template>> result = new LinkedHashMap<>();
        for (GitTemplateCache row : rows) {
            result.computeIfAbsent(row.getBranch(), k -> new LinkedHashMap<>())
                    .put(row.getTemplateName(), toTemplate(row, tenantSlug));
        }
        return result;
    }

    @Override
    public Optional<GitEntry> findFirst(String tenantSlug, String name) {
        return getBranchTemplates(tenantSlug).entrySet().stream()
                .filter(e -> e.getValue().containsKey(name))
                .findFirst()
                .map(e -> new GitEntry(e.getKey(), e.getValue().get(name)));
    }

    private Template toTemplate(GitTemplateCache row, String tenantSlug) {
        return Template.builder()
                .name(row.getTemplateName())
                .tenantId(tenantSlug)
                .html(row.getHtml())
                .subject(row.getSubject())
                .plain(row.getPlain())
                .build();
    }
}
