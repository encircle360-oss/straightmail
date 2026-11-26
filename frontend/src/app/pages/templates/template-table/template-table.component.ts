import { ChangeDetectionStrategy, Component, input, output, signal, Signal } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { EmailTemplate } from '../../../core/services/api.service';

@Component({
  selector: 'app-template-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe],
  templateUrl: './template-table.component.html',
  styleUrl: './template-table.component.scss',
})
/**
 * Presentational table component for displaying the list of email templates.
 *
 * Renders the templates signal supplied by the parent and emits {@link view}, {@link edit}, and
 * {@link delete} events when the user clicks the corresponding action buttons. DATABASE templates
 * show edit/delete actions; GIT and FILE templates show a view-only action.
 *
 * The ID column shows a truncated 8-character prefix with a copy-to-clipboard button. On screens
 * narrower than xl (< 1200 px) tags are capped at 2 with an overflow badge; at xl+ all tags are
 * shown via CSS without any JavaScript breakpoint detection.
 */
export class TemplateTableComponent {
  /** Signal emitting the currently loaded list of templates to display. */
  readonly templates = input.required<Signal<EmailTemplate[]>>();
  /** Signal emitting whether a data request is in progress (disables actions while loading). */
  readonly loading = input.required<Signal<boolean>>();
  /** Emitted when the user clicks the view button for a read-only (GIT/FILE) template. */
  readonly view = output<EmailTemplate>();
  /** Emitted when the user clicks the edit button for an editable (DATABASE) template. */
  readonly edit = output<EmailTemplate>();
  /** Emitted when the user clicks the delete button for an editable (DATABASE) template. */
  readonly delete = output<EmailTemplate>();

  /** Set of template IDs currently displaying copy-confirmed feedback (bi-check-lg icon). */
  readonly copiedIds = signal<Set<string>>(new Set());

  /** Returns the tags array with source tags sorted to the front. */
  sortedTags(tags: string[]): string[] {
    return [...tags].sort((a, b) => {
      const aIsSource = a.startsWith('source:');
      const bIsSource = b.startsWith('source:');
      if (aIsSource === bIsSource) return 0;
      return aIsSource ? -1 : 1;
    });
  }

  /** Returns the count of tags hidden behind the overflow badge in the table (tags beyond index 2). */
  hiddenTagCount(tags: string[]): number {
    return Math.max(0, this.sortedTags(tags).length - 2);
  }

  /**
   * Returns tags beyond the first 2 as a newline-separated string for the overflow badge tooltip.
   */
  hiddenTagsTooltip(tags: string[]): string {
    return this.sortedTags(tags).slice(2).join('\n');
  }

  /** Returns the first 8 characters of a template ID for the truncated column display. */
  shortId(id: string): string {
    return id.slice(0, 8);
  }

  /**
   * Copies the full template ID to the clipboard and briefly shows a checkmark icon as
   * confirmation, reverting after 1.5 s.
   *
   * @param id The full template UUID to copy.
   * @param event DOM event — propagation is stopped to prevent row-level click handlers.
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
}
