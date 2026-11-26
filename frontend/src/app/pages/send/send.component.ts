import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  OnInit,
} from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { NgbNavModule } from '@ng-bootstrap/ng-bootstrap';
import { AccordionItemComponent } from '../../shared/components/accordion/accordion-item/accordion-item.component';
import { Store } from '@ngxs/store';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { catchError, distinctUntilChanged, filter, map, merge, of, skip, switchMap } from 'rxjs';
import { ApiService, EmailTemplate } from '../../core/services/api.service';
import { TenantState } from '../../store/tenant/tenant.state';
import { SendActions } from './store/send.actions';
import { SendState } from './store/send.state';
import { jsonValidator } from '../../shared/validators/json.validator';
import { ChipFieldComponent } from '../../shared/components/fields/chip-field/chip-field.component';
import { PageHeaderComponent } from '../../shared/components/layout/page-header/page-header.component';
import { LoadingButtonComponent } from '../../shared/components/buttons/loading-button/loading-button.component';
import { JsonModelFieldComponent } from '../../shared/components/fields/json-model-field/json-model-field.component';
import { HtmlBodyFieldComponent } from '../../shared/components/fields/html-body-field/html-body-field.component';
import { TextFieldComponent } from '../../shared/components/fields/text-field/text-field.component';
import { SelectFieldComponent } from '../../shared/components/fields/select-field/select-field.component';

@Component({
  selector: 'app-send',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    NgbNavModule,
    TranslatePipe,
    AccordionItemComponent,
    ChipFieldComponent,
    PageHeaderComponent,
    LoadingButtonComponent,
    JsonModelFieldComponent,
    HtmlBodyFieldComponent,
    TextFieldComponent,
    SelectFieldComponent,
  ],
  templateUrl: './send.component.html',
  styleUrl: './send.component.scss',
})
/**
 * Page component for composing and sending emails.
 *
 * Provides two send modes via tabbed navigation:
 * - **By template ID**: selects a stored template and supplies recipient/model data.
 * - **Inline**: enters a FreeMarker template string directly.
 *
 * Form values are persisted to {@link SendState} so they survive navigation.
 * The available template list is loaded via a route resolver and refreshed when the active
 * tenant changes.
 */
export class SendComponent implements OnInit {
  /** Zero-based index of the active tab (1 = by template ID, 2 = inline). */
  activeTab = 1;
  /** Supported locale codes shown in the locale selector. */
  localeOptions = ['en', 'de'];

  private readonly fb = inject(FormBuilder);
  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly api = inject(ApiService);

  loading = this.store.selectSignal(SendState.loading);
  templates = toSignal(
    merge(
      this.route.data.pipe(map((d) => d['templates'] as EmailTemplate[])),
      this.store.select(TenantState.selectedTenantId).pipe(
        distinctUntilChanged(),
        skip(1),
        filter(Boolean),
        switchMap(() =>
          this.api.getTemplates(0, 1000).pipe(
            map((p) => p.content),
            catchError(() => of([])),
          ),
        ),
      ),
    ),
    { initialValue: [] },
  );

  readonly groupedTemplates = computed(() => {
    const all = this.templates();
    const nonGit = all.filter((t) => t.source !== 'GIT');
    const gitTemplates = all.filter((t) => t.source === 'GIT');
    const branches = [
      ...new Set(gitTemplates.map((t) => t.gitBranch).filter((b): b is string => !!b)),
    ];
    const gitGroups = branches.map((branch) => ({
      branch,
      templates: gitTemplates.filter((t) => t.gitBranch === branch),
    }));
    return { nonGit, gitGroups };
  });

  // ── By Template ID form ──────────────────────────────────────────────────────
  byIdForm = this.fb.group({
    // Validators.required rejects an empty array, so the form can no longer be submitted
    // without a single recipient — the template already showed the error, nothing enforced it.
    recipients: this.fb.array<FormControl<string | null>>([], Validators.required),
    recipientInput: [''],
    sender: ['', [Validators.required, Validators.email]],
    emailTemplateId: ['', Validators.required],
    subject: [''],
    locale: ['en'],
    cc: this.fb.array<FormControl<string | null>>([]),
    ccInput: [''],
    bcc: this.fb.array<FormControl<string | null>>([]),
    bccInput: [''],
    model: ['', jsonValidator],
  });

  /** Mirrors the emailTemplateId control value as a signal for derived computeds. */
  private readonly emailTemplateId$ = toSignal(this.byIdForm.get('emailTemplateId')!.valueChanges, {
    initialValue: this.byIdForm.get('emailTemplateId')!.value ?? '',
  });

  /** The full template object currently selected in the by-ID dropdown, or undefined. */
  readonly selectedTemplate = computed(() => {
    const id = this.emailTemplateId$();
    if (!id) return undefined;
    return this.templates().find((t) => t.id === id);
  });

  /**
   * True when the selected template's subject is resolved from a companion `_subject.ftl` file
   * (FILE or GIT source). In that case the subject field must be locked to prevent an empty
   * override that would suppress the file-derived subject.
   */
  readonly isSubjectFromFile = computed(() => {
    const src = this.selectedTemplate()?.source;
    return src === 'FILE' || src === 'GIT';
  });

  constructor() {
    effect(() => {
      const subjectCtrl = this.byIdForm.get('subject')!;
      const template = this.selectedTemplate();

      if (this.isSubjectFromFile()) {
        // Disable first so the group's valueChanges won't fire for the patchValue below
        // (disabled controls are excluded from FormGroup.valueChanges).
        subjectCtrl.disable({ emitEvent: false });
        subjectCtrl.patchValue(template?.subject ?? '');
      } else {
        subjectCtrl.enable({ emitEvent: false });
        // Always pre-fill from the template subject so the user sees the default value.
        // For DATABASE templates the field stays editable so the user can override it.
        subjectCtrl.patchValue(template?.subject ?? '');
      }
    });
  }

  // ── Inline form ──────────────────────────────────────────────────────────────
  inlineForm = this.fb.group({
    // Validators.required rejects an empty array, so the form can no longer be submitted
    // without a single recipient — the template already showed the error, nothing enforced it.
    recipients: this.fb.array<FormControl<string | null>>([], Validators.required),
    recipientInput: [''],
    sender: ['', [Validators.required, Validators.email]],
    subject: ['', Validators.required],
    emailTemplate: ['', Validators.required],
    locale: ['en'],
    cc: this.fb.array<FormControl<string | null>>([]),
    ccInput: [''],
    bcc: this.fb.array<FormControl<string | null>>([]),
    bccInput: [''],
    model: ['', jsonValidator],
  });

  /** Restores the persisted active tab and form values from {@link SendState} on component init. */
  ngOnInit(): void {
    // Restore persisted active tab
    this.activeTab = this.store.selectSnapshot(SendState.activeTab);

    // Restore persisted form values
    const savedById = this.store.selectSnapshot(SendState.formById);
    const savedInline = this.store.selectSnapshot(SendState.formInline);

    this.restoreForm(this.byIdForm, savedById);
    this.restoreForm(this.inlineForm, savedInline);

    // Subscribe to changes
    this.byIdForm.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((v) => {
      this.store.dispatch(new SendActions.SaveForm('byId', v as Record<string, unknown>));
    });

    this.inlineForm.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((v) => {
      this.store.dispatch(new SendActions.SaveForm('inline', v as Record<string, unknown>));
    });
  }

  /** Delegates to {@link submitById} or {@link submitInline} based on the active tab. */
  submit(): void {
    this.activeTab === 1 ? this.submitById() : this.submitInline();
  }

  /**
   * Persists the newly selected tab index to {@link SendState}.
   *
   * @param tabId The zero-based index of the newly activated tab.
   */
  onTabChange(tabId: number): void {
    this.store.dispatch(new SendActions.UpdateActiveTab(tabId));
  }

  private restoreForm(form: FormGroup, values: Record<string, any>): void {
    if (!values || !Object.keys(values).length) return;

    // Restore FormArrays first. The restored chips carry the same email validator the chip field
    // attaches, so a persisted draft is judged exactly like a freshly typed one.
    ['recipients', 'cc', 'bcc'].forEach((key) => {
      const arr = form.get(key) as FormArray;
      const val = values[key] as string[];
      if (arr && val && Array.isArray(val)) {
        arr.clear();
        val.forEach((v) => {
          if (v) arr.push(new FormControl(v, Validators.email));
        });
      }
    });

    form.patchValue(values);
  }

  /** Validates and dispatches a {@link SendActions.SendByTemplate} action if the form is valid. */
  submitById(): void {
    if (this.byIdForm.invalid) {
      this.byIdForm.markAllAsTouched();
      return;
    }
    // getRawValue() includes disabled controls so the pre-filled subject of FILE/GIT
    // templates is always included in the request payload.
    const v = this.byIdForm.getRawValue();
    const model = v.model ? JSON.parse(v.model) : undefined;
    const cc = (this.byIdForm.get('cc') as FormArray).getRawValue().filter(Boolean);
    const bcc = (this.byIdForm.get('bcc') as FormArray).getRawValue().filter(Boolean);
    this.store.dispatch(
      new SendActions.SendByTemplate({
        recipients: (this.byIdForm.get('recipients') as FormArray).getRawValue().filter(Boolean),
        sender: v.sender!,
        emailTemplateId: v.emailTemplateId!,
        subject: v.subject || undefined,
        locale: v.locale || undefined,
        cc: cc.length ? cc : undefined,
        bcc: bcc.length ? bcc : undefined,
        model,
      }),
    );
  }

  /** Validates and dispatches a {@link SendActions.SendInline} action if the form is valid. */
  submitInline(): void {
    if (this.inlineForm.invalid) {
      this.inlineForm.markAllAsTouched();
      return;
    }
    const v = this.inlineForm.value;
    const model = v.model ? JSON.parse(v.model) : undefined;
    const cc = (this.inlineForm.get('cc') as FormArray).value.filter(Boolean);
    const bcc = (this.inlineForm.get('bcc') as FormArray).value.filter(Boolean);
    this.store.dispatch(
      new SendActions.SendInline({
        recipients: (this.inlineForm.get('recipients') as FormArray).value.filter(Boolean),
        sender: v.sender!,
        subject: v.subject!,
        emailTemplate: v.emailTemplate!,
        locale: v.locale || undefined,
        cc: cc.length ? cc : undefined,
        bcc: bcc.length ? bcc : undefined,
        model,
      }),
    );
  }
}
