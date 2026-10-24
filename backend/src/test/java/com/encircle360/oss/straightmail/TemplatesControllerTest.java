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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StraightmailApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles({"database", "test"})
class TemplatesControllerTest extends AbstractTest {

    @Autowired
    private TemplateRepository templateRepository;

    @BeforeEach
    void clean() {
        templateRepository.deleteAll();
    }

    @Test
    void get_nonexistent_template_returns_404() throws Exception {
        get("/v1/templates/" + UUID.randomUUID(), status().isNotFound());
    }

    @Test
    void get_unknown_template_name_returns_404() throws Exception {
        get("/v1/templates/not-a-valid-uuid", status().isNotFound());
    }

    @Test
    void get_encoded_file_path_with_no_matching_template_returns_404() throws Exception {
        // "foo::bar" decodes to "foo/bar" and is resolved as a file/Git template name;
        // since no such template exists in the test environment, 404 is expected.
        get("/v1/templates/foo::bar", status().isNotFound());
    }

    @Test
    void delete_nonexistent_template_returns_404() throws Exception {
        delete("/v1/templates/" + UUID.randomUUID(), status().isNotFound());
    }

    @Test
    void update_nonexistent_template_returns_404() throws Exception {
        CreateUpdateTemplateDTO dto = CreateUpdateTemplateDTO.builder()
                .name("updated")
                .html("<p>updated</p>")
                .build();
        put("/v1/templates/" + UUID.randomUUID(), dto, status().isNotFound());
    }

    @Test
    void create_template_without_required_fields_returns_400() throws Exception {
        CreateUpdateTemplateDTO dto = CreateUpdateTemplateDTO.builder().build();
        post("/v1/templates", dto, status().isBadRequest());
    }

    @Test
    void create_template_without_html_returns_400() throws Exception {
        CreateUpdateTemplateDTO dto = CreateUpdateTemplateDTO.builder()
                .name("valid-name")
                .build();
        post("/v1/templates", dto, status().isBadRequest());
    }

    @Test
    void update_existing_template_returns_updated_data() throws Exception {
        CreateUpdateTemplateDTO createDto = CreateUpdateTemplateDTO.builder()
                .name("original-name")
                .html("<p>original</p>")
                .build();
        MvcResult createResult = post("/v1/templates", createDto, status().isCreated());
        TemplateDTO created = resultToObject(createResult, TemplateDTO.class);

        CreateUpdateTemplateDTO updateDto = CreateUpdateTemplateDTO.builder()
                .name("updated-name")
                .html("<p>updated</p>")
                .subject("Updated Subject")
                .build();
        MvcResult updateResult = put("/v1/templates/" + created.getId(), updateDto, status().isOk());
        TemplateDTO updated = resultToObject(updateResult, TemplateDTO.class);

        assertEquals("updated-name", updated.getName());
        assertEquals("<p>updated</p>", updated.getHtml());
        assertEquals("Updated Subject", updated.getSubject());
        assertEquals(created.getId(), updated.getId());
    }

    @Test
    void list_templates_with_pagination() throws Exception {
        for (int i = 0; i < 5; i++) {
            CreateUpdateTemplateDTO dto = CreateUpdateTemplateDTO.builder()
                    .name("paged-template-" + i)
                    .html("<p>" + i + "</p>")
                    .build();
            post("/v1/templates", dto, status().isCreated());
        }

        MvcResult result = get("/v1/templates?page=0&size=2", status().isOk());
        PageContainer<TemplateDTO> page = mapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });

        assertEquals(2, page.getContent().size());
        assertEquals(5, page.getTotalElements());
    }

    @Test
    void list_templates_page_1_returns_remaining_items() throws Exception {
        for (int i = 0; i < 5; i++) {
            CreateUpdateTemplateDTO dto = CreateUpdateTemplateDTO.builder()
                    .name("paged-template-" + i)
                    .html("<p>" + i + "</p>")
                    .build();
            post("/v1/templates", dto, status().isCreated());
        }

        MvcResult result = get("/v1/templates?page=1&size=3", status().isOk());
        PageContainer<TemplateDTO> page = mapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });

        assertEquals(2, page.getContent().size(), "Page 1 with size 3 should return 2 remaining items");
        assertEquals(5, page.getTotalElements());
    }

    @Test
    void list_templates_empty_returns_empty_page() throws Exception {
        MvcResult result = get("/v1/templates", status().isOk());
        PageContainer<TemplateDTO> page = mapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });

        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
    }

    @Test
    void list_templates_with_tag_filter_returns_matching_only() throws Exception {
        CreateUpdateTemplateDTO withTag = CreateUpdateTemplateDTO.builder()
                .name("tagged-template")
                .html("<p>tagged</p>")
                .tags(List.of("promo", "newsletter"))
                .build();
        post("/v1/templates", withTag, status().isCreated());

        CreateUpdateTemplateDTO withoutTag = CreateUpdateTemplateDTO.builder()
                .name("untagged-template")
                .html("<p>untagged</p>")
                .build();
        post("/v1/templates", withoutTag, status().isCreated());

        MvcResult result = get("/v1/templates?tag=promo&tag=newsletter", status().isOk());
        PageContainer<TemplateDTO> page = mapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });

        assertEquals(1, page.getTotalElements());
        assertEquals("tagged-template", page.getContent().getFirst().getName());
    }

    @Test
    void list_templates_tag_filter_requires_all_tags() throws Exception {
        CreateUpdateTemplateDTO dto = CreateUpdateTemplateDTO.builder()
                .name("partial-tags")
                .html("<p>partial</p>")
                .tags(List.of("foo"))
                .build();
        post("/v1/templates", dto, status().isCreated());

        // Request both "foo" and "bar" — template only has "foo"
        MvcResult result = get("/v1/templates?tag=foo&tag=bar", status().isOk());
        PageContainer<TemplateDTO> page = mapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });

        assertEquals(0, page.getTotalElements());
    }

    @Test
    void create_and_get_template_returns_same_data() throws Exception {
        CreateUpdateTemplateDTO dto = CreateUpdateTemplateDTO.builder()
                .name("roundtrip-template")
                .html("<h1>Hello</h1>")
                .subject("Hello Subject")
                .plain("Hello")
                .locale("en")
                .tags(List.of("test"))
                .build();

        MvcResult createResult = post("/v1/templates", dto, status().isCreated());
        TemplateDTO created = resultToObject(createResult, TemplateDTO.class);

        MvcResult getResult = get("/v1/templates/" + created.getId(), status().isOk());
        TemplateDTO retrieved = resultToObject(getResult, TemplateDTO.class);

        assertEquals(created.getId(), retrieved.getId());
        assertEquals("roundtrip-template", retrieved.getName());
        assertEquals("<h1>Hello</h1>", retrieved.getHtml());
        assertEquals("Hello Subject", retrieved.getSubject());
    }

    @Test
    void delete_template_removes_it() throws Exception {
        CreateUpdateTemplateDTO dto = CreateUpdateTemplateDTO.builder()
                .name("to-delete")
                .html("<p>bye</p>")
                .build();
        MvcResult createResult = post("/v1/templates", dto, status().isCreated());
        TemplateDTO created = resultToObject(createResult, TemplateDTO.class);

        delete("/v1/templates/" + created.getId(), status().isNoContent());
        get("/v1/templates/" + created.getId(), status().isNotFound());
    }
}
