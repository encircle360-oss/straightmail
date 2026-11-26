import { Component, inject, input } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './confirm-dialog.component.html',
})
/**
 * Generic confirmation dialog component opened via {@link ConfirmService}.
 *
 * The caller closes the modal by calling {@code modal.close()} (confirm) or
 * {@code modal.dismiss()} (cancel). The result is surfaced as a {@code Promise<boolean>} by the
 * service. Signal inputs are set programmatically via {@code ComponentRef.setInput()} because
 * ngb-modal does not expose a public API for this on signal inputs.
 */
export class ConfirmDialogComponent {
  /** Translation key (or literal string) for the dialog body message. */
  readonly message = input.required<string>();
  /** Optional interpolation parameters for the message translation key. */
  readonly messageParams = input<Record<string, string>>();
  /** Translation key for the confirm button label; defaults to {@code 'common.delete'}. */
  readonly confirmLabel = input('common.delete');
  /** Visual variant for the confirm button: {@code 'danger'} (red) or {@code 'primary'} (blue). */
  readonly variant = input<'danger' | 'primary'>('danger');
  /** Reference to the active ngb-modal instance for programmatic close/dismiss. */
  readonly modal = inject(NgbActiveModal);
}
