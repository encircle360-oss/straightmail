import { RenderRequest, RenderResponse } from '../../../core/services/api.service';

/** Actions and events for the {@link RenderState} NGXS state slice. */
export namespace RenderActions {
  /** Submits a template render request to the backend. */
  export class Render {
    static readonly type = '[Render] Render';

    /**
     * @param req The render request containing the template ID and optional model.
     */
    constructor(public req: RenderRequest) {}
  }

  /** Clears the current render result and error from state. */
  export class Clear {
    static readonly type = '[Render] Clear';
  }

  /** Persists the current form values so they survive navigation. */
  export class SaveForm {
    static readonly type = '[Render] Save Form';

    /**
     * @param values The template ID and JSON model string entered by the user.
     */
    constructor(public values: { templateId: string; model: string }) {}
  }

  // ── Events ────────────────────────────────────────────────────────────────

  /** Emitted when a render request completes successfully. */
  export class RenderSuccess {
    static readonly type = '[Render] Render Success';

    /**
     * @param result The rendered HTML and plain-text content.
     */
    constructor(public result: RenderResponse) {}
  }

  /** Emitted when a render request fails. */
  export class RenderFailed {
    static readonly type = '[Render] Render Failed';

    /**
     * @param error The error message from the failed request.
     */
    constructor(public error: string) {}
  }
}
