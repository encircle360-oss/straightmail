package com.encircle360.oss.straightmail.tenant.filter;

import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.repository.TenantRepository;
import com.encircle360.oss.straightmail.tenant.JwtTenantClaimsExtractor;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the tenant boundary in OIDC mode.
 *
 * <p>The {@code X-Tenant-ID} header is caller-supplied, so it must be checked against the tenant
 * claims in the JWT rather than trusted. Nothing covered this at the filter level before: a
 * regression here would let any authenticated user operate as any tenant, which is the widest
 * failure this application can have short of remote code execution.
 */
class JwtTenantResolutionFilterTest {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    private TenantContext tenantContext;
    private JwtTenantResolutionFilter filter;
    private FilterChain chain;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        tenantContext = new TenantContext();
        chain = mock(FilterChain.class);

        TenantRepository repository = mock(TenantRepository.class);
        when(repository.existsBySlug(anyString())).thenReturn(true);

        ObjectProvider<TenantRepository> repositoryProvider = mock(ObjectProvider.class);
        when(repositoryProvider.getIfAvailable()).thenReturn(repository);

        // the claim names are @Value-injected in production; set them directly for the unit test
        JwtTenantClaimsExtractor extractor = new JwtTenantClaimsExtractor();
        ReflectionTestUtils.setField(extractor, "tenantIdsClaim", "tenant_ids");
        ReflectionTestUtils.setField(extractor, "tenantClaim", "tenant_id");

        filter = new JwtTenantResolutionFilter(
                tenantContext, repositoryProvider, new TenantProperties(), extractor);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void a_foreign_tenant_in_the_header_is_refused() throws Exception {
        authenticateWithTenants("acme-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestWithTenant("acme-2"), response, chain);

        assertEquals(403, response.getStatus(), "a tenant outside the JWT claims must be refused");
        assertNull(tenantContext.getTenantId(), "no tenant may be resolved on refusal");
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void an_own_tenant_in_the_header_is_accepted() throws Exception {
        authenticateWithTenants("acme-1", "acme-2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestWithTenant("acme-2"), response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("acme-2", tenantContext.getTenantId());
    }

    @Test
    void a_single_tenant_claim_is_auto_selected_without_a_header() throws Exception {
        authenticateWithTenants("acme-1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/templates"), response, chain);

        assertEquals("acme-1", tenantContext.getTenantId());
    }

    @Test
    void multiple_tenant_claims_require_an_explicit_header() throws Exception {
        authenticateWithTenants("acme-1", "acme-2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/templates"), response, chain);

        assertEquals(400, response.getStatus(), "ambiguity must not be resolved silently");
        assertNull(tenantContext.getTenantId());
    }

    @Test
    void a_token_without_tenant_claims_is_refused() throws Exception {
        authenticateWithTenants();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(requestWithTenant("acme-1"), response, chain);

        assertEquals(403, response.getStatus());
        assertNull(tenantContext.getTenantId());
    }

    private MockHttpServletRequest requestWithTenant(String tenantId) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/templates");
        request.addHeader(TENANT_HEADER, tenantId);
        return request;
    }

    private void authenticateWithTenants(String... tenantIds) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .claim("tenant_ids", List.of(tenantIds))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(), "user"));
    }
}
