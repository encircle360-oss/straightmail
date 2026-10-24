package com.encircle360.oss.straightmail.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.ConstructorDetector;

/**
 * Jackson configuration for request and response bodies.
 *
 * <p>The DTOs in this application are mutable beans: Lombok gives them a no-args constructor and
 * setters, and an all-args constructor for the builder. Jackson 3 introspects that all-args
 * constructor as an implicit creator even though a no-args constructor exists, which changes what
 * an absent property means. As a creator argument it arrives as {@code null}, and since
 * {@code FAIL_ON_NULL_FOR_PRIMITIVES} is on by default in Jackson 3 (it was off in Jackson 2), any
 * DTO with a primitive field rejects every payload that leaves that field out — {@code
 * POST /v1/tenants} answered 400 unless {@code smtpTls}, {@code smtpSsl} and {@code active} were
 * all present. Through setters an absent property simply leaves the field at its declared default.
 */
@Configuration
public class JacksonConfig {

    /**
     * Makes Jackson prefer the no-args constructor whenever a class has one, matching the DTO
     * convention of this codebase. Classes without a no-args constructor — records, and value
     * types with a single constructor — keep their implicit creator.
     *
     * @return the customizer applied to the application's {@code JsonMapper}
     */
    @Bean
    public JsonMapperBuilderCustomizer preferNoArgsConstructor() {
        return builder -> builder.constructorDetector(
                ConstructorDetector.DEFAULT.withAllowImplicitWithDefaultConstructor(false));
    }
}
