package com.encircle360.oss.straightmail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for statically declared tenants under the {@code tenants} prefix.
 *
 * <p>Tenants declared here are reconciled with the database at startup by
 * {@link com.encircle360.oss.straightmail.service.TenantReconciliationService}. This allows
 * tenants to be provisioned via {@code application.yml} or environment variables without
 * requiring manual API calls.
 *
 * <p>Example configuration:
 * <pre>{@code
 * tenants:
 *   config:
 *     - id: acme
 *       displayName: ACME Corp
 *       smtpHost: smtp.acme.com
 *       smtpPort: 587
 * }</pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "tenants")
public class TenantProperties {

    /**
     * Slug of the built-in default tenant that is protected from deletion.
     * Defaults to {@code "default"} and can be overridden via {@code tenants.default-id}
     * (env: {@code TENANTS_DEFAULT_ID}).
     */
    private String defaultId = "default";

    /**
     * List of statically configured tenants to reconcile at startup.
     */
    private List<TenantConfig> config = new ArrayList<>();

    /**
     * Configuration block for a single statically declared tenant.
     */
    @Data
    public static class TenantConfig {
        /**
         * Unique slug identifier for the tenant (must match {@link com.encircle360.oss.straightmail.tenant.TenantValidation#SLUG_PATTERN}).
         */
        private String id;
        /**
         * Human-readable display name.
         */
        private String displayName;
        /**
         * SMTP server host. Falls back to the global SMTP host if not set.
         */
        private String smtpHost;
        /**
         * SMTP server port. Falls back to the global SMTP port if not set.
         */
        private Integer smtpPort;
        /**
         * SMTP authentication username.
         */
        private String smtpUser;
        /**
         * SMTP authentication password (will be encrypted before storage).
         */
        private String smtpPassword;
        /**
         * Default sender address for outgoing emails.
         */
        private String smtpSender;
        /**
         * Whether to enable STARTTLS for this tenant's SMTP connection.
         */
        private boolean smtpTls;
        /**
         * Whether to enable SSL for this tenant's SMTP connection.
         */
        private boolean smtpSsl;
        /**
         * Git access token for private repositories (will be encrypted before storage).
         */
        private String gitToken;
        /**
         * URL of the Git repository containing FreeMarker templates.
         */
        private String gitRepoUrl;
        /**
         * Branches to synchronise from the Git repository (defaults to {@code main}).
         */
        private List<String> gitBranches;
        /**
         * Tenant logo URL for branding purposes.
         */
        private String logoUrl;
        /**
         * Brand colour in hex format (e.g. {@code #FF5500}) for tenant branding.
         */
        private String brandColor;
        /**
         * Plain-text API key (write-only). Stored as a SHA-256 hash in the database.
         * Should be supplied via an environment variable to avoid plain-text secrets in version control.
         */
        private String apiKey;
        /**
         * Whether this tenant is active. Defaults to {@code true} when not set.
         * Use {@code null} to leave the existing value unchanged during reconciliation updates;
         * use {@code false} to explicitly disable a tenant via config.
         */
        private Boolean active;
    }
}
