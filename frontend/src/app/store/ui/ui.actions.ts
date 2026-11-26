/** Actions for the {@link UiState} NGXS state slice. */
export namespace UiActions {
  /** Updates the collapsed/expanded state of an accordion panel identified by key. */
  export class UpdateAccordionStatus {
    static readonly type = '[UI] Update Accordion Status';

    /**
     * @param key Unique identifier for the accordion panel (e.g. component name + section).
     * @param collapsed {@code true} if the panel should be collapsed, {@code false} if expanded.
     */
    constructor(
      public key: string,
      public collapsed: boolean,
    ) {}
  }

  /** Persists the preferred tenant list view mode ({@code grid} or {@code list}). */
  export class SetTenantsViewMode {
    static readonly type = '[UI] Set Tenants View Mode';

    /**
     * @param mode The view mode to activate: {@code grid} for card grid, {@code list} for table.
     */
    constructor(public mode: 'grid' | 'list') {}
  }

  /** Persists the preferred templates list view mode ({@code grid} or {@code list}). */
  export class SetTemplatesViewMode {
    static readonly type = '[UI] Set Templates View Mode';

    /**
     * @param mode The view mode to activate: {@code grid} for card grid, {@code list} for table.
     */
    constructor(public mode: 'grid' | 'list') {}
  }

  /** Persists the last active tab in the tenant create/edit form. */
  export class SetTenantFormTab {
    static readonly type = '[UI] Set Tenant Form Tab';

    /**
     * @param tab The tab identifier to activate ({@code smtp}, {@code git}, or {@code security}).
     */
    constructor(public tab: 'smtp' | 'git' | 'security') {}
  }

  /** Persists the selected UI language and applies it to {@code TranslateService}. */
  export class SetLanguage {
    static readonly type = '[UI] Set Language';

    /**
     * @param lang BCP 47 language code to activate (e.g. {@code en}, {@code de}).
     */
    constructor(public lang: string) {}
  }
}
