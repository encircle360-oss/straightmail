import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { PageHeaderComponent } from '../../shared/components/layout/page-header/page-header.component';
import { ApiStatusCardComponent } from './api-status-card/api-status-card.component';
import { GitSyncCardComponent } from './git-sync-card/git-sync-card.component';
import { FileSyncCardComponent } from './file-sync-card/file-sync-card.component';
import { ApiService } from '../../core/services/api.service';
import { TranslatePipe } from '@ngx-translate/core';
import { Store } from '@ngxs/store';
import { TenantDTO, TenantState } from '../../store/tenant/tenant.state';
import { DashboardState } from './store/dashboard.state';
import { LoadSyncStatus, TriggerGitSync } from './store/dashboard.actions';
import { AuthState } from '../../store/auth/auth.state';
import { environment } from '../../../environments/environment';

type HealthStatus = 'checking' | 'up' | 'down';

@Component({
  selector: 'app-dashboard',
  imports: [
    RouterLink,
    PageHeaderComponent,
    ApiStatusCardComponent,
    GitSyncCardComponent,
    FileSyncCardComponent,
    TranslatePipe,
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
/**
 * Landing page component that displays an overview of the application, API health status,
 * and template sync source status (Git-sync per tenant, file-template directory accessibility).
 *
 * Calls the backend info endpoint on init and delegates sync status loading and Git-sync
 * operations to {@link DashboardState}. The sync status section is only rendered when the
 * status endpoint is reachable (i.e. the backend runs in database mode).
 */
export class DashboardComponent implements OnInit {
  private readonly apiService = inject(ApiService);
  private readonly store = inject(Store);

  readonly tenants = toSignal(this.store.select(TenantState.tenants), { initialValue: [] });

  private readonly roles = toSignal(this.store.select(AuthState.roles), {
    initialValue: [] as string[],
  });

  /**
   * True when the current user may trigger a Git-sync.
   * In API-key and no-auth modes there is no role system, so every caller is treated as admin.
   * In OIDC mode the {@code ADMIN} role is required.
   */
  readonly isAdmin = computed(
    () => environment.authMode !== 'oidc' || this.roles().includes('ADMIN'),
  );

  /**
   * Git-sync entries filtered to only the tenants accessible to the current user.
   * Acts as a defense-in-depth complement to the backend's server-side filtering.
   */
  readonly accessibleGitSync = computed(() => {
    const status = this.syncStatus();
    if (!status) return null;
    const accessibleSlugs = new Set(this.tenants().map((t) => t.slug));
    return status.gitSync.filter((s) => accessibleSlugs.has(s.tenantId));
  });

  /** Signal representing the current backend health state. */
  healthStatus = signal<HealthStatus>('checking');

  /** Aggregated sync status from {@link DashboardState}. {@code null} when unavailable. */
  readonly syncStatus = toSignal(this.store.select(DashboardState.syncStatus), {
    initialValue: null,
  });

  /** Slugs of tenants whose Git-sync is currently running, from {@link DashboardState}. */
  readonly syncingTenants = toSignal(this.store.select(DashboardState.syncingTenants), {
    initialValue: [],
  });

  /**
   * Looks up the {@link TenantDTO} for a given slug from the NGXS tenant store,
   * allowing the Git-sync cards to display branding alongside the raw status data.
   *
   * @param slug The tenant slug from {@link GitSyncStatusDTO#tenantId}.
   */
  tenantForSync(slug: string): TenantDTO | null {
    return this.tenants().find((t) => t.slug === slug) ?? null;
  }

  /** Returns {@code true} while a Git-sync is in progress for the given tenant slug. */
  isSyncing(slug: string): boolean {
    return this.syncingTenants().includes(slug);
  }

  ngOnInit(): void {
    this.checkHealth();
    this.store.dispatch(new LoadSyncStatus());
  }

  /** Calls the backend info endpoint and updates {@link healthStatus} based on the response. */
  checkHealth(): void {
    this.healthStatus.set('checking');
    this.apiService.getInfo().subscribe({
      next: () => this.healthStatus.set('up'),
      error: () => this.healthStatus.set('down'),
    });
  }

  /**
   * Dispatches {@link TriggerGitSync} for the given tenant. The state handles the API call,
   * updates the sync result, and shows a success or error toast on completion.
   *
   * @param tenantSlug The slug of the tenant to sync.
   */
  onGitSync(tenantSlug: string): void {
    this.store.dispatch(new TriggerGitSync(tenantSlug));
  }
}
