import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { catchError, EMPTY, tap } from 'rxjs';
import { ApiService, StatusDTO } from '../../../core/services/api.service';
import { ShowToast } from '../../../store/toast/toast.actions';
import { LoadSyncStatus, TriggerGitSync } from './dashboard.actions';

/** Shape of the data managed by the NGXS {@link DashboardState}. */
export interface DashboardStateModel {
  /** Aggregated sync status returned by {@code GET /v1/status}; {@code null} when unavailable. */
  syncStatus: StatusDTO | null;
  /** Slugs of tenants whose Git-sync is currently in progress. */
  syncingTenants: string[];
}

/**
 * NGXS state for the dashboard page.
 *
 * Manages the aggregated backend sync status and coordinates Git-sync operations per tenant.
 * A toast notification is shown after every sync attempt to communicate success or failure
 * to the user. State is not persisted between sessions.
 */
@State<DashboardStateModel>({
  name: 'dashboard',
  defaults: { syncStatus: null, syncingTenants: [] },
})
@Injectable()
export class DashboardState {
  private readonly api = inject(ApiService);

  /** Selector that emits the current aggregated sync status, or {@code null} when unavailable. */
  @Selector()
  static syncStatus(state: DashboardStateModel): StatusDTO | null {
    return state.syncStatus;
  }

  /** Selector that emits the slugs of tenants whose Git-sync is currently running. */
  @Selector()
  static syncingTenants(state: DashboardStateModel): string[] {
    return state.syncingTenants;
  }

  /**
   * Fetches the aggregated sync status from {@code GET /v1/status}. Errors are swallowed
   * so the sync section is simply hidden when the database profile is not active.
   */
  @Action(LoadSyncStatus)
  loadSyncStatus(ctx: StateContext<DashboardStateModel>) {
    return this.api.getStatus().pipe(
      tap((status) => ctx.patchState({ syncStatus: status })),
      catchError(() => {
        ctx.patchState({ syncStatus: null });
        return EMPTY;
      }),
    );
  }

  /**
   * Triggers an immediate Git-sync for the given tenant. Marks the tenant as syncing for the
   * duration of the request, updates the corresponding entry in {@link syncStatus} on success,
   * and dispatches a toast notification regardless of outcome.
   */
  @Action(TriggerGitSync)
  triggerGitSync(ctx: StateContext<DashboardStateModel>, action: TriggerGitSync) {
    ctx.patchState({ syncingTenants: [...ctx.getState().syncingTenants, action.tenantSlug] });

    return this.api.triggerGitSync(action.tenantSlug).pipe(
      tap((updated) => {
        const current = ctx.getState().syncStatus;
        if (current) {
          ctx.patchState({
            syncStatus: {
              ...current,
              gitSync: current.gitSync.map((entry) =>
                entry.tenantId === action.tenantSlug ? updated : entry,
              ),
            },
          });
        }
        this.removeSyncing(ctx, action.tenantSlug);
        ctx.dispatch(new ShowToast('dashboard.sync_success', 'success'));
      }),
      catchError(() => {
        this.removeSyncing(ctx, action.tenantSlug);
        ctx.dispatch(new ShowToast('dashboard.sync_error', 'error'));
        return EMPTY;
      }),
    );
  }

  private removeSyncing(ctx: StateContext<DashboardStateModel>, slug: string): void {
    ctx.patchState({
      syncingTenants: ctx.getState().syncingTenants.filter((s) => s !== slug),
    });
  }
}
