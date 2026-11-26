import { Component, input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { FormErrorComponent } from '../form-error/form-error.component';

@Component({
  selector: 'app-select-field',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe, FormErrorComponent],
  templateUrl: './select-field.component.html',
})
/**
 * Reusable select (dropdown) field that wraps a {@link FormControl}.
 *
 * Binds the control directly via {@code [formControl]}, so value synchronisation, dirty/touched
 * state and the disabled state are handled by reactive forms. The available options are projected
 * via content projection in the template.
 */
export class SelectFieldComponent {
  /** The reactive form control to bind to this field. */
  readonly control = input.required<FormControl<string | null>>();
  /** Translation key used as the field label. */
  readonly label = input.required<string>();
  /** Whether the field is required (used to display an asterisk). */
  readonly required = input(false);
  /** Bootstrap icon class rendered as a prefix icon. */
  readonly icon = input<string>('');
}
