package com.encircle360.oss.straightmail.config;

import com.encircle360.oss.straightmail.service.template.provider.FileTemplateSourceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Logs all resolved environment-configuration values once after application startup.
 * Secrets ({@code encryption.key}, {@code api.key}) are masked so they never appear in plain text.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupConfigLogger {

    @Value("${cors.allowed-origins}")
    private String corsAllowedOrigins;

    @Value("${encryption.key}")
    private String encryptionKey;

    @Value("${jwt.tenant-claim}")
    private String jwtTenantClaim;

    @Value("${jwt.tenant-ids-claim}")
    private String jwtTenantIdsClaim;

    @Value("${auth.mode:oidc}")
    private String authMode;

    @Value("${auth.issuer-uri:}")
    private String authIssuerUri;

    @Value("${api.prefix}")
    private String apiPrefix;

    @Value("${api.key:}")
    private String apiKey;

    @Value("${templates.git-sync.base-path:}")
    private String gitSyncBasePath;

    @Value("${templates.git-sync.enabled}")
    private boolean gitSyncEnabled;

    @Value("${templates.git-sync.cron}")
    private String gitSyncCron;

    @Value("${templates.git-sync.lock-at-least}")
    private String gitSyncLockMin;

    @Value("${templates.git-sync.lock-at-most}")
    private String gitSyncLockMax;

    @Value("${templates.file.enabled}")
    private boolean fileEnabled;

    @Value("${templates.file.base-path}")
    private String fileBasePath;

    private final ObjectProvider<FileTemplateSourceProvider> fileTemplateProvider;

    @EventListener(ApplicationReadyEvent.class)
    public void logConfig() {
        FileTemplateSourceProvider provider = fileTemplateProvider.getIfAvailable();
        String fileTemplateCountDisplay = fileEnabled
                ? String.valueOf(provider != null ? provider.countAllTemplates() : 0L)
                : "(disabled)";

        log.info("""
                        \n╔════════════════════════════════════════════════════════════════════════════╗
                        ║         straightmail — active configuration                                ║
                        ╚════════════════════════════════════════════════════════════════════════════╝
                          CORS allowed-origins   : {}
                          Encryption key         : {}
                          JWT tenant-claim       : {}
                          JWT tenant-ids-claim   : {}
                          Auth mode              : {}
                          Auth issuer-uri        : {}
                          API prefix             : {}
                          API key                : {}
                          Git-sync enabled       : {}
                          Git-sync base path     : {}
                          Git-sync cron          : {}
                          Git-sync lock-min      : {}
                          Git-sync lock-max      : {}
                          File templates enabled : {}
                          File base-path         : {}
                          File templates count   : {}
                        ══════════════════════════════════════════════════════════════════════════════""",
                corsAllowedOrigins,
                this.maskSecret(encryptionKey),
                jwtTenantClaim,
                jwtTenantIdsClaim,
                authMode,
                !authMode.equals("oidc") || authIssuerUri.isBlank() ? "(not set)" : authIssuerUri,
                apiPrefix,
                this.maskSecret(apiKey),
                gitSyncEnabled,
                gitSyncBasePath,
                gitSyncCron,
                gitSyncLockMin,
                gitSyncLockMax,
                fileEnabled,
                fileBasePath,
                fileTemplateCountDisplay
        );
    }

    private String maskSecret(String value) {
        if (value == null || value.isBlank()) {
            return "(not set)";
        }
        return value.length() <= 4 ? "***" : value.substring(0, 4) + "***";
    }
}
