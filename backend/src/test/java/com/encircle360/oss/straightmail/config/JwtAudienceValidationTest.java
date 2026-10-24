package com.encircle360.oss.straightmail.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the {@code aud} check that {@link SecurityConfig#tokenValidator(String)} adds by hand.
 *
 * <p>The custom {@link org.springframework.security.oauth2.jwt.JwtDecoder} bean replaces Boot's
 * resource-server autoconfiguration, so {@code spring.security.oauth2.resourceserver.jwt.audiences}
 * is only enforced because that method applies it. Without the check, a token issued by the same
 * realm to a different client would be accepted.
 */
class JwtAudienceValidationTest {

    private static final String ISSUER = "https://idp.example/realms/straightmail";

    @Test
    void rejectsTokenIssuedToAnotherClient() {
        OAuth2TokenValidatorResult result = validate("straightmail", List.of("other-client"));

        assertTrue(result.hasErrors());
    }

    @Test
    void acceptsTokenWithConfiguredAudience() {
        OAuth2TokenValidatorResult result = validate("straightmail", List.of("straightmail", "account"));

        assertFalse(result.hasErrors());
    }

    @Test
    void acceptsAnyOfSeveralConfiguredAudiences() {
        OAuth2TokenValidatorResult result = validate("straightmail, acme-backend", List.of("acme-backend"));

        assertFalse(result.hasErrors());
    }

    @Test
    void rejectsTokenWithoutAudienceClaim() {
        OAuth2TokenValidatorResult result = validate("straightmail", null);

        assertTrue(result.hasErrors());
    }

    @Test
    void skipsAudienceCheckWhenNoneConfigured() {
        OAuth2TokenValidatorResult result = validate("", List.of("other-client"));

        assertFalse(result.hasErrors());
    }

    private OAuth2TokenValidatorResult validate(String configuredAudiences, List<String> tokenAudience) {
        SecurityConfig config = new SecurityConfig(null, null, null, null);
        ReflectionTestUtils.setField(config, "expectedAudiences", configuredAudiences);

        OAuth2TokenValidator<Jwt> validator = config.tokenValidator(ISSUER);
        return validator.validate(token(tokenAudience));
    }

    private Jwt token(List<String> audience) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("iss", ISSUER)
                .issuedAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES));

        if (audience != null) {
            builder.audience(audience);
        }

        return builder.build();
    }
}
