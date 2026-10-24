import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { UiActions } from './ui.actions';
import { TranslateService } from '@ngx-translate/core';

/** Shape of the data persisted in the NGXS {@link UiState}. */
export interface UiStateModel {
  /** Map of accordion panel keys to their collapsed state ({@code true} = collapsed). */
  accordionStatus: Record<string, boolean>;
  /** Currently active BCP 47 language code (e.g. {@code en}, {@code de}). */
  language: string;
  /** Preferred display mode for the tenant list page ({@code grid} or {@code list}). */
  tenantsViewMode: 'grid' | 'list';
  /** Preferred display mode for the templates list page ({@code grid} or {@code list}). */
  templatesViewMode: 'grid' | 'list';
  /** Last active tab in the tenant create/edit form ({@code smtp}, {@code git}, or {@code security}). */
  tenantFormTab: 'smtp' | 'git' | 'security';
}

/**
 * NGXS state for UI preferences.
 *
 * Persists user interface preferences across sessions, including accordion panel states and the
 * selected display language. Language changes are immediately applied to {@link TranslateService}.
 * State is persisted in {@code LOCAL_STORAGE} via the NGXS persist plugin.
 */
@State<UiStateModel>({
  name: 'ui',
  defaults: {
    accordionStatus: {},
    language: 'en',
    tenantsViewMode: 'grid',
    templatesViewMode: 'list',
    tenantFormTab: 'smtp',
  },
})
@Injectable()
export class UiState {
  private readonly translate = inject(TranslateService);

  /** Selector that emits the map of accordion panel collapsed states. */
  @Selector()
  static accordionStatus(state: UiStateModel): Record<string, boolean> {
    return state.accordionStatus ?? {};
  }

  /** Selector that emits the currently active UI language code. */
  @Selector()
  static language(state: UiStateModel): string {
    return state.language;
  }

  /** Selector that emits the preferred tenant list view mode. */
  @Selector()
  static tenantsViewMode(state: UiStateModel): 'grid' | 'list' {
    return state.tenantsViewMode ?? 'grid';
  }

  /** Selector that emits the preferred templates list view mode. */
  @Selector()
  static templatesViewMode(state: UiStateModel): 'grid' | 'list' {
    return state.templatesViewMode ?? 'list';
  }

  /** Selector that emits the last active tab in the tenant form. */
  @Selector()
  static tenantFormTab(state: UiStateModel): 'smtp' | 'git' | 'security' {
    return state.tenantFormTab ?? 'smtp';
  }

  @Action(UiActions.UpdateAccordionStatus)
  updateAccordionStatus(ctx: StateContext<UiStateModel>, action: UiActions.UpdateAccordionStatus) {
    ctx.patchState({
      accordionStatus: {
        ...ctx.getState().accordionStatus,
        [action.key]: action.collapsed,
      },
    });
  }

  @Action(UiActions.SetLanguage)
  setLanguage(ctx: StateContext<UiStateModel>, action: UiActions.SetLanguage) {
    ctx.patchState({
      language: action.lang,
    });
    this.translate.use(action.lang);
  }

  @Action(UiActions.SetTenantsViewMode)
  setTenantsViewMode(ctx: StateContext<UiStateModel>, action: UiActions.SetTenantsViewMode) {
    ctx.patchState({ tenantsViewMode: action.mode });
  }

  @Action(UiActions.SetTemplatesViewMode)
  setTemplatesViewMode(ctx: StateContext<UiStateModel>, action: UiActions.SetTemplatesViewMode) {
    ctx.patchState({ templatesViewMode: action.mode });
  }

  @Action(UiActions.SetTenantFormTab)
  setTenantFormTab(ctx: StateContext<UiStateModel>, action: UiActions.SetTenantFormTab) {
    ctx.patchState({ tenantFormTab: action.tab });
  }
}
