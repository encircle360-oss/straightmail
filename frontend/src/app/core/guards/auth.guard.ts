import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { Store } from '@ngxs/store';
import { map, take } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { ApiKeyState } from '../../store/api-key/api-key.state';

/**
 * Route guard that enforces authentication before allowing navigation.
 *
 * - {@code none}: always permits navigation — no authentication required.
 * - {@code api-key}: checks for a stored API key in {@link ApiKeyState} and redirects to
 *   {@code /api-key-login} if absent.
 * - {@code oidc}: checks {@code OidcSecurityService.isAuthenticated$} and triggers the
 *   authorization redirect if the user is not authenticated.
 */
export const authGuard: CanActivateFn = () => {
  if (environment.authMode === 'none') {
    return true;
  }

  if (environment.authMode === 'api-key') {
    const store = inject(Store);
    const router = inject(Router);
    const apiKey = store.selectSnapshot(ApiKeyState.apiKey);
    if (!apiKey) {
      router.navigate(['/api-key-login']);
      return false;
    }
    return true;
  }

  const oidcSecurityService = inject(OidcSecurityService);

  return oidcSecurityService.isAuthenticated$.pipe(
    take(1),
    map(({ isAuthenticated }) => {
      if (!isAuthenticated) {
        oidcSecurityService.authorize();
        return false;
      }
      return true;
    }),
  );
};
