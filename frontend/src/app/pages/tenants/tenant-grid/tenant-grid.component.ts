import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { TenantDTO } from '../../../store/tenant/tenant.state';
import { TenantAvatarComponent } from '../../../shared/components/layout/tenant-avatar/tenant-avatar.component';

@Component({
  selector: 'app-tenant-grid',
  standalone: true,
  imports: [TranslatePipe, TenantAvatarComponent],
  templateUrl: './tenant-grid.component.html',
  styleUrl: './tenant-grid.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
/**
 * Presentational grid component for displaying tenants as interactive cards.
 *
 * Each card shows the tenant avatar (initials placeholder or logo), display name, slug,
 * active status, and capability indicators (SMTP, Git, API key). Clicking a card navigates
 * directly to the edit form. A delete button is revealed on hover for non-default tenants.
 */
export class TenantGridComponent {
  /** The list of tenants to display as cards. */
  readonly tenants = input.required<TenantDTO[]>();
  /** Whether a data request is currently in progress. */
  readonly loading = input.required<boolean>();
  /** Emitted when the user clicks the delete button on a tenant card. */
  readonly delete = output<TenantDTO>();

  private readonly router = inject(Router);

  /**
   * Navigates to the tenant edit form for DB-backed tenants, or to the read-only view
   * for config-based tenants ({@code editable=false}).
   *
   * @param tenant The tenant whose card was clicked.
   */
  navigate(tenant: TenantDTO): void {
    const path = tenant.editable !== false ? 'edit' : 'view';
    this.router.navigate(['/tenants', tenant.slug, path]);
  }
}
