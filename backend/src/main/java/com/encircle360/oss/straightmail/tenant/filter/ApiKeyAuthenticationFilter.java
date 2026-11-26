package com.encircle360.oss.straightmail.tenant.filter;

import com.encircle360.oss.straightmail.repository.TenantRepository;
import com.encircle360.oss.straightmail.service.TenantService;
import com.encircle360.oss.straightmail.tenant.TenantContext;
import com.encircle360.oss.straightmail.util.ApiPaths;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

/**
 * Servlet filter that authenticates incoming API requests using the {@code X-API-KEY} header.
 *
 * <p>Active when {@code auth.mode=api-key}. Does not require the {@code database} profile;
 * if the database is unavailable only the global key check is performed.
 *
 * <p>Implements a two-step key validation:
 * <ol>
 *   <li><b>Global key</b>: compares the incoming key directly against the configured global key
 *       using a constant-time byte comparison to prevent timing attacks. Sets {@code ROLE_ADMIN}
 *       in the security context.</li>
 *   <li><b>Per-tenant key</b> (database profile only): hashes the incoming key with SHA-256 and
 *       looks it up in the database. If found, the corresponding tenant is set on
 *       {@link TenantContext} and {@code ROLE_USER} is placed in the security context.</li>
 * </ol>
 *
 * <p>Requests to {@code /actuator} and paths outside the configured API prefix are skipped.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "auth.mode", havingValue = "api-key")
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-KEY";

    private final byte[] globalKeyBytes;
    private final String apiPrefix;
    private final ObjectProvider<TenantRepository> tenantRepositoryProvider;
    private final ObjectProvider<TenantContext> tenantContextProvider;

    /**
     * Constructs the filter and validates that the global API key is configured.
     *
     * @param apiKey                   the global API key from the {@code api.key} property
     * @param apiPrefix                the API path prefix from the {@code api.prefix} property (default: {@code /api})
     * @param tenantRepositoryProvider optional repository for per-tenant API key hash lookups (requires {@code database} profile)
     * @param tenantContextProvider    optional request-scoped context to populate for per-tenant key matches
     * @throws IllegalStateException if {@code apiKey} is blank or not set
     */
    public ApiKeyAuthenticationFilter(@Value("${api.key:}") String apiKey,
                                      @Value("${api.prefix:/api}") String apiPrefix,
                                      ObjectProvider<TenantRepository> tenantRepositoryProvider,
                                      ObjectProvider<TenantContext> tenantContextProvider) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "API_KEY is not set. Set the api.key property to enable API key authentication.");
        }
        this.globalKeyBytes = apiKey.getBytes(StandardCharsets.UTF_8);
        this.apiPrefix = apiPrefix;
        this.tenantRepositoryProvider = tenantRepositoryProvider;
        this.tenantContextProvider = tenantContextProvider;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/actuator")) return true;
        if (path.matches(".*/v1/info$")) return true;
        return !ApiPaths.isApiPath(path, apiPrefix);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String providedKey = request.getHeader(API_KEY_HEADER);

        if (providedKey == null || providedKey.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing X-API-KEY header");
            return;
        }

        // 1. Check global key (constant-time comparison to prevent timing attacks) — grants ROLE_ADMIN
        byte[] providedKeyBytes = providedKey.getBytes(StandardCharsets.UTF_8);
        if (MessageDigest.isEqual(globalKeyBytes, providedKeyBytes)) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("global", null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
            );
            chain.doFilter(request, response);
            return;
        }

        // 2. Check per-tenant key via SHA-256 hash lookup (requires database profile)
        TenantRepository tenantRepository = tenantRepositoryProvider.getIfAvailable();
        TenantContext tenantContext = tenantContextProvider.getIfAvailable();
        if (tenantRepository != null && tenantContext != null) {
            String keyHash = TenantService.hashApiKey(providedKey);
            Optional<com.encircle360.oss.straightmail.model.Tenant> maybeTenant =
                    tenantRepository.findByApiKeyHashAndActiveTrue(keyHash);
            if (maybeTenant.isPresent()) {
                String slug = maybeTenant.get().getSlug();
                tenantContext.setTenantId(slug);
                tenantContext.setAccessibleTenantIds(List.of(slug));
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(slug, null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")))
                );
                chain.doFilter(request, response);
                return;
            }
        }

        log.warn("Invalid API key provided from {}", request.getRemoteAddr());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
    }
}
