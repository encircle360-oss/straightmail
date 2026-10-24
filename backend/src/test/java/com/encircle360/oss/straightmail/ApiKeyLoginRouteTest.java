package com.encircle360.oss.straightmail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins that the API-key login page of the admin console stays reachable.
 *
 * <p>In api-key mode the SPA route {@code /api-key-login} shares its first characters with the API
 * prefix {@code /api}. Matching the prefix without a segment boundary made the tenant-resolution
 * filter reject direct navigation to it with "Missing X-Tenant-ID header".
 */
@SpringBootTest(
        classes = StraightmailApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"auth.mode=api-key", "api.prefix=/api", "api.key=test-api-key"})
@ActiveProfiles({"database", "test"})
class ApiKeyLoginRouteTest {

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:file::memory:?cache=shared&mode=memory");
        registry.add("spring.datasource.driver-class-name", () -> "org.sqlite.JDBC");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "1");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.community.dialect.SQLiteDialect");
    }

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void apiKeyLoginRoute_servesTheConsoleWithoutAnyHeaders() throws Exception {
        HttpResponse<String> response = get("/api-key-login");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<app-root"),
                "expected the Angular index.html, got: " + response.body());
    }

    @Test
    void realApiPath_stillRequiresTheApiKey() throws Exception {
        assertEquals(401, get("/api/v1/templates").statusCode());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
