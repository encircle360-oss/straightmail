import { Component, inject, input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TranslatePipe } from '@ngx-translate/core';
import { CodeEditorComponent } from '../code-editor/code-editor.component';
import { FormErrorComponent } from '../form-error/form-error.component';

@Component({
  selector: 'app-html-body-field',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe, CodeEditorComponent, FormErrorComponent],
  templateUrl: './html-body-field.component.html',
})
/**
 * HTML body editor field that wraps a {@link CodeEditorComponent} (CodeMirror) for HTML.
 *
 * Binds the control directly via {@code [formControl]}, so value synchronisation, dirty/touched
 * state and the disabled state are handled by reactive forms. Provides a "load sample" action
 * that fetches a sample HTML body from {@code /samples/sample.html}.
 */
export class HtmlBodyFieldComponent {
  /** The reactive form control to bind to this field. */
  readonly control = input.required<FormControl<string | null>>();
  /** Translation key used as the field label. */
  readonly label = input('templates.field_html');
  /** Number of visible rows for the code editor. */
  readonly rows = input(12);
  /** Bootstrap icon class rendered as a prefix icon. */
  readonly icon = input<string>('');
  /** When {@code true}, hides the "load sample" action. */
  readonly disabled = input(false);

  private readonly http = inject(HttpClient);

  /** Fetches a sample HTML body from {@code /samples/sample.html} and writes it into the control. */
  loadSample(): void {
    this.http.get('/samples/sample.html', { responseType: 'text' }).subscribe((content) => {
      this.control().setValue(content);
      this.control().markAsDirty();
    });
  }
}
