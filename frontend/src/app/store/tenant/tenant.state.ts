import { inject, Injectable } from '@angular/core';
import { Action, Selector, State, StateContext } from '@ngxs/store';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs/operators';
import { TenantActions } from './tenant.actions';
import { environment } from '../../../environments/environment';
import { TenantDTO } from './tenant.model';

export type { TenantDTO } from './tenant.model';

/** Shape of the data persisted in the NGXS {@link TenantState}. */
export interface TenantStateModel {
  /** All tenants accessible to the current user. */
  tenants: TenantDTO[];
  /** Slug of the currently active tenant, or {@code null} if none is selected. */
  selectedTenantId: string | null;
}

/**
 * NGXS state for multi-tenant management.
 *
 * Loads the list of tenants the current user belongs to from {@code GET /v1/tenants/me} and
 * tracks the selected active tenant. The selected tenant slug is forwarded as the
 * {@code X-Tenant-ID} header by the HTTP interceptors. State is persisted in
 * {@code SESSION_STORAGE} via the NGXS persist plugin.
 */
@State<TenantStateModel>({
  name: 'tenant',
  defaults: {
    tenants: [],
    selectedTenantId: null,
  },
})
@Injectable()
export class TenantState {
  private readonly http = inject(HttpClient);

  /** Selector that emits all tenants accessible to the current user. */
  @Selector()
  static tenants(state: TenantStateModel): TenantDTO[] {
    return state?.tenants ?? [];
  }

  /** Selector that emits the slug of the currently active tenant, or {@code null}. */
  @Selector()
  static selectedTenantId(state: TenantStateModel): string | null {
    return state?.selectedTenantId ?? null;
  }

  /**
   * Selector that emits the full {@link TenantDTO} for the currently active tenant,
   * or {@code null} when no tenant is selected or the tenant list is not yet loaded.
   */
  @Selector()
  static selectedTenant(state: TenantStateModel): TenantDTO | null {
    return state?.tenants?.find((t) => t.slug === state.selectedTenantId) ?? null;
  }

  /**
   * Selector that emits {@code true} when at least one tenant in the list has
   * {@code editable === true}, indicating the backend is running with the {@code database} profile.
   * Config-based tenants always have {@code editable=false}; DB-backed tenants always have
   * {@code editable=true}.
   */
  @Selector()
  static isDatabaseMode(state: TenantStateModel): boolean {
    return (state?.tenants ?? []).some((t) => t.editable === true);
  }

  @Action(TenantActions.LoadTenants)
  load(ctx: StateContext<TenantStateModel>) {
    return this.http.get<TenantDTO[]>(`${environment.apiUrl}/v1/tenants/me`).pipe(
      tap((tenants) => {
        const currentId = ctx.getState().selectedTenantId;
        const isValid = tenants.some((t) => t.slug === currentId);
        ctx.patchState({
          tenants,
          selectedTenantId: isValid ? currentId : (tenants[0]?.slug ?? null),
        });
      }),
    );
  }

  @Action(TenantActions.SetTenants)
  setTenants(ctx: StateContext<TenantStateModel>, action: TenantActions.SetTenants) {
    ctx.patchState({ tenants: action.tenants });
    if (action.tenants.length > 0 && !ctx.getState().selectedTenantId) {
      ctx.patchState({ selectedTenantId: action.tenants[0].slug });
    }
  }

  @Action(TenantActions.SelectTenant)
  select(ctx: StateContext<TenantStateModel>, action: TenantActions.SelectTenant) {
    ctx.patchState({ selectedTenantId: action.tenantId });
  }

  @Action(TenantActions.ClearTenants)
  clear(ctx: StateContext<TenantStateModel>) {
    ctx.patchState({ tenants: [], selectedTenantId: null });
  }
}
