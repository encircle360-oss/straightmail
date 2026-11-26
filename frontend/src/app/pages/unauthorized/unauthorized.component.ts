import { Component, inject } from '@angular/core';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-unauthorized',
  templateUrl: './unauthorized.component.html',
})
/**
 * Displayed when the user is authenticated but not authorised (HTTP 401/403 equivalent).
 *
 * In OIDC mode provides a "Login" button that triggers a new OIDC authorization redirect.
 * In API-key mode the user is directed to re-enter their key. In no-auth mode the button is hidden.
 */
export class UnauthorizedComponent {
  private readonly oidcSecurityService = inject(OidcSecurityService);

  /** Active authentication mode for this deployment. */
  readonly authMode = environment.authMode;

  /** Initiates the OIDC authorization redirect to allow the user to re-authenticate. */
  login(): void {
    this.oidcSecurityService.authorize();
  }
}
