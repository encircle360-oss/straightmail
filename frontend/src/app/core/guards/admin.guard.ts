import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Store } from '@ngxs/store';
import { AuthState } from '../../store/auth/auth.state';
import { environment } from '../../../environments/environment';

/**
 * Route guard that restricts access to users with admin privileges.
 *
 * - {@code none} and {@code api-key}: all authenticated users are treated as admins.
 * - {@code oidc}: reads roles from {@link AuthState} and redirects to the application root
 *   if the {@code ADMIN} role is not present.
 */
export const adminGuard: CanActivateFn = () => {
  if (environment.authMode !== 'oidc') {
    return true;
  }

  const store = inject(Store);
  const router = inject(Router);

  const roles = store.selectSnapshot(AuthState.roles);

  if (!roles.includes('ADMIN')) {
    router.navigate(['/']);
    return false;
  }

  return true;
};
