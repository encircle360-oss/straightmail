package com.encircle360.oss.straightmail.tenant.filter;

import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.repository.TenantRepository;
import com.encircle360.oss.straightmail.tenant.JwtTenantClaimsExtractor;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import com.encircle360.oss.straightmail.util.LogSanitizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Servlet filter that resolves the current tenant from the authenticated JWT in OIDC mode.
 *
 * <p>Active when {@code auth.mode=oidc}. Does not require the {@code database} profile;
 * when no database is active, tenant existence is validated against {@code tenants.config}
 * from {@link TenantProperties}.
 *
 * <p>Runs after Spring Security's {@link org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter}.
 *
 * <p>The filter extracts the list of allowed tenant IDs from the JWT using the configurable claims
 * {@code tenant_ids} (array) or {@code tenant_id} (single value). The tenant to use for the request
 * is then determined as follows:
 * <ol>
 *   <li>If the {@code X-Tenant-ID} header is present, it is validated against the allowed list.</li>
 *   <li>If the header is absent and the user has access to exactly one tenant, that tenant is
 *       selected automatically (backwards compatibility).</li>
 *   <li>If the header is absent and the user has access to multiple tenants, a {@code 400} error
 *       is returned requiring an explicit selection.</li>
 * </ol>
 *
 * <p>The resolved tenant ID is validated against the slug format and — if a database is available —
 * against the {@link org.springframework.data.jpa.repository.JpaRepository}; otherwise against
 * {@link TenantProperties#getConfig()}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "auth.mode", havingValue = "oidc", matchIfMissing = true)
public class JwtTenantResolutionFilter extends AbstractTenantResolutionFilter {

    private final JwtTenantClaimsExtractor claimsExtractor;

    public JwtTenantResolutionFilter(TenantContext tenantContext,
                                     ObjectProvider<TenantRepository> tenantRepositoryProvider,
                                     TenantProperties tenantProperties,
                                     JwtTenantClaimsExtractor claimsExtractor) {
        super(tenantContext, tenantRepositoryProvider, tenantProperties);
        this.claimsExtractor = claimsExtractor;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().matches(".*/v1/tenants/me$");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }

        if (!(authentication instanceof JwtAuthenticationToken jwtToken)) {
            chain.doFilter(request, response);
            return;
        }

        List<String> allowedTenants = claimsExtractor.extract(jwtToken);
        if (allowedTenants.isEmpty()) {
            log.warn("JWT missing tenant claims");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing tenant claim in JWT");
            return;
        }

        String requestedTenant = request.getHeader(TENANT_ID_HEADER);
        if (requestedTenant == null || requestedTenant.isBlank()) {
            if (allowedTenants.size() == 1) {
                // Single tenant: auto-select for backwards compatibility
                requestedTenant = allowedTenants.getFirst();
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "X-Tenant-ID header required when user has access to multiple tenants");
                return;
            }
        }

        if (!allowedTenants.contains(requestedTenant)) {
            log.warn("Tenant '{}' not in user's allowed tenant list", LogSanitizer.sanitize(requestedTenant));
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access to tenant not permitted");
            return;
        }

        if (!this.validateTenantId(requestedTenant, response)) {
            return;
        }

        tenantContext.setAccessibleTenantIds(allowedTenants);
        tenantContext.setTenantId(requestedTenant);
        chain.doFilter(request, response);
    }
}
