package com.encircle360.oss.straightmail.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;

/**
 * Spring MVC and OpenAPI configuration.
 *
 * <p>Configures:
 * <ul>
 *   <li><b>CORS</b>: allows cross-origin requests from the origins listed in
 *       {@code cors.allowed-origins}, exposing the custom headers {@code X-API-KEY} and
 *       {@code X-Tenant-ID} required by the frontend.</li>
 *   <li><b>API prefix</b>: all {@link RestController} endpoints are automatically prefixed with
 *       the value of {@code api.prefix} (default: {@code /api}), separating them from static
 *       SPA assets served at the root.</li>
 *   <li><b>OpenAPI definition</b>: Swagger UI metadata for the SpringDoc integration.</li>
 * </ul>
 */
@OpenAPIDefinition(
        info = @Info(
                title = "Straightmail",
                version = "0.2.0"
        ),
        servers = {
                @Server(
                        url = "http://localhost:50003"
                )
        }
)
@Configuration
public class ApiConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${api.prefix:/api}")
    private String apiPrefix;

    /**
     * Registers CORS mappings for all API paths under the configured prefix.
     *
     * @param registry the CORS registry to configure
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(apiPrefix + "/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-API-KEY", "X-Tenant-ID", "*")
                .allowCredentials(true);
    }

    /**
     * Registers a custom {@link RequestMappingHandlerMapping} that prepends the API prefix to all
     * {@link RestController} handler paths.
     *
     * @param prefix the path prefix from the {@code api.prefix} property
     * @return a {@link WebMvcRegistrations} that overrides the default handler mapping
     */
    @Bean
    public WebMvcRegistrations webMvcRegistrations(@Value("${api.prefix:/api}") String prefix) {
        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
                mapping.setPathPrefixes(Map.of(
                        prefix,
                        org.springframework.web.method.HandlerTypePredicate.forAnnotation(RestController.class)
                ));
                return mapping;
            }
        };
    }
}
