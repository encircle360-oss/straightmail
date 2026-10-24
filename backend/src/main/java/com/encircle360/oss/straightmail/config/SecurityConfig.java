package com.encircle360.oss.straightmail.config;

import com.encircle360.oss.straightmail.tenant.filter.ApiKeyAuthenticationFilter;
import com.encircle360.oss.straightmail.tenant.filter.ApiKeyTenantResolutionFilter;
import com.encircle360.oss.straightmail.tenant.filter.JwtTenantResolutionFilter;
import com.encircle360.oss.straightmail.tenant.filter.NoAuthTenantResolutionFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Spring Security configuration for straightmail.
 *
 * <p>Supports three mutually exclusive authentication modes, selected via the {@code auth.mode} property:
 *
 * <ul>
 *   <li><b>OIDC / JWT mode ({@code auth.mode=oidc}, default)</b>: configures an OAuth2 resource
 *       server. JWTs are validated against the OIDC issuer at {@code auth.issuer-uri} and the
 *       {@code aud} claim is verified against {@code spring.security.oauth2.resourceserver.jwt.audiences}.
 *       Roles are extracted from the {@code realm_access.roles} JWT claim and mapped to {@code ROLE_*}
 *       Spring authorities. The {@link JwtTenantResolutionFilter} is added after
 *       {@link BearerTokenAuthenticationFilter} to populate {@link com.encircle360.oss.straightmail.tenant.TenantContext}.</li>
 *   <li><b>API-key mode ({@code auth.mode=api-key})</b>: Spring Security authentication is disabled at
 *       the framework level. The {@link ApiKeyAuthenticationFilter} enforces API-key validation and
 *       sets {@code ROLE_ADMIN} for the global key or {@code ROLE_USER} for per-tenant keys, allowing
 *       {@code @PreAuthorize} to work uniformly across all modes.</li>
 *   <li><b>No-auth mode ({@code auth.mode=none})</b>: no authentication required. All anonymous
 *       requests are granted {@code ROLE_ADMIN}, so admin endpoints remain accessible. If the
 *       {@code database} profile is active, the {@link NoAuthTenantResolutionFilter} resolves the
 *       tenant from the {@code X-Tenant-ID} header.</li>
 * </ul>
 *
 * <p>Custom filters are registered via {@link FilterRegistrationBean} with {@code enabled=false} to
 * prevent Spring Boot from auto-registering them outside the security filter chain.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${api.prefix:/api}")
    private String apiPrefix;

    @Value("${auth.issuer-uri:}")
    private String issuerUriForCsp;

    /**
     * Comma-separated list of accepted {@code aud} values. A custom {@link JwtDecoder} bean replaces
     * Boot's resource-server autoconfiguration, so this property has to be applied by hand — without
     * it any token from the same realm, including one issued to a different client, would be accepted.
     */
    @Value("${spring.security.oauth2.resourceserver.jwt.audiences:}")
    private String expectedAudiences;

    /** Full Content-Security-Policy override; when blank the policy is derived from the deployment. */
    @Value("${security.content-security-policy:}")
    private String contentSecurityPolicyOverride;

    private final ObjectProvider<JwtTenantResolutionFilter> jwtTenantResolutionFilter;
    private final ObjectProvider<ApiKeyAuthenticationFilter> apiKeyAuthenticationFilter;
    private final ObjectProvider<ApiKeyTenantResolutionFilter> apiKeyTenantResolutionFilter;
    private final ObjectProvider<NoAuthTenantResolutionFilter> noAuthTenantResolutionFilter;

    /**
     * Creates the {@link JwtDecoder} used to validate incoming JWT tokens in OIDC mode.
     *
     * <p>Two modes are supported:
     * <ul>
     *   <li><b>Split-URL mode</b> (when {@code auth.jwk-uri} is set): fetches JWK keys from the
     *       internal {@code auth.jwk-uri} (e.g. a Docker-internal service name) while validating
     *       the {@code iss} claim against the public {@code auth.issuer-uri} (e.g. {@code localhost}).
     *       Use this when the backend container cannot reach the public Keycloak hostname.</li>
     *   <li><b>Standard mode</b> (when {@code auth.jwk-uri} is empty): performs full OIDC discovery
     *       via {@code auth.issuer-uri}, which must be reachable from the backend at startup.</li>
     * </ul>
     *
     * <p>Both modes validate {@code iss}, {@code exp}, {@code nbf} and {@code aud}; see
     * {@link #tokenValidator(String)}.
     *
     * @param issuerUri the public OIDC issuer URI; used for {@code iss} claim validation
     * @param jwkUri    optional internal JWK set URI; when set, OIDC discovery is skipped
     * @return a {@link JwtDecoder} configured for the given issuer
     */
    @Bean
    @ConditionalOnProperty(name = "auth.mode", havingValue = "oidc", matchIfMissing = true)
    public JwtDecoder jwtDecoder(
            @Value("${auth.issuer-uri}") String issuerUri,
            @Value("${auth.jwk-uri:}") String jwkUri
    ) {

        NimbusJwtDecoder decoder = StringUtils.hasText(jwkUri)
                ? NimbusJwtDecoder.withJwkSetUri(jwkUri).build()
                : NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

        decoder.setJwtValidator(this.tokenValidator(issuerUri));
        return decoder;
    }

    /**
     * Builds the token validator: the framework defaults ({@code exp}, {@code nbf}, {@code iss})
     * plus an {@code aud} check against the configured audiences.
     *
     * <p>When no audience is configured the audience check is skipped, which keeps deployments that
     * deliberately run without one working.
     *
     * <p>Package-private so the audience rule can be asserted directly, without a live issuer.
     *
     * @param issuerUri the public issuer the {@code iss} claim must match
     * @return the combined validator
     */
    OAuth2TokenValidator<Jwt> tokenValidator(String issuerUri) {
        OAuth2TokenValidator<Jwt> defaults = JwtValidators.createDefaultWithIssuer(issuerUri);

        List<String> audiences = this.configuredAudiences();
        if (audiences.isEmpty()) {
            log.warn("No JWT audience configured — tokens issued to any client of this realm will be accepted");
            return defaults;
        }

        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD, aud -> aud != null && !Collections.disjoint(aud, audiences));
        return new DelegatingOAuth2TokenValidator<>(defaults, audience);
    }

    /**
     * Splits the configured audience property into a list, dropping blank entries.
     *
     * @return the accepted {@code aud} values, empty when none are configured
     */
    private List<String> configuredAudiences() {
        if (expectedAudiences == null || expectedAudiences.isBlank()) {
            return List.of();
        }
        return Arrays.stream(expectedAudiences.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Creates a {@link JwtAuthenticationConverter} that maps Keycloak {@code realm_access.roles}
     * to Spring Security {@code ROLE_*} granted authorities.
     *
     * @return the configured {@link JwtAuthenticationConverter}
     */
    @Bean
    @ConditionalOnProperty(name = "auth.mode", havingValue = "oidc", matchIfMissing = true)
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null) return Collections.emptyList();
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles == null) return Collections.emptyList();
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });
        return converter;
    }

    /**
     * Configures the security filter chain for OIDC / JWT authentication mode ({@code auth.mode=oidc}).
     *
     * <p>All {@code /api/**} paths require authentication. JWT audience is validated against
     * {@code spring.security.oauth2.resourceserver.jwt.audiences}. The {@link JwtTenantResolutionFilter}
     * is injected after JWT validation when the {@code database} profile is active.
     *
     * @param http                       the {@link HttpSecurity} builder
     * @param jwtDecoder                 the JWT decoder bean
     * @param jwtAuthenticationConverter the authority converter bean
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the filter chain cannot be built
     */
    @Bean
    @ConditionalOnProperty(name = "auth.mode", havingValue = "oidc", matchIfMissing = true)
    public SecurityFilterChain securedFilterChain(HttpSecurity http, JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
                .cors(withDefaults())
                .csrf(this::exemptApiFromCsrf)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(apiPrefix + "/v1/info").permitAll()
                        .requestMatchers(apiPrefix + "/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )
                );

        this.applySecurityHeaders(http);

        JwtTenantResolutionFilter jwtFilter = jwtTenantResolutionFilter.getIfAvailable();
        if (jwtFilter != null) {
            http.addFilterAfter(jwtFilter, BearerTokenAuthenticationFilter.class);
        }

        return http.build();
    }

    /**
     * Configures the security filter chain for API-key authentication mode ({@code auth.mode=api-key}).
     *
     * <p>Spring Security authentication is disabled at the framework level. The
     * {@link ApiKeyAuthenticationFilter} enforces key validation and sets {@code ROLE_ADMIN}
     * (global key) or {@code ROLE_USER} (per-tenant key) in the {@link org.springframework.security.core.context.SecurityContext},
     * ensuring that {@code @PreAuthorize} annotations work uniformly across all modes.
     *
     * @param http the {@link HttpSecurity} builder
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the filter chain cannot be built
     */
    @Bean
    @ConditionalOnProperty(name = "auth.mode", havingValue = "api-key")
    public SecurityFilterChain apiKeyFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(this::exemptApiFromCsrf)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        this.applySecurityHeaders(http);

        ApiKeyAuthenticationFilter apiKeyFilter = apiKeyAuthenticationFilter.getIfAvailable();
        ApiKeyTenantResolutionFilter apiKeyTenantFilter = apiKeyTenantResolutionFilter.getIfAvailable();
        if (apiKeyFilter != null) {
            http.addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);
        }
        if (apiKeyTenantFilter != null && apiKeyFilter != null) {
            http.addFilterAfter(apiKeyTenantFilter, ApiKeyAuthenticationFilter.class);
        }

        return http.build();
    }

    /**
     * Configures the security filter chain for no-authentication mode ({@code auth.mode=none}).
     *
     * <p>All requests are permitted without any authentication. Anonymous requests are granted
     * {@code ROLE_ADMIN} so that {@code @PreAuthorize("hasRole('ADMIN')")} on admin endpoints
     * is satisfied. If the {@code database} profile is active, the {@link NoAuthTenantResolutionFilter}
     * resolves the tenant from the {@code X-Tenant-ID} header.
     *
     * @param http the {@link HttpSecurity} builder
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the filter chain cannot be built
     */
    @Bean
    @ConditionalOnProperty(name = "auth.mode", havingValue = "none")
    public SecurityFilterChain openFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(withDefaults())
                .csrf(this::exemptApiFromCsrf)
                .anonymous(anon -> anon.authorities("ROLE_ADMIN"))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        this.applySecurityHeaders(http);

        NoAuthTenantResolutionFilter noAuthFilter = noAuthTenantResolutionFilter.getIfAvailable();
        if (noAuthFilter != null) {
            http.addFilterBefore(noAuthFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    @ConditionalOnBean(JwtTenantResolutionFilter.class)
    public FilterRegistrationBean<JwtTenantResolutionFilter> jwtTenantResolutionFilterRegistration(
            JwtTenantResolutionFilter filter) {
        FilterRegistrationBean<JwtTenantResolutionFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    @ConditionalOnBean(ApiKeyAuthenticationFilter.class)
    public FilterRegistrationBean<ApiKeyAuthenticationFilter> apiKeyAuthenticationFilterRegistration(
            ApiKeyAuthenticationFilter filter) {
        FilterRegistrationBean<ApiKeyAuthenticationFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    @ConditionalOnBean(ApiKeyTenantResolutionFilter.class)
    public FilterRegistrationBean<ApiKeyTenantResolutionFilter> apiKeyTenantResolutionFilterRegistration(
            ApiKeyTenantResolutionFilter filter) {
        FilterRegistrationBean<ApiKeyTenantResolutionFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    @Bean
    @ConditionalOnBean(NoAuthTenantResolutionFilter.class)
    public FilterRegistrationBean<NoAuthTenantResolutionFilter> noAuthTenantResolutionFilterRegistration(
            NoAuthTenantResolutionFilter filter) {
        FilterRegistrationBean<NoAuthTenantResolutionFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setEnabled(false);
        return reg;
    }

    /**
     * Keeps CSRF protection on, but exempts the API, in all three auth modes.
     *
     * <p>Every API call authenticates with a credential the browser never attaches on its own: a
     * bearer token or an {@code X-API-KEY} header, both set explicitly by the SPA or the API client.
     * No session cookie authenticates a request, so a cross-site request carries nothing to abuse
     * and a CSRF token would only break non-browser clients. Any route outside the API prefix stays
     * protected, so a future cookie-backed or form endpoint does not silently lose the check.
     *
     * @param csrf the CSRF configurer of the chain under construction
     */
    private void exemptApiFromCsrf(CsrfConfigurer<HttpSecurity> csrf) {
        csrf.ignoringRequestMatchers(apiPrefix + "/**");
    }

    /**
     * Applies the response security headers shared by all three auth modes.
     *
     * <p>Spring Security already sends {@code X-Content-Type-Options}, {@code X-Frame-Options} and
     * the no-cache headers by default. What is missing without this block is a
     * Content-Security-Policy and a {@code Referrer-Policy}, which matter here because the admin SPA
     * is served from the same origin as the API and holds the access token in browser storage: a
     * script injected into that origin would read it. HSTS is emitted by Spring only on secure
     * requests, so {@code server.forward-headers-strategy} must be set for it to appear behind a
     * TLS terminator.
     *
     * @param http the chain under construction
     * @throws Exception if the headers cannot be configured
     */
    private void applySecurityHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(this.buildContentSecurityPolicy()))
                .referrerPolicy(referrer -> referrer.policy(
                        org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000))
        );
    }

    /**
     * Builds the Content-Security-Policy for this deployment.
     *
     * <p>The allowances are exactly what the shipped admin UI needs: Google Fonts for the stylesheet
     * and font files ({@code index.html}), Gravatar for user avatars, and — in OIDC mode — the
     * identity provider, which the browser must reach for the token endpoint, the silent-renew
     * iframe and the authorize redirect. {@code style-src} needs {@code 'unsafe-inline'} because
     * Angular injects component styles as inline style elements; scripts do not, so
     * {@code script-src} stays strict.
     *
     * @return the policy directives, or the configured override when one is set
     */
    private String buildContentSecurityPolicy() {
        if (contentSecurityPolicyOverride != null && !contentSecurityPolicyOverride.isBlank()) {
            return contentSecurityPolicyOverride;
        }

        String idp = this.identityProviderOrigin();
        return "default-src 'self'; "
                + "script-src 'self'; "
                + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                + "font-src 'self' https://fonts.gstatic.com data:; "
                + "img-src 'self' data: https://www.gravatar.com; "
                + "connect-src 'self'" + idp + "; "
                + "frame-src 'self'" + idp + "; "
                + "form-action 'self'" + idp + "; "
                + "frame-ancestors 'none'; "
                + "base-uri 'self'; "
                + "object-src 'none'";
    }

    /**
     * Returns the identity provider's origin as a CSP source, prefixed with a space, or an empty
     * string when no issuer is configured (api-key and none modes).
     *
     * @return {@code " https://idp.example"} or {@code ""}
     */
    private String identityProviderOrigin() {
        if (issuerUriForCsp == null || issuerUriForCsp.isBlank()) {
            return "";
        }
        try {
            java.net.URI uri = java.net.URI.create(issuerUriForCsp.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "";
            }
            String origin = uri.getScheme() + "://" + uri.getHost();
            return uri.getPort() > -1 ? " " + origin + ":" + uri.getPort() : " " + origin;
        } catch (IllegalArgumentException e) {
            log.warn("auth.issuer-uri is not a valid URI, omitting it from the Content-Security-Policy");
            return "";
        }
    }
}
