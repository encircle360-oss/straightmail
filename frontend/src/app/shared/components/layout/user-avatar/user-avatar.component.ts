import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { Md5 } from 'ts-md5';

@Component({
  selector: 'app-user-avatar',
  standalone: true,
  templateUrl: './user-avatar.component.html',
  styleUrl: './user-avatar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'user-avatar',
    '[style.background-color]': '(!gravatarUrl() || gravatarFailed()) ? bgColor() : "transparent"',
  },
})
/**
 * Displays a user avatar in the top navigation bar.
 *
 * Shows the user's Gravatar image when one is registered for the given email address.
 * Falls back to a deterministic initials avatar (same algorithm as {@link TenantAvatarComponent})
 * when no Gravatar is found (HTTP 404) or no email is provided.
 */
export class UserAvatarComponent {
  /** The user's email address used to look up the Gravatar. */
  readonly email = input<string | null>(null);

  /** The user's display name, used to derive initials for the fallback avatar. */
  readonly name = input<string>('');

  /** Set to {@code true} when the Gravatar image request returns a 404. */
  readonly gravatarFailed = signal(false);

  /**
   * Returns the Gravatar URL for the given email, or {@code null} when no email is available.
   * Uses {@code d=404} so the server returns HTTP 404 instead of a default image,
   * allowing the {@code (error)} handler to activate the initials fallback.
   */
  readonly gravatarUrl = computed(() => {
    const e = this.email();
    if (!e) return null;
    const hash = Md5.hashStr(e.trim().toLowerCase());
    return `https://www.gravatar.com/avatar/${hash}?d=404&s=32`;
  });

  /** Derives up to two uppercase initials from the display name. */
  readonly initials = computed(() => {
    return (
      this.name()
        .split(/\s+/)
        .map((w) => w[0])
        .join('')
        .slice(0, 2)
        .toUpperCase() || '?'
    );
  });

  /**
   * Returns a deterministic HSL background color derived from the email or name.
   * The same input always produces the same color, giving each user a stable visual identity.
   */
  readonly bgColor = computed(() => {
    const key = this.email() ?? this.name();
    if (!key) return 'hsl(0, 0%, 40%)';
    const hues = [210, 142, 271, 32, 180, 356, 56, 320];
    const hash = key.split('').reduce((acc, c) => acc + c.charCodeAt(0), 0);
    return `hsl(${hues[hash % hues.length]}, 60%, 42%)`;
  });
}
