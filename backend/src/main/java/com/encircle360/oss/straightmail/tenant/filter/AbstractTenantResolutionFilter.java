package com.encircle360.oss.straightmail.tenant.filter;

import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.repository.TenantRepository;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import com.encircle360.oss.straightmail.tenant.TenantValidation;
import com.encircle360.oss.straightmail.util.ApiPaths;
import com.encircle360.oss.straightmail.util.LogSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Abstract base class for all tenant-resolution filters.
 *
 * <p>Centralises the logic shared across {@link ApiKeyTenantResolutionFilter},
 * {@link NoAuthTenantResolutionFilter}, and {@link JwtTenantResolutionFilter}:
 * <ul>
 *   <li>The {@code X-Tenant-ID} header constant</li>
 *   <li>Slug-format validation via {@link TenantValidation#SLUG_PATTERN}</li>
 *   <li>Tenant existence checks against the database or static configuration</li>
 *   <li>The default {@link #shouldNotFilter} implementation used by API-key and no-auth modes</li>
 * </ul>
 *
 * <p>Subclasses must implement {@link #doFilterInternal} and may override
 * {@link #shouldNotFilter} when a different exclusion strategy is required (e.g. JWT mode).
 */
@Slf4j
public abstract class AbstractTenantResolutionFilter extends OncePerRequestFilter {

    protected static final String TENANT_ID_HEADER = "X-Tenant-ID";

    protected final TenantContext tenantContext;
    protected final ObjectProvider<TenantRepository> tenantRepositoryProvider;
    protected final TenantProperties tenantProperties;

    @Value("${api.prefix:/api}")
    protected String apiPrefix;

    private final Pattern slugPattern = Pattern.compile(TenantValidation.SLUG_PATTERN);

    protected AbstractTenantResolutionFilter(TenantContext tenantContext,
                                             ObjectProvider<TenantRepository> tenantRepositoryProvider,
                                             TenantProperties tenantProperties) {
        this.tenantContext = tenantContext;
        this.tenantRepositoryProvider = tenantRepositoryProvider;
        this.tenantProperties = tenantProperties;
    }

    /**
     * Default path-exclusion strategy for API-key and no-auth modes.
     *
     * <p>Skips tenant resolution for:
     * <ul>
     *   <li>Actuator endpoints (paths starting with {@code /actuator})</li>
     *   <li>The info endpoint (paths matching {@code v1/info})</li>
     *   <li>Paths outside the configured API prefix, judged on segment boundaries so that the
     *       SPA route {@code /api-key-login} is not mistaken for an API path (see {@link ApiPaths})</li>
     *   <li>{@code GET /v1/tenants} and {@code GET /v1/tenants/me}: accessible without a tenant header
     *       to support the initial login / tenant-selection flow</li>
     * </ul>
     *
     * <p>JWT-based filters override this method with a narrower exclusion set.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator")) return true;
        if (path.matches(".*/v1/info$")) return true;
        if (!ApiPaths.isApiPath(path, apiPrefix)) return true;
        if (!"GET".equalsIgnoreCase(request.getMethod())) return false;
        // Tenant list endpoints accessible without X-Tenant-ID (for login flow)
        return path.matches(".*/v1/tenants/?$") || path.matches(".*/v1/tenants/me$");
    }

    /**
     * Validates the tenant identifier format and existence, writing an appropriate HTTP error
     * to the response if the check fails.
     *
     * <p>Validates the slug format first (via {@link TenantValidation#SLUG_PATTERN}), then checks
     * existence against the database (if active) or {@link TenantProperties#getConfig()}.
     *
     * @param tenantId the tenant identifier to validate
     * @param response the current HTTP response used to send error codes
     * @return {@code true} if the tenant ID is valid and known; {@code false} with an error sent otherwise
     */
    protected boolean validateTenantId(String tenantId, HttpServletResponse response) throws IOException {
        if (!slugPattern.matcher(tenantId).matches()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid tenant identifier format");
            return false;
        }

        TenantRepository repo = tenantRepositoryProvider.getIfAvailable();
        if (repo != null) {
            if (!repo.existsBySlug(tenantId)) {
                log.warn("Tenant '{}' not found in database", LogSanitizer.sanitize(tenantId));
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Tenant not found");
                return false;
            }
        } else {
            boolean exists = tenantProperties.getConfig().stream().anyMatch(c -> tenantId.equals(c.getId()));
            if (!exists) {
                log.warn("Tenant '{}' not found in configuration", LogSanitizer.sanitize(tenantId));
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Tenant not found");
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether a tenant with the given identifier exists, consulting the database repository
     * (if active) or the static {@link TenantProperties} configuration as a fallback.
     *
     * @param tenantId the tenant slug to check
     * @return {@code true} if the tenant exists; {@code false} otherwise
     */
    protected boolean tenantExists(String tenantId) {
        TenantRepository repo = tenantRepositoryProvider.getIfAvailable();
        if (repo != null) {
            return repo.existsBySlug(tenantId);
        }
        return tenantProperties.getConfig().stream().anyMatch(c -> tenantId.equals(c.getId()));
    }
}
