import { CanDeactivateFn } from '@angular/router';

/**
 * Interface that form components must implement to participate in unsaved-changes protection.
 *
 * The {@link canDeactivateForm} guard delegates to this method to determine whether
 * navigation away from the component should be allowed.
 */
export interface CanDeactivateComponent {
  /**
   * Returns {@code true} (or resolves to {@code true}) to allow navigation, or {@code false}
   * (or resolves to {@code false}) to block it.
   */
  canDeactivate(): boolean | Promise<boolean>;
}

/**
 * Route deactivation guard that delegates to the component's own {@link CanDeactivateComponent.canDeactivate}
 * method. Applied to form routes (e.g. template and tenant create/edit) to warn users about
 * unsaved changes before navigating away.
 */
export const canDeactivateForm: CanDeactivateFn<CanDeactivateComponent> = (component) =>
  component.canDeactivate();
