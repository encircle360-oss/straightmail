package com.encircle360.oss.straightmail.tenant;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

/**
 * Request-scoped bean that holds the current tenant identifier for the duration of a single HTTP request.
 *
 * <p>The tenant ID is populated by either {@link com.encircle360.oss.straightmail.tenant.filter.JwtTenantResolutionFilter} (OIDC mode) or
 * {@link com.encircle360.oss.straightmail.tenant.filter.ApiKeyAuthenticationFilter} /
 * {@link com.encircle360.oss.straightmail.tenant.filter.ApiKeyTenantResolutionFilter} (API-key mode) before
 * the request reaches any controller or service.
 *
 * <p>All downstream services that need to scope their operations to a tenant must read the tenant ID
 * exclusively from this context — never directly from the HTTP request.
 */
@Data
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class TenantContext {

    @Value("${tenants.default-id:default}")
    private String defaultTenantId = "default";

    private String tenantId;

    /**
     * Full list of tenant slugs the current user is permitted to access.
     *
     * <p>{@code null} means unrestricted access (global API key). Set by
     * {@link com.encircle360.oss.straightmail.tenant.filter.JwtTenantResolutionFilter} (OIDC mode)
     * or {@link com.encircle360.oss.straightmail.tenant.filter.ApiKeyAuthenticationFilter}
     * (per-tenant API key mode).
     */
    private List<String> accessibleTenantIds;

    /**
     * Returns {@code true} if the current tenant is the default (system) tenant.
     *
     * @return {@code true} if {@link #tenantId} equals the configured {@code tenants.default-id}
     */
    public boolean isDefault() {
        return defaultTenantId.equals(tenantId);
    }
}
