package com.encircle360.oss.straightmail.dto.status;

import java.time.Instant;

/**
 * Status snapshot for a single tenant's Git-sync.
 *
 * @param tenantId     the tenant slug
 * @param lastSyncAt   timestamp of the most recent sync attempt, or {@code null} if never attempted
 * @param result       outcome of the most recent attempt
 * @param errorMessage human-readable error description when {@code result} is {@link SyncResult#FAILED},
 *                     {@code null} otherwise
 */
public record GitSyncStatusDTO(String tenantId, Instant lastSyncAt, SyncResult result, String errorMessage) {
}
