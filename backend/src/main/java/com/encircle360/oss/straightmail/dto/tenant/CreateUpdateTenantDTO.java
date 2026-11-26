package com.encircle360.oss.straightmail.dto.tenant;

import com.encircle360.oss.straightmail.tenant.TenantValidation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.util.List;

/**
 * Request DTO for creating or updating a tenant via the admin API.
 *
 * <p>Used with {@code POST /v1/tenants} (create) and {@code PUT /v1/tenants/{slug}} (update).
 * Sensitive fields ({@code smtpPassword}, {@code gitToken}, {@code apiKey}) are write-only:
 * they are encrypted or hashed before storage and never returned in responses.
 * Leave sensitive fields blank on update to retain their existing values.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateUpdateTenant")
public class CreateUpdateTenantDTO {

    @NotBlank(message = "Slug is required")
    @Pattern(regexp = TenantValidation.SLUG_PATTERN, message = "Slug must be lowercase alphanumeric with hyphens")
    @Schema(description = "Unique slug (immutable after creation, e.g. acme-corp)")
    private String slug;

    @NotBlank(message = "Display name is required")
    @Schema(description = "Display name of the tenant")
    private String displayName;

    @URL(message = "Logo URL must be a valid URL")
    @Schema(description = "Optional URL of the tenant logo image")
    private String logoUrl;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Brand color must be a valid hex color (e.g. #3a86ff)")
    @Schema(description = "Optional brand color as a CSS hex value (e.g. #3a86ff)")
    private String brandColor;

    @Schema(description = "SMTP host for outgoing mail")
    private String smtpHost;

    @Schema(description = "SMTP port for outgoing mail")
    private Integer smtpPort;

    @Schema(description = "SMTP username")
    private String smtpUser;

    @Schema(description = "SMTP password (write-only, encrypted at rest)")
    private String smtpPassword;

    @Schema(description = "Default sender email address")
    private String smtpSender;

    @Schema(description = "Enable STARTTLS for SMTP")
    private boolean smtpTls;

    @Schema(description = "Enable SSL for SMTP")
    private boolean smtpSsl;

    @Schema(description = "Git token for repository access (write-only, encrypted at rest)")
    private String gitToken;

    @Schema(description = "Git repository URL for template sync")
    private String gitRepoUrl;

    @Schema(description = "Git branches to sync templates from")
    private List<String> gitBranches;

    @Schema(description = "Per-tenant API key (write-only, stored as SHA-256 hash, never returned). Set to issue a new key; leave blank to keep existing.")
    private String apiKey;

    @Builder.Default
    @Schema(description = "Whether this tenant is active")
    private boolean active = true;
}
