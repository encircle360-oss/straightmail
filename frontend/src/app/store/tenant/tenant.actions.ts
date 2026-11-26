import { TenantDTO } from './tenant.model';

/** Actions for the {@link TenantState} NGXS state slice. */
export namespace TenantActions {
  /** Fetches the list of tenants accessible to the current user from the backend. */
  export class LoadTenants {
    static readonly type = '[Tenant] Load Tenants';
  }

  /** Directly sets the tenant list in state (e.g. when populated from a static config). */
  export class SetTenants {
    static readonly type = '[Tenant] Set Tenants';
    /**
     * @param tenants The full list of tenant DTOs to store.
     */
    constructor(public tenants: TenantDTO[]) {}
  }

  /** Selects the active tenant by its slug identifier. */
  export class SelectTenant {
    static readonly type = '[Tenant] Select Tenant';
    /**
     * @param tenantId The slug of the tenant to make active.
     */
    constructor(public tenantId: string) {}
  }

  /** Clears all tenants and the selected tenant from state (e.g. on logout). */
  export class ClearTenants {
    static readonly type = '[Tenant] Clear Tenants';
  }
}
