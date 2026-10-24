package com.encircle360.oss.straightmail.dto.status;

/**
 * Outcome of the most recent Git-sync run for a single tenant.
 */
public enum SyncResult {
    /**
     * All configured branches were synchronised without errors.
     */
    SUCCESS,
    /**
     * At least one branch failed to synchronise.
     */
    FAILED,
    /**
     * No sync has been attempted yet since the application started.
     */
    NEVER
}
