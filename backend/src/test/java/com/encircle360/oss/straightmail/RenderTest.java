package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.dto.template.CreateUpdateTemplateDTO;
import com.encircle360.oss.straightmail.dto.template.RenderedTemplateDTO;
import com.encircle360.oss.straightmail.dto.template.TemplateDTO;
import com.encircle360.oss.straightmail.dto.template.TemplateRenderRequestDTO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StraightmailApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles({"database", "test"})
class RenderTest extends AbstractTest {

    @Test
    void testRender() throws Exception {
        CreateUpdateTemplateDTO createUpdateTemplateDTO = CreateUpdateTemplateDTO
                .builder()
                .name("lululu")
                .html("<b>I'm ${foo}</b>")
                .plain("I'm ${foo}")
                .locale("de")
                .build();
        MvcResult result = post("/v1/templates", createUpdateTemplateDTO, status().isCreated());
        TemplateDTO templateDTO = resultToObject(result, TemplateDTO.class);

        HashMap<String, JsonNode> nodeHashMap = new HashMap<>();
        nodeHashMap.put("foo", JsonNodeFactory.instance.stringNode("bar"));

        TemplateRenderRequestDTO templateRenderRequestDTO = TemplateRenderRequestDTO
                .builder()
                .templateId(templateDTO.getId())
                .model(nodeHashMap)
                .build();
        MvcResult renderResult = post("/v1/render", templateRenderRequestDTO, status().isOk());
        RenderedTemplateDTO rendered = resultToObject(renderResult, RenderedTemplateDTO.class);

        Assertions.assertEquals("<b>I'm bar</b>", rendered.getHtml());
        Assertions.assertEquals("I'm bar", rendered.getPlain());
    }

    @Test
    void testRenderTemplateError() throws Exception {
        CreateUpdateTemplateDTO createUpdateTemplateDTO = CreateUpdateTemplateDTO
                .builder()
                .name("broken-template")
                .html("<b>${missingVar</b>")  // invalid FreeMarker syntax
                .plain("${missingVar")
                .locale("de")
                .build();
        MvcResult result = post("/v1/templates", createUpdateTemplateDTO, status().isCreated());
        TemplateDTO templateDTO = resultToObject(result, TemplateDTO.class);

        TemplateRenderRequestDTO renderRequest = TemplateRenderRequestDTO
                .builder()
                .templateId(templateDTO.getId())
                .build();
        MvcResult renderResult = post("/v1/render", renderRequest, status().isUnprocessableContent());

        Map<?, ?> errorBody = resultToObject(renderResult, Map.class);
        Assertions.assertNotNull(errorBody.get("message"), "Error response must contain a 'message' field");
        Assertions.assertFalse(errorBody.get("message").toString().isBlank(), "Error message must not be blank");
    }
}
