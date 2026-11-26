package com.encircle360.oss.straightmail.service.template.provider;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.model.Template;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link GitTemplateRegistry} implementation for deployments without the {@code database} profile.
 *
 * <p>Templates are populated by {@link com.encircle360.oss.straightmail.service.GitSyncService}
 * after each successful clone and stored in a thread-safe {@link ConcurrentHashMap}.
 * State resets on application restart and is local to each pod.
 *
 * <p>Branch insertion order is preserved via {@link LinkedHashMap}, which reflects the configured
 * branch priority in {@code Tenant.gitBranches}. {@link #findFirst} exploits this order to return
 * the highest-priority branch match when a template exists in multiple branches.
 */
@Component
@ConditionalOnProperty(name = "templates.git-sync.enabled", havingValue = "true")
@Profile("!" + DatabaseConfig.PROFILE)
public class InMemoryGitTemplateRegistry implements GitTemplateRegistry {

    // tenantSlug → (branchName → (templateName → Template))
    // Inner map is an unmodifiable LinkedHashMap; order = sync order = configured branch priority.
    private final ConcurrentHashMap<String, Map<String, Map<String, Template>>> registry = new ConcurrentHashMap<>();

    @Override
    public void updateBranch(String tenantSlug, String branch, int branchOrder, Map<String, Template> templates) {
        registry.compute(tenantSlug, (key, current) -> {
            LinkedHashMap<String, Map<String, Template>> updated =
                    current != null ? new LinkedHashMap<>(current) : new LinkedHashMap<>();
            updated.put(branch, Map.copyOf(templates));
            return Collections.unmodifiableMap(updated);
        });
    }

    @Override
    public Map<String, Map<String, Template>> getBranchTemplates(String tenantSlug) {
        return registry.getOrDefault(tenantSlug, Collections.emptyMap());
    }

    @Override
    public Optional<GitEntry> findFirst(String tenantSlug, String name) {
        return getBranchTemplates(tenantSlug).entrySet().stream()
                .filter(e -> e.getValue().containsKey(name))
                .findFirst()
                .map(e -> new GitEntry(e.getKey(), e.getValue().get(name)));
    }
}
