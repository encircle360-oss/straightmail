import { Component, inject } from '@angular/core';
import { NgbToastModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslatePipe } from '@ngx-translate/core';
import { Store } from '@ngxs/store';
import { ToastState } from '../../../../store/toast/toast.state';
import { RemoveToast } from '../../../../store/toast/toast.actions';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [NgbToastModule, TranslatePipe],
  templateUrl: './toast-container.component.html',
})
/**
 * Global toast notification container rendered in the root {@link LayoutComponent}.
 *
 * Reads active toasts from {@link ToastState} and renders them as ngb-toast elements.
 * Each toast can be manually dismissed via the close button, dispatching {@link RemoveToast}.
 */
export class ToastContainerComponent {
  private readonly store = inject(Store);

  /** Signal emitting the list of currently active toast notifications. */
  toasts = this.store.selectSignal(ToastState.toasts);

  /**
   * Returns the CSS classes for the toast background based on its type.
   *
   * @param type The toast type: {@code 'success'}, {@code 'error'}, or {@code 'info'}.
   * @returns Bootstrap background/text utility class string.
   */
  toastClass(type: string): string {
    const map: Record<string, string> = {
      success: 'bg-success text-white',
      error: 'bg-danger text-white',
      info: 'bg-primary text-white',
    };
    return map[type] ?? 'bg-primary text-white';
  }

  /**
   * Returns the Bootstrap icon class for the toast type.
   *
   * @param type The toast type: {@code 'success'}, {@code 'error'}, or {@code 'info'}.
   * @returns Bootstrap icon class string.
   */
  iconClass(type: string): string {
    const map: Record<string, string> = {
      success: 'bi bi-check-circle-fill',
      error: 'bi bi-exclamation-triangle-fill',
      info: 'bi bi-info-circle-fill',
    };
    return map[type] ?? 'bi bi-info-circle-fill';
  }

  /**
   * Manually dismisses a toast by dispatching a {@link RemoveToast} action.
   *
   * @param id The numeric identifier of the toast to remove.
   */
  remove(id: number): void {
    this.store.dispatch(new RemoveToast(id));
  }
}
