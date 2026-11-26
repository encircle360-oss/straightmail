import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext, Store } from '@ngxs/store';
import { RemoveToast, ShowToast } from './toast.actions';

/** A single toast notification entry managed by {@link ToastState}. */
export interface Toast {
  /** Unique auto-assigned numeric identifier. */
  id: number;
  /** Translation key or literal message string. */
  message: string;
  /** Visual variant controlling the toast colour. */
  toastType: 'success' | 'error' | 'info';
}

/** Shape of the data managed by the NGXS {@link ToastState}. */
export interface ToastStateModel {
  /** Currently active toast notifications. */
  toasts: Toast[];
  /** Auto-incrementing counter used to assign unique IDs to new toasts. */
  nextId: number;
}

/**
 * NGXS state for transient toast notification management.
 *
 * Each {@link ShowToast} action appends a new entry to the queue and schedules its automatic
 * removal after 4 seconds via {@link RemoveToast}. Toast state is not persisted.
 */
@State<ToastStateModel>({
  name: 'toast',
  defaults: { toasts: [], nextId: 1 },
})
@Injectable()
export class ToastState {
  private readonly store = inject(Store);

  /** Selector that emits the list of currently active toast notifications. */
  @Selector()
  static toasts(state: ToastStateModel): Toast[] {
    return state.toasts;
  }

  @Action(ShowToast)
  show(ctx: StateContext<ToastStateModel>, action: ShowToast): void {
    const state = ctx.getState();
    const id = state.nextId;
    ctx.patchState({
      toasts: [...state.toasts, { id, message: action.message, toastType: action.toastType }],
      nextId: id + 1,
    });
    setTimeout(() => this.store.dispatch(new RemoveToast(id)), 4000);
  }

  @Action(RemoveToast)
  remove(ctx: StateContext<ToastStateModel>, action: RemoveToast): void {
    ctx.patchState({
      toasts: ctx.getState().toasts.filter((t) => t.id !== action.id),
    });
  }
}
