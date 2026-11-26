package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.dto.tenant.CreateUpdateTenantDTO;
import com.encircle360.oss.straightmail.dto.template.CreateUpdateTemplateDTO;
import com.encircle360.oss.straightmail.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the behaviour of {@code auth.mode=none}.
 *
 * <p>This mode grants every anonymous caller {@code ROLE_ADMIN} and lets the {@code X-Tenant-ID}
 * header select any existing tenant. That is intentional — it exists for deployments that are
 * protected at the network layer — but it is the widest configuration the application has, and
 * nothing pinned it before. These tests make the blast radius explicit, so that narrowing or
 * widening it becomes a deliberate change with a failing test attached.
 */
@SpringBootTest(
        classes = StraightmailApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "auth.mode=none")
@AutoConfigureMockMvc
@ActiveProfiles({"database", "test"})
class AuthModeNoneTest extends AbstractTest {

    @Autowired
    private TenantService tenantService;

    @BeforeEach
    void createTenants() {
        ensureTenant("none-a", "None A");
        ensureTenant("none-b", "None B");
    }

    private void ensureTenant(String slug, String displayName) {
        boolean present = tenantService.findAll().stream()
                .anyMatch(t -> slug.equals(t.getSlug()));
        if (!present) {
            tenantService.create(CreateUpdateTenantDTO.builder()
                    .slug(slug).displayName(displayName).build());
        }
    }

    @Test
    void an_anonymous_caller_reaches_the_api_without_any_credential() throws Exception {
        mock.perform(MockMvcRequestBuilders.get("/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    void an_anonymous_caller_holds_the_admin_role() throws Exception {
        // /v1/tenants is @PreAuthorize("hasRole('ADMIN')"); reaching it proves the anonymous
        // authority granted by openFilterChain satisfies method security.
        mock.perform(MockMvcRequestBuilders.get("/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    void the_tenant_header_selects_any_existing_tenant_and_data_stays_isolated() throws Exception {
        // GET /v1/tenants and /v1/tenants/me are deliberately excluded from tenant resolution
        // (the login flow needs them), so isolation is observed on a tenant-scoped resource.
        createTemplate("none-a", "only-in-a");

        assertTrue(templateNamesFor("none-a").contains("only-in-a"),
                "the tenant that created the template sees it");
        assertFalse(templateNamesFor("none-b").contains("only-in-a"),
                "a different tenant must not see it");
    }

    @Test
    void the_tenant_context_does_not_leak_between_requests() throws Exception {
        // TenantContext is a request-scoped bean; if that ever became a singleton or a
        // thread-local without cleanup, a pooled worker thread would serve the previous
        // caller's tenant. This asserts the isolation rather than the annotation.
        createTemplate("none-b", "only-in-b");

        templateNamesFor("none-a");
        assertTrue(templateNamesFor("none-b").contains("only-in-b"));
        assertFalse(templateNamesFor("none-a").contains("only-in-b"));
    }

    @Test
    void an_unknown_tenant_is_refused() throws Exception {
        mock.perform(MockMvcRequestBuilders.get("/v1/templates")
                        .header("X-Tenant-ID", "does-not-exist")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void a_malformed_tenant_slug_is_refused() throws Exception {
        mock.perform(MockMvcRequestBuilders.get("/v1/templates")
                        .header("X-Tenant-ID", "not a slug!")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest());
    }

    private void createTemplate(String tenantId, String name) throws Exception {
        CreateUpdateTemplateDTO dto = CreateUpdateTemplateDTO.builder()
                .name(name)
                .subject("Subject")
                .html("<p>body</p>")
                .build();

        mock.perform(MockMvcRequestBuilders.post("/v1/templates")
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    private String templateNamesFor(String tenantId) throws Exception {
        MvcResult result = mock.perform(MockMvcRequestBuilders.get("/v1/templates")
                        .header("X-Tenant-ID", tenantId)
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }
}
