import {
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Store } from '@ngxs/store';
import { TranslatePipe } from '@ngx-translate/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { EmailTemplate } from '../../core/services/api.service';
import { RenderActions } from './store/render.actions';
import { RenderState } from './store/render.state';
import { jsonValidator } from '../../shared/validators/json.validator';
import { PageHeaderComponent } from '../../shared/components/layout/page-header/page-header.component';
import { RenderPreviewComponent } from './render-preview/render-preview.component';
import { LoadingButtonComponent } from '../../shared/components/buttons/loading-button/loading-button.component';
import { JsonModelFieldComponent } from '../../shared/components/fields/json-model-field/json-model-field.component';
import { SelectFieldComponent } from '../../shared/components/fields/select-field/select-field.component';

@Component({
  selector: 'app-render',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    PageHeaderComponent,
    RenderPreviewComponent,
    LoadingButtonComponent,
    JsonModelFieldComponent,
    SelectFieldComponent,
  ],
  templateUrl: './render.component.html',
  styleUrl: './render.component.scss',
})
/**
 * Page component for rendering a template preview without sending an email.
 *
 * Submits a render request (template ID + optional JSON model) to the backend via
 * {@link RenderState} and displays the resulting HTML and plain-text output. The rendered HTML
 * is sanitised before insertion into the DOM. Form values are persisted across navigation.
 * The render result is cleared on component destruction.
 */
export class RenderComponent implements OnInit, OnDestroy {
  /** Signal holding the sanitised HTML output ready for safe binding. */
  sanitizedHtml = signal<SafeHtml | null>(null);

  /** Controls which panel is visible on mobile: form or preview. Ignored on desktop. */
  mobilePanelTab = signal<'form' | 'preview'>('form');

  private readonly fb = inject(FormBuilder);
  private readonly store = inject(Store);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  form = this.fb.group({
    templateId: ['', Validators.required],
    model: ['', jsonValidator],
  });

  loading = this.store.selectSignal(RenderState.loading);
  result = this.store.selectSignal(RenderState.result);
  error = this.store.selectSignal(RenderState.error);
  templates = toSignal(this.route.data.pipe(map((d) => d['templates'] as EmailTemplate[])), {
    initialValue: [],
  });

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

  private readonly sanitizer = inject(DomSanitizer);

  constructor() {
    this.store.select(RenderState.result).subscribe((result) => {
      this.sanitizedHtml.set(result ? this.sanitizer.bypassSecurityTrustHtml(result.html) : null);
    });

    // Auto-switch to preview tab on mobile when a result or template error arrives
    effect(() => {
      if (this.result() || this.error()) {
        this.mobilePanelTab.set('preview');
      }
    });
  }

  /** Restores the persisted form values from {@link RenderState} and subscribes to auto-save. */
  ngOnInit(): void {
    // Restore persisted form values
    const saved = this.store.selectSnapshot(RenderState.formValues);
    if (saved.templateId || saved.model) {
      this.form.patchValue(saved);
    }

    // Auto-save on every change
    this.form.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((v) => {
      this.store.dispatch(
        new RenderActions.SaveForm({
          templateId: v.templateId ?? '',
          model: v.model ?? '',
        }),
      );
    });
  }

  /** Validates the form and dispatches a {@link RenderActions.Render} action if valid. */
  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.value;
    const model = v.model ? JSON.parse(v.model) : undefined;
    this.store.dispatch(
      new RenderActions.Render({
        templateId: v.templateId!,
        model,
      }),
    );
  }

  /** Dispatches {@link RenderActions.Clear} and resets the form. */
  clear(): void {
    this.store.dispatch(new RenderActions.Clear());
    this.form.reset();
  }

  ngOnDestroy(): void {
    this.store.dispatch(new RenderActions.Clear());
  }
}
