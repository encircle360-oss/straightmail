package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.dto.email.EmailResultDTO;
import com.encircle360.oss.straightmail.dto.email.EmailTemplateFileRequestDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A 0.4.x deployment migrated as is: flat templates, {@code AUTH_MODE=none}, no database and no
 * tenant entries. {@code /v1/email} still serves the flat layout there.
 */
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "auth.mode=none",
                "tenants.config="
        }
)
class FlatTemplateLayoutTest extends AbstractTest {

    @Autowired
    private TenantProperties tenantProperties;

    @Test
    void withoutDeclaredTenants_flatTemplateIsSent() throws Exception {
        assertTrue(tenantProperties.getConfig().isEmpty(), "this test needs an empty tenants.config");

        HashMap<String, JsonNode> model = new HashMap<>();
        model.put("test", JsonNodeFactory.instance.numberNode(1));

        EmailTemplateFileRequestDTO request = EmailTemplateFileRequestDTO.builder()
                .recipients(List.of("recipient@test.com"))
                .sender("sender@test.com")
                .model(model)
                .emailTemplateId("test")
                .build();

        MvcResult result = mock.perform(MockMvcRequestBuilders.post("/v1/email")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertTrue(resultToObject(result, EmailResultDTO.class).isSuccess());
    }
}
