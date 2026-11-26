package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.dto.PageContainer;
import com.encircle360.oss.straightmail.dto.template.CreateUpdateTemplateDTO;
import com.encircle360.oss.straightmail.dto.template.TemplateDTO;
import com.encircle360.oss.straightmail.repository.TemplateRepository;
import tools.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StraightmailApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles({"database", "test"})
class TemplateTest extends AbstractTest {

    @Autowired
    TemplateRepository templateRepository;

    @BeforeEach
    void clean() {
        templateRepository.deleteAll();
    }

    @Test
    void test_create_template() throws Exception {
        CreateUpdateTemplateDTO createUpdateTemplateDTO = CreateUpdateTemplateDTO.builder().build();

        mock.perform(withAuth(MockMvcRequestBuilders.post("/v1/templates"))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(createUpdateTemplateDTO)))
                .andExpect(status().isBadRequest());

        createUpdateTemplateDTO = CreateUpdateTemplateDTO
                .builder()
                .name("test")
                .html("test")
                .tags(List.of("test", "foo", "bar"))
                .build();

        assertNotNull(createUpdateTemplateDTO.getName());
        assertNotNull(createUpdateTemplateDTO.getHtml());

        MvcResult result = post("/v1/templates", createUpdateTemplateDTO, status().isCreated());
        TemplateDTO templateDTO = resultToObject(result, TemplateDTO.class);

        assertNotNull(templateDTO);
        assertNotNull(templateDTO.getId());
        assertNotNull(templateDTO.getName());
        assertNotNull(templateDTO.getHtml());

        result = get("/v1/templates/" + templateDTO.getId(), status().isOk());

        templateDTO = resultToObject(result, TemplateDTO.class);

        assertNotNull(templateDTO);
        assertNotNull(templateDTO.getId());
        assertNotNull(templateDTO.getName());
        assertNotNull(templateDTO.getHtml());

        MvcResult listResult = get("/v1/templates?tag=foo&tag=bar", status().isOk());
        PageContainer<TemplateDTO> templateDTOPageContainer = mapper.readValue(listResult.getResponse().getContentAsString(), new TypeReference<>() {
        });
        assertNotNull(templateDTOPageContainer);
        assertEquals(1, templateDTOPageContainer.getTotalElements());

        listResult = get("/v1/templates?tag=foo&tag=baz", status().isOk());
        templateDTOPageContainer = mapper.readValue(listResult.getResponse().getContentAsString(), new TypeReference<>() {
        });
        assertNotNull(templateDTOPageContainer);
        assertEquals(0, templateDTOPageContainer.getTotalElements());

        delete("/v1/templates/" + templateDTO.getId(), status().isNoContent());
        get("/v1/templates/" + templateDTO.getId(), status().isNotFound());
    }
}
