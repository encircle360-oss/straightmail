package com.encircle360.oss.straightmail.tenant.filter;

import com.encircle360.oss.straightmail.model.Tenant;
import com.encircle360.oss.straightmail.repository.TenantRepository;
import com.encircle360.oss.straightmail.service.TenantService;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiKeyAuthenticationFilterTest {

    private static final String GLOBAL_API_KEY = "global-test-api-key";
    private static final String API_PREFIX = "/v1";

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private ObjectProvider<TenantRepository> tenantRepositoryProvider;

    @Mock
    private ObjectProvider<TenantContext> tenantContextProvider;

    private ApiKeyAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        when(tenantRepositoryProvider.getIfAvailable()).thenReturn(tenantRepository);
        when(tenantContextProvider.getIfAvailable()).thenReturn(tenantContext);
        filter = new ApiKeyAuthenticationFilter(GLOBAL_API_KEY, API_PREFIX, tenantRepositoryProvider, tenantContextProvider);
    }

    @Test
    void constructor_rejects_blank_api_key() {
        assertThrows(IllegalStateException.class,
                () -> new ApiKeyAuthenticationFilter("", API_PREFIX, tenantRepositoryProvider, tenantContextProvider));
    }

    @Test
    void constructor_rejects_null_api_key() {
        assertThrows(IllegalStateException.class,
                () -> new ApiKeyAuthenticationFilter(null, API_PREFIX, tenantRepositoryProvider, tenantContextProvider));
    }

    @Test
    void shouldNotFilter_actuator_paths() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/health");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_paths_not_matching_api_prefix() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/some-other-path");
        assertTrue(filter.shouldNotFilter(request));
    }

    @Test
    void shouldFilter_paths_matching_api_prefix() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v1/email");
        assertFalse(filter.shouldNotFilter(request));
    }

    @Test
    void missing_api_key_header_returns_401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void blank_api_key_header_returns_401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-KEY", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void valid_global_api_key_passes_through() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-KEY", GLOBAL_API_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    void valid_tenant_api_key_passes_and_sets_tenant_context() throws Exception {
        String tenantApiKey = "tenant-specific-key";
        String keyHash = TenantService.hashApiKey(tenantApiKey);
        Tenant tenant = Tenant.builder().slug("acme").active(true).build();

        when(tenantRepository.findByApiKeyHashAndActiveTrue(keyHash)).thenReturn(Optional.of(tenant));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-KEY", tenantApiKey);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(tenantContext).setTenantId("acme");
        verify(chain).doFilter(request, response);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    void invalid_api_key_returns_401() throws Exception {
        when(tenantRepository.findByApiKeyHashAndActiveTrue(any())).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-KEY", "wrong-unknown-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void shouldNotFilter_spa_routes_that_share_api_prefix_string() {
        ApiKeyAuthenticationFilter f = new ApiKeyAuthenticationFilter(
                GLOBAL_API_KEY, "/api", tenantRepositoryProvider, tenantContextProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api-key-login");
        assertTrue(f.shouldNotFilter(request));
    }

    @Test
    void shouldFilter_api_paths_with_default_prefix() {
        ApiKeyAuthenticationFilter f = new ApiKeyAuthenticationFilter(
                GLOBAL_API_KEY, "/api", tenantRepositoryProvider, tenantContextProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/tenants/me");
        assertFalse(f.shouldNotFilter(request));
    }

    @Test
    void global_key_does_not_set_tenant_context() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-KEY", GLOBAL_API_KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verifyNoInteractions(tenantContext);
    }
}
