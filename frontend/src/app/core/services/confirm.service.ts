import { ComponentRef, inject, Injectable } from '@angular/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { ConfirmDialogComponent } from '../../shared/components/overlays/confirm-dialog/confirm-dialog.component';

/**
 * Service for displaying modal confirmation dialogs.
 *
 * Opens a {@link ConfirmDialogComponent} via ngb-modal and returns a promise that resolves to
 * {@code true} when the user confirms or {@code false} when they dismiss/cancel.
 */
@Injectable({ providedIn: 'root' })
export class ConfirmService {
  private readonly modal = inject(NgbModal);

  /**
   * Opens a confirmation dialog with the given message and optional customisation.
   *
   * @param message Translation key (or literal string) for the dialog body.
   * @param options Optional configuration for interpolation params, button label, and colour variant.
   * @returns A promise that resolves to {@code true} on confirmation or {@code false} on dismissal.
   */
  confirm(
    message: string,
    options?: {
      messageParams?: Record<string, string>;
      confirmLabel?: string;
      variant?: 'danger' | 'primary';
    },
  ): Promise<boolean> {
    const ref = this.modal.open(ConfirmDialogComponent);
    // NgbModalRef does not expose ComponentRef publicly; access it via the internal
    // _contentRef to call setInput(), which is required for signal inputs (input()).
    const componentRef: ComponentRef<ConfirmDialogComponent> = (ref as any)._contentRef
      .componentRef;
    componentRef.setInput('message', message);
    if (options?.messageParams) componentRef.setInput('messageParams', options.messageParams);
    if (options?.confirmLabel) componentRef.setInput('confirmLabel', options.confirmLabel);
    if (options?.variant) componentRef.setInput('variant', options.variant);
    return ref.result.then(
      () => true,
      () => false,
    );
  }
}
