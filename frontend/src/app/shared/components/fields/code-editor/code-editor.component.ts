import {
  AfterViewInit,
  Component,
  effect,
  ElementRef,
  forwardRef,
  inject,
  input,
  OnDestroy,
  ViewChild,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { basicSetup } from 'codemirror';
import { Compartment, EditorState } from '@codemirror/state';
import { EditorView, keymap, placeholder as placeholderExt } from '@codemirror/view';
import { html } from '@codemirror/lang-html';
import { json } from '@codemirror/lang-json';
import { indentWithTab } from '@codemirror/commands';

@Component({
  selector: 'app-code-editor',
  standalone: true,
  template: ` <div #host></div>`,
  styleUrl: './code-editor.component.scss',
  providers: [
    { provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => CodeEditorComponent), multi: true },
  ],
})
/**
 * CodeMirror-based code editor component implementing {@link ControlValueAccessor}.
 *
 * Renders a full-featured code editor with syntax highlighting and indentation support for HTML
 * and JSON. Language mode and placeholder are reconfigurable via signals at runtime. The editor
 * height is derived from the {@link rows} input. Integrates seamlessly with Angular reactive forms
 * via the standard {@code NG_VALUE_ACCESSOR} token.
 */
export class CodeEditorComponent implements ControlValueAccessor, AfterViewInit, OnDestroy {
  /** Syntax language to activate in the editor; defaults to {@code 'html'}. */
  readonly language = input<'html' | 'json'>('html');
  /** Height of the editor in text rows (each row ≈ 21 px). */
  readonly rows = input(8);
  /** Placeholder text shown when the editor is empty. */
  readonly placeholder = input('');

  @ViewChild('host', { static: true }) host!: ElementRef<HTMLDivElement>;
  private readonly elRef = inject(ElementRef);

  private view?: EditorView;
  private langCompartment = new Compartment();
  private editableCompartment = new Compartment();
  private skipUpdate = false;
  /**
   * Value and disabled state as last set by the form directive. Reactive forms call
   * {@link writeValue} and {@link setDisabledState} during directive init, which runs before
   * {@link ngAfterViewInit} creates the editor — so both are buffered here and applied when the
   * editor is built (and re-applied whenever it is rebuilt for a placeholder change).
   */
  private pendingValue = '';
  private disabled = false;

  constructor() {
    effect(() => {
      this.elRef.nativeElement.style.setProperty('--editor-rows', `${this.rows() * 21}px`);
    });
    effect(() => {
      const ext = this.language() === 'json' ? json() : html();
      this.view?.dispatch({ effects: this.langCompartment.reconfigure(ext) });
    });
    effect(() => {
      const p = this.placeholder();
      if (!this.view) return;
      const doc = this.view.state.doc.toString();
      this.view.destroy();
      this.buildEditor(doc, p);
    });
  }

  ngAfterViewInit(): void {
    this.buildEditor(this.pendingValue, this.placeholder());
  }

  writeValue(value: string | null): void {
    this.pendingValue = value ?? '';
    if (!this.view) return;
    const cur = this.view.state.doc.toString();
    const next = value ?? '';
    if (cur === next) return;
    this.skipUpdate = true;
    this.view.dispatch({ changes: { from: 0, to: cur.length, insert: next } });
    this.skipUpdate = false;
  }

  registerOnChange(fn: (v: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(disabled: boolean): void {
    this.disabled = disabled;
    this.view?.dispatch({
      effects: this.editableCompartment.reconfigure(EditorView.editable.of(!disabled)),
    });
  }

  ngOnDestroy(): void {
    this.view?.destroy();
  }

  private onChange: (v: string) => void = () => {};

  private onTouched: () => void = () => {};

  private buildEditor(initialValue = '', placeholderText = ''): void {
    const langExt = this.language() === 'json' ? json() : html();
    this.view = new EditorView({
      state: EditorState.create({
        doc: initialValue,
        extensions: [
          basicSetup,
          keymap.of([indentWithTab]),
          this.langCompartment.of(langExt),
          this.editableCompartment.of(EditorView.editable.of(!this.disabled)),
          placeholderExt(placeholderText),
          EditorView.updateListener.of((u) => {
            if (u.docChanged && !this.skipUpdate) {
              this.pendingValue = u.state.doc.toString();
              this.onChange(this.pendingValue);
            }
          }),
          EditorView.domEventHandlers({
            blur: () => {
              this.onTouched();
              return false;
            },
          }),
        ],
      }),
      parent: this.host.nativeElement,
    });
  }
}
