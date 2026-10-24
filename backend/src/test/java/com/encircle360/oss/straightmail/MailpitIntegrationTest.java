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
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test that verifies email sending with Mailpit.
 * This test sends emails and then queries the Mailpit API to verify they were received.
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class MailpitIntegrationTest extends AbstractTest {

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    void shouldSendEmailAndVerifyInMailpit() throws Exception {
        // Arrange — clear Mailpit first
        try {
            restTemplate.delete(getMailpitApiUrl() + "/api/v1/messages");
            Thread.sleep(500);
        } catch (Exception e) {
            // Ignore if delete fails (e.g., no messages to delete)
        }

        String recipient = "recipient@test.com";
        String sender = "sender@test.com";
        String subject = "Test Email from Integration Test";
        String emailBody = "Hello, this is a test email with value: ${testValue}";

        HashMap<String, JsonNode> model = new HashMap<>();
        model.put("testValue", JsonNodeFactory.instance.stringNode("Integration Test Value"));

        EmailInlineTemplateRequestDTO emailRequest = EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of(recipient))
                .sender(sender)
                .subject(subject)
                .model(model)
                .emailTemplate(emailBody)
                .build();

        // Act — send email through the application
        post("/v1/email/inline", emailRequest, status().is2xxSuccessful());

        // Wait for email to be processed
        Thread.sleep(2000);

        // Assert — verify email was received in Mailpit
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                getMailpitApiUrl() + "/api/v1/messages",
                HttpMethod.GET,
                null,
                JsonNode.class
        );

        assertNotNull(response.getBody());
        JsonNode messages = response.getBody().get("messages");
        assertNotNull(messages);
        assertTrue(messages.isArray());
        assertFalse(messages.isEmpty(), "No emails found in Mailpit");

        // Find our test email
        boolean emailFound = false;
        for (JsonNode message : messages) {
            JsonNode subjectNode = message.get("Subject");
            if (subjectNode != null && subject.equals(subjectNode.asString())) {
                emailFound = true;

                // Verify recipient
                JsonNode toArray = message.get("To");
                assertNotNull(toArray);
                assertTrue(toArray.isArray());
                assertTrue(toArray.get(0).get("Address").asString().contains(recipient));

                // Verify sender
                JsonNode fromNode = message.get("From");
                assertNotNull(fromNode);
                assertTrue(fromNode.get("Address").asString().contains(sender));

                break;
            }
        }

        assertTrue(emailFound, "Email with subject '" + subject + "' not found in Mailpit");
    }

    @Test
    void shouldSendMultipleEmailsAndVerifyCount() throws Exception {
        // Arrange — clear Mailpit first
        try {
            restTemplate.delete(getMailpitApiUrl() + "/api/v1/messages");
            Thread.sleep(500);
        } catch (Exception e) {
            // Ignore if delete fails
        }

        // Act — send 3 emails
        for (int i = 0; i < 3; i++) {
            HashMap<String, JsonNode> model = new HashMap<>();
            model.put("index", JsonNodeFactory.instance.numberNode(i));

            EmailInlineTemplateRequestDTO emailRequest = EmailInlineTemplateRequestDTO.builder()
                    .recipients(List.of("recipient" + i + "@test.com"))
                    .sender("sender@test.com")
                    .subject("Test Email " + i)
                    .model(model)
                    .emailTemplate("Email number: ${index}")
                    .build();

            post("/v1/email/inline", emailRequest, status().is2xxSuccessful());
        }

        // Wait for emails to be processed
        Thread.sleep(2000);

        // Assert — verify 3 emails were received
        ResponseEntity<JsonNode> finalResponse = restTemplate.exchange(
                getMailpitApiUrl() + "/api/v1/messages",
                HttpMethod.GET,
                null,
                JsonNode.class
        );
        int finalCount = Objects.requireNonNull(finalResponse.getBody()).get("messages").size();

        assertEquals(3, finalCount, "Expected 3 emails in Mailpit");
    }

    @Test
    void shouldClearMailpitMessages() throws Exception {
        // Arrange — send an email first
        HashMap<String, JsonNode> model = new HashMap<>();
        model.put("test", JsonNodeFactory.instance.stringNode("value"));

        EmailInlineTemplateRequestDTO emailRequest = EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of("test@test.com"))
                .sender("sender@test.com")
                .subject("Email to be deleted")
                .model(model)
                .emailTemplate("This email will be deleted")
                .build();

        post("/v1/email/inline", emailRequest, status().is2xxSuccessful());
        Thread.sleep(1000);

        // Act — delete all messages
        restTemplate.delete(getMailpitApiUrl() + "/api/v1/messages");
        Thread.sleep(500);

        // Assert — verify no messages remain
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                getMailpitApiUrl() + "/api/v1/messages",
                HttpMethod.GET,
                null,
                JsonNode.class
        );

        JsonNode messages = Objects.requireNonNull(response.getBody()).get("messages");
        assertNotNull(messages);
        assertEquals(0, messages.size(), "Mailpit should have no messages after deletion");
    }
}
