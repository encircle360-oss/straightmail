package com.encircle360.oss.straightmail;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.encircle360.oss.straightmail.dto.template.CreateUpdateTemplateDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class TemplateTest {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mock;

    @Test
    public void test_create_template() throws Exception {
        CreateUpdateTemplateDTO createUpdateTemplateDTO = CreateUpdateTemplateDTO.builder().build();

        mock.perform(MockMvcRequestBuilders.post("/templates")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(createUpdateTemplateDTO)))
            .andExpect(status().isBadRequest());

        createUpdateTemplateDTO = CreateUpdateTemplateDTO
            .builder()
            .name("test")
            .content("test")
            .build();

        Assertions.assertNotNull(createUpdateTemplateDTO.getName());
        Assertions.assertNotNull(createUpdateTemplateDTO.getContent());

        mock.perform(MockMvcRequestBuilders.post("/templates")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(mapper.writeValueAsString(createUpdateTemplateDTO)))
            .andExpect(status().isCreated());

    }
}
