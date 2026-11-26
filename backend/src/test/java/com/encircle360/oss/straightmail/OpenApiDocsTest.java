package com.encircle360.oss.straightmail;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the published OpenAPI document: its version is the application version, and the
 * documented server follows the request instead of a fixed host.
 *
 * <p>The forward-headers strategy is set here because the test {@code application.yml} replaces
 * the main one, which sets the same value.
 */
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "server.forward-headers-strategy=framework"
)
class OpenApiDocsTest extends AbstractTest {

    @Autowired
    private BuildProperties buildProperties;

    @Test
    void apiDocsVersionIsApplicationVersion() throws Exception {
        JsonNode docs = mapper.readTree(get("/v3/api-docs", status().isOk()).getResponse().getContentAsString());

        assertFalse(buildProperties.getVersion().isBlank());
        assertEquals(buildProperties.getVersion(), docs.get("info").get("version").asString());
    }

    @Test
    void apiDocsServerFollowsForwardedHost() throws Exception {
        MvcResult result = mock.perform(withAuth(MockMvcRequestBuilders.get("/v3/api-docs"))
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Host", "mail.example.org"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode servers = mapper.readTree(result.getResponse().getContentAsString()).get("servers");

        assertEquals(1, servers.size());
        assertEquals("https://mail.example.org", servers.get(0).get("url").asString());
    }
}
