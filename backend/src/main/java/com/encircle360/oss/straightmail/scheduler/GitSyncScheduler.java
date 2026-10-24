package com.encircle360.oss.straightmail.scheduler;

import com.encircle360.oss.straightmail.config.TenantProperties;
import com.encircle360.oss.straightmail.repository.TenantRepository;
import com.encircle360.oss.straightmail.service.GitSyncService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled component that periodically synchronises Git-based templates for all eligible tenants.
 *
 * <p>Active when {@code templates.git-sync.enabled=true}. Does not require the {@code database}
 * profile; when no database is active, tenant list is sourced from {@code tenants.config} via
 * {@link TenantProperties}.
 *
 * <p>Performs an initial sync immediately after the application is ready, then continues on a
 * cron schedule configured via {@code templates.git-sync.cron} (default: hourly). The cron-triggered
 * sync is protected by a ShedLock distributed lock (active only when the {@code database} profile
 * and {@link com.encircle360.oss.straightmail.config.ShedLockConfig} are loaded); without the lock
 * provider the {@code @SchedulerLock} annotation is silently ignored.
 *
 * <p>Delegates the actual sync logic to {@link GitSyncService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "templates.git-sync.enabled", havingValue = "true")
public class GitSyncScheduler {
    private final GitSyncService gitSyncService;
    private final ObjectProvider<TenantRepository> tenantRepositoryProvider;
    private final TenantProperties tenantProperties;

    @PostConstruct
    void init() {
        log.info("GitSyncScheduler bean created — git-sync is active.");
    }

    /**
     * Triggers an initial Git sync immediately after the application context is fully started.
     * This ensures templates are up-to-date without waiting for the first cron tick.
     * Runs without ShedLock — use the cron-triggered {@link #sync()} for distributed-lock protection.
     */
    @EventListener(ApplicationReadyEvent.class)
    void onApplicationReady() {
        log.info("Running initial git-sync on startup...");
        this.doSync();
    }

    /**
     * Triggers a Git sync for all eligible tenants.
     * Protected by a ShedLock distributed lock when the {@code database} profile is active.
     */
    @Scheduled(cron = "${templates.git-sync.cron:0 0 * * * *}")
    @SchedulerLock(name = "git-tenant-sync", lockAtLeastFor = "${templates.git-sync.lock-at-least}", lockAtMostFor = "${templates.git-sync.lock-at-most}")
    public void sync() {
        this.doSync();
    }

    void doSync() {
        log.info("Starting git-sync for all tenants with git repositories...");
        TenantRepository repo = tenantRepositoryProvider.getIfAvailable();
        if (repo != null) {
            repo.findAllByGitRepoUrlNotNullAndActiveTrue().forEach(tenant -> {
                log.debug("Syncing tenant '{}'", tenant.getSlug());
                gitSyncService.syncTenant(tenant);
            });
        } else {
            tenantProperties.getConfig().stream()
                    .filter(c -> c.getGitRepoUrl() != null && !c.getGitRepoUrl().isBlank())
                    .forEach(c -> {
                        log.debug("Syncing tenant '{}'", c.getId());
                        gitSyncService.syncTenant(c.getId(), c.getGitRepoUrl(), c.getGitBranches(), c.getGitToken());
                    });
        }
        log.info("Git-sync completed.");
    }
}
