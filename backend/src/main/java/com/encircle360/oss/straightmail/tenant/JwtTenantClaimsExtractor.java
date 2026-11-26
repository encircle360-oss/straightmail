package com.encircle360.oss.straightmail.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Shared utility for extracting tenant ID lists from a JWT in OIDC mode.
 *
 * <p>Reads the configurable claims {@code tenant_ids} (array or single string) and
 * {@code tenant_id} (single string) from the token and returns the combined result.
 * Used by both {@link com.encircle360.oss.straightmail.tenant.filter.JwtTenantResolutionFilter}
 * and {@link com.encircle360.oss.straightmail.controller.tenant.TenantReadController} to avoid
 * duplicating this logic.
 */
@Component
public class JwtTenantClaimsExtractor {

    @Value("${jwt.tenant-ids-claim:tenant_ids}")
    private String tenantIdsClaim;

    @Value("${jwt.tenant-claim:tenant_id}")
    private String tenantClaim;

    /**
     * Extracts the list of tenant IDs the caller is permitted to access from the JWT.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>Array claim ({@code tenant_ids}) — returned as-is if non-empty.</li>
     *   <li>Single-value {@code tenant_ids} — wrapped in a list.</li>
     *   <li>Single {@code tenant_id} claim — wrapped in a list as fallback.</li>
     * </ol>
     *
     * @param jwtAuth the authenticated JWT token
     * @return list of tenant ID strings, or an empty list if no tenant claim is present
     */
    public List<String> extract(JwtAuthenticationToken jwtAuth) {
        Object idsValue = jwtAuth.getToken().getClaim(tenantIdsClaim);
        if (idsValue instanceof List<?> list && !list.isEmpty()) {
            return list.stream().map(Object::toString).toList();
        }
        if (idsValue instanceof String s && !s.isBlank()) {
            return List.of(s);
        }
        Object singleValue = jwtAuth.getToken().getClaim(tenantClaim);
        if (singleValue != null) {
            String val = singleValue.toString();
            if (!val.isBlank()) return List.of(val);
        }
        return List.of();
    }
}
