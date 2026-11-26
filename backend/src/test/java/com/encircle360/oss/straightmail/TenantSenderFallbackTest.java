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
 * Pins the sender fallback, the other half of {@link TenantSenderEnforcementTest}.
 *
 * <p>When a tenant has no {@code smtpSender} configured, {@code EmailService} uses the sender from
 * the request verbatim. That is documented and intentional, but it means an authenticated caller
 * for such a tenant chooses its own {@code From:} — over the operator's relay if the tenant also
 * inherits the global SMTP settings. Only the enforcing branch was covered before, so this
 * behaviour could have changed in either direction unnoticed.
 *
 * <p>If a sender allowlist or a mandatory {@code smtpSender} is introduced later, this test is the
 * one that must be updated deliberately.
 */
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "tenants.config[0].id=default",
                "tenants.config[0].display-name=Default"
                // deliberately no smtp-sender
        }
)
class TenantSenderFallbackTest extends AbstractTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void withoutTenantSenderTheRequestSenderIsUsed() throws Exception {
        try {
            restTemplate.delete(getMailpitApiUrl() + "/api/v1/messages");
            Thread.sleep(500);
        } catch (Exception ignored) {
        }

        HashMap<String, JsonNode> model = new HashMap<>();
        model.put("value", JsonNodeFactory.instance.stringNode("test"));

        EmailInlineTemplateRequestDTO emailRequest = EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of("recipient@test.com"))
                .sender("caller-provided@test.com")
                .subject("Tenant Sender Fallback Test")
                .model(model)
                .emailTemplate("Sender fallback test body: ${value}")
                .build();

        post("/v1/email/inline", emailRequest, status().is2xxSuccessful());
        Thread.sleep(2000);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                getMailpitApiUrl() + "/api/v1/messages", HttpMethod.GET, null, JsonNode.class);

        assertNotNull(response.getBody());
        JsonNode messages = response.getBody().get("messages");
        assertNotNull(messages);
        assertFalse(messages.isEmpty(), "No emails found in Mailpit");

        boolean emailFound = false;
        for (JsonNode message : messages) {
            JsonNode subjectNode = message.get("Subject");
            if (subjectNode == null || !"Tenant Sender Fallback Test".equals(subjectNode.asString())) continue;

            emailFound = true;
            JsonNode fromNode = message.get("From");
            assertNotNull(fromNode, "From field must be present");
            String from = fromNode.get("Address").asString();
            assertTrue(from.contains("caller-provided@test.com"),
                    "Without a tenant sender the request sender is authoritative, but From was: " + from);
            break;
        }

        assertTrue(emailFound, "Email 'Tenant Sender Fallback Test' not found in Mailpit");
    }
}
