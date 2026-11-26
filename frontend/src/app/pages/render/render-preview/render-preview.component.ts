import { Component, ElementRef, input, ViewChild } from '@angular/core';
import { SafeHtml } from '@angular/platform-browser';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  selector: 'app-render-preview',
  standalone: true,
  imports: [NgbNavModule, TranslatePipe],
  templateUrl: './render-preview.component.html',
  styleUrl: './render-preview.component.scss',
})
/**
 * Tabbed preview component that displays the rendered HTML and plain-text output side by side.
 *
 * The HTML content must be pre-sanitised (as {@link SafeHtml}) by the parent before passing it
 * here. Used inside {@link RenderComponent}.
 */
export class RenderPreviewComponent {
  /** Sanitised HTML to render in the HTML tab; {@code null} when no render result is available. */
  readonly html = input.required<SafeHtml | null>();
  /** Plain-text body to display in the plain-text tab; {@code null} when no render result is available. */
  readonly plain = input.required<string | null>();
  /** Template error message from a failed render (e.g. FreeMarker syntax error); {@code null} on success or before first render. */
  readonly error = input.required<string | null>();
  /** Zero-based index of the currently active preview tab. */
  activeTab = 1;

  @ViewChild('previewIframe') private iframeRef?: ElementRef<HTMLIFrameElement>;

  /**
   * Called when the iframe finishes loading its srcdoc content. Defers the height calculation
   * to the next animation frame so the browser has completed layout before scrollHeight is read.
   *
   * Uses event.target instead of @ViewChild because the load event can fire before Angular has
   * updated the view query (e.g. on the first render while the nav tab is being created).
   */
  onIframeLoad(event: Event): void {
    const iframe = event.target as HTMLIFrameElement;
    requestAnimationFrame(() => this.applyHeight(iframe));
  }

  /**
   * Called by the ngbNavItem (shown) output when the HTML tab becomes visible after being hidden.
   * Recalculates the iframe height because scrollHeight returns 0 while the pane is display:none.
   */
  adjustIframeHeight(): void {
    const iframe = this.iframeRef?.nativeElement;
    if (iframe) requestAnimationFrame(() => this.applyHeight(iframe));
  }

  private applyHeight(iframe: HTMLIFrameElement): void {
    const doc = iframe.contentDocument;
    // Prefer body.scrollHeight — email templates often set height:100% on <html>,
    // which would make documentElement.scrollHeight equal to the (small) viewport height.
    const height = doc?.body?.scrollHeight ?? doc?.documentElement?.scrollHeight;
    if (height) iframe.style.height = `${height}px`;
  }
}
