package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.dto.email.EmailInlineTemplateRequestDTO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that the {@code smtpSender} configured for a tenant (non-database mode)
 * is always authoritative and overrides any {@code sender} value supplied in the request.
 */
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "tenants.config[0].id=default",
                "tenants.config[0].display-name=Default",
                "tenants.config[0].smtp-sender=noreply@tenant.com"
        }
)
class TenantSenderEnforcementTest extends AbstractTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void tenantConfiguredSenderOverridesRequestSender() throws Exception {
        // Arrange
        try {
            restTemplate.delete(getMailpitApiUrl() + "/api/v1/messages");
            Thread.sleep(500);
        } catch (Exception ignored) {
        }

        HashMap<String, JsonNode> model = new HashMap<>();
        model.put("value", JsonNodeFactory.instance.stringNode("test"));

        EmailInlineTemplateRequestDTO emailRequest = EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of("recipient@test.com"))
                .sender("caller-provided@test.com")   // must be overridden by tenant smtpSender
                .subject("Tenant Sender Enforcement Test")
                .model(model)
                .emailTemplate("Sender enforcement test body: ${value}")
                .build();

        // Act
        post("/v1/email/inline", emailRequest, status().is2xxSuccessful());
        Thread.sleep(2000);

        // Assert — From header must reflect the tenant-configured sender
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                getMailpitApiUrl() + "/api/v1/messages", HttpMethod.GET, null, JsonNode.class);

        assertNotNull(response.getBody());
        JsonNode messages = response.getBody().get("messages");
        assertNotNull(messages);
        assertFalse(messages.isEmpty(), "No emails found in Mailpit");

        boolean emailFound = false;
        for (JsonNode message : messages) {
            JsonNode subjectNode = message.get("Subject");
            if (subjectNode == null || !"Tenant Sender Enforcement Test".equals(subjectNode.asString())) continue;

            emailFound = true;
            JsonNode fromNode = message.get("From");
            assertNotNull(fromNode, "From field must be present");
            String from = fromNode.get("Address").asString();
            assertTrue(from.contains("noreply@tenant.com"),
                    "Expected From to contain tenant-configured 'noreply@tenant.com' but was: " + from);
            assertFalse(from.contains("caller-provided@test.com"),
                    "Request sender must not leak into From when tenant sender is configured");
            break;
        }

        assertTrue(emailFound, "Email 'Tenant Sender Enforcement Test' not found in Mailpit");
    }
}
