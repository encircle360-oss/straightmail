import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type PageIcon = 'dashboard' | 'templates' | 'preview' | 'send' | 'building' | 'edit';

@Component({
  selector: 'app-nav-icon',
  standalone: true,
  templateUrl: './nav-icon.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
/** Renders the navigation icon for a given route. Sizing is inherited from the parent via `font-size`. */
export class NavIconComponent {
  /** The icon identifier to render. */
  readonly icon = input.required<PageIcon>();
}
