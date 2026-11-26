package com.encircle360.oss.straightmail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins how unmatched paths are answered once the API sits behind a prefix.
 *
 * <p>The rest of the suite runs with {@code api.prefix=""}, which hides the problem entirely: with
 * an empty prefix every path counts as an API path. Production uses {@code /api}, where a catch-all
 * SPA mapping used to swallow unknown API endpoints and answer them with a page of HTML.
 *
 * <p>Runs against a real servlet container rather than MockMvc, because the SPA fallback lives on
 * the container's error dispatch, which MockMvc does not perform.
 */
@SpringBootTest(
        classes = StraightmailApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"auth.mode=none", "api.prefix=/api"})
@ActiveProfiles({"database", "test"})
class SpaRoutingTest {

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
    void unknownApiPath_returnsNotFound() throws Exception {
        assertEquals(404, browserGet("/api/v1/does-not-exist").statusCode());
    }

    @Test
    void unknownApiPath_forJsonClient_returnsNotFound() throws Exception {
        assertEquals(404, get("/api/v1/does-not-exist", "application/json").statusCode());
    }

    @Test
    void deepSpaRoute_servesTheConsole() throws Exception {
        HttpResponse<String> response = browserGet("/tenants/acme/templates/welcome/edit");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<app-root"),
                "expected the Angular index.html, got: " + response.body());
    }

    @Test
    void missingStaticAsset_returnsNotFound() throws Exception {
        assertEquals(404, browserGet("/assets/does-not-exist.png").statusCode());
    }

    private HttpResponse<String> browserGet(String path) throws IOException, InterruptedException {
        return get(path, "text/html,application/xhtml+xml,*/*;q=0.8");
    }

    private HttpResponse<String> get(String path, String accept) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", accept)
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
