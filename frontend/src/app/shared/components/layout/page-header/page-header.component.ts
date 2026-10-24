import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { NavIconComponent, PageIcon } from '../nav-icon/nav-icon.component';

export type { PageIcon };

@Component({
  selector: 'app-page-header',
  standalone: true,
  imports: [TranslatePipe, NavIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './page-header.component.html',
  styleUrl: './page-header.component.scss',
})
/**
 * Standardised page header component for authenticated page views.
 *
 * Renders an optional kicker (e.g. breadcrumb or category label), a primary title, and an
 * optional horizontal rule separator. An optional {@code icon} renders the matching sidebar
 * nav icon to the left of the heading text.
 */
export class PageHeaderComponent {
  /** Translation key for the main heading text. */
  readonly title = input.required<string>();
  /** Optional translation key for a smaller label displayed above the title. */
  readonly kicker = input<string>();
  /** When {@code true}, renders a horizontal rule beneath the title. */
  readonly showRule = input(true);
  /** Optional sidebar icon to display left of the heading. */
  readonly icon = input<PageIcon>();
}
