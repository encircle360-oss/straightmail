package com.encircle360.oss.straightmail;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins that the tenant admin API accepts a payload that omits optional properties.
 *
 * <p>Goes through the HTTP message converter rather than the {@code ObjectMapper} bean directly,
 * because that is the mapper an API client actually meets.
 */
@SpringBootTest(
        classes = StraightmailApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "auth.mode=none")
@AutoConfigureMockMvc
@ActiveProfiles({"database", "test"})
class TenantApiPayloadTest extends AbstractTest {

    @Test
    void create_withoutOptionalBooleans_isAccepted() throws Exception {
        mock.perform(MockMvcRequestBuilders.post("/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"payload-a\",\"displayName\":\"Payload A\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.smtpTls").value(false));
    }

    @Test
    void update_withoutOptionalBooleans_isAccepted() throws Exception {
        mock.perform(MockMvcRequestBuilders.post("/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"payload-b\",\"displayName\":\"Payload B\"}"))
                .andExpect(status().isCreated());

        mock.perform(MockMvcRequestBuilders.put("/v1/tenants/payload-b")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"payload-b\",\"displayName\":\"Payload B renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Payload B renamed"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void explicitFalseIsHonoured() throws Exception {
        mock.perform(MockMvcRequestBuilders.post("/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slug\":\"payload-c\",\"displayName\":\"Payload C\",\"active\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(false));
    }
}
