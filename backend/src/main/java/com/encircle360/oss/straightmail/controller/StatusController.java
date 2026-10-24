package com.encircle360.oss.straightmail.controller;

import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.dto.status.FileStatusDTO;
import com.encircle360.oss.straightmail.dto.status.GitSyncStatusDTO;
import com.encircle360.oss.straightmail.dto.status.StatusDTO;
import com.encircle360.oss.straightmail.model.Tenant;
import com.encircle360.oss.straightmail.repository.TenantRepository;
import com.encircle360.oss.straightmail.service.GitSyncService;
import com.encircle360.oss.straightmail.service.template.provider.FileTemplateSourceProvider;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * REST controller that exposes the current status of template sync sources.
 *
 * <p>Works in both database and non-database mode:
 * <ul>
 *   <li><b>Database mode:</b> tenant lookup for {@code POST /v1/sync/git/{tenantSlug}} uses the
 *       database via {@link TenantRepository}.</li>
 *   <li><b>Non-database mode:</b> tenant lookup falls back to statically configured tenants in
 *       {@link TenantProperties}.</li>
 * </ul>
 *
 * <p>Provides:
 * <ul>
 *   <li>{@code GET /v1/status} — aggregated status of Git-sync (per tenant) and file-template
 *       directory accessibility</li>
 *   <li>{@code POST /v1/sync/git/{tenantSlug}} — triggers an immediate Git-sync for the specified
 *       tenant and returns the updated status</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Status", description = "Template sync source status")
public class StatusController {

    private final ObjectProvider<TenantRepository> tenantRepositoryProvider;
    private final ObjectProvider<GitSyncService> gitSyncServiceProvider;
    private final ObjectProvider<FileTemplateSourceProvider> fileProviderProvider;
    private final TenantContext tenantContext;
    private final TenantProperties tenantProperties;

    /**
     * Returns the aggregated sync status for all template sources.
     *
     * <p>The {@code gitSync} list contains one entry per tenant that has been synced at least once
     * since the application started. The {@code fileTemplates} field reflects whether the configured
     * file-template base directory is currently accessible; it is {@code null} when the file-template
     * provider is disabled.
     *
     * @return {@code 200 OK} with a {@link StatusDTO} containing per-tenant Git-sync status and
     * file-template accessibility
     */
    @GetMapping(value = "/v1/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "getStatus", description = "Returns aggregated sync status for all template sources visible to the current user")
    public ResponseEntity<StatusDTO> getStatus() {
        List<GitSyncStatusDTO> gitSync = gitSyncServiceProvider
                .stream()
                .findFirst()
                .map(GitSyncService::getStatuses)
                .orElse(List.of());

        // Filter to only the tenants the current user is permitted to see.
        // accessibleTenantIds is null for global API-key holders (unrestricted).
        List<String> accessible = tenantContext.getAccessibleTenantIds();
        if (accessible != null) {
            gitSync = gitSync.stream()
                    .filter(s -> accessible.contains(s.tenantId()))
                    .toList();
        }

        FileStatusDTO fileStatus = fileProviderProvider
                .stream()
                .findFirst()
                .map(p -> new FileStatusDTO(p.isBaseDirAccessible()))
                .orElse(null);

        return ResponseEntity.ok(new StatusDTO(gitSync, fileStatus));
    }

    /**
     * Triggers an immediate Git-sync for the given tenant and returns the updated sync status.
     *
     * <p>Returns {@code 404} if the tenant does not exist, {@code 400} if the tenant has no Git
     * repository configured, or {@code 503} if Git-sync is disabled globally.
     *
     * @param tenantSlug the URL-safe identifier of the tenant to sync
     * @return {@code 200 OK} with the updated {@link GitSyncStatusDTO} for the tenant
     */
    @PostMapping(value = "/v1/sync/git/{tenantSlug}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "triggerGitSync", description = "Triggers an immediate Git-sync for a tenant. Requires ADMIN role in OIDC mode.")
    public ResponseEntity<GitSyncStatusDTO> triggerGitSync(@PathVariable String tenantSlug) {
        // Ensure the requesting user has access to this specific tenant.
        // accessibleTenantIds is null for global API-key holders (unrestricted).
        List<String> accessible = tenantContext.getAccessibleTenantIds();
        if (accessible != null && !accessible.contains(tenantSlug)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // A sync is an infrastructure operation, so it needs ADMIN in every auth mode. In api-key
        // mode that is the global key, in none mode every anonymous request already carries the role.
        // (This used to be gated on a property "auth.enabled" that exists nowhere, so it was always
        // on — the gate is dropped rather than kept as a no-op.)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        GitSyncService gitSyncService = gitSyncServiceProvider.getIfAvailable();
        if (gitSyncService == null) {
            return ResponseEntity.status(503).build();
        }

        TenantRepository repo = tenantRepositoryProvider.getIfAvailable();
        if (repo != null) {
            Optional<Tenant> tenantOpt = repo.findBySlug(tenantSlug);
            if (tenantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            Tenant tenant = tenantOpt.get();
            if (tenant.getGitRepoUrl() == null || tenant.getGitRepoUrl().isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            gitSyncService.syncTenant(tenant);
        } else {
            TenantProperties.TenantConfig cfg = tenantProperties.getConfig().stream()
                    .filter(c -> tenantSlug.equals(c.getId()))
                    .findFirst()
                    .orElse(null);
            if (cfg == null) {
                return ResponseEntity.notFound().build();
            }
            if (cfg.getGitRepoUrl() == null || cfg.getGitRepoUrl().isBlank()) {
                return ResponseEntity.badRequest().build();
            }
            gitSyncService.syncTenant(cfg.getId(), cfg.getGitRepoUrl(), cfg.getGitBranches(), cfg.getGitToken());
        }

        return gitSyncService.getStatus(tenantSlug)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
