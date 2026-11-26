import { Component, effect, inject, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { CodeEditorComponent } from '../code-editor/code-editor.component';
import { FormErrorComponent } from '../form-error/form-error.component';

@Component({
  selector: 'app-json-model-field',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe, CodeEditorComponent, FormErrorComponent],
  templateUrl: './json-model-field.component.html',
})
/**
 * JSON model editor field that wraps a {@link CodeEditorComponent} (CodeMirror) for JSON.
 *
 * Binds the control directly via {@code [formControl]}, so value synchronisation, dirty/touched
 * state and the disabled state are handled by reactive forms. The placeholder is resolved through
 * {@code TranslateService} so translation keys are supported. Provides a "load sample" action that
 * fetches a sample JSON model from {@code /samples/sample.json}.
 */
export class JsonModelFieldComponent {
  /** The reactive form control to bind to this field. */
  readonly control = input.required<FormControl<string | null>>();
  /** Translation key used as the field label. */
  readonly label = input.required<string>();
  /** Translation key for the editor placeholder text. */
  readonly placeholder = input('');
  /** Number of visible rows for the code editor. */
  readonly rows = input(4);
  /** Bootstrap icon class rendered as a prefix icon. */
  readonly icon = input<string>('');

  protected resolvedPlaceholder = signal('');

  private readonly translate = inject(TranslateService);
  private readonly http = inject(HttpClient);

  constructor() {
    effect(() => {
      const key = this.placeholder();
      this.resolvedPlaceholder.set(key ? this.translate.instant(key) : '');
    });
  }

  /** Fetches a sample JSON model from {@code /samples/sample.json} and writes it into the control. */
  loadSample(): void {
    this.http.get('/samples/sample.json', { responseType: 'text' }).subscribe((content) => {
      this.control().setValue(content);
      this.control().markAsDirty();
    });
  }
}
