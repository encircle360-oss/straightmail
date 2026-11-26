package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.dto.email.EmailResultDTO;
import com.encircle360.oss.straightmail.dto.email.EmailTemplateFileRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Two tenants declared, their folders below the root the flat 0.4.x lookup also reads (the layout
 * the shipped Compose stacks use). A template ID that names another tenant's folder must be
 * "template not found" for the caller's tenant.
 *
 * <p>{@code /v1/email/inline} takes no template ID, so it has no counterpart here.
 */
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "tenants.config[0].id=tenant-a",
                "tenants.config[0].display-name=Tenant A",
                "tenants.config[1].id=tenant-b",
                "tenants.config[1].display-name=Tenant B"
        }
)
class TenantTemplateIsolationTest extends AbstractTest {

    private static final String TENANT_B_SUBJECT = "Template owned by tenant-b";

    private final RestTemplate restTemplate = new RestTemplate();

    @DynamicPropertySource
    static void templateRoot(DynamicPropertyRegistry registry) {
        registry.add("templates.file.base-path", TenantTemplateIsolationTest::classpathTemplateRoot);
    }

    private static String classpathTemplateRoot() {
        try {
            return new ClassPathResource("templates").getFile().getAbsolutePath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void templateOfAnotherTenant_isNotFound() throws Exception {
        MvcResult result = sendAsTenantA("tenant-b/private", status().isBadRequest());

        EmailResultDTO body = resultToObject(result, EmailResultDTO.class);
        assertFalse(body.isSuccess());
        assertEquals("Template not found", body.getMessage());
        assertFalse(mailpitSubjects().contains(TENANT_B_SUBJECT), "Tenant B's template must not be sent");
    }

    @Test
    void encodedTemplateIdOfAnotherTenant_isNotFound() throws Exception {
        MvcResult result = sendAsTenantA("tenant-b::private", status().isBadRequest());

        assertEquals("Template not found", resultToObject(result, EmailResultDTO.class).getMessage());
    }

    @Test
    void ownTemplate_isSent() throws Exception {
        MvcResult result = sendAsTenantA("welcome", status().is2xxSuccessful());

        assertTrue(resultToObject(result, EmailResultDTO.class).isSuccess());
        assertTrue(mailpitSubjects().contains("Welcome from tenant-a"));
    }

    private MvcResult sendAsTenantA(String templateId, ResultMatcher expected)
            throws Exception {
        HashMap<String, JsonNode> model = new HashMap<>();
        model.put("value", JsonNodeFactory.instance.stringNode("isolation"));

        EmailTemplateFileRequestDTO request = EmailTemplateFileRequestDTO.builder()
                .recipients(List.of("recipient@test.com"))
                .sender("sender@test.com")
                .model(model)
                .emailTemplateId(templateId)
                .build();

        return mock.perform(MockMvcRequestBuilders.post("/v1/email")
                        .header("X-API-KEY", "test-api-key")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(expected)
                .andReturn();
    }

    private List<String> mailpitSubjects() throws InterruptedException {
        Thread.sleep(1000);
        ResponseEntity<JsonNode> response = restTemplate.exchange(
                getMailpitApiUrl() + "/api/v1/messages", HttpMethod.GET, null, JsonNode.class);
        assertNotNull(response.getBody());
        JsonNode messages = response.getBody().get("messages");
        assertNotNull(messages);
        List<String> subjects = new ArrayList<>();
        for (JsonNode message : messages) {
            JsonNode subject = message.get("Subject");
            if (subject != null) {
                subjects.add(subject.asString());
            }
        }
        return subjects;
    }
}
