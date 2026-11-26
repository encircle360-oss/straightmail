import { HttpErrorResponse, HttpHeaders, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { Store } from '@ngxs/store';
import { EMPTY, Observable, of, throwError } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { TenantState } from '../../store/tenant/tenant.state';

// environment.apiUrl may be relative (the backend's default is "/api"), so a base is
// required — new URL('/api') throws.
const apiBasePath = new URL(environment.apiUrl, window.location.origin).pathname;

/**
 * The refresh currently in flight, or {@code null}.
 *
 * With silent renew enabled several requests can fail with 401 at the same moment. Letting each of
 * them call {@code forceRefreshSession()} individually rotates the refresh token once per call, so
 * every caller but the last ends up holding an already-invalidated token and is bounced to
 * {@code /unauthorized}. Sharing one refresh keeps that to a single rotation.
 */
let pendingRefresh: Observable<string | null> | null = null;

/**
 * Returns the shared refresh, starting one if none is running.
 *
 * @param oidc The OIDC service performing the actual refresh.
 * @returns An observable emitting the new access token, or {@code null} if the refresh failed.
 */
function sharedRefresh(oidc: OidcSecurityService): Observable<string | null> {
  if (!pendingRefresh) {
    pendingRefresh = oidc.forceRefreshSession().pipe(
      map((result) => (result?.isAuthenticated && result.accessToken ? result.accessToken : null)),
      catchError(() => of(null)),
      finalize(() => {
        pendingRefresh = null;
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
  }
  return pendingRefresh;
}

/**
 * Builds the request headers, adding the bearer token and the tenant selection.
 *
 * @param headers The original request headers.
 * @param token The access token to send, if any.
 * @param tenantId The selected tenant, if any.
 * @returns The headers to send.
 */
function withCredentials(
  headers: HttpHeaders,
  token: string | null,
  tenantId: string | null,
): HttpHeaders {
  let enriched = headers;
  if (token) {
    enriched = enriched.set('Authorization', `Bearer ${token}`);
  }
  if (tenantId) {
    enriched = enriched.set('X-Tenant-ID', tenantId);
  }
  return enriched;
}

/**
 * HTTP interceptor for OIDC/JWT authentication mode ({@code environment.authEnabled === true}).
 *
 * For every outgoing request whose path starts with the configured API base path, this interceptor
 * appends the {@code Authorization: Bearer <token>} header and, when a tenant is selected, the
 * {@code X-Tenant-ID} header from {@link TenantState}. The token is read from the OIDC library on
 * every request rather than from the store, because silent renew replaces it in the background and
 * a stored copy goes stale within minutes.
 * On a {@code 401} response it joins the shared silent refresh and retries the original request
 * with the new token and the same tenant header. If the refresh fails, the user is redirected to
 * {@code /unauthorized}. Requests outside the API base path are forwarded unchanged.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Credentials are attached by origin AND path: a same-path URL on a foreign host must never
  // receive the bearer token, however that URL came to be.
  const target = new URL(req.url, window.location.origin);
  if (target.origin !== window.location.origin || !target.pathname.startsWith(apiBasePath)) {
    return next(req);
  }

  const oidcSecurityService = inject(OidcSecurityService);
  const store = inject(Store);
  const router = inject(Router);

  const tenantId = store.selectSnapshot(TenantState.selectedTenantId);

  return oidcSecurityService.getAccessToken().pipe(
    switchMap((token) =>
      next(req.clone({ headers: withCredentials(req.headers, token, tenantId) })).pipe(
        catchError((error: HttpErrorResponse) => {
          if (error.status !== 401) {
            return throwError(() => error);
          }

          return sharedRefresh(oidcSecurityService).pipe(
            switchMap((refreshed) => {
              if (!refreshed) {
                router.navigate(['/unauthorized']);
                return EMPTY;
              }
              return next(
                req.clone({ headers: withCredentials(req.headers, refreshed, tenantId) }),
              );
            }),
          );
        }),
      ),
    ),
  );
};
