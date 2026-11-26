import { Component, input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';
import { FormErrorComponent } from '../form-error/form-error.component';

@Component({
  selector: 'app-text-field',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe, FormErrorComponent],
  templateUrl: './text-field.component.html',
})
/**
 * Reusable text input field that wraps a {@link FormControl}.
 *
 * Binds the control directly via {@code [formControl]}, so value synchronisation, dirty/touched
 * state and the disabled state are handled by reactive forms. Displays validation errors via
 * {@link FormErrorComponent}.
 */
export class TextFieldComponent {
  /** The reactive form control to bind to this field. */
  readonly control = input.required<FormControl<string | null>>();
  /** Translation key used as the field label. */
  readonly label = input.required<string>();
  /** HTML input type; defaults to {@code 'text'}. */
  readonly type = input<'text' | 'email'>('text');
  /** Placeholder text for the input. */
  readonly placeholder = input('');
  /** Whether the field is required (used to display an asterisk). */
  readonly required = input(false);
  /** Bootstrap icon class rendered as a prefix icon (e.g. {@code 'bi-envelope'}). */
  readonly icon = input<string>('');
}
