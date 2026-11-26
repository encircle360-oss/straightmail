import { SendEmailRequest, SendInlineEmailRequest } from '../../../core/services/api.service';

/** Actions and events for the {@link SendState} NGXS state slice. */
export namespace SendActions {
  /** Sends an email using a stored template referenced by ID. */
  export class SendByTemplate {
    static readonly type = '[Send] Send By Template';

    /**
     * @param req The send-by-template request payload.
     */
    constructor(public req: SendEmailRequest) {}
  }

  /** Sends an email using an inline FreeMarker template string. */
  export class SendInline {
    static readonly type = '[Send] Send Inline';

    /**
     * @param req The inline send request payload.
     */
    constructor(public req: SendInlineEmailRequest) {}
  }

  /** Resets the loading and error state without clearing the stored form values. */
  export class Reset {
    static readonly type = '[Send] Reset';
  }

  /** Persists the current form values for the active tab so they survive navigation. */
  export class SaveForm {
    static readonly type = '[Send] Save Form';

    /**
     * @param tab Which form to save: {@code 'byId'} for template-ID mode, {@code 'inline'} for inline mode.
     * @param values The form field values to persist.
     */
    constructor(
      public tab: 'byId' | 'inline',
      public values: Record<string, unknown>,
    ) {}
  }

  /** Updates the index of the active send tab. */
  export class UpdateActiveTab {
    static readonly type = '[Send] Update Active Tab';

    /**
     * @param activeTab Zero-based index of the tab to activate.
     */
    constructor(public activeTab: number) {}
  }

  // ── Events ────────────────────────────────────────────────────────────────

  /** Emitted when a template-ID send request completes successfully. */
  export class SendByTemplateSuccess {
    static readonly type = '[Send] Send By Template Success';
  }

  /** Emitted when an inline send request completes successfully. */
  export class SendInlineSuccess {
    static readonly type = '[Send] Send Inline Success';
  }

  /** Emitted when any send request fails. */
  export class SendFailed {
    static readonly type = '[Send] Send Failed';

    /**
     * @param error The error message from the failed request.
     */
    constructor(public error: string) {}
  }
}
