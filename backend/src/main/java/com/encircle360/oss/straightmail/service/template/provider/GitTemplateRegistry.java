package com.encircle360.oss.straightmail.service.template.provider;

import com.encircle360.oss.straightmail.model.Template;

import java.util.Map;
import java.util.Optional;

/**
 * Registry that stores the most recently synchronised Git templates per tenant, organised by branch.
 *
 * <p>Two implementations exist:
 * <ul>
 *   <li>{@link InMemoryGitTemplateRegistry} — active when the {@code database} profile is <em>not</em> set;
 *       holds templates in a {@code ConcurrentHashMap} (resets on restart, local to each pod).</li>
 *   <li>{@link DatabaseGitTemplateRegistry} — active when the {@code database} profile is set;
 *       persists templates in the {@code git_template_cache} table so all pods in a cluster share
 *       a consistent view after the sync pod completes its work.</li>
 * </ul>
 */
public interface GitTemplateRegistry {

    /**
     * Result type for {@link #findFirst}, carrying the matched template together with its source branch.
     */
    record GitEntry(String branch, Template template) {
    }

    /**
     * Stores or replaces the templates for the given tenant and branch.
     *
     * @param tenantSlug  the tenant slug whose branch should be updated
     * @param branch      the branch name
     * @param branchOrder the zero-based index of this branch in the tenant's configured branch list,
     *                    used to preserve priority ordering across storage implementations
     * @param templates   the new template map (name → template) for that branch
     */
    void updateBranch(String tenantSlug, String branch, int branchOrder, Map<String, Template> templates);

    /**
     * Returns all branch-to-template maps currently held for the given tenant.
     *
     * @param tenantSlug the tenant slug
     * @return a map of branch name → (template name → template); empty map if no templates are registered
     */
    Map<String, Map<String, Template>> getBranchTemplates(String tenantSlug);

    /**
     * Returns the first template matching the given name, searching branches in priority order
     * (lowest {@code branchOrder} first, i.e. the configured branch priority from {@code Tenant.gitBranches}).
     *
     * @param tenantSlug the tenant slug
     * @param name       the template name
     * @return an {@link Optional} containing the matching {@link GitEntry}, or empty if not found in any branch
     */
    Optional<GitEntry> findFirst(String tenantSlug, String name);
}
