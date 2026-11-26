import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { Store } from '@ngxs/store';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { distinctUntilChanged, filter, finalize } from 'rxjs';
import { ApiService, EmailTemplate, GitSyncStatusDTO } from '../../core/services/api.service';
import { TemplatesActions } from './store/templates.actions';
import { TemplateSourceTab, TemplatesState } from './store/templates.state';
import { TenantState } from '../../store/tenant/tenant.state';
import { ConfirmService } from '../../core/services/confirm.service';
import { PageHeaderComponent } from '../../shared/components/layout/page-header/page-header.component';
import { TemplateTableComponent } from './template-table/template-table.component';
import { TemplateGridComponent } from './template-grid/template-grid.component';
import { GitSyncCardComponent } from '../dashboard/git-sync-card/git-sync-card.component';
import { UiState } from '../../store/ui/ui.state';
import { UiActions } from '../../store/ui/ui.actions';
import { ShowToast } from '../../store/toast/toast.actions';

@Component({
  selector: 'app-templates',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    NgbPaginationModule,
    TranslatePipe,
    PageHeaderComponent,
    TemplateTableComponent,
    TemplateGridComponent,
    GitSyncCardComponent,
  ],
  templateUrl: './templates.component.html',
  styleUrl: './templates.component.scss',
})
/**
 * Page component for listing, filtering, and managing email templates.
 *
 * Loads the paginated template list when the active tenant changes and supports tag-based
 * filtering. Create and edit actions navigate to {@code /templates/new} and
 * {@code /templates/:id/edit} respectively. GIT/FILE templates navigate to the readonly view at
 * {@code /templates/:id/view}. Delete operations are guarded by a confirmation dialog.
 */
export class TemplatesComponent implements OnInit {
  private readonly store = inject(Store);
  private readonly destroyRef = inject(DestroyRef);
  private readonly apiService = inject(ApiService);
  /** Signal emitting the currently loaded page of templates. */
  templates = this.store.selectSignal(TemplatesState.templates);
  /** Signal emitting {@code true} while a backend request is in flight. */
  loading = this.store.selectSignal(TemplatesState.loading);
  /** Signal emitting the total number of templates across all pages. */
  totalElements = this.store.selectSignal(TemplatesState.totalElements);
  /** Signal emitting the zero-based current page index. */
  currentPage = this.store.selectSignal(TemplatesState.currentPage);
  /** Signal emitting the number of items per page. */
  pageSize = this.store.selectSignal(TemplatesState.pageSize);
  /** Signal emitting the currently active tag filter, or {@code undefined}. */
  activeTag = this.store.selectSignal(TemplatesState.activeTag);
  /** Signal emitting the currently active source filter tab. */
  activeSourceTab = this.store.selectSignal(TemplatesState.activeSourceTab);
  /** Signal emitting {@code true} when the backend runs with the {@code database} profile. */
  readonly isDbMode = this.store.selectSignal(TenantState.isDatabaseMode);
  /** Signal emitting the preferred templates list view mode ({@code grid} or {@code list}). */
  readonly viewMode = this.store.selectSignal(UiState.templatesViewMode);
  private readonly router = inject(Router);
  private readonly confirm = inject(ConfirmService);

  /** Git sync status for the currently selected tenant, or {@code null} if none. */
  readonly tenantSyncStatus = signal<GitSyncStatusDTO | null>(null);
  /** Whether a git sync is currently in progress for the active tenant. */
  readonly syncingCurrentTenant = signal<boolean>(false);
  /** Slug of the currently active tenant — used to pass to onGitSync. */
  readonly currentTenantSlug = signal<string | null>(null);

  /**
   * All known custom tags for the current source scope, persisted across tag filter changes so that
   * filter buttons stay visible even when a tag filter narrows the template list.
   */
  readonly allTags = this.store.selectSignal(TemplatesState.allTags);

  /**
   * Custom tags present in the currently loaded (possibly filtered) page. Used to dim filter
   * buttons for tags that don't appear in the current result set.
   */
  readonly tagsInCurrentPage = computed(() => {
    const tags = this.templates().flatMap((t) => t.tags ?? []);
    return new Set(tags.filter((tag) => !tag.startsWith('source:')));
  });

  /** Subscribes to tenant changes and triggers an initial template load on component init. */
  ngOnInit(): void {
    // Reset a persisted DATABASE source tab when the backend runs without the database profile.
    if (!this.isDbMode() && this.activeSourceTab() === 'DATABASE') {
      this.store.dispatch(new TemplatesActions.SetActiveSourceTab('ALL'));
    }

    this.store
      .select(TenantState.selectedTenantId)
      .pipe(filter(Boolean), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((tenantSlug) => {
        this.currentTenantSlug.set(tenantSlug);
        this.store.dispatch(new TemplatesActions.LoadTemplates(0, this.pageSize()));
        this.loadSyncStatus(tenantSlug);
      });
  }

  /**
   * Fetches the status endpoint and extracts the git-sync entry for the given tenant.
   * Silently sets {@code tenantSyncStatus} to {@code null} if the tenant has no git-sync entry
   * or the request fails (e.g. backend without the database profile).
   *
   * @param tenantSlug The slug of the tenant whose sync status to load.
   */
  private loadSyncStatus(tenantSlug: string): void {
    this.apiService.getStatus().subscribe({
      next: (status) => {
        this.tenantSyncStatus.set(status.gitSync.find((s) => s.tenantId === tenantSlug) ?? null);
      },
      error: () => this.tenantSyncStatus.set(null),
    });
  }

  /**
   * Triggers a manual git sync for the given tenant and updates the local status signal.
   * Releases the button and shows a toast whether the sync succeeds or fails.
   *
   * @param tenantSlug The slug of the tenant to sync.
   */
  onGitSync(tenantSlug: string): void {
    this.syncingCurrentTenant.set(true);
    // finalize, not complete: RxJS never calls complete after an error, which used to leave the
    // spinner running and the button disabled with nothing to explain it.
    this.apiService
      .triggerGitSync(tenantSlug)
      .pipe(finalize(() => this.syncingCurrentTenant.set(false)))
      .subscribe({
        next: (updated) => this.tenantSyncStatus.set(updated),
        error: () => this.store.dispatch(new ShowToast('dashboard.sync_error', 'error')),
      });
  }

  /**
   * Switches the active source filter and reloads the template list from page zero.
   * Any active custom tag filter is preserved across source switches.
   *
   * @param tab The source to activate.
   */
  onSourceChange(tab: TemplateSourceTab): void {
    // Clicking the already-active non-ALL chip toggles back to ALL.
    const next = this.activeSourceTab() === tab && tab !== 'ALL' ? 'ALL' : tab;
    this.store.dispatch(new TemplatesActions.SetActiveSourceTab(next));
  }

  /**
   * Loads the requested page of templates using the combined source + custom tag filters.
   *
   * @param page One-based page number emitted by the ngb-pagination component.
   */
  onPageChange(page: number): void {
    const snapshot = this.store.selectSnapshot((s) => s.templates);
    this.store.dispatch(
      new TemplatesActions.LoadTemplates(
        page - 1,
        this.pageSize(),
        TemplatesState.resolveTags(snapshot),
      ),
    );
  }

  /**
   * Applies a tag filter and reloads templates from page zero.
   *
   * @param tag The tag to filter by, or {@code undefined} to clear the filter.
   */
  filterByTag(tag: string | undefined): void {
    this.store.dispatch(
      new TemplatesActions.SetActiveTag(this.activeTag() === tag ? undefined : tag),
    );
  }

  /**
   * Persists the selected view mode in {@link UiState} so the preference survives navigation
   * and browser sessions.
   *
   * @param mode The view mode to activate.
   */
  setViewMode(mode: 'grid' | 'list'): void {
    this.store.dispatch(new UiActions.SetTemplatesViewMode(mode));
  }

  /** Navigates to the template creation form at {@code /templates/new}. */
  openCreate(): void {
    this.router.navigate(['/templates/new']);
  }

  /**
   * Navigates to the readonly template view for GIT- and FILE-sourced templates.
   *
   * @param template The read-only template to view.
   */
  openView(template: EmailTemplate): void {
    this.router.navigate(['/templates', template.id, 'view']);
  }

  /**
   * Navigates to the template edit form.
   *
   * @param template The template to edit.
   */
  openEdit(template: EmailTemplate): void {
    this.router.navigate(['/templates', template.id, 'edit']);
  }

  /**
   * Shows a confirmation dialog and, if confirmed, dispatches a delete action.
   *
   * @param template The template to delete.
   */
  async confirmDelete(template: EmailTemplate): Promise<void> {
    const ok = await this.confirm.confirm('templates.delete_confirm', {
      messageParams: { id: template.id! },
    });
    if (ok) this.store.dispatch(new TemplatesActions.DeleteTemplate(template.id!));
  }
}
