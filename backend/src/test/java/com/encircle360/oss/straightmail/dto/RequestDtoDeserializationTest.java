package com.encircle360.oss.straightmail.dto;

import com.encircle360.oss.straightmail.AbstractTest;
import com.encircle360.oss.straightmail.TestApplication;
import com.encircle360.oss.straightmail.dto.email.EmailInlineTemplateRequestDTO;
import com.encircle360.oss.straightmail.dto.email.EmailTemplateFileRequestDTO;
import com.encircle360.oss.straightmail.dto.template.CreateUpdateTemplateDTO;
import com.encircle360.oss.straightmail.dto.template.TemplateRenderRequestDTO;
import com.encircle360.oss.straightmail.dto.tenant.CreateUpdateTenantDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins that every request body survives a payload that leaves optional properties out.
 *
 * <p>Under Jackson 3 an absent property reaches a constructor-based creator as {@code null}, and
 * {@code FAIL_ON_NULL_FOR_PRIMITIVES} (on by default, unlike in Jackson 2) then rejects the whole
 * request with 400. Any DTO whose own all-args constructor carries a primitive is affected, which
 * is why these tests deserialize the smallest legal payload for each request body.
 *
 * <p>Deliberately run against the application's own mapper rather than a fresh one, because the
 * behaviour under test is configured in {@link com.encircle360.oss.straightmail.config.JacksonConfig}.
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class RequestDtoDeserializationTest extends AbstractTest {

    @Test
    void tenant_minimalPayload_appliesDeclaredDefaults() {
        CreateUpdateTenantDTO dto = mapper.readValue(
                "{\"slug\":\"acme\",\"displayName\":\"Acme\"}", CreateUpdateTenantDTO.class);

        assertEquals("acme", dto.getSlug());
        assertEquals("Acme", dto.getDisplayName());
        assertFalse(dto.isSmtpTls());
        assertFalse(dto.isSmtpSsl());
        assertTrue(dto.isActive(), "active is declared as defaulting to true");
    }

    @Test
    void tenant_explicitValuesWin() {
        CreateUpdateTenantDTO dto = mapper.readValue(
                "{\"slug\":\"acme\",\"displayName\":\"Acme\",\"smtpTls\":true,\"active\":false}",
                CreateUpdateTenantDTO.class);

        assertTrue(dto.isSmtpTls());
        assertFalse(dto.isActive());
    }

    @Test
    void templateCreate_minimalPayload() {
        CreateUpdateTemplateDTO dto = mapper.readValue(
                "{\"name\":\"welcome\"}", CreateUpdateTemplateDTO.class);

        assertEquals("welcome", dto.getName());
        assertNull(dto.getLocale());
    }

    @Test
    void mailByTemplateId_minimalPayload() {
        EmailTemplateFileRequestDTO dto = mapper.readValue(
                "{\"recipients\":[\"a@b.de\"],\"emailTemplateId\":\"welcome\"}",
                EmailTemplateFileRequestDTO.class);

        assertEquals("welcome", dto.getEmailTemplateId());
        assertFalse(dto.isVerbose());
    }

    @Test
    void inlineMail_minimalPayload() {
        EmailInlineTemplateRequestDTO dto = mapper.readValue(
                "{\"recipients\":[\"a@b.de\"],\"emailTemplate\":\"hi\"}",
                EmailInlineTemplateRequestDTO.class);

        assertEquals("hi", dto.getEmailTemplate());
        assertFalse(dto.isVerbose());
    }

    @Test
    void render_minimalPayload() {
        TemplateRenderRequestDTO dto = mapper.readValue(
                "{\"templateId\":\"welcome\"}", TemplateRenderRequestDTO.class);

        assertEquals("welcome", dto.getTemplateId());
        assertNull(dto.getModel());
    }
}
