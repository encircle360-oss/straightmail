package com.encircle360.oss.straightmail.service;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.dto.tenant.CreateUpdateTenantDTO;
import com.encircle360.oss.straightmail.mapper.TenantMapper;
import com.encircle360.oss.straightmail.model.Tenant;
import com.encircle360.oss.straightmail.repository.GitSyncStatusRepository;
import com.encircle360.oss.straightmail.repository.GitTemplateCacheRepository;
import com.encircle360.oss.straightmail.repository.TenantRepository;
import com.encircle360.oss.straightmail.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

/**
 * Service for managing {@link Tenant} entities in the database.
 *
 * <p>Only active when the {@code database} Spring profile is enabled (see {@link DatabaseConfig#PROFILE}).
 * Handles full CRUD operations for tenants, including encryption of sensitive credentials
 * (SMTP password, Git token) via {@link EncryptionService} and SHA-256 hashing of API keys.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile(DatabaseConfig.PROFILE)
public class TenantService {

    private final TenantRepository tenantRepository;
    private final GitSyncStatusRepository gitSyncStatusRepository;
    private final GitTemplateCacheRepository gitTemplateCacheRepository;
    private final EncryptionService encryptionService;
    private final TenantMapper mapper;
    private final TenantProperties tenantProperties;

    /**
     * Returns all tenants stored in the database.
     *
     * @return list of all {@link Tenant} entities
     */
    public List<Tenant> findAll() {
        return tenantRepository.findAll();
    }

    /**
     * Retrieves a tenant by its slug identifier.
     *
     * @param slug the tenant's unique slug
     * @return the matching {@link Tenant}
     * @throws org.springframework.web.server.ResponseStatusException with {@code 404 NOT FOUND} if no tenant exists with this slug
     */
    public Tenant get(String slug) {
        return tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tenant not found: " + slug));
    }

    /**
     * Creates a new tenant from the given DTO.
     *
     * @param dto the create request containing tenant configuration
     * @return the persisted {@link Tenant} entity
     * @throws org.springframework.web.server.ResponseStatusException with {@code 409 CONFLICT} if a tenant with the same slug already exists
     */
    public Tenant create(CreateUpdateTenantDTO dto) {
        if (tenantRepository.existsBySlug(dto.getSlug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tenant with slug '" + dto.getSlug() + "' already exists");
        }
        Tenant tenant = mapper.fromDto(dto);
        this.encryptCredentials(tenant, dto);
        return tenantRepository.save(tenant);
    }

    /**
     * Updates an existing tenant identified by {@code slug} with the given DTO.
     *
     * @param slug the tenant's unique slug
     * @param dto  the update request containing new tenant configuration
     * @return the updated and persisted {@link Tenant} entity
     * @throws org.springframework.web.server.ResponseStatusException with {@code 404 NOT FOUND} if no tenant exists with this slug
     */
    public Tenant update(String slug, CreateUpdateTenantDTO dto) {
        Tenant tenant = tenantRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tenant not found: " + slug));

        String oldRepoUrl = tenant.getGitRepoUrl();
        List<String> oldBranches = tenant.getGitBranches() != null
                ? List.copyOf(tenant.getGitBranches()) : List.of();

        mapper.updateFromDto(dto, tenant);
        this.encryptCredentials(tenant, dto);

        String newRepoUrl = tenant.getGitRepoUrl();
        if (hasValue(oldRepoUrl) && !hasValue(newRepoUrl)) {
            // Git repo removed: clean up all Git data for this tenant
            gitSyncStatusRepository.deleteById(slug);
            gitTemplateCacheRepository.deleteAllByTenantId(slug);
        } else {
            // Individual branches removed: clean up the cache for the removed branches
            Set<String> newBranches = tenant.getGitBranches() != null
                    ? Set.copyOf(tenant.getGitBranches()) : Set.of();
            oldBranches.stream()
                    .filter(b -> !newBranches.contains(b))
                    .forEach(b -> gitTemplateCacheRepository.deleteAllByTenantIdAndBranch(slug, b));
        }

        return tenantRepository.save(tenant);
    }

    /**
     * Deletes the tenant identified by {@code slug}.
     *
     * <p>The default tenant (identified by {@link Tenant#DEFAULT_ID}) cannot be deleted.
     *
     * @param slug the tenant's unique slug
     * @throws org.springframework.web.server.ResponseStatusException with {@code 400 BAD REQUEST} if attempting to delete the default tenant
     */
    public void delete(String slug) {
        if (tenantProperties.getDefaultId().equals(slug)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The default tenant cannot be deleted");
        }
        gitTemplateCacheRepository.deleteAllByTenantId(slug);
        gitSyncStatusRepository.deleteById(slug);
        tenantRepository.deleteById(slug);
        log.info("Tenant '{}' deleted", LogSanitizer.sanitize(slug));
    }

    /**
     * Imports a tenant from a static configuration entry if it does not already exist in the database.
     *
     * <p>Intended for use by {@link com.encircle360.oss.straightmail.service.TenantReconciliationService}
     * at application startup. This is a one-time seed operation: if a tenant with the given slug is
     * already present in the database (created from a previous startup or via the API), the call is a
     * no-op and the existing record is left completely unchanged.
     *
     * <p>On creation, encrypts the SMTP password and Git token via {@link EncryptionService} and stores
     * the API key as a SHA-256 hash.
     *
     * @param cfg the static tenant configuration entry to import
     */
    public void importFromConfig(TenantProperties.TenantConfig cfg) {
        if (tenantRepository.existsBySlug(cfg.getId())) {
            log.debug("Tenant '{}' already exists — skipping config import", cfg.getId());
            return;
        }
        Tenant tenant = mapper.fromConfig(cfg);
        this.encryptSmtpAndGit(tenant, cfg.getSmtpPassword(), cfg.getGitToken());
        if (cfg.getApiKey() != null && !cfg.getApiKey().isBlank()) {
            tenant.setApiKeyHash(hashApiKey(cfg.getApiKey()));
        }
        tenantRepository.save(tenant);
        log.info("Tenant '{}' created from config", cfg.getId());
    }

    private static boolean hasValue(String s) {
        return s != null && !s.isBlank();
    }

    private void encryptCredentials(Tenant tenant, CreateUpdateTenantDTO dto) {
        this.encryptSmtpAndGit(tenant, dto.getSmtpPassword(), dto.getGitToken());
        if (dto.getApiKey() != null && !dto.getApiKey().isBlank()) {
            tenant.setApiKeyHash(hashApiKey(dto.getApiKey()));
        }
    }

    private void encryptSmtpAndGit(Tenant tenant, String smtpPassword, String gitToken) {
        if (smtpPassword != null && !smtpPassword.isBlank()) {
            tenant.setSmtpPassword(encryptionService.encryptIfNotEncrypted(smtpPassword));
        }
        if (gitToken != null && !gitToken.isBlank()) {
            tenant.setGitToken(encryptionService.encryptIfNotEncrypted(gitToken));
        }
    }

    /**
     * Computes a SHA-256 hex digest of the given API key for secure storage.
     *
     * <p>Only the hash is persisted; the raw key is never stored. Authentication filters compare
     * the hash of the incoming {@code X-API-KEY} header against this stored hash.
     *
     * @param key the raw API key to hash
     * @return lowercase hex-encoded SHA-256 digest
     * @throws IllegalStateException if the SHA-256 algorithm is not available (should never happen on standard JVMs)
     */
    public static String hashApiKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
