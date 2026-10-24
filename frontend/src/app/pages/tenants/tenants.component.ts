import { Component, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { Store } from '@ngxs/store';
import { TenantDTO, TenantState } from '../../store/tenant/tenant.state';
import { TenantTableComponent } from './tenant-table/tenant-table.component';
import { TenantGridComponent } from './tenant-grid/tenant-grid.component';
import { ConfirmService } from '../../core/services/confirm.service';
import { PageHeaderComponent } from '../../shared/components/layout/page-header/page-header.component';
import { UiState } from '../../store/ui/ui.state';
import { UiActions } from '../../store/ui/ui.actions';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-tenants',
  standalone: true,
  imports: [TenantTableComponent, TenantGridComponent, PageHeaderComponent, TranslatePipe],
  templateUrl: './tenants.component.html',
})
/**
 * Page component for listing and managing tenants (admin-only).
 *
 * Fetches all tenants from the admin endpoint ({@code GET /v1/tenants}) on init and after each
 * delete. Create and edit actions navigate to dedicated tenant form pages. Delete operations are
 * guarded by a confirmation dialog. Supports toggling between a card grid view and a table list
 * view; the preference is persisted in {@link UiState}.
 */
export class TenantsComponent implements OnInit {
  /** The currently loaded list of tenants. */
  readonly tenants = signal<TenantDTO[]>([]);
  /** Whether a backend request is currently in flight. */
  readonly loading = signal(false);

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);
  private readonly translate = inject(TranslateService);
  private readonly store = inject(Store);
  private readonly apiUrl = `${environment.apiUrl}/v1/tenants`;

  /** Signal that reflects the persisted tenant list view mode ('grid' or 'list'). */
  readonly viewMode = this.store.selectSignal(UiState.tenantsViewMode);
  /** Signal emitting {@code true} when the backend runs with the {@code database} profile. */
  readonly isDbMode = this.store.selectSignal(TenantState.isDatabaseMode);

  /** Triggers the initial tenant list load on component init. */
  ngOnInit(): void {
    this.loadTenants();
  }

  /** Fetches all tenants from the admin endpoint and updates the component state. */
  loadTenants(): void {
    this.loading.set(true);
    this.http.get<TenantDTO[]>(this.apiUrl).subscribe({
      next: (tenants) => {
        this.tenants.set(tenants);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  /** Navigates to the tenant creation form at {@code /tenants/new}. */
  openCreate(): void {
    this.router.navigate(['/tenants/new']);
  }

  /**
   * Navigates to the tenant edit form.
   *
   * @param tenant The tenant to edit.
   */
  openEdit(tenant: TenantDTO): void {
    this.router.navigate(['/tenants', tenant.slug, 'edit']);
  }

  /**
   * Navigates to the read-only tenant view.
   *
   * @param tenant The tenant to view.
   */
  openView(tenant: TenantDTO): void {
    this.router.navigate(['/tenants', tenant.slug, 'view']);
  }

  /**
   * Shows a confirmation dialog and, if confirmed, deletes the tenant and reloads the list.
   *
   * @param tenant The tenant to delete.
   */
  async confirmDelete(tenant: TenantDTO): Promise<void> {
    const ok = await this.confirm.confirm(
      this.translate.instant('tenants.delete_confirm', { slug: tenant.slug }),
      { variant: 'danger', confirmLabel: this.translate.instant('common.delete') },
    );
    if (!ok) return;
    this.http.delete(`${this.apiUrl}/${tenant.slug}`).subscribe({
      next: () => this.loadTenants(),
    });
  }

  /**
   * Persists the selected view mode in {@link UiState} so the preference survives navigation
   * and browser sessions.
   *
   * @param mode The view mode to activate.
   */
  setViewMode(mode: 'grid' | 'list'): void {
    this.store.dispatch(new UiActions.SetTenantsViewMode(mode));
  }
}
