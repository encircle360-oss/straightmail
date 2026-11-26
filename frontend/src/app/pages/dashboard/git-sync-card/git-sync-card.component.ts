import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { GitSyncStatusDTO } from '../../../core/services/api.service';
import { TenantDTO } from '../../../store/tenant/tenant.model';
import { TenantAvatarComponent } from '../../../shared/components/layout/tenant-avatar/tenant-avatar.component';

@Component({
  selector: 'app-git-sync-card',
  standalone: true,
  imports: [TranslatePipe, TenantAvatarComponent],
  templateUrl: './git-sync-card.component.html',
  styleUrl: './git-sync-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GitSyncCardComponent {
  /** Current sync status for this tenant. */
  readonly syncStatus = input.required<GitSyncStatusDTO>();

  /**
   * Optional tenant DTO for branding (avatar, display name).
   * When {@code null}, the tenant slug from {@link syncStatus} is used as a fallback label.
   */
  readonly tenant = input<TenantDTO | null>(null);

  /**
   * Whether a sync is currently in progress for this tenant.
   * Controlled by the parent so the button is disabled while the API call runs.
   */
  readonly syncing = input<boolean>(false);

  /**
   * Whether the current user is permitted to trigger a sync for this tenant.
   * When {@code false} the sync button is hidden entirely.
   */
  readonly canSync = input<boolean>(true);

  /** Emitted when the user clicks the sync button. Carries the tenant slug. */
  readonly sync = output<string>();

  /** Returns the CSS modifier class for the pip indicator based on the sync result. */
  get pipClass(): string {
    switch (this.syncStatus().result) {
      case 'SUCCESS':
        return 'pip--up';
      case 'FAILED':
        return 'pip--down';
      default:
        return 'pip--checking';
    }
  }

  /** Returns the i18n key for the status badge label. */
  get badgeKey(): string {
    switch (this.syncStatus().result) {
      case 'SUCCESS':
        return 'dashboard.sync_result_success';
      case 'FAILED':
        return 'dashboard.sync_result_failed';
      default:
        return 'dashboard.sync_result_never';
    }
  }

  /** Returns the CSS modifier class for the status badge. */
  get badgeClass(): string {
    switch (this.syncStatus().result) {
      case 'SUCCESS':
        return 'status-badge--up';
      case 'FAILED':
        return 'status-badge--down';
      default:
        return 'status-badge--checking';
    }
  }

  /** Formats the lastSyncAt timestamp as a relative time string (e.g. "2m ago"). */
  get relativeTime(): string | null {
    const raw = this.syncStatus().lastSyncAt;
    if (!raw) {
      return null;
    }
    const diffMs = Date.now() - new Date(raw).getTime();
    const diffSec = Math.floor(diffMs / 1000);
    if (diffSec < 60) return `${diffSec}s ago`;
    const diffMin = Math.floor(diffSec / 60);
    if (diffMin < 60) return `${diffMin}m ago`;
    const diffH = Math.floor(diffMin / 60);
    return `${diffH}h ago`;
  }
}
