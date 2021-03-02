package com.encircle360.oss.straightmail;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;

import com.encircle360.oss.straightmail.config.MongoDbConfig;
import com.encircle360.oss.straightmail.dto.template.CreateUpdateTemplateDTO;
import com.encircle360.oss.straightmail.dto.template.RenderedTemplateDTO;
import com.encircle360.oss.straightmail.dto.template.TemplateDTO;
import com.encircle360.oss.straightmail.dto.template.TemplateRenderRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

@SpringBootTest
@ActiveProfiles(MongoDbConfig.PROFILE)
public class RenderTest extends AbstractTest {

    @Test
    public void testRender() throws Exception {
        CreateUpdateTemplateDTO createUpdateTemplateDTO = CreateUpdateTemplateDTO
            .builder()
            .name("lululu")
            .html("<b>I'm ${foo}</b>")
            .plain("I'm ${foo}")
            .locale("de")
            .build();
        MvcResult result = post("/templates", createUpdateTemplateDTO, status().isCreated());
        TemplateDTO templateDTO = resultToObject(result, TemplateDTO.class);

        HashMap<String, JsonNode> nodeHashMap = new HashMap<>();
        nodeHashMap.put("foo", new TextNode("bar"));

        TemplateRenderRequestDTO templateRenderRequestDTO = TemplateRenderRequestDTO
            .builder()
            .templateId(templateDTO.getId())
            .model(nodeHashMap)
            .build();
        MvcResult renderResult = post("/render", templateRenderRequestDTO, status().isOk());
        RenderedTemplateDTO rendered = resultToObject(renderResult, RenderedTemplateDTO.class);

        Assertions.assertEquals("<b>I'm bar</b>", rendered.getHtml());
        Assertions.assertEquals("I'm bar", rendered.getPlain());
    }
}
