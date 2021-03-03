package com.encircle360.oss.straightmail;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.encircle360.oss.straightmail.dto.email.EmailInlineTemplateRequestDTO;
import com.encircle360.oss.straightmail.dto.email.EmailTemplateFileRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

@SpringBootTest
public class StraightmailApplicationTests extends AbstractTest {

    @Test
    public void basicRequest() throws Exception {
        emptyPost("/email", status().is4xxClientError());
        post("/email", EmailTemplateFileRequestDTO.builder().build(), status().isBadRequest());

        HashMap<String, JsonNode> testMap = new HashMap<>();
        testMap.put("test", JsonNodeFactory.instance.numberNode(200.8));

        EmailTemplateFileRequestDTO emailInlineTemplateRequestDTO = EmailTemplateFileRequestDTO.builder()
            .recipients(List.of("test@encircle360.com"))
            .sender("test@encircle360.com")
            .model(testMap)
            .emailTemplateId("test")
            .build();
        post("/email", emailInlineTemplateRequestDTO, status().is2xxSuccessful());
    }

    @Test
    public void jsonNodeModel() throws Exception {
        emptyPost("/email", status().is4xxClientError());
        post("/email", EmailTemplateFileRequestDTO.builder().build(), status().is4xxClientError());

        TestPojo testPojo = TestPojo
            .builder()
            .doubles(List.of(1d, 2d, 344.34, 32432.3))
            .booleans(List.of(true, false, true, false, false))
            .integers(List.of(1, 2, 34, 556456, 433))
            .strings(List.of("I'm", "a", "string", "list"))
            .singleString("I'm a string")
            .singleBoolean(true)
            .singleDouble(32434.44)
            .singleInteger(344)
            .build();

        HashMap<String, JsonNode> testMap = new HashMap<>();
        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        testMap.put("number", nodeFactory.numberNode(200.8));
        testMap.put("string", nodeFactory.textNode("I'm a string"));
        testMap.put("bool", nodeFactory.booleanNode(false));
        testMap.put("object", nodeFactory.pojoNode(testPojo));

        EmailTemplateFileRequestDTO emailTemplateFileRequestDTO = EmailTemplateFileRequestDTO.builder()
            .recipients(List.of("test@encircle360.com"))
            .sender("test@encircle360.com")
            .model(testMap)
            .emailTemplateId("test_json_node")
            .locale("de")
            .build();

        post("/email", emailTemplateFileRequestDTO, status().is2xxSuccessful());
    }

    @Test
    public void inlineTemplateTest() throws Exception {
        emptyPost("/email", status().is4xxClientError());
        post("/email", EmailTemplateFileRequestDTO.builder().build(), status().is4xxClientError());

        HashMap<String, JsonNode> testMap = new HashMap<>();
        testMap.put("test", JsonNodeFactory.instance.numberNode(200.8));

        EmailInlineTemplateRequestDTO emailInlineTemplateRequestDTO = EmailInlineTemplateRequestDTO.builder()
            .recipients(List.of("test@encircle360.com"))
            .sender("test@encircle360.com")
            .subject("test mail")
            .model(testMap)
            .emailTemplate("${test!\"\"}")
            .locale("de")
            .build();

        post("/email/inline", emailInlineTemplateRequestDTO, status().is2xxSuccessful());
    }

    @Test
    public void testEmailValidation() throws Exception {
        HashMap<String, JsonNode> testMap = new HashMap<>();
        testMap.put("test", JsonNodeFactory.instance.numberNode(200.8));
        EmailInlineTemplateRequestDTO emailInlineTemplateRequestDTO = EmailInlineTemplateRequestDTO.builder()
            .recipients(List.of("test@encircle360.com"))
            .sender("tes1 t@encircle360.com")
            .subject("test mail")
            .model(testMap)
            .emailTemplate("${test!\"\"}")
            .locale("de")
            .build();

        post("/email/inline", emailInlineTemplateRequestDTO, status().is4xxClientError());

        emailInlineTemplateRequestDTO = EmailInlineTemplateRequestDTO.builder()
            .recipients(List.of("tes t@encircle360.com"))
            .sender("tes1t@encircle360.com")
            .subject("test mail")
            .model(testMap)
            .emailTemplate("${test!\"\"}")
            .locale("de")
            .build();
        post("/email/inline", emailInlineTemplateRequestDTO, status().is4xxClientError());

        emailInlineTemplateRequestDTO = EmailInlineTemplateRequestDTO.builder()
            .recipients(List.of("tes t@encircle360.com"))
            .cc(List.of("tes t@encircle360.com"))
            .sender("tes1t@encircle360.com")
            .subject("test mail")
            .model(testMap)
            .emailTemplate("${test!\"\"}")
            .locale("de")
            .build();

        post("/email/inline", emailInlineTemplateRequestDTO, status().is4xxClientError());

        emailInlineTemplateRequestDTO =
            EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of("tes t@encircle360.com"))
                .bcc(List.of("tes t@encircle360.com"))
                .sender("tes1t@encircle360.com")
                .subject("test mail")
                .model(testMap)
                .emailTemplate("${test!\"\"}")
                .locale("de")
                .build();

        post("/email/inline", emailInlineTemplateRequestDTO, status().is4xxClientError());

        emailInlineTemplateRequestDTO = EmailInlineTemplateRequestDTO.builder()
            .recipients(List.of("test@encircle360ö.com"))
            .cc(List.of("test@encircle360ö.berlin"))
            .bcc(List.of("test@encircle360ö.cloud"))
            .sender("tes1t@encircle360ö.com")
            .subject("test mail")
            .model(testMap)
            .emailTemplate("${test!\"\"}")
            .locale("de")
            .build();
        post("/email/inline", emailInlineTemplateRequestDTO, status().is4xxClientError());

        emailInlineTemplateRequestDTO = EmailInlineTemplateRequestDTO.builder()
            .recipients(List.of("test@encircle360.com"))
            .cc(List.of("test@encircle360.berlin"))
            .bcc(List.of("test@encircle360.cloud"))
            .sender("tes1t@encircle360.com")
            .subject("test mail")
            .model(testMap)
            .emailTemplate("${test!\"\"}")
            .locale("de")
            .build();
        post("/email/inline", emailInlineTemplateRequestDTO, status().is2xxSuccessful());

    }
}
