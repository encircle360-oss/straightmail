import { Component, inject, input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TranslatePipe } from '@ngx-translate/core';
import { FormErrorComponent } from '../form-error/form-error.component';

@Component({
  selector: 'app-textarea-field',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe, FormErrorComponent],
  templateUrl: './textarea-field.component.html',
})
/**
 * Reusable multi-line textarea field that wraps a {@link FormControl}.
 *
 * Binds the control directly via {@code [formControl]}, so value synchronisation, dirty/touched
 * state and the disabled state are handled by reactive forms. Supports an optional monospace
 * display mode and a "load sample" action that fetches a sample plain-text body from
 * {@code /samples/sample.txt}.
 */
export class TextareaFieldComponent {
  /** The reactive form control to bind to this field. */
  readonly control = input.required<FormControl<string | null>>();
  /** Translation key used as the field label. */
  readonly label = input.required<string>();
  /** Number of visible text rows in the textarea. */
  readonly rows = input(4);
  /** Placeholder text shown when the textarea is empty. */
  readonly placeholder = input('');
  /** When {@code true}, renders the textarea in a monospace font. */
  readonly mono = input(false);
  /** Bootstrap icon class rendered as a prefix icon. */
  readonly icon = input<string>('');
  /** When {@code true}, hides the "load sample" action. */
  readonly disabled = input(false);

  private readonly http = inject(HttpClient);

  /** Fetches a sample plain-text body from {@code /samples/sample.txt} and writes it into the control. */
  loadSample(): void {
    this.http.get('/samples/sample.txt', { responseType: 'text' }).subscribe((content) => {
      this.control().setValue(content);
      this.control().markAsDirty();
    });
  }
}
