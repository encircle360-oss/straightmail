package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.dto.email.EmailInlineTemplateRequestDTO;
import com.encircle360.oss.straightmail.dto.email.EmailTemplateFileRequestDTO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class StraightmailApplicationTests extends AbstractTest {

    @Test
    void basicRequest() throws Exception {
        emptyPost("/v1/email", status().is4xxClientError());
        super.post("/v1/email", EmailTemplateFileRequestDTO.builder().build(), status().isBadRequest());

        HashMap<String, JsonNode> testMap = new HashMap<>();
        testMap.put("test", JsonNodeFactory.instance.numberNode(200.8));

        EmailTemplateFileRequestDTO emailInlineTemplateRequestDTO = EmailTemplateFileRequestDTO.builder()
                .recipients(List.of("test@encircle360.com"))
                .sender("test@encircle360.com")
                .model(testMap)
                .emailTemplateId("test")
                .build();
        super.post("/v1/email", emailInlineTemplateRequestDTO, status().is2xxSuccessful());
    }

    @Test
    void jsonNodeModel() throws Exception {
        emptyPost("/v1/email", status().is4xxClientError());
        super.post("/v1/email", EmailTemplateFileRequestDTO.builder().build(), status().is4xxClientError());

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
        testMap.put("string", nodeFactory.stringNode("I'm a string"));
        testMap.put("bool", nodeFactory.booleanNode(false));
        testMap.put("object", nodeFactory.pojoNode(testPojo));

        EmailTemplateFileRequestDTO emailTemplateFileRequestDTO = EmailTemplateFileRequestDTO.builder()
                .recipients(List.of("test@encircle360.com"))
                .sender("test@encircle360.com")
                .model(testMap)
                .emailTemplateId("test_json_node")
                .locale("de")
                .build();

        super.post("/v1/email", emailTemplateFileRequestDTO, status().is2xxSuccessful());
    }

    @Test
    void inlineTemplateTest() throws Exception {
        emptyPost("/v1/email", status().is4xxClientError());
        super.post("/v1/email", EmailTemplateFileRequestDTO.builder().build(), status().is4xxClientError());

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

        super.post("/v1/email/inline", emailInlineTemplateRequestDTO, status().is2xxSuccessful());
    }

    @Test
    void testEmailValidation() throws Exception {
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

        super.post("/v1/email/inline", emailInlineTemplateRequestDTO, status().is4xxClientError());

        emailInlineTemplateRequestDTO = EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of("tes t@encircle360.com"))
                .sender("tes1t@encircle360.com")
                .subject("test mail")
                .model(testMap)
                .emailTemplate("${test!\"\"}")
                .locale("de")
                .build();
        super.post("/v1/email/inline", emailInlineTemplateRequestDTO, status().is4xxClientError());

        emailInlineTemplateRequestDTO = EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of("tes t@encircle360.com"))
                .cc(List.of("tes t@encircle360.com"))
                .sender("tes1t@encircle360.com")
                .subject("test mail")
                .model(testMap)
                .emailTemplate("${test!\"\"}")
                .locale("de")
                .build();

        super.post("/v1/email/inline", emailInlineTemplateRequestDTO, status().is4xxClientError());

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

        super.post("/v1/email/inline", emailInlineTemplateRequestDTO, status().is4xxClientError());

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
        super.post("/v1/email/inline", emailInlineTemplateRequestDTO, status().is4xxClientError());

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
        super.post("/v1/email/inline", emailInlineTemplateRequestDTO, status().is2xxSuccessful());

    }
}
