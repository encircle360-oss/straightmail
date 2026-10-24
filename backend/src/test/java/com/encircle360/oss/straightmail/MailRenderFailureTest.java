package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.dto.email.EmailInlineTemplateRequestDTO;
import com.encircle360.oss.straightmail.dto.email.EmailResultDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards the client-error contract for templates that cannot be rendered.
 *
 * <p>A FreeMarker syntax error or a missing model variable is a caller mistake, so it has to come
 * back as a 400 with an explanatory message — not as a 500, which would read as a server fault and
 * hide the cause from the caller.
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class MailRenderFailureTest extends AbstractTest {

    @Test
    void brokenTemplateSyntax_returnsBadRequest() throws Exception {
        EmailInlineTemplateRequestDTO request = EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of("recipient@test.com"))
                .sender("sender@test.com")
                .subject("Subject")
                // Unclosed interpolation — FreeMarker fails while parsing the body.
                .emailTemplate("Hello ${name")
                .build();

        MvcResult result = post("/v1/email/inline", request, status().isBadRequest());

        EmailResultDTO body = resultToObject(result, EmailResultDTO.class);
        assertFalse(body.isSuccess());
        assertEquals("Error while rendering template", body.getMessage());
    }

    @Test
    void missingModelVariable_returnsBadRequest() throws Exception {
        EmailInlineTemplateRequestDTO request = EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of("recipient@test.com"))
                .sender("sender@test.com")
                .subject("Subject")
                .emailTemplate("Hello ${missingVariable}")
                .build();

        MvcResult result = post("/v1/email/inline", request, status().isBadRequest());

        EmailResultDTO body = resultToObject(result, EmailResultDTO.class);
        assertFalse(body.isSuccess());
        assertEquals("Error while rendering template", body.getMessage());
    }
}
