import { Component, input } from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-form-error',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './form-error.component.html',
})
/**
 * Displays validation error messages for a reactive form control.
 *
 * Only shows errors after the control has been touched. Accepts a configurable map of
 * error keys to translation keys, defaulting to {@code required}, {@code email}, and
 * {@code invalidJson} validators.
 */
export class FormErrorComponent {
  /** The form control whose errors should be displayed. */
  control = input<AbstractControl | null>(null);
  /**
   * Map of Angular validator error keys to translation keys for their messages.
   * Defaults cover {@code required}, {@code email}, and {@code invalidJson}.
   */
  errors = input<Record<string, string>>({
    required: 'common.required',
    email: 'common.email_invalid',
    invalidJson: 'common.json_invalid',
  });

  /** Returns the list of currently active (touched) validation errors with their message keys. */
  get activeErrors(): { key: string; msg: string }[] {
    const ctrl = this.control();
    if (!ctrl?.touched) return [];
    return Object.entries(this.errors())
      .filter(([key]) => ctrl.hasError(key))
      .map(([key, msg]) => ({ key, msg }));
  }
}
