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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StraightmailApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles({"database", "test"})
class TemplateDatabaseTest extends AbstractTest {

    @Autowired
    TemplateRepository templateRepository;

    @BeforeEach
    void clean() {
        templateRepository.deleteAll();
    }

    @Test
    void test_pagination_is_db_side() throws Exception {
        for (int i = 0; i < 5; i++) {
            CreateUpdateTemplateDTO dto = CreateUpdateTemplateDTO.builder()
                    .name("template-" + i)
                    .html("<p>content</p>")
                    .tags(List.of("tag" + i))
                    .build();
            post("/v1/templates", dto, status().isCreated());
        }

        MvcResult result = get("/v1/templates?size=2&page=0", status().isOk());
        PageContainer<TemplateDTO> page = mapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });

        assertEquals(5, page.getTotalElements());
        assertEquals(2, page.getContent().size());
    }

    @Test
    void test_tag_filter_pagination() throws Exception {
        for (int i = 0; i < 3; i++) {
            CreateUpdateTemplateDTO dto = CreateUpdateTemplateDTO.builder()
                    .name("shared-" + i)
                    .html("<p>x</p>")
                    .tags(List.of("shared", "extra" + i))
                    .build();
            post("/v1/templates", dto, status().isCreated());
        }
        post("/v1/templates", CreateUpdateTemplateDTO.builder()
                        .name("unrelated").html("<p>x</p>").tags(List.of("other")).build(),
                status().isCreated());

        MvcResult result = get("/v1/templates?tag=shared&size=2&page=0", status().isOk());
        PageContainer<TemplateDTO> page = mapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });

        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getContent().size());

        result = get("/v1/templates?tag=shared&size=2&page=1", status().isOk());
        page = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
        });
        assertEquals(1, page.getContent().size());
    }

    @Test
    void test_id_is_valid_uuid_string() throws Exception {
        CreateUpdateTemplateDTO dto = CreateUpdateTemplateDTO.builder()
                .name("uuid-test")
                .html("<p>test</p>")
                .tags(List.of("a"))
                .build();

        MvcResult result = post("/v1/templates", dto, status().isCreated());
        TemplateDTO templateDTO = resultToObject(result, TemplateDTO.class);

        assertNotNull(templateDTO.getId());
        UUID parsed = UUID.fromString(templateDTO.getId());
        assertNotNull(parsed);

        MvcResult getResult = get("/v1/templates/" + templateDTO.getId(), status().isOk());
        TemplateDTO fetched = resultToObject(getResult, TemplateDTO.class);
        assertEquals(templateDTO.getId(), fetched.getId());
    }
}
