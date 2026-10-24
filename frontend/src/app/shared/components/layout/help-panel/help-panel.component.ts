import { AfterViewInit, Component, ElementRef, inject, signal } from '@angular/core';
import { NgbActiveOffcanvas } from '@ng-bootstrap/ng-bootstrap';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-help-panel',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './help-panel.component.html',
  styleUrl: './help-panel.component.scss',
})
/**
 * Context-sensitive help off-canvas panel.
 *
 * Opened by the topbar's help button and scrolls automatically to the section that matches
 * the current route ({@link activeSection}). The panel is closed via {@link NgbActiveOffcanvas}.
 */
export class HelpPanelComponent implements AfterViewInit {
  /**
   * Identifier of the section to scroll into view after the panel opens
   * (e.g. {@code 'dashboard'}, {@code 'templates'}, {@code 'send'}, {@code 'render'}).
   * {@code null} means no auto-scroll.
   */
  readonly activeSection = signal<string | null>(null);

  private readonly el = inject(ElementRef);
  readonly activeOffcanvas = inject(NgbActiveOffcanvas);

  ngAfterViewInit(): void {
    const active = this.el.nativeElement.querySelector('.help-section--active');
    active?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
