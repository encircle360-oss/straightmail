import { Component, input } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-chip-field',
  standalone: true,
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './chip-field.component.html',
  styleUrl: './chip-field.component.scss',
})
/**
 * Chip (tag-list) input field backed by a {@link FormArray}.
 *
 * Values are added by pressing Enter or comma and removed by clicking the chip's dismiss button.
 * The parent form is marked dirty on any change.
 */
export class ChipFieldComponent {
  /** The parent reactive form group containing both the {@link FormArray} and the text input control. */
  readonly form = input.required<FormGroup>();
  /** Name of the {@link FormArray} control within the form group. */
  readonly arrayName = input.required<string>();
  /** Name of the scratch text input control used for staging new chip values. */
  readonly inputName = input.required<string>();
  /** Translation key used as the field label. */
  readonly label = input.required<string>();
  /** Whether at least one chip is required. */
  readonly required = input(false);
  /** Placeholder text shown in the text input. */
  readonly placeholder = input('');
  /** HTML input type for the staging input (e.g. {@code 'email'} for email validation). */
  readonly inputType = input<'email' | 'text'>('text');
  /** Whether to display a required validation error without the control being touched. */
  readonly showRequiredError = input(false);
  /** Bootstrap icon class rendered as a prefix icon. */
  readonly icon = input<string>('');
  /** When {@code true}, hides the add/remove controls and disables the staging input. */
  readonly disabled = input(false);

  /** Returns the {@link FormArray} for the chip values. */
  get array(): FormArray {
    return this.form().get(this.arrayName()) as FormArray;
  }

  /**
   * Reads the staging input value, appends it as a new chip, and clears the input.
   *
   * The chip carries the same validator as the staging input, so a malformed address stays
   * invalid after it becomes a chip. The form is set to novalidate, so the browser's own
   * type="email" check never runs.
   */
  add(): void {
    const form = this.form();
    const inputName = this.inputName();
    const val = (form.get(inputName)?.value as string)?.trim();
    if (!val) return;
    const validators = this.inputType() === 'email' ? [Validators.email] : [];
    this.array.push(new FormControl(val, validators));
    form.get(inputName)?.setValue('');
    form.markAsDirty();
  }

  /**
   * Removes the chip at the given index.
   *
   * @param index Zero-based index of the chip to remove.
   */
  remove(index: number): void {
    this.array.removeAt(index);
    this.form().markAsDirty();
  }

  /**
   * Handles Enter and comma keydown events to trigger chip addition.
   *
   * @param event The keyboard event from the staging input.
   */
  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      this.add();
    }
  }
}
