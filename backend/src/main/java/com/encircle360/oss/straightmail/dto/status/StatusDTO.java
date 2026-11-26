package com.encircle360.oss.straightmail.dto.status;

import java.util.List;

/**
 * Aggregated status of all template sync sources.
 *
 * @param gitSync       per-tenant Git-sync status entries; empty when Git-sync is disabled
 * @param fileTemplates file-template directory accessibility status;
 *                      {@code null} when the file-template provider is disabled
 */
public record StatusDTO(List<GitSyncStatusDTO> gitSync, FileStatusDTO fileTemplates) {
}
