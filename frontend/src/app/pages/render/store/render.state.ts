import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { catchError, EMPTY, tap } from 'rxjs';
import { ApiService, RenderResponse } from '../../../core/services/api.service';
import { ShowToast } from '../../../store/toast/toast.actions';
import { RenderActions } from './render.actions';

/** Shape of the data persisted in the NGXS {@link RenderState}. */
export interface RenderStateModel {
  /** Whether a render request is currently in flight. */
  loading: boolean;
  /** The most recent render result, or {@code null} before any render or after a clear. */
  result: RenderResponse | null;
  /** Error message from the last failed render attempt, or {@code null} if successful. */
  error: string | null;
  /** Persisted form values (template ID and JSON model string) for the render page. */
  formValues: { templateId: string; model: string };
}

/**
 * NGXS state for the template render preview page.
 *
 * Submits render requests to the backend and exposes the resulting HTML and plain-text output.
 * Form values are persisted so the user's input survives navigation. On success or failure,
 * a {@link ShowToast} event is dispatched alongside the domain event action.
 */
@State<RenderStateModel>({
  name: 'render',
  defaults: {
    loading: false,
    result: null,
    error: null,
    formValues: { templateId: '', model: '' },
  },
})
@Injectable()
export class RenderState {
  private readonly api = inject(ApiService);

  /** Selector that emits {@code true} while a render request is in flight. */
  @Selector()
  static loading(state: RenderStateModel): boolean {
    return state.loading;
  }

  /** Selector that emits the most recent render result, or {@code null}. */
  @Selector()
  static result(state: RenderStateModel): RenderResponse | null {
    return state.result;
  }

  /** Selector that emits the last render error message, or {@code null}. */
  @Selector()
  static error(state: RenderStateModel): string | null {
    return state.error;
  }

  /** Selector that emits the persisted render form values. */
  @Selector()
  static formValues(state: RenderStateModel): { templateId: string; model: string } {
    return state.formValues;
  }

  @Action(RenderActions.Render)
  render(ctx: StateContext<RenderStateModel>, action: RenderActions.Render) {
    ctx.patchState({ loading: true, error: null });
    return this.api.render(action.req).pipe(
      tap((result) => {
        ctx.patchState({ loading: false, result });
        ctx.dispatch([
          new RenderActions.RenderSuccess(result),
          new ShowToast('render.success', 'success'),
        ]);
      }),
      catchError((err) => {
        // Use the backend's structured error message (422 template errors) when available.
        // Generic HTTP error strings (err.message) are not shown to keep internals hidden.
        const templateError: string | null = err.error?.message ?? null;
        ctx.patchState({ loading: false, error: templateError });
        ctx.dispatch([
          new RenderActions.RenderFailed(templateError ?? err.message),
          new ShowToast('render.error', 'error'),
        ]);
        return EMPTY;
      }),
    );
  }

  @Action(RenderActions.SaveForm)
  saveForm(ctx: StateContext<RenderStateModel>, action: RenderActions.SaveForm) {
    ctx.patchState({ formValues: action.values });
  }

  @Action(RenderActions.Clear)
  clear(ctx: StateContext<RenderStateModel>) {
    ctx.patchState({ loading: false, result: null, error: null });
  }
}
