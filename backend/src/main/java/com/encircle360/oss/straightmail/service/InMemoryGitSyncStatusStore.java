package com.encircle360.oss.straightmail.service;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.dto.status.GitSyncStatusDTO;
import com.encircle360.oss.straightmail.dto.status.SyncResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link GitSyncStatusStore} implementation for deployments without the {@code database} profile.
 *
 * <p>Status is stored in a {@link ConcurrentHashMap} and resets on application restart.
 * Suitable for single-pod, file-based deployments where persistence across restarts is not required.
 */
@Component
@ConditionalOnProperty(name = "templates.git-sync.enabled", havingValue = "true")
@Profile("!" + DatabaseConfig.PROFILE)
public class InMemoryGitSyncStatusStore implements GitSyncStatusStore {

    private final ConcurrentHashMap<String, GitSyncStatusDTO> statusMap = new ConcurrentHashMap<>();

    @Override
    public void record(String tenantId, Instant syncAt, SyncResult result, String errorMessage) {
        statusMap.put(tenantId, new GitSyncStatusDTO(tenantId, syncAt, result, errorMessage));
    }

    @Override
    public List<GitSyncStatusDTO> getAll() {
        return statusMap.values().stream()
                .sorted(Comparator.comparing(GitSyncStatusDTO::tenantId))
                .toList();
    }

    @Override
    public Optional<GitSyncStatusDTO> get(String tenantId) {
        return Optional.ofNullable(statusMap.get(tenantId));
    }
}
