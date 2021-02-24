package com.encircle360.oss.straightmail;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.encircle360.oss.straightmail.config.MongoDbConfig;
import com.encircle360.oss.straightmail.dto.template.CreateUpdateTemplateDTO;
import com.encircle360.oss.straightmail.dto.template.TemplateDTO;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(MongoDbConfig.PROFILE)
public class TemplateTest extends AbstractTest {

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

        MvcResult result = post("/templates", createUpdateTemplateDTO, status().isCreated());
        TemplateDTO templateDTO = resultToObject(result, TemplateDTO.class);

        Assertions.assertNotNull(templateDTO);
        Assertions.assertNotNull(templateDTO.getId());
        Assertions.assertNotNull(templateDTO.getName());
        Assertions.assertNotNull(templateDTO.getContent());

        result = get("/templates/" + templateDTO.getId(), status().isOk());

        templateDTO = resultToObject(result, TemplateDTO.class);

        Assertions.assertNotNull(templateDTO);
        Assertions.assertNotNull(templateDTO.getId());
        Assertions.assertNotNull(templateDTO.getName());
        Assertions.assertNotNull(templateDTO.getContent());

        delete("/templates/" + templateDTO.getId(), status().isNoContent());
        get("/templates/" + templateDTO.getId(), status().isNotFound());
    }
}
