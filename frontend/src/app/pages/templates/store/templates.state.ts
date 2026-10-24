import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { catchError, EMPTY, tap } from 'rxjs';
import { ApiService, EmailTemplate } from '../../../core/services/api.service';
import { ShowToast } from '../../../store/toast/toast.actions';
import { TenantActions } from '../../../store/tenant/tenant.actions';
import { TemplatesActions } from './templates.actions';

/** The active source tab in the templates list view. */
export type TemplateSourceTab = 'ALL' | 'DATABASE' | 'FILE' | 'GIT';

/** Shape of the data persisted in the NGXS {@link TemplatesState}. */
export interface TemplatesStateModel {
  /** The currently loaded page of email templates. */
  templates: EmailTemplate[];
  /** Total number of templates across all pages (for pagination UI). */
  totalElements: number;
  /** Zero-based index of the currently displayed page. */
  currentPage: number;
  /** Number of items per page. */
  pageSize: number;
  /** Whether a backend request is currently in flight. */
  loading: boolean;
  /** Currently active custom tag filter, or {@code undefined} when unfiltered. */
  activeTag: string | undefined;
  /** The currently active source tab. Determines which implicit source tag is prepended to API requests. */
  activeSourceTab: TemplateSourceTab;
  /**
   * All known custom tags for the current source scope, collected from the last unfiltered load.
   * Used to keep tag filter buttons visible even when a tag filter is active.
   */
  allTags: string[];
}

/**
 * NGXS state for the email templates management page.
 *
 * Manages CRUD operations for email templates stored in the backend database. Supports paginated
 * loading with optional tag filtering. The state is reset when the active tenant changes
 * (reacting to {@link TenantActions.SelectTenant}) to avoid showing stale cross-tenant data.
 */
@State<TemplatesStateModel>({
  name: 'templates',
  defaults: {
    templates: [],
    totalElements: 0,
    currentPage: 0,
    pageSize: 20,
    loading: false,
    activeTag: undefined,
    activeSourceTab: 'ALL',
    allTags: [],
  },
})
@Injectable()
export class TemplatesState {
  private readonly api = inject(ApiService);

  /** Selector that emits the currently loaded page of email templates. */
  @Selector()
  static templates(state: TemplatesStateModel): EmailTemplate[] {
    return state.templates;
  }

  /** Selector that emits {@code true} while a backend request is in flight. */
  @Selector()
  static loading(state: TemplatesStateModel): boolean {
    return state.loading;
  }

  /** Selector that emits the total number of templates across all pages. */
  @Selector()
  static totalElements(state: TemplatesStateModel): number {
    return state.totalElements;
  }

  /** Selector that emits the zero-based index of the currently displayed page. */
  @Selector()
  static currentPage(state: TemplatesStateModel): number {
    return state.currentPage;
  }

  /** Selector that emits the number of items per page. */
  @Selector()
  static pageSize(state: TemplatesStateModel): number {
    return state.pageSize;
  }

  /** Selector that emits the active custom tag filter, or {@code undefined} when unfiltered. */
  @Selector()
  static activeTag(state: TemplatesStateModel): string | undefined {
    return state.activeTag;
  }

  /** Selector that emits the currently active source tab. */
  @Selector()
  static activeSourceTab(state: TemplatesStateModel): TemplateSourceTab {
    return state.activeSourceTab ?? 'ALL';
  }

  /**
   * Selector that emits all known custom tags for the current source scope. Updated on every
   * unfiltered load so that tag filter buttons remain visible when a tag filter is active.
   */
  @Selector()
  static allTags(state: TemplatesStateModel): string[] {
    return state.allTags ?? [];
  }

  /**
   * Builds the effective tag list for an API request from the current state. Combines the implicit
   * source tag (when a source tab other than 'ALL' is active) with any custom tag filter. Returns
   * {@code undefined} when no filtering applies.
   */
  static resolveTags(state: TemplatesStateModel): string[] | undefined {
    const tags: string[] = [];
    if (state.activeSourceTab !== 'ALL') {
      tags.push(`source:${state.activeSourceTab.toLowerCase()}`);
    }
    if (state.activeTag) {
      tags.push(state.activeTag);
    }
    return tags.length > 0 ? tags : undefined;
  }

  @Action(TemplatesActions.LoadTemplates)
  load(ctx: StateContext<TemplatesStateModel>, action: TemplatesActions.LoadTemplates) {
    ctx.patchState({ loading: true });
    return this.api.getTemplates(action.page, action.size, action.tags).pipe(
      tap((page) => {
        const state = ctx.getState();
        const patch: Partial<TemplatesStateModel> = {
          loading: false,
          templates: page.content,
          totalElements: page.totalElements,
          currentPage: action.page,
          pageSize: action.size,
        };
        // Update the known tag list only when no custom tag filter is active so that
        // all filter buttons remain visible even after a tag filter narrows the result.
        if (!state.activeTag) {
          const tags = page.content
            .flatMap((t) => t.tags ?? [])
            .filter((t) => !t.startsWith('source:'));
          patch.allTags = [...new Set(tags)].sort();
        }
        ctx.patchState(patch);
      }),
      catchError((err) => {
        ctx.patchState({ loading: false });
        ctx.dispatch(new ShowToast(err.message, 'error'));
        return EMPTY;
      }),
    );
  }

  @Action(TemplatesActions.CreateTemplate)
  create(ctx: StateContext<TemplatesStateModel>, action: TemplatesActions.CreateTemplate) {
    ctx.patchState({ loading: true });
    return this.api.createTemplate(action.template).pipe(
      tap((template) => {
        ctx.patchState({ loading: false });
        ctx.dispatch([
          new TemplatesActions.CreateTemplateSuccess(template),
          new ShowToast('templates.created', 'success'),
        ]);
        const s = ctx.getState();
        ctx.dispatch(
          new TemplatesActions.LoadTemplates(
            s.currentPage,
            s.pageSize,
            TemplatesState.resolveTags(s),
          ),
        );
      }),
      catchError((err) => {
        ctx.patchState({ loading: false });
        ctx.dispatch([
          new TemplatesActions.CreateTemplateFailed(err.message),
          new ShowToast('templates.create_error', 'error'),
        ]);
        return EMPTY;
      }),
    );
  }

  @Action(TemplatesActions.UpdateTemplate)
  update(ctx: StateContext<TemplatesStateModel>, action: TemplatesActions.UpdateTemplate) {
    ctx.patchState({ loading: true });
    return this.api.updateTemplate(action.id, action.template).pipe(
      tap((template) => {
        ctx.patchState({ loading: false });
        ctx.dispatch([
          new TemplatesActions.UpdateTemplateSuccess(template),
          new ShowToast('templates.updated', 'success'),
        ]);
        const s = ctx.getState();
        ctx.dispatch(
          new TemplatesActions.LoadTemplates(
            s.currentPage,
            s.pageSize,
            TemplatesState.resolveTags(s),
          ),
        );
      }),
      catchError((err) => {
        ctx.patchState({ loading: false });
        ctx.dispatch([
          new TemplatesActions.UpdateTemplateFailed(err.message),
          new ShowToast('templates.update_error', 'error'),
        ]);
        return EMPTY;
      }),
    );
  }

  @Action(TemplatesActions.DeleteTemplate)
  delete(ctx: StateContext<TemplatesStateModel>, action: TemplatesActions.DeleteTemplate) {
    ctx.patchState({ loading: true });
    return this.api.deleteTemplate(action.id).pipe(
      tap(() => {
        ctx.patchState({ loading: false });
        ctx.dispatch([
          new TemplatesActions.DeleteTemplateSuccess(action.id),
          new ShowToast('templates.deleted', 'success'),
        ]);
        const s = ctx.getState();
        ctx.dispatch(
          new TemplatesActions.LoadTemplates(
            s.currentPage,
            s.pageSize,
            TemplatesState.resolveTags(s),
          ),
        );
      }),
      catchError((err) => {
        ctx.patchState({ loading: false });
        ctx.dispatch([
          new TemplatesActions.DeleteTemplateFailed(err.message),
          new ShowToast('templates.delete_error', 'error'),
        ]);
        return EMPTY;
      }),
    );
  }

  @Action(TemplatesActions.SetActiveTag)
  setActiveTag(ctx: StateContext<TemplatesStateModel>, action: TemplatesActions.SetActiveTag) {
    ctx.patchState({ activeTag: action.tag });
    const s = ctx.getState();
    ctx.dispatch(new TemplatesActions.LoadTemplates(0, s.pageSize, TemplatesState.resolveTags(s)));
  }

  /**
   * Switches the active source tab, resets pagination to page zero, and reloads templates using
   * the corresponding implicit source tag combined with any active custom tag filter.
   */
  @Action(TemplatesActions.SetActiveSourceTab)
  setActiveSourceTab(
    ctx: StateContext<TemplatesStateModel>,
    action: TemplatesActions.SetActiveSourceTab,
  ) {
    // FILE and GIT templates carry no user-defined tags, so a custom tag filter would always
    // produce empty results. Clear it when switching to those tabs.
    const clearTag = action.tab === 'FILE' || action.tab === 'GIT';
    ctx.patchState({
      activeSourceTab: action.tab,
      currentPage: 0,
      ...(clearTag ? { activeTag: undefined } : {}),
    });
    const s = ctx.getState();
    ctx.dispatch(new TemplatesActions.LoadTemplates(0, s.pageSize, TemplatesState.resolveTags(s)));
  }

  @Action(TenantActions.SelectTenant)
  onTenantChange(ctx: StateContext<TemplatesStateModel>) {
    ctx.patchState({
      templates: [],
      totalElements: 0,
      currentPage: 0,
      activeTag: undefined,
      activeSourceTab: 'ALL',
      allTags: [],
    });
  }
}
