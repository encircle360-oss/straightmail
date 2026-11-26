import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Store } from '@ngxs/store';
import { TranslatePipe } from '@ngx-translate/core';
import { SetApiKey } from '../../store/api-key/api-key.actions';
import { TenantDTO } from '../../store/tenant/tenant.state';
import { TenantActions } from '../../store/tenant/tenant.actions';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-api-key-login',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './api-key-login.component.html',
  styleUrl: './api-key-login.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
/**
 * Login page component for API-key authentication mode ({@code environment.authEnabled === false}).
 *
 * Validates the entered API key by calling {@code GET /v1/tenants/me} with the key as the
 * {@code X-API-KEY} header. On success, the key and resolved tenant list are persisted to the
 * NGXS store and the user is redirected to the dashboard. On a {@code 401} response an inline
 * error message is shown.
 */
export class ApiKeyLoginComponent {
  /** The API key currently entered in the login form. */
  apiKey = '';
  /** Whether a login request is currently in flight. */
  readonly loading = signal(false);
  /** Translation key of the inline error message to display when login fails. */
  readonly errorMessage = signal('');

  private readonly store = inject(Store);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);

  /** Validates the API key and, on success, stores it and navigates to the dashboard. */
  submit(): void {
    if (!this.apiKey) return;
    this.loading.set(true);
    this.errorMessage.set('');

    const headers = new HttpHeaders({ 'X-API-KEY': this.apiKey });
    this.http.get<TenantDTO[]>(`${environment.apiUrl}/v1/tenants/me`, { headers }).subscribe({
      next: (tenants) => {
        this.store.dispatch(new SetApiKey(this.apiKey));
        this.store.dispatch(new TenantActions.SetTenants(tenants));
        if (tenants.length > 0) {
          this.store.dispatch(new TenantActions.SelectTenant(tenants[0].slug));
        }
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.status === 401 ? 'login.invalid_key' : 'login.connection_error');
      },
    });
  }
}
