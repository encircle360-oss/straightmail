import { Component, computed, inject, input, signal } from '@angular/core';
import { NgbAccordionModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslatePipe } from '@ngx-translate/core';
import { Store } from '@ngxs/store';
import { UiState } from '../../../../store/ui/ui.state';
import { UiActions } from '../../../../store/ui/ui.actions';

@Component({
  selector: 'app-accordion-item',
  standalone: true,
  imports: [NgbAccordionModule, TranslatePipe],
  templateUrl: './accordion-item.component.html',
  styleUrl: './accordion-item.component.scss',
})
/**
 * Accordion panel component with optional state persistence via {@link UiState}.
 *
 * When {@link persist} is {@code true} (default), the collapsed/expanded state is stored in
 * {@link UiState} under the provided {@link stateKey} so it survives navigation. When
 * {@code persist} is {@code false}, state is kept locally in a signal and lost on destroy.
 */
export class AccordionItemComponent {
  /** Unique key used to store the panel state in {@link UiState} when persisting. */
  readonly stateKey = input.required<string>();
  /** Translation key used as the accordion panel header label. */
  readonly label = input.required<string>();
  /** Optional Bootstrap icon class rendered in the panel header. */
  readonly icon = input<string>('');
  /** Whether the panel starts collapsed; overridden by persisted state when {@link persist} is {@code true}. */
  readonly defaultCollapsed = input<boolean>(false);
  /** Whether to persist the collapsed state in {@link UiState}; defaults to {@code true}. */
  readonly persist = input<boolean>(true);

  private readonly store = inject(Store);
  private readonly localCollapsed = signal<boolean | null>(null);

  protected readonly collapsed = computed(() => {
    if (!this.persist()) {
      return this.localCollapsed() ?? this.defaultCollapsed();
    }
    const status = this.store.selectSignal(UiState.accordionStatus)();
    const key = this.stateKey();
    return key in status ? status[key] : this.defaultCollapsed();
  });

  protected onShown(): void {
    this.onChange(false);
  }

  protected onHidden(): void {
    this.onChange(true);
  }

  private onChange(collapsed: boolean): void {
    if (this.persist()) {
      this.store.dispatch(new UiActions.UpdateAccordionStatus(this.stateKey(), collapsed));
    } else {
      this.localCollapsed.set(collapsed);
    }
  }
}
