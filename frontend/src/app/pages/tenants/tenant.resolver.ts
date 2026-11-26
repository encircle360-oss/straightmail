import { inject } from '@angular/core';
import { ResolveFn, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { catchError, EMPTY, switchMap } from 'rxjs';
import { TenantDTO } from '../../store/tenant/tenant.state';
import { environment } from '../../../environments/environment';

/**
 * Route resolver for the edit form. Fetches a tenant by slug and redirects to {@code /tenants}
 * if the tenant is not editable (config-based) or if the fetch fails.
 */
export const tenantResolver: ResolveFn<TenantDTO> = (route) => {
  const http = inject(HttpClient);
  const router = inject(Router);
  return http.get<TenantDTO>(`${environment.apiUrl}/v1/tenants/${route.paramMap.get('slug')}`).pipe(
    catchError(() => {
      router.navigate(['/tenants']);
      return EMPTY;
    }),
    switchMap((tenant) => {
      if (tenant.editable === false) {
        router.navigate(['/tenants']);
        return EMPTY;
      }
      return [tenant];
    }),
  );
};

/**
 * Route resolver for the read-only detail view. Fetches a tenant by slug and redirects to
 * {@code /tenants} only on fetch error. Config-based tenants ({@code editable=false}) are
 * allowed through — the view route is their primary navigation target.
 */
export const tenantViewResolver: ResolveFn<TenantDTO> = (route) => {
  const http = inject(HttpClient);
  const router = inject(Router);
  return http.get<TenantDTO>(`${environment.apiUrl}/v1/tenants/${route.paramMap.get('slug')}`).pipe(
    catchError(() => {
      router.navigate(['/tenants']);
      return EMPTY;
    }),
  );
};
