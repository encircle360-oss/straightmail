import { CanDeactivateFn } from '@angular/router';

export interface CanDeactivateComponent {
  canDeactivate(): boolean | Promise<boolean>;
}

export const canDeactivateTemplateForm: CanDeactivateFn<CanDeactivateComponent> = (component) =>
  component.canDeactivate();
