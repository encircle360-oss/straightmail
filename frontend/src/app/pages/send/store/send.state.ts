import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { catchError, EMPTY, tap } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { ShowToast } from '../../../store/toast/toast.actions';
import { SendActions } from './send.actions';

/** Shape of the data persisted in the NGXS {@link SendState}. */
export interface SendStateModel {
  /** Whether a send request is currently in flight. */
  loading: boolean;
  /** Error message from the last failed send attempt, or {@code null} if successful. */
  error: string | null;
  /** Result of the last send operation (truthy on success). */
  lastResult: unknown;
  /** Persisted form values for the "send by template ID" tab. */
  formById: Record<string, unknown>;
  /** Persisted form values for the "send inline" tab. */
  formInline: Record<string, unknown>;
  /** Zero-based index of the active send tab. */
  activeTab: number;
}

/**
 * NGXS state for the email send page.
 *
 * Handles two send modes — by stored template ID and inline FreeMarker template. Form values are
 * persisted so the user's input survives navigation. On success or failure, a {@link ShowToast}
 * event is dispatched alongside the domain event action.
 */
@State<SendStateModel>({
  name: 'send',
  defaults: {
    loading: false,
    error: null,
    lastResult: null,
    formById: {},
    formInline: {},
    activeTab: 1,
  },
})
@Injectable()
export class SendState {
  private readonly api = inject(ApiService);

  /** Selector that emits {@code true} while a send request is in flight. */
  @Selector()
  static loading(state: SendStateModel): boolean {
    return state.loading;
  }

  /** Selector that emits the last send error message, or {@code null}. */
  @Selector()
  static error(state: SendStateModel): string | null {
    return state.error;
  }

  /** Selector that emits the persisted form values for the "send by template ID" tab. */
  @Selector()
  static formById(state: SendStateModel): Record<string, unknown> {
    return state.formById;
  }

  /** Selector that emits the persisted form values for the "send inline" tab. */
  @Selector()
  static formInline(state: SendStateModel): Record<string, unknown> {
    return state.formInline;
  }

  /** Selector that emits the zero-based index of the active send tab. */
  @Selector()
  static activeTab(state: SendStateModel): number {
    return state.activeTab;
  }

  @Action(SendActions.SendByTemplate)
  sendByTemplate(ctx: StateContext<SendStateModel>, action: SendActions.SendByTemplate) {
    ctx.patchState({ loading: true, error: null });
    return this.api.sendEmail(action.req).pipe(
      tap(() => {
        ctx.patchState({ loading: false, lastResult: true });
        ctx.dispatch([
          new SendActions.SendByTemplateSuccess(),
          new ShowToast('send.success', 'success'),
        ]);
      }),
      catchError((err) => {
        const backendMsg: string | undefined = err?.error?.message;
        const errorMsg = backendMsg ?? err.message;
        ctx.patchState({ loading: false, error: errorMsg });
        ctx.dispatch([
          new SendActions.SendFailed(errorMsg),
          new ShowToast(backendMsg ?? 'send.error', 'error'),
        ]);
        return EMPTY;
      }),
    );
  }

  @Action(SendActions.SendInline)
  sendInline(ctx: StateContext<SendStateModel>, action: SendActions.SendInline) {
    ctx.patchState({ loading: true, error: null });
    return this.api.sendInlineEmail(action.req).pipe(
      tap(() => {
        ctx.patchState({ loading: false, lastResult: true });
        ctx.dispatch([
          new SendActions.SendInlineSuccess(),
          new ShowToast('send.success', 'success'),
        ]);
      }),
      catchError((err) => {
        const backendMsg: string | undefined = err?.error?.message;
        const errorMsg = backendMsg ?? err.message;
        ctx.patchState({ loading: false, error: errorMsg });
        ctx.dispatch([
          new SendActions.SendFailed(errorMsg),
          new ShowToast(backendMsg ?? 'send.error', 'error'),
        ]);
        return EMPTY;
      }),
    );
  }

  @Action(SendActions.SaveForm)
  saveForm(ctx: StateContext<SendStateModel>, action: SendActions.SaveForm) {
    if (action.tab === 'byId') {
      ctx.patchState({ formById: action.values });
    } else {
      ctx.patchState({ formInline: action.values });
    }
  }

  @Action(SendActions.UpdateActiveTab)
  updateActiveTab(ctx: StateContext<SendStateModel>, action: SendActions.UpdateActiveTab) {
    ctx.patchState({ activeTab: action.activeTab });
  }

  @Action(SendActions.Reset)
  reset(ctx: StateContext<SendStateModel>) {
    ctx.patchState({ loading: false, error: null, lastResult: null });
  }
}
