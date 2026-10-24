import { AbstractControl, ValidationErrors } from '@angular/forms';

/**
 * Angular reactive form validator that checks whether the control's string value is valid JSON.
 *
 * Returns {@code null} when the value is empty or parses correctly, and
 * {@code { invalidJson: true }} when {@code JSON.parse} throws.
 *
 * @param control The form control to validate.
 * @returns A {@link ValidationErrors} object with key {@code invalidJson} on failure, or {@code null}.
 */
export function jsonValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  try {
    JSON.parse(control.value);
    return null;
  } catch {
    return { invalidJson: true };
  }
}
