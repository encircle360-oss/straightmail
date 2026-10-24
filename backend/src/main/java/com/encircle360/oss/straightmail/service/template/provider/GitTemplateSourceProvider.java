package com.encircle360.oss.straightmail.service.template.provider;

import com.encircle360.oss.straightmail.dto.template.TemplateView;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * {@link TemplateSourceProvider} that serves templates loaded from Git repositories.
 *
 * <p>Only active when {@code templates.git-sync.enabled=true}. Delegates to the in-memory
 * {@link GitTemplateRegistry} which is populated by {@link com.encircle360.oss.straightmail.service.GitSyncService}.
 *
 * <p>Listing returns one {@link TemplateView} per branch-template combination, so a template that
 * exists in multiple configured branches appears as multiple entries (one per branch). This lets
 * the UI display the branch name alongside each template row.
 *
 * <p>Single-template resolution ({@link #findTemplate}) uses insertion order of the branch map —
 * which mirrors the configured {@code Tenant.gitBranches} order — and returns the first match,
 * giving earlier-listed branches higher priority for email sending.
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "templates.git-sync.enabled", havingValue = "true")
public class GitTemplateSourceProvider implements TemplateSourceProvider {

    private final GitTemplateRegistry registry;

    @Override
    public TemplateSource getSource() {
        return TemplateSource.GIT;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public List<TemplateView> listTemplates(String tenantId, List<String> tags) {
        List<TemplateView> all = registry.getBranchTemplates(tenantId).entrySet().stream()
                .flatMap(e -> e.getValue().values().stream()
                        .map(t -> TemplateView.ofGit(t, e.getKey())))
                .toList();
        // If the caller requests a different source explicitly, no git templates can match.
        if (hasConflictingSource(tags)) {
            return List.of();
        }
        // "source:git" is a virtual tag — strip it before applying user-defined tag filters.
        return applyTagFilter(all, stripSourceTag(tags));
    }

    @Override
    public Optional<TemplateView> findTemplate(String tenantId, String name) {
        return registry.findFirst(tenantId, name)
                .map(e -> TemplateView.ofGit(e.template(), e.branch()));
    }
}
