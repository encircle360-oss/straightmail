import { Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { ClearApiKey, SetApiKey } from './api-key.actions';

/** Shape of the data persisted in the NGXS {@link ApiKeyState}. */
export interface ApiKeyStateModel {
  /** Stored plain-text API key, or {@code null} when the user is not authenticated. */
  apiKey: string | null;
}

/**
 * NGXS state for API-key authentication mode ({@code environment.authEnabled === false}).
 *
 * Stores the API key entered on the {@code /api-key-login} page. The key is read by
 * {@code apiKeyInterceptor} and appended to all API requests as the {@code X-API-KEY} header.
 * State is persisted in {@code SESSION_STORAGE} via the NGXS persist plugin.
 */
@State<ApiKeyStateModel>({
  name: 'apiKey',
  defaults: {
    apiKey: null,
  },
})
@Injectable()
export class ApiKeyState {
  /** Selector that emits the stored API key, or {@code null} when unauthenticated. */
  @Selector()
  static apiKey(state: ApiKeyStateModel): string | null {
    return state.apiKey;
  }

  @Action(SetApiKey)
  set(ctx: StateContext<ApiKeyStateModel>, action: SetApiKey) {
    ctx.patchState({ apiKey: action.apiKey });
  }

  @Action(ClearApiKey)
  clear(ctx: StateContext<ApiKeyStateModel>) {
    ctx.patchState({ apiKey: null });
  }
}
