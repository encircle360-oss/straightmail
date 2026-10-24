package com.encircle360.oss.straightmail.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller that provides application metadata and runtime configuration for the frontend.
 *
 * <p>Exposes {@code GET /v1/info} which returns the application name, status, and the runtime
 * configuration properties required by the Angular frontend to bootstrap itself without
 * environment-specific build parameters. This endpoint is always publicly accessible,
 * regardless of the configured {@code auth.mode}.
 *
 * <p>The frontend fetches this endpoint before bootstrapping Angular, so that {@code authMode},
 * {@code apiUrl}, and {@code oidcAuthority} are available when providers and interceptors are
 * registered.
 */
@RestController
@RequestMapping("/v1/info")
@RequiredArgsConstructor
public class InfoController {

    @Value("${spring.application.name:straightmail}")
    private String applicationName;

    @Value("${auth.mode:oidc}")
    private String authMode;

    @Value("${api.url:/api}")
    private String apiUrl;

    @Value("${auth.issuer-uri:}")
    private String oidcAuthority;

    /**
     * Returns application metadata together with the runtime configuration required by the frontend.
     *
     * <p>Response fields:
     * <ul>
     *   <li>{@code application} — the application name from {@code spring.application.name}</li>
     *   <li>{@code status} — always {@code "running"}</li>
     *   <li>{@code authMode} — active authentication mode: {@code oidc}, {@code api-key}, or {@code none}</li>
     *   <li>{@code apiUrl} — base URL of the REST API as seen by the browser (from {@code api.url})</li>
     *   <li>{@code oidcAuthority} — OIDC issuer URI (only relevant when {@code authMode=oidc})</li>
     * </ul>
     *
     * @return {@code 200 OK} with the application metadata and frontend runtime configuration
     */
    @GetMapping
    @Operation(operationId = "getInfo", description = "Get application info and frontend runtime configuration")
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("application", applicationName);
        info.put("status", "running");
        info.put("authMode", authMode);
        info.put("apiUrl", apiUrl);
        if (authMode.equals("oidc")) {
            info.put("oidcAuthority", oidcAuthority);
        }
        return ResponseEntity.ok(info);
    }
}
