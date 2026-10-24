import { ChangeDetectionStrategy, Component, inject, output } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Observable } from 'rxjs';
import { NgbOffcanvas } from '@ng-bootstrap/ng-bootstrap';
import { Store } from '@ngxs/store';
import { AuthService } from '../../core/services/auth.service';
import { environment } from '../../../environments/environment';
import { HelpPanelComponent } from '../../shared/components/layout/help-panel/help-panel.component';
import { UserAvatarComponent } from '../../shared/components/layout/user-avatar/user-avatar.component';
import { TenantActions } from '../../store/tenant/tenant.actions';
import { ClearApiKey } from '../../store/api-key/api-key.actions';

/**
 * Top navigation bar component.
 *
 * Displays the menu toggle button (mobile) and user account actions (help panel, logout).
 * Supports both OIDC and API-key authentication modes:
 * - In OIDC mode, user data is read from {@link AuthService} and logout delegates to the OIDC flow.
 * - In API-key mode, logout clears the stored API key and tenant, then redirects to the login page.
 * - In no-auth mode, no logout button is shown.
 * The help off-canvas is context-aware: it opens to the section matching the current route.
 */
@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [AsyncPipe, TranslatePipe, UserAvatarComponent],
  templateUrl: './topbar.component.html',
  styleUrl: './topbar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TopbarComponent {
  /** Emitted when the hamburger menu button is clicked, toggling the sidebar on mobile. */
  readonly menuToggle = output<void>();
  /** Observable emitting the OIDC user data claims (display name, etc.). */
  userData$: Observable<any>;
  /** Active authentication mode for this deployment. */
  readonly authMode = environment.authMode;
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly offcanvas = inject(NgbOffcanvas);
  private readonly store = inject(Store);

  constructor() {
    this.userData$ = this.authService.userData$;
  }

  /** Logs the user out. In OIDC mode triggers the OIDC logoff flow; in API-key mode clears stored state. */
  logout(): void {
    if (environment.authMode === 'api-key') {
      this.store.dispatch(new ClearApiKey());
      this.store.dispatch(new TenantActions.ClearTenants());
      this.router.navigate(['/api-key-login']);
    } else {
      this.authService.logout();
    }
  }

  /** Opens the context-sensitive help off-canvas panel for the current route section. */
  openHelp(): void {
    const url = this.router.url;
    let section: string | null = null;
    if (url.startsWith('/dashboard')) section = 'dashboard';
    else if (url.startsWith('/templates')) section = 'templates';
    else if (url.startsWith('/send')) section = 'send';
    else if (url.startsWith('/render')) section = 'render';
    else if (url.startsWith('/tenants')) section = 'tenants';

    const isMobile = window.innerWidth < 768;
    const ref = this.offcanvas.open(HelpPanelComponent, {
      position: isMobile ? 'bottom' : 'end',
      panelClass: isMobile ? 'help-offcanvas help-offcanvas--bottom' : 'help-offcanvas',
    });
    ref.componentInstance.activeSection.set(section);
  }
}
