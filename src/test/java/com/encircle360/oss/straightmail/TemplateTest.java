package com.encircle360.oss.straightmail;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.encircle360.oss.straightmail.config.MongoDbConfig;
import com.encircle360.oss.straightmail.dto.PageContainer;
import com.encircle360.oss.straightmail.dto.template.CreateUpdateTemplateDTO;
import com.encircle360.oss.straightmail.dto.template.TemplateDTO;
import com.encircle360.oss.straightmail.repository.TemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles(MongoDbConfig.PROFILE)
public class TemplateTest extends AbstractTest {

    @Autowired TemplateRepository templateRepository;

    @BeforeEach
    public void clean() {
        templateRepository.deleteAll();
    }

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
            .html("test")
            .tags(List.of("test", "foo", "bar"))
            .build();

        Assertions.assertNotNull(createUpdateTemplateDTO.getName());
        Assertions.assertNotNull(createUpdateTemplateDTO.getHtml());

        MvcResult result = post("/templates", createUpdateTemplateDTO, status().isCreated());
        TemplateDTO templateDTO = resultToObject(result, TemplateDTO.class);

        Assertions.assertNotNull(templateDTO);
        Assertions.assertNotNull(templateDTO.getId());
        Assertions.assertNotNull(templateDTO.getName());
        Assertions.assertNotNull(templateDTO.getHtml());

        result = get("/templates/" + templateDTO.getId(), status().isOk());

        templateDTO = resultToObject(result, TemplateDTO.class);

        Assertions.assertNotNull(templateDTO);
        Assertions.assertNotNull(templateDTO.getId());
        Assertions.assertNotNull(templateDTO.getName());
        Assertions.assertNotNull(templateDTO.getHtml());

        MvcResult listResult = get("/templates?tag=foo&tag=bar", status().isOk());
        PageContainer<TemplateDTO> templateDTOPageContainer = mapper.readValue(listResult.getResponse().getContentAsString(), new TypeReference<PageContainer<TemplateDTO>>() {});
        Assertions.assertNotNull(templateDTOPageContainer);
        Assertions.assertEquals(1, templateDTOPageContainer.getTotalElements());

        delete("/templates/" + templateDTO.getId(), status().isNoContent());
        get("/templates/" + templateDTO.getId(), status().isNotFound());
    }
}
