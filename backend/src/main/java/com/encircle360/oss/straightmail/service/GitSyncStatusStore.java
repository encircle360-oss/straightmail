package com.encircle360.oss.straightmail.service;

import com.encircle360.oss.straightmail.dto.status.GitSyncStatusDTO;
import com.encircle360.oss.straightmail.dto.status.SyncResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Storage abstraction for per-tenant Git-sync status.
 *
 * <p>Two implementations exist:
 * <ul>
 *   <li>{@link InMemoryGitSyncStatusStore} — active when the {@code database} profile is <em>not</em> set;
 *       stores status in a {@code ConcurrentHashMap} (resets on restart).</li>
 *   <li>{@link DatabaseGitSyncStatusStore} — active when the {@code database} profile is set;
 *       persists the last-known status per tenant in the {@code git_sync_status} table
 *       (survives restarts, visible across all pods).</li>
 * </ul>
 */
public interface GitSyncStatusStore {

    /**
     * Records the outcome of a sync attempt for the given tenant, replacing any previously stored status.
     *
     * @param tenantId     the tenant slug
     * @param syncAt       the timestamp of this sync attempt
     * @param result       the outcome of the attempt
     * @param errorMessage human-readable error description when {@code result} is {@link SyncResult#FAILED},
     *                     {@code null} otherwise
     */
    void record(String tenantId, Instant syncAt, SyncResult result, String errorMessage);

    /**
     * Returns the last-known sync status for all tenants that have been synced at least once.
     *
     * @return unmodifiable list of per-tenant sync status entries, ordered by tenant ID
     */
    List<GitSyncStatusDTO> getAll();

    /**
     * Returns the last-known sync status for a specific tenant.
     *
     * @param tenantId the tenant slug
     * @return an {@link Optional} containing the status, or empty if the tenant has never been synced
     */
    Optional<GitSyncStatusDTO> get(String tenantId);
}
