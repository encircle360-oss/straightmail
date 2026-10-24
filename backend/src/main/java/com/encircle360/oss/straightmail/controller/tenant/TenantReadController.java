package com.encircle360.oss.straightmail.controller.tenant;

import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.dto.tenant.TenantDTO;
import com.encircle360.oss.straightmail.mapper.TenantMapper;
import com.encircle360.oss.straightmail.service.TenantService;
import com.encircle360.oss.straightmail.tenant.JwtTenantClaimsExtractor;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * REST controller for reading tenant data.
 *
 * <p>Works in both database and non-database mode:
 * <ul>
 *   <li><b>Database mode:</b> reads tenant records from the database via {@link TenantService}.</li>
 *   <li><b>Non-database mode:</b> reads statically configured tenants from {@link TenantProperties}.</li>
 * </ul>
 *
 * <p>The {@code /me} endpoint is caller-aware: in OIDC mode it filters tenants by JWT claims;
 * in API-key mode it returns the tenant associated with the key, or all tenants for a global key.
 */
@RestController
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Tenant information")
public class TenantReadController {

    private final ObjectProvider<TenantService> tenantService;
    private final TenantMapper tenantMapper;
    private final TenantContext tenantContext;
    private final TenantProperties tenantProperties;
    private final JwtTenantClaimsExtractor claimsExtractor;

    /**
     * Returns all tenants in the system.
     *
     * <p>Administration-only: a {@link TenantDTO} carries another tenant's mail relay host, SMTP
     * user and git repository URL, so this must not be readable by a tenant-scoped caller. Callers
     * that only need their own tenants use {@link #me()}, which filters by identity.
     *
     * @return {@code 200 OK} with a list of all {@link TenantDTO} objects
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "listTenants", description = "Returns all tenants (admin only)")
    public ResponseEntity<List<TenantDTO>> list() {
        TenantService service = tenantService.getIfAvailable();
        if (service != null) {
            return ResponseEntity.ok(service.findAll().stream().map(tenantMapper::toDto).toList());
        }
        return ResponseEntity.ok(tenantProperties.getConfig().stream().map(tenantMapper::configToDto).toList());
    }

    /**
     * Returns the tenants accessible to the current caller.
     *
     * <ul>
     *   <li><b>OIDC mode:</b> filters tenants by the tenant IDs extracted from the JWT claims
     *       ({@code tenant_ids} or {@code tenant_id}).</li>
     *   <li><b>API-key mode (per-tenant key):</b> returns the single tenant associated with the key.</li>
     *   <li><b>API-key mode (global key):</b> returns all tenants.</li>
     * </ul>
     *
     * @return {@code 200 OK} with the list of accessible {@link TenantDTO} objects
     */
    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "getMyTenants", description = "Returns tenants accessible to the current caller")
    public ResponseEntity<List<TenantDTO>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        TenantService service = tenantService.getIfAvailable();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            List<String> allowed = claimsExtractor.extract(jwtAuth);
            if (allowed.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }
            Set<String> allowedSet = Set.copyOf(allowed);
            if (service != null) {
                return ResponseEntity.ok(service.findAll().stream()
                        .filter(t -> allowedSet.contains(t.getSlug()))
                        .map(tenantMapper::toDto)
                        .toList());
            }
            return ResponseEntity.ok(tenantProperties.getConfig().stream()
                    .filter(c -> allowedSet.contains(c.getId()))
                    .map(tenantMapper::configToDto)
                    .toList());
        }

        // API-key mode: per-tenant key sets tenantContext; global key leaves it null
        String currentTenantId = tenantContext.getTenantId();
        if (currentTenantId != null) {
            if (service != null) {
                return ResponseEntity.ok(List.of(tenantMapper.toDto(service.get(currentTenantId))));
            }
            return ResponseEntity.ok(tenantProperties.getConfig().stream()
                    .filter(c -> currentTenantId.equals(c.getId()))
                    .map(tenantMapper::configToDto)
                    .toList());
        }

        // Global API key: return all tenants
        if (service != null) {
            return ResponseEntity.ok(service.findAll().stream().map(tenantMapper::toDto).toList());
        }
        return ResponseEntity.ok(tenantProperties.getConfig().stream().map(tenantMapper::configToDto).toList());
    }

    /**
     * Returns a single tenant by its slug identifier.
     *
     * <p>Administration-only for the same reason as {@link #list()}.
     *
     * @param slug the tenant's unique slug
     * @return {@code 200 OK} with the matching {@link TenantDTO}
     * @throws ResponseStatusException {@code 404 NOT FOUND} if no tenant exists with this slug
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "getTenant", description = "Returns a tenant by slug (admin only)")
    public ResponseEntity<TenantDTO> get(@PathVariable String slug) {
        TenantService service = tenantService.getIfAvailable();
        if (service != null) {
            return ResponseEntity.ok(tenantMapper.toDto(service.get(slug)));
        }
        return tenantProperties.getConfig().stream()
                .filter(c -> slug.equals(c.getId()))
                .map(tenantMapper::configToDto)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Tenant not found: " + slug));
    }
}
