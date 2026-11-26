import { ChangeDetectionStrategy, Component, input, output, signal, Signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { EmailTemplate } from '../../../core/services/api.service';

@Component({
  selector: 'app-template-grid',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  templateUrl: './template-grid.component.html',
  styleUrl: './template-grid.component.scss',
})
/**
 * Presentational grid component for displaying email templates as interactive cards.
 *
 * Each card shows the template name, locale, source tags, and a truncated ID with a
 * copy-to-clipboard button. Clicking a card navigates to the edit form for DATABASE templates,
 * or the readonly view for GIT/FILE templates. A delete button is revealed on hover for
 * editable templates.
 */
export class TemplateGridComponent {
  /** Signal emitting the currently loaded list of templates to display. */
  readonly templates = input.required<Signal<EmailTemplate[]>>();
  /** Signal emitting whether a data request is in progress. */
  readonly loading = input.required<Signal<boolean>>();
  /** Emitted when the user clicks a read-only (GIT/FILE) template card. */
  readonly view = output<EmailTemplate>();
  /** Emitted when the user clicks an editable (DATABASE) template card. */
  readonly edit = output<EmailTemplate>();
  /** Emitted when the user clicks the delete button on a template card. */
  readonly delete = output<EmailTemplate>();

  /** Set of template IDs currently displaying copy-confirmed feedback (bi-check-lg icon). */
  readonly copiedIds = signal<Set<string>>(new Set());

  /**
   * Handles a card click by emitting the appropriate event based on the template's editability.
   *
   * @param template The template whose card was clicked.
   */
  onCardClick(template: EmailTemplate): void {
    if (template.editable !== false) {
      this.edit.emit(template);
    } else {
      this.view.emit(template);
    }
  }

  /** Returns the tags array with source tags sorted to the front. */
  sortedTags(tags: string[]): string[] {
    return [...tags].sort((a, b) => {
      const aIsSource = a.startsWith('source:');
      const bIsSource = b.startsWith('source:');
      if (aIsSource === bIsSource) return 0;
      return aIsSource ? -1 : 1;
    });
  }

  /** Returns true when the tag encodes the template's source origin (e.g. 'source:git'). */
  isSourceTag(tag: string): boolean {
    return tag.startsWith('source:');
  }

  /** Returns the Bootstrap Icon class for a source tag (e.g. 'source:git' → 'bi-git'). */
  sourceTagIcon(tag: string): string {
    switch (tag) {
      case 'source:database':
        return 'bi-database-fill';
      case 'source:git':
        return 'bi-git';
      default:
        return 'bi-file-earmark-text';
    }
  }

  /** Returns the i18n key for the human-readable source tag label. */
  sourceTagKey(tag: string): string {
    switch (tag) {
      case 'source:database':
        return 'templates.source_database';
      case 'source:git':
        return 'templates.source_git';
      default:
        return 'templates.source_file';
    }
  }

  /** Returns the first 8 characters of a template ID for the truncated card display. */
  shortId(id: string): string {
    return id.slice(0, 8);
  }

  /**
   * Copies the full template ID to the clipboard and briefly shows a checkmark icon as
   * confirmation, reverting after 1.5 s.
   *
   * @param id The full template UUID to copy.
   * @param event DOM event — propagation is stopped to prevent the card click handler from firing.
   */
  copyId(id: string, event: Event): void {
    event.stopPropagation();
    navigator.clipboard.writeText(id).then(() => {
      this.copiedIds.update((prev) => new Set([...prev, id]));
      setTimeout(() => {
        this.copiedIds.update((prev) => {
          const next = new Set(prev);
          next.delete(id);
          return next;
        });
      }, 1500);
    });
  }
}
