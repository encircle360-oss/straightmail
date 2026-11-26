package com.encircle360.oss.straightmail.dto.tenant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO representing a tenant's public configuration.
 *
 * <p>Returned by all read endpoints under {@code /v1/tenants}. Sensitive fields such as
 * {@code smtpPassword}, {@code gitToken}, and the raw API key are intentionally omitted.
 * The {@code hasApiKey} flag indicates whether a per-tenant API key is configured
 * without exposing the key itself.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Tenant")
public class TenantDTO {

    @Schema(description = "Unique slug identifier of the tenant")
    private String slug;

    @Schema(description = "Display name of the tenant")
    private String displayName;

    @Schema(description = "URL of the tenant logo image")
    private String logoUrl;

    @Schema(description = "Brand color as a CSS hex value (e.g. #3a86ff)")
    private String brandColor;

    @Schema(description = "SMTP host for outgoing mail")
    private String smtpHost;

    @Schema(description = "SMTP port for outgoing mail")
    private Integer smtpPort;

    @Schema(description = "SMTP username")
    private String smtpUser;

    // smtpPassword: intentionally omitted — write-only, never returned in responses

    @Schema(description = "Default sender email address")
    private String smtpSender;

    @Schema(description = "Enable STARTTLS for SMTP")
    private boolean smtpTls;

    @Schema(description = "Enable SSL for SMTP")
    private boolean smtpSsl;

    @Schema(description = "Git repository URL for template sync")
    private String gitRepoUrl;

    @Schema(description = "Git branches to sync templates from")
    private List<String> gitBranches;

    // gitToken: intentionally omitted — write-only, never returned in responses

    @Schema(description = "Whether a per-tenant API key is configured (key itself is never returned)")
    private boolean hasApiKey;

    @Schema(description = "Whether this tenant is active")
    private boolean active;

    @Schema(description = "Whether this tenant can be modified via API. false for config-based tenants.")
    private boolean editable;
}
