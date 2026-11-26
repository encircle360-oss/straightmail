package com.encircle360.oss.straightmail.service;

import com.encircle360.oss.straightmail.config.GitSyncProperties;
import com.encircle360.oss.straightmail.dto.status.GitSyncStatusDTO;
import com.encircle360.oss.straightmail.dto.status.SyncResult;
import com.encircle360.oss.straightmail.model.Template;
import com.encircle360.oss.straightmail.model.Tenant;
import com.encircle360.oss.straightmail.service.template.provider.GitTemplateRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/**
 * Service that clones Git repositories containing FreeMarker templates and registers them
 * in the {@link GitTemplateRegistry} for use as a template source.
 *
 * <p>Only active when {@code templates.git-sync.enabled=true} is set in the configuration.
 * For each tenant with a configured Git repository URL, all configured branches are cloned
 * into a temporary directory, template files ({@code .ftl}) are discovered, and the registry
 * is updated with the parsed {@link Template} objects.
 *
 * <p>Git authentication is performed via a token stored in encrypted form on the tenant entity;
 * decryption is handled by {@link EncryptionService} before use.
 *
 * <p>Temporary clone directories are always deleted after processing, even on failure.
 * The base directory for clones can be controlled via {@code templates.git-sync.base-path}
 * (env: {@code GIT_SYNC_BASE_PATH}); when absent the OS default temp directory is used.
 *
 * <p>Sync status per tenant is tracked via {@link GitSyncStatusStore}, which persists to the
 * database when the {@code database} profile is active, or falls back to in-memory storage otherwise.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "templates.git-sync.enabled", havingValue = "true")
public class GitSyncService {

    private final GitTemplateRegistry registry;
    private final EncryptionService encryptionService;
    private final GitSyncProperties properties;
    private final GitSyncStatusStore statusStore;

    /**
     * Returns the current sync status for all tenants that have been attempted at least once.
     *
     * @return unmodifiable list of per-tenant sync status entries
     */
    public List<GitSyncStatusDTO> getStatuses() {
        return statusStore.getAll();
    }

    /**
     * Returns the current sync status for a specific tenant.
     *
     * @param tenantSlug the tenant slug
     * @return an {@link Optional} containing the status, or empty if the tenant has never been synced
     */
    public Optional<GitSyncStatusDTO> getStatus(String tenantSlug) {
        return statusStore.get(tenantSlug);
    }

    /**
     * Synchronises all configured Git branches for a database-backed tenant and records the outcome.
     *
     * <p>Skips silently if the tenant has no Git repository URL configured.
     * Defaults to the {@code main} branch if no branches are explicitly listed.
     * The Git token on the tenant entity is expected to be AES-256-GCM encrypted via {@link EncryptionService}.
     *
     * @param tenant the tenant whose Git repository should be synchronised
     */
    public void syncTenant(Tenant tenant) {
        if (tenant.getGitRepoUrl() == null || tenant.getGitRepoUrl().isBlank()) {
            log.debug("Tenant '{}' has no git repo URL, skipping", tenant.getSlug());
            return;
        }
        String plainToken = tenant.getGitToken() != null ? encryptionService.decrypt(tenant.getGitToken()) : null;
        this.doSyncTenant(tenant.getSlug(), tenant.getGitRepoUrl(), tenant.getGitBranches(), plainToken);
    }

    /**
     * Synchronises all configured Git branches for a config-based tenant (no database required).
     *
     * <p>Used when the {@code database} profile is not active; tenant data is sourced from
     * {@code tenants.config} in the application configuration. The {@code plainToken} is expected
     * in plain text (not encrypted), as it comes directly from the YAML configuration.
     *
     * @param tenantId    the tenant slug identifier
     * @param gitRepoUrl  URL of the Git repository to clone
     * @param gitBranches branches to synchronise; defaults to {@code main} if null or empty
     * @param plainToken  plain-text Git access token, or {@code null} for public repositories
     */
    public void syncTenant(String tenantId, String gitRepoUrl, List<String> gitBranches, String plainToken) {
        if (gitRepoUrl == null || gitRepoUrl.isBlank()) {
            log.debug("Tenant '{}' has no git repo URL, skipping", tenantId);
            return;
        }
        this.doSyncTenant(tenantId, gitRepoUrl, gitBranches, plainToken);
    }

    private void doSyncTenant(String tenantId, String gitRepoUrl, List<String> gitBranches, String plainToken) {
        List<String> branches = (gitBranches == null || gitBranches.isEmpty()) ? List.of("main") : gitBranches;

        List<String> failedBranches = new ArrayList<>();
        String lastError = null;

        for (int i = 0; i < branches.size(); i++) {
            String branch = branches.get(i);
            String error = this.syncBranch(tenantId, gitRepoUrl, plainToken, branch, i);
            if (error != null) {
                failedBranches.add(branch);
                lastError = error;
            }
        }

        Instant now = Instant.now();
        if (failedBranches.isEmpty()) {
            statusStore.record(tenantId, now, SyncResult.SUCCESS, null);
        } else {
            statusStore.record(tenantId, now, SyncResult.FAILED, lastError);
        }
    }

    private String syncBranch(String tenantId, String gitRepoUrl, String plainToken, String branch, int branchOrder) {
        Path tmpDir = null;
        try {
            String prefix = "gitsync-" + tenantId + "-";
            tmpDir = (properties.getBasePath() != null && !properties.getBasePath().isBlank())
                    ? Files.createTempDirectory(Path.of(properties.getBasePath()), prefix)
                    : Files.createTempDirectory(prefix);
            Path repoDir = this.cloneRepo(gitRepoUrl, plainToken, branch, tmpDir);
            List<Path> templateFiles = this.traverseTemplateFiles(repoDir);
            Map<String, Template> templates = this.buildTemplateMap(templateFiles, repoDir, tenantId);
            registry.updateBranch(tenantId, branch, branchOrder, templates);
            log.info("Git-sync completed for tenant '{}' branch '{}': {} templates loaded", tenantId, branch, templates.size());
            return null;
        } catch (Exception e) {
            log.error("Git-sync failed for tenant '{}' branch '{}'", tenantId, branch, e);
            return e.getMessage();
        } finally {
            if (tmpDir != null) {
                this.deleteDirectory(tmpDir);
            }
        }
    }

    private Path cloneRepo(String repoUrl, String plainToken, String branch, Path targetDir) throws Exception {
        CloneCommand cloneCmd = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(targetDir.toFile())
                .setBranch(branch)
                .setDepth(1);

        if (plainToken != null && !plainToken.isBlank()) {
            cloneCmd.setCredentialsProvider(new UsernamePasswordCredentialsProvider("token", plainToken));
        }

        try (Git ignored = cloneCmd.call()) {
            log.debug("Cloned repo '{}' branch '{}' to '{}'", repoUrl, branch, targetDir);
        }

        return targetDir;
    }

    private List<Path> traverseTemplateFiles(Path repoDir) throws IOException {
        try (Stream<Path> paths = Files.walk(repoDir)) {
            return paths
                    .filter(p -> p.toString().endsWith(".ftl"))
                    .filter(p -> !p.getFileName().toString().contains("_subject"))
                    .filter(p -> !p.getFileName().toString().contains("_plain"))
                    .filter(p -> !p.toString().contains("/.git/"))
                    .toList();
        }
    }

    private Map<String, Template> buildTemplateMap(List<Path> files, Path repoDir, String tenantSlug) {
        Map<String, Template> templates = new HashMap<>();
        for (Path file : files) {
            String relativePath = repoDir.relativize(file).toString();
            String name = relativePath.endsWith(".ftl")
                    ? relativePath.substring(0, relativePath.length() - 4)
                    : relativePath;

            Path subjectFile = file.getParent().resolve(file.getFileName().toString()
                    .replace(".ftl", "_subject.ftl"));
            Path plainFile = file.getParent().resolve(file.getFileName().toString()
                    .replace(".ftl", "_plain.ftl"));

            try {
                Template template = Template.builder()
                        .name(name)
                        .tenantId(tenantSlug)
                        .html(Files.readString(file))
                        .subject(Files.exists(subjectFile) ? Files.readString(subjectFile) : null)
                        .plain(Files.exists(plainFile) ? Files.readString(plainFile) : null)
                        .build();
                templates.put(name, template);
            } catch (IOException e) {
                log.error("Error reading template file '{}' for tenant '{}'", file, tenantSlug, e);
            }
        }
        return templates;
    }

    private void deleteDirectory(Path dir) {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted((a, b) -> -a.compareTo(b))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            log.warn("Could not delete temp file '{}'", p);
                        }
                    });
        } catch (IOException e) {
            log.warn("Could not clean up temp directory '{}'", dir);
        }
    }
}
