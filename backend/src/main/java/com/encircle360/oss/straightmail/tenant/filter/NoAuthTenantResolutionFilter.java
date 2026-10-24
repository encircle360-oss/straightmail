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
 * in no-authentication mode.
 *
 * <p>Active when {@code auth.mode=none}. Does not require the {@code database} profile;
 * when no database is active, tenant existence is validated against {@code tenants.config}
 * from {@link TenantProperties}. This allows unauthenticated multi-tenant deployments where
 * access control is managed at the network level.
 *
 * <p>Tenant list endpoints ({@code GET /v1/tenants} and {@code GET /v1/tenants/me}) are excluded
 * from tenant resolution to support the initial tenant selection flow.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "auth.mode", havingValue = "none")
public class NoAuthTenantResolutionFilter extends AbstractTenantResolutionFilter {

    public NoAuthTenantResolutionFilter(TenantContext tenantContext,
                                        ObjectProvider<TenantRepository> tenantRepositoryProvider,
                                        TenantProperties tenantProperties) {
        super(tenantContext, tenantRepositoryProvider, tenantProperties);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String tenantId = request.getHeader(TENANT_ID_HEADER);

        if (tenantId == null || tenantId.isBlank()) {
            // Fall back to the configured default tenant in no-auth mode.
            // Validation is skipped for the default — it is implicitly trusted.
            tenantContext.setTenantId(tenantProperties.getDefaultId());
            chain.doFilter(request, response);
            return;
        }

        if (!this.validateTenantId(tenantId, response)) {
            return;
        }

        tenantContext.setTenantId(tenantId);
        chain.doFilter(request, response);
    }
}
