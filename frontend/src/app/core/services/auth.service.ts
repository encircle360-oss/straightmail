import { inject, Injectable } from '@angular/core';
import { Store } from '@ngxs/store';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { Observable } from 'rxjs';
import { CheckAuth, Login, Logout } from '../../store/auth/auth.actions';
import { AuthState } from '../../store/auth/auth.state';

/**
 * Facade service for OIDC-based authentication.
 *
 * Wraps NGXS {@link AuthState} selectors and exposes observable streams for authentication
 * status, user data and roles. The access token comes from the OIDC library via
 * {@link AuthService#getAccessToken}, not from the store. Auth lifecycle actions ({@code CheckAuth},
 * {@code Login}, {@code Logout}) are dispatched through the NGXS store. Only used when
 * {@code environment.authEnabled} is {@code true}.
 */
@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly store = inject(Store);
  private readonly oidcSecurityService = inject(OidcSecurityService);

  /** Observable that emits {@code true} while the user is authenticated. */
  get isAuthenticated$(): Observable<boolean> {
    return this.store.select(AuthState.isAuthenticated);
  }

  /** Observable that emits the OIDC user-data claims object. */
  get userData$(): Observable<any> {
    return this.store.select(AuthState.userData);
  }

  /** Observable that emits the list of roles extracted from the JWT claims. */
  get roles$(): Observable<string[]> {
    return this.store.select(AuthState.roles);
  }

  /** Dispatches a {@link CheckAuth} action to verify and restore the current OIDC session. */
  checkAuth(): void {
    this.store.dispatch(new CheckAuth());
  }

  /** Dispatches a {@link Login} action, initiating the OIDC authorization redirect. */
  login(): void {
    this.store.dispatch(new Login());
  }

  /** Dispatches a {@link Logout} action, ending the OIDC session and clearing stored state. */
  logout(): void {
    this.store.dispatch(new Logout());
  }

  /**
   * Returns an observable of the current OIDC access token directly from the OIDC library.
   *
   * @returns An observable emitting the raw access token string.
   */
  getAccessToken(): Observable<string> {
    return this.oidcSecurityService.getAccessToken();
  }
}
