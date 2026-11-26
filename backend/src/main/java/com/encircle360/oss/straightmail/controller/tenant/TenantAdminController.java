package com.encircle360.oss.straightmail.controller.tenant;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.dto.tenant.CreateUpdateTenantDTO;
import com.encircle360.oss.straightmail.dto.tenant.TenantDTO;
import com.encircle360.oss.straightmail.mapper.TenantMapper;
import com.encircle360.oss.straightmail.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for tenant administration (CRUD write operations).
 *
 * <p>Only active when the {@code database} Spring profile is enabled. All endpoints require the
 * caller to hold the {@code ROLE_ADMIN} authority, enforced via {@link PreAuthorize}.
 * In OIDC mode this is granted by the Keycloak role; in API-key mode by the global API key;
 * in no-auth mode by the anonymous authority configured in {@link com.encircle360.oss.straightmail.config.SecurityConfig}.
 *
 * <p>Exposes {@code POST}, {@code PUT}, and {@code DELETE} on {@code /v1/tenants}.
 * Read operations (list, get) are served by {@link TenantReadController}.
 */
@RestController
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
@Profile(DatabaseConfig.PROFILE)
@Tag(name = "Tenant Admin", description = "Tenant management (ADMIN only)")
public class TenantAdminController {

    private final TenantService tenantService;
    private final TenantMapper tenantMapper;

    /**
     * Creates a new tenant.
     *
     * @param dto the tenant configuration to create
     * @return {@code 201 CREATED} with the created {@link TenantDTO}
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "createTenant", description = "Creates a new tenant (ADMIN only)")
    public ResponseEntity<TenantDTO> create(@RequestBody @Valid CreateUpdateTenantDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tenantMapper.toDto(tenantService.create(dto)));
    }

    /**
     * Updates an existing tenant identified by {@code slug}.
     *
     * @param slug the tenant's unique slug
     * @param dto  the updated tenant configuration
     * @return {@code 200 OK} with the updated {@link TenantDTO}
     */
    @PutMapping(value = "/{slug}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "updateTenant", description = "Updates an existing tenant (ADMIN only)")
    public ResponseEntity<TenantDTO> update(@PathVariable String slug,
                                            @RequestBody @Valid CreateUpdateTenantDTO dto) {
        return ResponseEntity.ok(tenantMapper.toDto(tenantService.update(slug, dto)));
    }

    /**
     * Deletes a tenant and all associated data.
     *
     * @param slug the tenant's unique slug
     * @return {@code 204 NO CONTENT} on success
     */
    @DeleteMapping(value = "/{slug}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(operationId = "deleteTenant", description = "Deletes a tenant and all its data (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable String slug) {
        tenantService.delete(slug);
        return ResponseEntity.noContent().build();
    }
}
