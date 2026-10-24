import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { TenantDTO } from '../../../store/tenant/tenant.state';
import { TenantAvatarComponent } from '../../../shared/components/layout/tenant-avatar/tenant-avatar.component';
import { PageHeaderComponent } from '../../../shared/components/layout/page-header/page-header.component';

@Component({
  selector: 'app-tenant-view',
  standalone: true,
  imports: [TranslatePipe, TenantAvatarComponent, PageHeaderComponent],
  templateUrl: './tenant-view.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
/**
 * Read-only detail view for a single tenant.
 *
 * Displays all tenant configuration (SMTP, Git Sync, Security) without form inputs.
 * Intended for config-based tenants ({@code editable=false}) that cannot be modified via the API,
 * but is accessible for any tenant navigated to via {@code /tenants/:slug/view}.
 */
export class TenantViewComponent {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly tenant: TenantDTO = this.route.snapshot.data['tenant'];

  /** Navigates back to the tenant list. */
  back(): void {
    this.router.navigate(['/tenants']);
  }
}
