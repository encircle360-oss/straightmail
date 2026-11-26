package com.encircle360.oss.straightmail;

import com.encircle360.oss.straightmail.dto.tenant.CreateUpdateTenantDTO;
import com.encircle360.oss.straightmail.dto.tenant.TenantDTO;
import com.encircle360.oss.straightmail.model.Tenant;
import com.encircle360.oss.straightmail.repository.TenantRepository;
import com.encircle360.oss.straightmail.service.TenantService;
import tools.jackson.core.type.TypeReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StraightmailApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles({"database", "test"})
class TenantIntegrationTest extends AbstractTest {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantRepository tenantRepository;

    @BeforeEach
    void cleanup() {
        tenantRepository.findAll().stream()
                .filter(t -> !Tenant.DEFAULT_ID.equals(t.getSlug()))
                .forEach(t -> tenantRepository.deleteById(t.getSlug()));
    }

    @Test
    void list_tenants_includes_default_tenant() throws Exception {
        MvcResult result = get("/v1/tenants", status().isOk());
        List<TenantDTO> tenants = mapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });

        assertFalse(tenants.isEmpty());
        assertTrue(tenants.stream().anyMatch(t -> Tenant.DEFAULT_ID.equals(t.getSlug())));
    }

    @Test
    void create_tenant_and_retrieve_via_http() throws Exception {
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("test-tenant")
                .displayName("Test Tenant")
                .build();
        tenantService.create(dto);

        MvcResult result = get("/v1/tenants", status().isOk());
        List<TenantDTO> tenants = mapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<>() {
                });

        assertTrue(tenants.stream().anyMatch(t -> "test-tenant".equals(t.getSlug())));
    }

    @Test
    void get_tenant_by_slug_returns_200() throws Exception {
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("my-tenant")
                .displayName("My Tenant")
                .build();
        tenantService.create(dto);

        MvcResult result = get("/v1/tenants/my-tenant", status().isOk());
        TenantDTO tenantDTO = resultToObject(result, TenantDTO.class);

        assertNotNull(tenantDTO);
        assertEquals("my-tenant", tenantDTO.getSlug());
        assertEquals("My Tenant", tenantDTO.getDisplayName());
    }

    @Test
    void get_nonexistent_tenant_returns_404() throws Exception {
        get("/v1/tenants/does-not-exist", status().isNotFound());
    }

    @Test
    void smtp_password_is_stored_encrypted() {
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("enc-tenant")
                .displayName("Encrypted Tenant")
                .smtpPassword("plaintext-password")
                .build();
        tenantService.create(dto);

        Tenant saved = tenantRepository.findBySlug("enc-tenant").orElseThrow();
        assertNotNull(saved.getSmtpPassword());
        assertTrue(saved.getSmtpPassword().startsWith("ENC("),
                "SMTP password must be stored encrypted (ENC(...)), got: " + saved.getSmtpPassword());
    }

    @Test
    void api_key_is_stored_as_hash_not_plaintext() {
        String rawKey = "super-secret-api-key";
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("keyed-tenant")
                .displayName("Keyed Tenant")
                .apiKey(rawKey)
                .build();
        tenantService.create(dto);

        Tenant saved = tenantRepository.findBySlug("keyed-tenant").orElseThrow();
        String expectedHash = TenantService.hashApiKey(rawKey);

        assertNotNull(saved.getApiKeyHash());
        assertEquals(expectedHash, saved.getApiKeyHash());
        assertNotEquals(rawKey, saved.getApiKeyHash(), "Raw key must not be stored");
    }

    @Test
    void tenant_dto_does_not_expose_smtp_password() throws Exception {
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("secure-tenant")
                .displayName("Secure Tenant")
                .smtpPassword("my-password")
                .build();
        tenantService.create(dto);

        MvcResult result = get("/v1/tenants/secure-tenant", status().isOk());
        String json = result.getResponse().getContentAsString();

        assertFalse(json.contains("smtpPassword"), "smtpPassword must not be returned in API response");
        assertFalse(json.contains("my-password"), "Plain password must not be returned in API response");
    }

    @Test
    void has_api_key_flag_is_true_when_api_key_set() throws Exception {
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("flagged-tenant")
                .displayName("Flagged Tenant")
                .apiKey("some-api-key")
                .build();
        tenantService.create(dto);

        MvcResult result = get("/v1/tenants/flagged-tenant", status().isOk());
        TenantDTO tenantDTO = resultToObject(result, TenantDTO.class);

        assertTrue(tenantDTO.isHasApiKey());
    }

    @Test
    void delete_default_tenant_throws_bad_request() {
        assertThrows(ResponseStatusException.class,
                () -> tenantService.delete(Tenant.DEFAULT_ID));

        assertTrue(tenantRepository.existsBySlug(Tenant.DEFAULT_ID),
                "Default tenant must still exist after failed delete");
    }

    @Test
    void delete_custom_tenant_removes_it() {
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("deletable")
                .displayName("Deletable Tenant")
                .build();
        tenantService.create(dto);
        assertTrue(tenantRepository.existsBySlug("deletable"));

        tenantService.delete("deletable");

        assertFalse(tenantRepository.existsBySlug("deletable"));
    }

    @Test
    void create_duplicate_slug_throws_conflict() {
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("dup-tenant")
                .displayName("Duplicate")
                .build();
        tenantService.create(dto);

        assertThrows(ResponseStatusException.class, () -> tenantService.create(dto));
    }

    @Test
    void tenant_list_is_refused_for_a_non_admin_api_key() throws Exception {
        // a per-tenant key authenticates as ROLE_USER; tenant administration is admin-only
        tenantService.create(CreateUpdateTenantDTO.builder()
                .slug("scoped-tenant")
                .displayName("Scoped Tenant")
                .apiKey("scoped-tenant-key")
                .build());

        mock.perform(MockMvcRequestBuilders.get("/v1/tenants")
                        .header("X-API-KEY", "scoped-tenant-key")
                        .header("X-Tenant-ID", "scoped-tenant")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden());
    }

    @Test
    void single_tenant_read_is_refused_for_a_non_admin_api_key() throws Exception {
        tenantService.create(CreateUpdateTenantDTO.builder()
                .slug("scoped-a")
                .displayName("Scoped A")
                .apiKey("scoped-a-key")
                .build());
        tenantService.create(CreateUpdateTenantDTO.builder()
                .slug("scoped-b")
                .displayName("Scoped B")
                .build());

        // reading a foreign tenant's mail and git configuration must not be possible
        mock.perform(MockMvcRequestBuilders.get("/v1/tenants/scoped-b")
                        .header("X-API-KEY", "scoped-a-key")
                        .header("X-Tenant-ID", "scoped-a")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden());
    }

    @Test
    void own_tenants_remain_readable_for_a_non_admin_api_key() throws Exception {
        tenantService.create(CreateUpdateTenantDTO.builder()
                .slug("scoped-me")
                .displayName("Scoped Me")
                .apiKey("scoped-me-key")
                .build());

        // /me is the endpoint the login flow depends on — it must stay open to ROLE_USER
        mock.perform(MockMvcRequestBuilders.get("/v1/tenants/me")
                        .header("X-API-KEY", "scoped-me-key")
                        .header("X-Tenant-ID", "scoped-me")
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());
    }
}
