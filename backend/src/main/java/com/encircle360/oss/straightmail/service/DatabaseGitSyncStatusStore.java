package com.encircle360.oss.straightmail.service;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.dto.status.GitSyncStatusDTO;
import com.encircle360.oss.straightmail.dto.status.SyncResult;
import com.encircle360.oss.straightmail.model.GitSyncStatus;
import com.encircle360.oss.straightmail.repository.GitSyncStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Database-backed {@link GitSyncStatusStore} implementation active when the {@code database} profile is set.
 *
 * <p>Persists the last-known sync status per tenant in the {@code git_sync_status} table using
 * upsert semantics. Status survives pod restarts and is visible across all pods in a cluster.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "templates.git-sync.enabled", havingValue = "true")
@Profile(DatabaseConfig.PROFILE)
public class DatabaseGitSyncStatusStore implements GitSyncStatusStore {

    private final GitSyncStatusRepository repository;

    @Override
    @Transactional
    public void record(String tenantId, Instant syncAt, SyncResult result, String errorMessage) {
        String trimmedError = errorMessage != null && errorMessage.length() > 1024
                ? errorMessage.substring(0, 1024)
                : errorMessage;
        GitSyncStatus entity = repository.findById(tenantId)
                .orElse(GitSyncStatus.builder().tenantId(tenantId).build());
        entity.setLastSyncAt(syncAt);
        entity.setResult(result);
        entity.setErrorMessage(trimmedError);
        repository.save(entity);
    }

    @Override
    public List<GitSyncStatusDTO> getAll() {
        return repository.findAllByOrderByTenantIdAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public Optional<GitSyncStatusDTO> get(String tenantId) {
        return repository.findById(tenantId).map(this::toDto);
    }

    private GitSyncStatusDTO toDto(GitSyncStatus entity) {
        return new GitSyncStatusDTO(entity.getTenantId(), entity.getLastSyncAt(),
                entity.getResult(), entity.getErrorMessage());
    }
}
