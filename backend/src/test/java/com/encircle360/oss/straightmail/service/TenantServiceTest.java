package com.encircle360.oss.straightmail.service;

import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.dto.tenant.CreateUpdateTenantDTO;
import com.encircle360.oss.straightmail.mapper.TenantMapper;
import com.encircle360.oss.straightmail.model.Tenant;
import com.encircle360.oss.straightmail.repository.GitSyncStatusRepository;
import com.encircle360.oss.straightmail.repository.GitTemplateCacheRepository;
import com.encircle360.oss.straightmail.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private GitSyncStatusRepository gitSyncStatusRepository;

    @Mock
    private GitTemplateCacheRepository gitTemplateCacheRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private TenantMapper mapper;

    @Mock
    private TenantProperties tenantProperties;

    @InjectMocks
    private TenantService tenantService;

    @BeforeEach
    void setUp() {
        lenient().when(tenantProperties.getDefaultId()).thenReturn("default");
    }

    @Test
    void create_success() {
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("acme")
                .displayName("Acme Corp")
                .build();
        Tenant tenant = Tenant.builder().slug("acme").displayName("Acme Corp").build();

        when(tenantRepository.existsBySlug("acme")).thenReturn(false);
        when(mapper.fromDto(dto)).thenReturn(tenant);
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        Tenant result = tenantService.create(dto);

        assertEquals("acme", result.getSlug());
        verify(tenantRepository).save(tenant);
    }

    @Test
    void create_encrypts_smtp_password() {
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("acme")
                .displayName("Acme")
                .smtpPassword("plain-password")
                .build();
        Tenant tenant = Tenant.builder().slug("acme").displayName("Acme").build();

        when(tenantRepository.existsBySlug("acme")).thenReturn(false);
        when(mapper.fromDto(dto)).thenReturn(tenant);
        when(encryptionService.encryptIfNotEncrypted("plain-password")).thenReturn("ENC(abc123)");
        when(tenantRepository.save(any())).thenReturn(tenant);

        tenantService.create(dto);

        verify(encryptionService).encryptIfNotEncrypted("plain-password");
        assertEquals("ENC(abc123)", tenant.getSmtpPassword());
    }

    @Test
    void create_hashes_api_key() {
        String rawKey = "my-api-key";
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("acme")
                .displayName("Acme Corp")
                .apiKey(rawKey)
                .build();
        Tenant tenant = Tenant.builder().slug("acme").displayName("Acme Corp").build();

        when(tenantRepository.existsBySlug("acme")).thenReturn(false);
        when(mapper.fromDto(dto)).thenReturn(tenant);
        when(tenantRepository.save(any())).thenReturn(tenant);

        tenantService.create(dto);

        String expectedHash = TenantService.hashApiKey(rawKey);
        assertEquals(expectedHash, tenant.getApiKeyHash(), "API key must be stored as SHA-256 hash");
        assertNotEquals(rawKey, tenant.getApiKeyHash(), "Raw API key must not be stored");
    }

    @Test
    void create_duplicate_slug_throws_conflict() {
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("existing")
                .displayName("Existing")
                .build();
        when(tenantRepository.existsBySlug("existing")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tenantService.create(dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void update_success() {
        Tenant existing = Tenant.builder().slug("acme").displayName("Acme").build();
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("acme")
                .displayName("Acme Updated")
                .build();

        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(existing));
        when(tenantRepository.save(any())).thenReturn(existing);

        Tenant result = tenantService.update("acme", dto);

        assertNotNull(result);
        verify(tenantRepository).save(existing);
    }

    @Test
    void update_not_found_throws_404() {
        when(tenantRepository.findBySlug("missing")).thenReturn(Optional.empty());
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("missing").displayName("Missing").build();

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tenantService.update("missing", dto));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void update_encrypts_smtp_password() {
        Tenant existing = Tenant.builder().slug("acme").displayName("Acme").build();
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("acme")
                .displayName("Acme")
                .smtpPassword("new-password")
                .build();

        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(existing));
        when(encryptionService.encryptIfNotEncrypted("new-password")).thenReturn("ENC(new-encrypted)");
        when(tenantRepository.save(any())).thenReturn(existing);

        tenantService.update("acme", dto);

        verify(encryptionService).encryptIfNotEncrypted("new-password");
        assertEquals("ENC(new-encrypted)", existing.getSmtpPassword());
    }

    @Test
    void update_encrypts_git_token() {
        Tenant existing = Tenant.builder().slug("acme").displayName("Acme").build();
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("acme")
                .displayName("Acme")
                .gitToken("my-git-token")
                .build();

        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(existing));
        when(encryptionService.encryptIfNotEncrypted("my-git-token")).thenReturn("ENC(git-encrypted)");
        when(tenantRepository.save(any())).thenReturn(existing);

        tenantService.update("acme", dto);

        verify(encryptionService).encryptIfNotEncrypted("my-git-token");
        assertEquals("ENC(git-encrypted)", existing.getGitToken());
    }

    @Test
    void delete_default_tenant_throws_bad_request() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tenantService.delete(Tenant.DEFAULT_ID));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(tenantRepository);
    }

    @Test
    void delete_removes_from_db() {
        tenantService.delete("acme");

        verify(gitTemplateCacheRepository).deleteAllByTenantId("acme");
        verify(gitSyncStatusRepository).deleteById("acme");
        verify(tenantRepository).deleteById("acme");
    }

    @Test
    void delete_cleans_up_git_data_before_tenant_removal() {
        tenantService.delete("acme");

        InOrder order = inOrder(gitTemplateCacheRepository, gitSyncStatusRepository, tenantRepository);
        order.verify(gitTemplateCacheRepository).deleteAllByTenantId("acme");
        order.verify(gitSyncStatusRepository).deleteById("acme");
        order.verify(tenantRepository).deleteById("acme");
    }

    @Test
    void update_clears_all_git_data_when_repo_url_removed() {
        Tenant existing = Tenant.builder()
                .slug("acme")
                .displayName("Acme")
                .gitRepoUrl("https://github.com/acme/templates.git")
                .gitBranches(new ArrayList<>(List.of("main")))
                .build();
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("acme")
                .displayName("Acme")
                .gitRepoUrl(null)
                .build();

        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(existing));
        doAnswer(inv -> {
            existing.setGitRepoUrl(null);
            existing.setGitBranches(null);
            return null;
        }).when(mapper).updateFromDto(dto, existing);
        when(tenantRepository.save(any())).thenReturn(existing);

        tenantService.update("acme", dto);

        verify(gitSyncStatusRepository).deleteById("acme");
        verify(gitTemplateCacheRepository).deleteAllByTenantId("acme");
        verify(gitTemplateCacheRepository, never()).deleteAllByTenantIdAndBranch(any(), any());
    }

    @Test
    void update_clears_cache_for_removed_branches() {
        Tenant existing = Tenant.builder()
                .slug("acme")
                .displayName("Acme")
                .gitRepoUrl("https://github.com/acme/templates.git")
                .gitBranches(new ArrayList<>(List.of("main", "dev")))
                .build();
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("acme")
                .displayName("Acme")
                .gitRepoUrl("https://github.com/acme/templates.git")
                .gitBranches(List.of("main"))
                .build();

        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(existing));
        doAnswer(inv -> {
            existing.setGitBranches(new ArrayList<>(List.of("main")));
            return null;
        }).when(mapper).updateFromDto(dto, existing);
        when(tenantRepository.save(any())).thenReturn(existing);

        tenantService.update("acme", dto);

        verify(gitTemplateCacheRepository).deleteAllByTenantIdAndBranch("acme", "dev");
        verify(gitTemplateCacheRepository, never()).deleteAllByTenantIdAndBranch("acme", "main");
        verify(gitSyncStatusRepository, never()).deleteById(any());
    }

    @Test
    void update_does_not_touch_git_data_when_repo_unchanged() {
        Tenant existing = Tenant.builder()
                .slug("acme")
                .displayName("Acme")
                .gitRepoUrl("https://github.com/acme/templates.git")
                .gitBranches(new ArrayList<>(List.of("main")))
                .build();
        CreateUpdateTenantDTO dto = CreateUpdateTenantDTO.builder()
                .slug("acme")
                .displayName("Acme Updated")
                .gitRepoUrl("https://github.com/acme/templates.git")
                .gitBranches(List.of("main"))
                .build();

        when(tenantRepository.findBySlug("acme")).thenReturn(Optional.of(existing));
        doAnswer(inv -> {
            existing.setDisplayName("Acme Updated");
            return null;
        }).when(mapper).updateFromDto(dto, existing);
        when(tenantRepository.save(any())).thenReturn(existing);

        tenantService.update("acme", dto);

        verify(gitSyncStatusRepository, never()).deleteById(any());
        verify(gitTemplateCacheRepository, never()).deleteAllByTenantId(any());
        verify(gitTemplateCacheRepository, never()).deleteAllByTenantIdAndBranch(any(), any());
    }

    @Test
    void get_not_found_throws_404() {
        when(tenantRepository.findBySlug("unknown")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> tenantService.get("unknown"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void hashApiKey_is_deterministic_and_not_plaintext() {
        String key = "my-api-key";
        String hash1 = TenantService.hashApiKey(key);
        String hash2 = TenantService.hashApiKey(key);

        assertEquals(hash1, hash2, "SHA-256 hash must be deterministic");
        assertNotEquals(key, hash1, "Hash must not equal the plaintext key");
        assertEquals(64, hash1.length(), "SHA-256 produces 64 hex characters");
    }

    @Test
    void hashApiKey_different_keys_produce_different_hashes() {
        String hash1 = TenantService.hashApiKey("key-one");
        String hash2 = TenantService.hashApiKey("key-two");
        assertNotEquals(hash1, hash2);
    }

    @Test
    void importFromConfig_creates_new_tenant_when_not_exists() {
        TenantProperties.TenantConfig cfg = new TenantProperties.TenantConfig();
        cfg.setId("acme");
        cfg.setDisplayName("Acme Corp");

        Tenant tenant = Tenant.builder().slug("acme").displayName("Acme Corp").build();
        when(tenantRepository.existsBySlug("acme")).thenReturn(false);
        when(mapper.fromConfig(cfg)).thenReturn(tenant);
        when(tenantRepository.save(tenant)).thenReturn(tenant);

        tenantService.importFromConfig(cfg);

        verify(tenantRepository).save(tenant);
    }

    @Test
    void importFromConfig_skips_existing_tenant() {
        TenantProperties.TenantConfig cfg = new TenantProperties.TenantConfig();
        cfg.setId("acme");
        cfg.setDisplayName("Acme Updated");

        when(tenantRepository.existsBySlug("acme")).thenReturn(true);

        tenantService.importFromConfig(cfg);

        verifyNoInteractions(mapper);
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void importFromConfig_encrypts_smtp_password_on_create() {
        TenantProperties.TenantConfig cfg = new TenantProperties.TenantConfig();
        cfg.setId("acme");
        cfg.setSmtpPassword("plain-pass");

        Tenant tenant = Tenant.builder().slug("acme").build();
        when(tenantRepository.existsBySlug("acme")).thenReturn(false);
        when(mapper.fromConfig(cfg)).thenReturn(tenant);
        when(encryptionService.encryptIfNotEncrypted("plain-pass")).thenReturn("ENC(abc)");
        when(tenantRepository.save(any())).thenReturn(tenant);

        tenantService.importFromConfig(cfg);

        assertEquals("ENC(abc)", tenant.getSmtpPassword());
    }

    @Test
    void importFromConfig_encrypts_git_token_on_create() {
        TenantProperties.TenantConfig cfg = new TenantProperties.TenantConfig();
        cfg.setId("acme");
        cfg.setGitToken("my-token");

        Tenant tenant = Tenant.builder().slug("acme").build();
        when(tenantRepository.existsBySlug("acme")).thenReturn(false);
        when(mapper.fromConfig(cfg)).thenReturn(tenant);
        when(encryptionService.encryptIfNotEncrypted("my-token")).thenReturn("ENC(token)");
        when(tenantRepository.save(any())).thenReturn(tenant);

        tenantService.importFromConfig(cfg);

        assertEquals("ENC(token)", tenant.getGitToken());
    }
}
