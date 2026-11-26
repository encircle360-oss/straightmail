import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Request payload for sending an email using a stored template referenced by ID. */
export interface SendEmailRequest {
  /** List of primary recipient email addresses. */
  recipients: string[];
  /** Sender email address. */
  sender: string;
  /** ID of the stored email template to use. */
  emailTemplateId: string;
  /** Optional email subject (overrides the template's default subject). */
  subject?: string;
  /** BCP 47 locale tag used for template selection (e.g. {@code en}, {@code de}). */
  locale?: string;
  /** Optional CC recipient email addresses. */
  cc?: string[];
  /** Optional BCC recipient email addresses. */
  bcc?: string[];
  /** Optional data model passed to the FreeMarker template for variable substitution. */
  model?: Record<string, unknown>;
}

/** Request payload for sending an email with a FreeMarker template provided inline. */
export interface SendInlineEmailRequest {
  /** List of primary recipient email addresses. */
  recipients: string[];
  /** Sender email address. */
  sender: string;
  /** Email subject line. */
  subject: string;
  /** Inline FreeMarker template string. */
  emailTemplate: string;
  /** BCP 47 locale tag used for rendering (e.g. {@code en}, {@code de}). */
  locale?: string;
  /** Optional CC recipient email addresses. */
  cc?: string[];
  /** Optional BCC recipient email addresses. */
  bcc?: string[];
  /** Optional data model passed to the FreeMarker template for variable substitution. */
  model?: Record<string, unknown>;
}

/** Request payload for rendering a template without sending an email. */
export interface RenderRequest {
  /** ID of the template to render. */
  templateId: string;
  /** Optional data model passed to the FreeMarker template for variable substitution. */
  model?: Record<string, unknown>;
}

/** Response from a template render operation, containing both HTML and plain-text output. */
export interface RenderResponse {
  /** Rendered HTML body. */
  html: string;
  /** Rendered plain-text body. */
  plain: string;
}

/** Represents a single email template as returned by the backend API. */
export interface EmailTemplate {
  /** Unique template identifier (absent for new templates). */
  id?: string;
  /** Template name used to identify it within a tenant. */
  name: string;
  /** BCP 47 locale tag this template variant is associated with. */
  locale: string;
  /** Tags for categorising and filtering the template. */
  tags: string[];
  /** Optional email subject stored alongside the template. */
  subject?: string;
  /** Optional HTML body of the template. */
  html?: string;
  /** Optional plain-text body of the template. */
  plain?: string;
  /** Origin of the template — DATABASE templates are editable; GIT and FILE templates are read-only. */
  source?: 'DATABASE' | 'GIT' | 'FILE';
  /** Whether this template can be created, updated, or deleted via the API. */
  editable?: boolean;
  /** Git branch this template was synced from; present only for GIT-source templates. */
  gitBranch?: string;
}

/** Outcome of the most recent Git-sync attempt for a single tenant. */
export type SyncResult = 'SUCCESS' | 'FAILED' | 'NEVER';

/** Per-tenant Git-sync status snapshot as returned by {@code GET /v1/status}. */
export interface GitSyncStatusDTO {
  /** Tenant slug. */
  tenantId: string;
  /** ISO-8601 timestamp of the last sync attempt, or {@code null} if never attempted. */
  lastSyncAt: string | null;
  /** Outcome of the most recent sync attempt. */
  result: SyncResult;
  /** Human-readable error when {@code result} is {@code FAILED}; absent otherwise. */
  errorMessage?: string;
}

/** Accessibility status of the configured file-template base directory. */
export interface FileStatusDTO {
  /** {@code true} if the directory exists and is readable. */
  accessible: boolean;
}

/** Aggregated sync status returned by {@code GET /v1/status}. */
export interface StatusDTO {
  /** Per-tenant Git-sync status; empty when Git-sync is disabled. */
  gitSync: GitSyncStatusDTO[];
  /** File-template directory status; {@code null} when the file provider is disabled. */
  fileTemplates: FileStatusDTO | null;
}

/**
 * Generic paginated response wrapper matching Spring's {@code Page<T>} serialisation.
 *
 * @template T The type of items contained in this page.
 */
export interface PageResponse<T> {
  /** Items on the current page. */
  content: T[];
  /** Total number of items across all pages. */
  totalElements: number;
  /** Total number of pages. */
  totalPages: number;
  /** Zero-based index of the current page. */
  number: number;
  /** Maximum number of items per page. */
  size: number;
}

/**
 * Central HTTP client facade for all backend API calls.
 *
 * All requests are sent to the base URL resolved from {@code environment.apiUrl}, which is
 * populated at bootstrap by the runtime configuration fetched from {@code GET /v1/info}.
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  /**
   * Sends an email using a stored template identified by ID.
   *
   * @param req The send-email request payload.
   * @returns An observable that completes when the email has been accepted by the backend.
   */
  sendEmail(req: SendEmailRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/v1/email`, req);
  }

  /**
   * Sends an email using an inline FreeMarker template string.
   *
   * @param req The inline send-email request payload.
   * @returns An observable that completes when the email has been accepted by the backend.
   */
  sendInlineEmail(req: SendInlineEmailRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/v1/email/inline`, req);
  }

  /**
   * Renders a stored template without sending an email and returns the HTML and plain-text output.
   *
   * @param req The render request containing the template ID and optional model.
   * @returns An observable emitting the rendered HTML and plain-text content.
   */
  render(req: RenderRequest): Observable<RenderResponse> {
    return this.http.post<RenderResponse>(`${this.apiUrl}/v1/render`, req);
  }

  /**
   * Fetches a single email template by its ID.
   *
   * @param id The unique template identifier.
   * @returns An observable emitting the template.
   */
  getTemplate(id: string): Observable<EmailTemplate> {
    return this.http.get<EmailTemplate>(`${this.apiUrl}/v1/templates/${id}`);
  }

  /**
   * Fetches a paginated list of email templates, optionally filtered by one or more tags.
   * Multiple tags are ANDed: only templates matching all supplied tags are returned.
   *
   * @param page Zero-based page index (default {@code 0}).
   * @param size Maximum number of items per page (default {@code 20}).
   * @param tags Optional tags to filter templates by. Each tag is sent as a separate {@code tag} query parameter.
   * @returns An observable emitting the paginated template list.
   */
  getTemplates(page = 0, size = 20, tags?: string[]): Observable<PageResponse<EmailTemplate>> {
    let params = new HttpParams().set('page', page).set('size', size);
    tags?.forEach((t) => (params = params.append('tag', t)));
    return this.http.get<PageResponse<EmailTemplate>>(`${this.apiUrl}/v1/templates`, { params });
  }

  /**
   * Creates a new email template.
   *
   * @param template The template data to persist.
   * @returns An observable emitting the newly created template (with assigned ID).
   */
  createTemplate(template: EmailTemplate): Observable<EmailTemplate> {
    return this.http.post<EmailTemplate>(`${this.apiUrl}/v1/templates`, template);
  }

  /**
   * Updates an existing email template.
   *
   * @param id The unique identifier of the template to update.
   * @param template The updated template data.
   * @returns An observable emitting the updated template.
   */
  updateTemplate(id: string, template: EmailTemplate): Observable<EmailTemplate> {
    return this.http.put<EmailTemplate>(`${this.apiUrl}/v1/templates/${id}`, template);
  }

  /**
   * Deletes an email template by its ID.
   *
   * @param id The unique identifier of the template to delete.
   * @returns An observable that completes when the template has been deleted.
   */
  deleteTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/v1/templates/${id}`);
  }

  /**
   * Fetches the aggregated sync status for all template sources (Git-sync per tenant,
   * file-template directory accessibility).
   *
   * @returns An observable emitting the {@link StatusDTO}; errors when the endpoint is unavailable
   *          (e.g. backend running without the {@code database} profile).
   */
  getStatus(): Observable<StatusDTO> {
    return this.http.get<StatusDTO>(`${this.apiUrl}/v1/status`);
  }

  /**
   * Triggers an immediate Git-sync for the given tenant and returns the updated sync status.
   *
   * @param tenantSlug The tenant slug to sync.
   * @returns An observable emitting the updated {@link GitSyncStatusDTO} for the tenant.
   */
  triggerGitSync(tenantSlug: string): Observable<GitSyncStatusDTO> {
    return this.http.post<GitSyncStatusDTO>(`${this.apiUrl}/v1/sync/git/${tenantSlug}`, null);
  }

  /**
   * Fetches backend info/health data, returning the full HTTP response (including status code).
   *
   * @returns An observable emitting the raw HTTP response.
   */
  getInfo(): Observable<any> {
    return this.http.get(`${this.apiUrl}/v1/info`, { observe: 'response' });
  }

  /**
   * Returns the resolved API base URL (runtime or compiled environment value).
   *
   * @returns The API base URL string.
   */
  getBase(): string {
    return this.apiUrl;
  }
}
