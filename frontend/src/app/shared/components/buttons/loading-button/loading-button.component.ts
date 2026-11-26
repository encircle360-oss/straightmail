import { Component, input } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-loading-button',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './loading-button.component.html',
})
/**
 * Button component with an integrated loading state.
 *
 * Displays a spinner and {@link loadingLabel} while {@link loading} is {@code true} and
 * disables the button to prevent double submission.
 */
export class LoadingButtonComponent {
  /** Whether the button should render in its loading (spinner) state. */
  loading = input.required<boolean>();
  /** Translation key for the button label shown when not loading. */
  label = input.required<string>();
  /** Translation key for the button label shown while loading. */
  loadingLabel = input.required<string>();
  /** Optional Bootstrap icon class rendered before the label. */
  icon = input<string>();
  /** HTML button type; defaults to {@code 'submit'}. */
  type = input<'submit' | 'button'>('submit');
  /** Bootstrap button variant CSS class; defaults to {@code 'btn-primary'}. */
  variant = input('btn-primary');
}
