package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.dto.email.EmailResultDTO;
import com.encircle360.oss.straightmail.dto.email.EmailTemplateFileRequestDTO;
import org.junit.jupiter.api.Test;
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
 * With exactly one declared tenant and no database, {@code /v1/email} still serves the flat 0.4.x
 * layout for that tenant.
 */
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "tenants.config[0].id=tenant-a",
                "tenants.config[0].display-name=Tenant A"
        }
)
class FlatTemplateLayoutSingleTenantTest extends AbstractTest {

    @Test
    void withOneDeclaredTenant_flatTemplateIsSent() throws Exception {
        HashMap<String, JsonNode> model = new HashMap<>();
        model.put("test", JsonNodeFactory.instance.numberNode(1));

        EmailTemplateFileRequestDTO request = EmailTemplateFileRequestDTO.builder()
                .recipients(List.of("recipient@test.com"))
                .sender("sender@test.com")
                .model(model)
                .emailTemplateId("test")
                .build();

        MvcResult result = mock.perform(MockMvcRequestBuilders.post("/v1/email")
                        .header("X-API-KEY", "test-api-key")
                        .header("X-Tenant-ID", "tenant-a")
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertTrue(resultToObject(result, EmailResultDTO.class).isSuccess());
    }
}
