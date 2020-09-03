package com.encircle360.oss.straightmail;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.encircle360.oss.straightmail.dto.EmailInlineTemplateRequestDTO;
import com.encircle360.oss.straightmail.dto.EmailTemplateDTO;
import com.encircle360.oss.straightmail.dto.EmailTemplateFileRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

@SpringBootTest
@AutoConfigureMockMvc
@RunWith(SpringRunner.class)
public class StraightmailApplicationTests {

    @Autowired
    private MockMvc mock;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void contextLoads() throws Exception {
        String[] arguments = {};
        StraightmailApplication.main(arguments);

        mock.perform(MockMvcRequestBuilders.post("/"))
            .andExpect(status().is4xxClientError());

        mock.perform(MockMvcRequestBuilders.post("/")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(objectMapper.writeValueAsString(EmailTemplateFileRequestDTO.builder().build())))
            .andExpect(status().is4xxClientError());

        String body = objectMapper.writeValueAsString(EmailInlineTemplateRequestDTO.builder()
            .recipients(List.of("test@encircle360.com"))
            .sender("test@encircle360.com")
            .subject("test mail")
            .build()
        );

        mock.perform(MockMvcRequestBuilders.post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void jsonNodeModel() throws Exception {
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
        String body = objectMapper.writeValueAsString(EmailTemplateFileRequestDTO.builder()
            .recipients(List.of("test@encircle360.com"))
            .sender("test@encircle360.com")
            .subject("test mail")
            .model(testMap)
            .emailTemplate(EmailTemplateDTO
                .builder()
                .locale("de")
                .id("test_template")
                .build()
            )
            .build()
        );

        mock.perform(MockMvcRequestBuilders.post("/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
            .andExpect(status().is2xxSuccessful());
    }
}
