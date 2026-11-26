package com.encircle360.oss.straightmail.service;

import com.encircle360.oss.straightmail.config.DatabaseConfig;
import com.encircle360.oss.straightmail.config.TenantProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service that seeds tenant records from configuration declared in {@code application.yml}
 * (via {@link TenantProperties}) into the database on application startup.
 *
 * <p>Runs automatically at application startup via the {@link ApplicationRunner} contract.
 * Only active when the {@code database} Spring profile is enabled (see {@link DatabaseConfig#PROFILE}).
 *
 * <p>This is a <strong>one-time, append-only</strong> operation:
 * <ul>
 *   <li>Config tenants that do not yet exist in the database are created.</li>
 *   <li>Config tenants that already exist in the database are left completely unchanged.</li>
 *   <li>Tenants present in the database but absent from the config are never deleted.</li>
 * </ul>
 *
 * <p>All credential encryption and hashing is delegated to {@link TenantService#importFromConfig}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Profile(DatabaseConfig.PROFILE)
public class TenantReconciliationService implements ApplicationRunner {

    private final TenantService tenantService;
    private final TenantProperties tenantProperties;

    /**
     * Entry point called by Spring at application startup. Triggers {@link #reconcile()}.
     *
     * @param args application arguments (not used)
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting tenant reconciliation...");
        this.reconcile();
        log.info("Tenant reconciliation completed.");
    }

    /**
     * Seeds tenants from config into the database. Each config entry is imported only if no tenant
     * with that slug already exists; existing tenants are never modified or deleted.
     *
     * <p>Has no effect if the tenant configuration list is empty.
     */
    public void reconcile() {
        List<TenantProperties.TenantConfig> configList = tenantProperties.getConfig();

        if (configList.isEmpty()) {
            return;
        }

        configList.forEach(tenantService::importFromConfig);
    }
}
