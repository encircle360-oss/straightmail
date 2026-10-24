import { Component, computed, inject, OnInit, signal } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Store } from '@ngxs/store';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { PageHeaderComponent } from '../../../shared/components/layout/page-header/page-header.component';
import { LoadingButtonComponent } from '../../../shared/components/buttons/loading-button/loading-button.component';
import { ChipFieldComponent } from '../../../shared/components/fields/chip-field/chip-field.component';
import { TenantDTO } from '../../../store/tenant/tenant.state';
import { UiState } from '../../../store/ui/ui.state';
import { UiActions } from '../../../store/ui/ui.actions';
import { environment } from '../../../../environments/environment';
import { ApiService, GitSyncStatusDTO } from '../../../core/services/api.service';
import { GitSyncCardComponent } from '../../dashboard/git-sync-card/git-sync-card.component';
import { CanDeactivateComponent } from '../../../core/guards/can-deactivate-form.guard';
import { ConfirmService } from '../../../core/services/confirm.service';
import { ShowToast } from '../../../store/toast/toast.actions';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-tenant-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    NgbNavModule,
    PageHeaderComponent,
    LoadingButtonComponent,
    ChipFieldComponent,
    GitSyncCardComponent,
  ],
  templateUrl: './tenant-form.component.html',
})
/**
 * Page component for creating and editing tenants (admin-only).
 *
 * Operates in create mode when no {@code tenant} is resolved in the route data, and edit mode
 * when a tenant is available. On successful save the user is navigated back to {@code /tenants}.
 * SMTP password, Git token, and API key fields are always blank in edit mode for security reasons —
 * submitting them empty means the backend retains the existing encrypted value.
 *
 * The SMTP, Git Sync, and Security configuration is organized into tabs. The active tab is
 * persisted in {@link UiState} so the last-used section is restored on re-entry.
 */
export class TenantFormComponent implements OnInit, CanDeactivateComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly http = inject(HttpClient);
  private readonly translate = inject(TranslateService);
  private readonly fb = inject(FormBuilder);
  private readonly store = inject(Store);
  private readonly apiService = inject(ApiService);
  private readonly confirm = inject(ConfirmService);
  private readonly apiUrl = `${environment.apiUrl}/v1/tenants`;

  /** Git sync status for this tenant, populated in edit mode when a git URL is configured. */
  readonly tenantSyncStatus = signal<GitSyncStatusDTO | null>(null);
  /** Whether a manual git sync is currently in progress. */
  readonly syncingTenant = signal<boolean>(false);

  /** The tenant being edited, or {@code undefined} when creating a new tenant. */
  readonly tenant = this.route.snapshot.data['tenant'] as TenantDTO | undefined;

  /** Returns {@code true} when the form is operating in edit mode. */
  get isEdit(): boolean {
    return !!this.tenant;
  }

  readonly form = this.fb.group({
    slug: ['', [Validators.required, Validators.pattern('^[a-z0-9][a-z0-9\\-]{0,61}[a-z0-9]$')]],
    displayName: ['', Validators.required],
    logoUrl: [''],
    brandColor: [null as string | null],
    active: [true],
    smtpHost: [''],
    smtpPort: [null as number | null],
    smtpUser: [''],
    smtpPassword: [''],
    smtpSender: [''],
    smtpTls: [false],
    smtpSsl: [false],
    gitRepoUrl: [''],
    gitToken: [''],
    gitBranches: this.fb.array<FormControl<string | null>>([]),
    gitBranchInput: [''],
    apiKey: [''],
  });

  /** Whether a save request is currently in flight. */
  readonly saving = signal(false);
  /** Error message to display when the save request fails. */
  readonly formError = signal('');
  /** Set to {@code true} after a successful save to suppress the unsaved-changes guard. */
  private saved = false;

  /** The currently active configuration tab, persisted in {@link UiState}. */
  readonly activeTab = this.store.selectSignal(UiState.tenantFormTab);

  /**
   * Tracks form validity changes so that tab error indicators stay in sync.
   * Used as a dependency trigger by the {@link smtpHasError}, {@link gitHasError},
   * and {@link securityHasError} computed signals.
   */
  private readonly formStatus = toSignal(this.form.statusChanges, { initialValue: 'VALID' });

  /** {@code true} if any SMTP field is invalid and has been touched by the user. */
  readonly smtpHasError = computed(() => {
    this.formStatus();
    return [
      'smtpHost',
      'smtpPort',
      'smtpUser',
      'smtpPassword',
      'smtpSender',
      'smtpTls',
      'smtpSsl',
    ].some((f) => {
      const c = this.form.get(f);
      return c?.invalid && c?.touched;
    });
  });

  /** {@code true} if any Git Sync field is invalid and has been touched by the user. */
  readonly gitHasError = computed(() => {
    this.formStatus();
    return ['gitRepoUrl', 'gitToken'].some((f) => {
      const c = this.form.get(f);
      return c?.invalid && c?.touched;
    });
  });

  /** {@code true} if the API key field is invalid and has been touched by the user. */
  readonly securityHasError = computed(() => {
    this.formStatus();
    const c = this.form.get('apiKey');
    return c?.invalid && c?.touched;
  });

  /** Returns the {@link FormArray} controlling the git branch chips. */
  get gitBranches(): FormArray {
    return this.form.get('gitBranches') as FormArray;
  }

  /** Populates the form with existing tenant data when in edit mode. */
  ngOnInit(): void {
    if (this.isEdit) {
      // Disable slug in edit mode: it is immutable after creation and must not participate
      // in form validation, since it is never filled and would otherwise block submit.
      this.form.get('slug')?.disable();
    }
    if (!this.tenant) return;
    this.form.patchValue({
      displayName: this.tenant.displayName,
      logoUrl: this.tenant.logoUrl ?? '',
      brandColor: this.tenant.brandColor ?? null,
      active: this.tenant.active,
      smtpHost: this.tenant.smtpHost ?? '',
      smtpPort: this.tenant.smtpPort ?? null,
      smtpUser: this.tenant.smtpUser ?? '',
      smtpSender: this.tenant.smtpSender ?? '',
      smtpTls: this.tenant.smtpTls,
      smtpSsl: this.tenant.smtpSsl,
      gitRepoUrl: this.tenant.gitRepoUrl ?? '',
    });
    (this.tenant.gitBranches ?? []).forEach((branch) =>
      this.gitBranches.push(new FormControl(branch)),
    );

    if (this.tenant.gitRepoUrl) {
      this.apiService.getStatus().subscribe({
        next: (status) => {
          this.tenantSyncStatus.set(
            status.gitSync.find((s) => s.tenantId === this.tenant!.slug) ?? null,
          );
        },
        error: () => {},
      });
    }
  }

  /**
   * Persists the selected configuration tab in {@link UiState}.
   *
   * @param tab The tab identifier to activate.
   */
  setActiveTab(tab: 'smtp' | 'git' | 'security'): void {
    this.store.dispatch(new UiActions.SetTenantFormTab(tab));
  }

  /** Submits the form as a PUT (edit) or POST (create) request and navigates on success. */
  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }

    this.saving.set(true);
    this.formError.set('');

    const raw = this.form.getRawValue();
    const branches = this.gitBranches.controls.map((c) => c.value as string).filter((v) => !!v);

    const slug = this.isEdit ? this.tenant!.slug : raw.slug;

    const payload: Record<string, unknown> = {
      slug,
      displayName: raw.displayName,
      logoUrl: raw.logoUrl || null,
      brandColor: raw.brandColor || null,
      active: raw.active,
      smtpHost: raw.smtpHost,
      smtpPort: raw.smtpPort,
      smtpUser: raw.smtpUser,
      smtpSender: raw.smtpSender,
      smtpTls: raw.smtpTls,
      smtpSsl: raw.smtpSsl,
      gitRepoUrl: raw.gitRepoUrl,
      gitBranches: branches,
    };

    // Only include write-only secrets when non-empty to keep existing encrypted values.
    if (raw.smtpPassword) payload['smtpPassword'] = raw.smtpPassword;
    if (raw.gitToken) payload['gitToken'] = raw.gitToken;
    if (raw.apiKey) payload['apiKey'] = raw.apiKey;

    const request = this.isEdit
      ? this.http.put(`${this.apiUrl}/${slug}`, payload)
      : this.http.post(this.apiUrl, payload);

    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.saved = true;
        this.router.navigate(['/tenants']);
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(err.error?.message ?? this.translate.instant('tenants.save_error'));
      },
    });
  }

  /**
   * Triggers a manual git sync for the current tenant and updates the status signal.
   * Releases the button and shows a toast whether the sync succeeds or fails.
   */
  onGitSync(): void {
    this.syncingTenant.set(true);
    // finalize, not complete: RxJS never calls complete after an error, which used to leave the
    // spinner running and the button disabled with nothing to explain it.
    this.apiService
      .triggerGitSync(this.tenant!.slug)
      .pipe(finalize(() => this.syncingTenant.set(false)))
      .subscribe({
        next: (updated) => this.tenantSyncStatus.set(updated),
        error: () => this.store.dispatch(new ShowToast('dashboard.sync_error', 'error')),
      });
  }

  /**
   * Guards against accidental navigation away from an unsaved form.
   *
   * @returns {@code true} immediately when the form is pristine or already saved; otherwise a
   *   promise that resolves to {@code true} only if the user confirms discarding changes.
   */
  canDeactivate(): boolean | Promise<boolean> {
    if (!this.form.dirty || this.saved) return true;
    return this.confirm.confirm('common.unsaved_changes', {
      confirmLabel: 'common.discard',
      variant: 'danger',
    });
  }

  /** Navigates back to the tenants list without saving. */
  cancel(): void {
    this.router.navigate(['/tenants']);
  }
}
