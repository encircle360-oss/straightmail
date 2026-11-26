/**
 * Read-only tenant data transfer object returned by the tenant endpoints.
 *
 * Sensitive fields (SMTP password, Git token, API key hash) are intentionally omitted.
 */
export interface TenantDTO {
  /** URL-safe unique identifier for the tenant. */
  slug: string;
  /** Human-readable display name shown in the UI. */
  displayName: string;
  /** Optional SMTP server hostname. */
  smtpHost?: string;
  /** Optional SMTP server port. */
  smtpPort?: number;
  /** Optional SMTP authentication username. */
  smtpUser?: string;
  /** Optional default sender email address for this tenant. */
  smtpSender?: string;
  /** Whether STARTTLS is enabled for the SMTP connection. */
  smtpTls: boolean;
  /** Whether implicit SSL is enabled for the SMTP connection. */
  smtpSsl: boolean;
  /** Optional URL of the Git repository used for template sync. */
  gitRepoUrl?: string;
  /** Optional list of branch names to sync from the Git repository. */
  gitBranches?: string[];
  /** Whether an API key has been configured for this tenant. */
  hasApiKey: boolean;
  /** Whether this tenant is active and can receive emails. */
  active: boolean;
  /** Optional URL of the tenant logo image. Will be set once logo upload is supported. */
  logoUrl?: string;
  /** Optional brand color as a CSS hex value (e.g. #3a86ff). Used as avatar background in the grid view. */
  brandColor?: string;
  /** Whether this tenant can be modified via API. false for config-based tenants, true or absent for DB-backed tenants. */
  editable?: boolean;
}
