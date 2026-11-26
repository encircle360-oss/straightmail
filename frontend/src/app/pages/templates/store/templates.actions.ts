import { EmailTemplate } from '../../../core/services/api.service';

/** Actions and events for the {@link TemplatesState} NGXS state slice. */
export namespace TemplatesActions {
  /** Loads a paginated and optionally tag-filtered list of templates from the backend. */
  export class LoadTemplates {
    static readonly type = '[Templates] Load';

    /**
     * @param page Zero-based page index (default {@code 0}).
     * @param size Maximum number of items per page (default {@code 20}).
     * @param tags Optional tags to filter templates by (ANDed). Each tag is sent as a separate query parameter.
     */
    constructor(
      public page = 0,
      public size = 20,
      public tags?: string[],
    ) {}
  }

  /** Creates a new email template via the backend API. */
  export class CreateTemplate {
    static readonly type = '[Templates] Create';

    /**
     * @param template The template data to persist.
     */
    constructor(public template: EmailTemplate) {}
  }

  /** Updates an existing email template by ID via the backend API. */
  export class UpdateTemplate {
    static readonly type = '[Templates] Update';

    /**
     * @param id The unique identifier of the template to update.
     * @param template The updated template data.
     */
    constructor(
      public id: string,
      public template: EmailTemplate,
    ) {}
  }

  /** Deletes an email template by ID via the backend API. */
  export class DeleteTemplate {
    static readonly type = '[Templates] Delete';

    /**
     * @param id The unique identifier of the template to delete.
     */
    constructor(public id: string) {}
  }

  /** Sets the active tag filter and reloads the template list from page zero. */
  export class SetActiveTag {
    static readonly type = '[Templates] Set Active Tag';

    /**
     * @param tag The tag to filter by, or {@code undefined} to clear the filter.
     */
    constructor(public tag: string | undefined) {}
  }

  /**
   * Switches the active source tab, clears any custom tag filter, and reloads templates
   * from page zero filtered by the corresponding source tag.
   */
  export class SetActiveSourceTab {
    static readonly type = '[Templates] Set Active Source Tab';

    /**
     * @param tab The source tab to activate. {@code 'ALL'} shows all templates unfiltered by source.
     */
    constructor(public tab: 'ALL' | 'DATABASE' | 'FILE' | 'GIT') {}
  }

  // ── Events ────────────────────────────────────────────────────────────────

  /** Emitted when a template creation request completes successfully. */
  export class CreateTemplateSuccess {
    static readonly type = '[Templates] Create Success';

    /**
     * @param template The newly created template returned by the backend.
     */
    constructor(public template: EmailTemplate) {}
  }

  /** Emitted when a template creation request fails. */
  export class CreateTemplateFailed {
    static readonly type = '[Templates] Create Failed';

    /**
     * @param error The error message from the failed request.
     */
    constructor(public error: string) {}
  }

  /** Emitted when a template update request completes successfully. */
  export class UpdateTemplateSuccess {
    static readonly type = '[Templates] Update Success';

    /**
     * @param template The updated template returned by the backend.
     */
    constructor(public template: EmailTemplate) {}
  }

  /** Emitted when a template update request fails. */
  export class UpdateTemplateFailed {
    static readonly type = '[Templates] Update Failed';

    /**
     * @param error The error message from the failed request.
     */
    constructor(public error: string) {}
  }

  /** Emitted when a template deletion request completes successfully. */
  export class DeleteTemplateSuccess {
    static readonly type = '[Templates] Delete Success';

    /**
     * @param id The ID of the deleted template.
     */
    constructor(public id: string) {}
  }

  /** Emitted when a template deletion request fails. */
  export class DeleteTemplateFailed {
    static readonly type = '[Templates] Delete Failed';

    /**
     * @param error The error message from the failed request.
     */
    constructor(public error: string) {}
  }
}
