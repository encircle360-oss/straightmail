package com.encircle360.oss.straightmail;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.encircle360.oss.straightmail.dto.EmailInlineTemplateRequestDTO;
import com.encircle360.oss.straightmail.dto.EmailTemplateFileRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

@SpringBootTest
@AutoConfigureMockMvc
public class StraightmailApplicationTests {

    @Autowired
    private MockMvc mock;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void basicRequest() throws Exception {
        mock.perform(MockMvcRequestBuilders.post("/"))
            .andExpect(status().is4xxClientError());

        mock.perform(MockMvcRequestBuilders.post("/")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(EmailTemplateFileRequestDTO.builder().build())))
            .andExpect(status().is4xxClientError());

        JsonNodeFactory nodeFactory = JsonNodeFactory.instance;
        HashMap<String, JsonNode> model = new HashMap<>();
        model.put("test", nodeFactory.textNode("test"));

        String body = objectMapper.writeValueAsString(EmailTemplateFileRequestDTO.builder()
            .recipients(List.of("test@encircle360.com"))
            .sender("test@encircle360.com")
            .emailTemplateId("test")
            .model(model)
            .build()
        );

        mock.perform(MockMvcRequestBuilders.post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void jsonNodeModel() throws Exception {
        mock.perform(MockMvcRequestBuilders.post("/"))
            .andExpect(status().is4xxClientError());

        mock.perform(MockMvcRequestBuilders.post("/")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(EmailTemplateFileRequestDTO.builder().build())))
            .andExpect(status().is4xxClientError());

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

        String body = objectMapper.writeValueAsString(
            EmailTemplateFileRequestDTO.builder()
                .recipients(List.of("test@encircle360.com"))
                .sender("test@encircle360.com")
                .model(testMap)
                .emailTemplateId("test_json_node")
                .locale("de")
                .build()
        );

        mock.perform(MockMvcRequestBuilders.post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void inlineTemplateTest() throws Exception {
        String[] arguments = {};
        StraightmailApplication.main(arguments);

        mock.perform(MockMvcRequestBuilders.post("/"))
            .andExpect(status().is4xxClientError());

        mock.perform(MockMvcRequestBuilders.post("/")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(EmailTemplateFileRequestDTO.builder().build())))
            .andExpect(status().is4xxClientError());

        HashMap<String, JsonNode> testMap = new HashMap<>();
        testMap.put("test", JsonNodeFactory.instance.numberNode(200.8));
        String body = objectMapper.writeValueAsString(
            EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of("test@encircle360.com"))
                .sender("test@encircle360.com")
                .subject("test mail")
                .model(testMap)
                .emailTemplate("${test!\"\"}")
                .locale("de")
                .build()
        );

        mock.perform(MockMvcRequestBuilders.post("/inline")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void testEmailValidation() throws Exception {
        HashMap<String, JsonNode> testMap = new HashMap<>();
        testMap.put("test", JsonNodeFactory.instance.numberNode(200.8));
        String body = objectMapper.writeValueAsString(
            EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of("test@encircle360.com"))
                .sender("tes1 t@encircle360.com")
                .subject("test mail")
                .model(testMap)
                .emailTemplate("${test!\"\"}")
                .locale("de")
                .build()
        );

        mock.perform(MockMvcRequestBuilders.post("/inline")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().is4xxClientError());

        body = objectMapper.writeValueAsString(
            EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of("tes t@encircle360.com"))
                .sender("tes1t@encircle360.com")
                .subject("test mail")
                .model(testMap)
                .emailTemplate("${test!\"\"}")
                .locale("de")
                .build()
        );

        mock.perform(MockMvcRequestBuilders.post("/inline")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().is4xxClientError());

        body = objectMapper.writeValueAsString(
            EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of("tes t@encircle360.com"))
                .cc(List.of("tes t@encircle360.com"))
                .sender("tes1t@encircle360.com")
                .subject("test mail")
                .model(testMap)
                .emailTemplate("${test!\"\"}")
                .locale("de")
                .build()
        );

        mock.perform(MockMvcRequestBuilders.post("/inline")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().is4xxClientError());

        body = objectMapper.writeValueAsString(
            EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of("tes t@encircle360.com"))
                .bcc(List.of("tes t@encircle360.com"))
                .sender("tes1t@encircle360.com")
                .subject("test mail")
                .model(testMap)
                .emailTemplate("${test!\"\"}")
                .locale("de")
                .build()
        );

        mock.perform(MockMvcRequestBuilders.post("/inline")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().is4xxClientError());

        body = objectMapper.writeValueAsString(
            EmailInlineTemplateRequestDTO.builder()
                .recipients(List.of("test@encircle360ö.com"))
                .cc(List.of("test@encircle360ö.berlin"))
                .bcc(List.of("test@encircle360ö.cloud"))
                .sender("tes1t@encircle360ö.com")
                .subject("test mail")
                .model(testMap)
                .emailTemplate("${test!\"\"}")
                .locale("de")
                .build()
        );

        mock.perform(MockMvcRequestBuilders.post("/inline")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().is2xxSuccessful());
    }
}
