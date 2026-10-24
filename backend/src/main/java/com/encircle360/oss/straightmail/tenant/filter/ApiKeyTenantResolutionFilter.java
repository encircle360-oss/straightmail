package com.encircle360.oss.straightmail.tenant.filter;

import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.repository.TenantRepository;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Servlet filter that resolves the current tenant from the {@code X-Tenant-ID} request header
 * in API-key authentication mode.
 *
 * <p>Active when {@code auth.mode=api-key}. Does not require the {@code database} profile;
 * when no database is active, tenant existence is validated against {@code tenants.config}
 * from {@link TenantProperties}.
 *
 * <p>Runs after {@link ApiKeyAuthenticationFilter}. If the tenant was already resolved by a per-tenant
 * API key in {@link ApiKeyAuthenticationFilter}, this filter delegates immediately without further
 * processing.
 *
 * <p>Validates that the tenant ID matches the allowed slug format and exists before
 * populating {@link TenantContext}.
 *
 * <p>Tenant list endpoints ({@code GET /v1/tenants} and {@code GET /v1/tenants/me}) are excluded
 * from tenant resolution to support the initial login flow (tenant selection before a key is chosen).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "auth.mode", havingValue = "api-key")
public class ApiKeyTenantResolutionFilter extends AbstractTenantResolutionFilter {

    public ApiKeyTenantResolutionFilter(TenantContext tenantContext,
                                        ObjectProvider<TenantRepository> tenantRepositoryProvider,
                                        TenantProperties tenantProperties) {
        super(tenantContext, tenantRepositoryProvider, tenantProperties);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        // Per-tenant API key already resolved the tenant in ApiKeyAuthenticationFilter
        if (tenantContext.getTenantId() != null) {
            chain.doFilter(request, response);
            return;
        }

        String tenantId = request.getHeader(TENANT_ID_HEADER);

        if (tenantId == null || tenantId.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing X-Tenant-ID header");
            return;
        }

        if (!this.validateTenantId(tenantId, response)) {
            return;
        }

        tenantContext.setTenantId(tenantId);
        chain.doFilter(request, response);
    }
}
