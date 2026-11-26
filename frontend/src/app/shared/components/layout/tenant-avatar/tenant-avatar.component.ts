import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TenantDTO } from '../../../../store/tenant/tenant.model';

@Component({
  selector: 'app-tenant-avatar',
  standalone: true,
  templateUrl: './tenant-avatar.component.html',
  styleUrl: './tenant-avatar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'tenant-avatar',
    '[class.tenant-avatar--sm]': 'size() === "sm"',
    '[class.tenant-avatar--md]': 'size() === "md"',
    '[class.tenant-avatar--lg]': 'size() === "lg"',
    '[style.background-color]': 'bgColor()',
  },
})
/**
 * Reusable tenant avatar component that displays either the tenant's logo image
 * or a fallback showing the tenant's initials on a deterministic or branded background.
 * When no tenant is provided, a neutral placeholder with a building icon is shown.
 */
export class TenantAvatarComponent {
  /** The tenant to display. Pass {@code null} to show a neutral placeholder. */
  readonly tenant = input.required<TenantDTO | null>();

  /** Size variant controlling the avatar dimensions. Defaults to {@code 'md'}. */
  readonly size = input<'sm' | 'md' | 'lg'>('md');

  /**
   * Resolved background color: {@code brandColor} if set, otherwise a deterministic
   * HSL color derived from the slug hash, or neutral gray for no tenant.
   */
  readonly bgColor = computed(() => {
    const t = this.tenant();
    if (!t) return 'hsl(0, 0%, 40%)';
    return t.brandColor ?? this.avatarColor(t.slug);
  });

  /**
   * Derives up to two uppercase initials from a display name.
   *
   * @param name The tenant display name.
   */
  initials(name: string): string {
    return name
      .split(/\s+/)
      .map((w) => w[0])
      .join('')
      .slice(0, 2)
      .toUpperCase();
  }

  /**
   * Returns a deterministic HSL background color for the tenant avatar based on the slug.
   * The same slug always produces the same color, giving tenants a stable visual identity.
   *
   * @param slug The tenant slug used as a hash input.
   */
  avatarColor(slug: string): string {
    const hues = [210, 142, 271, 32, 180, 356, 56, 320];
    const hash = slug.split('').reduce((acc, c) => acc + c.charCodeAt(0), 0);
    return `hsl(${hues[hash % hues.length]}, 60%, 42%)`;
  }
}
