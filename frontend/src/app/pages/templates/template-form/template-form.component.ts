import { Component, inject, OnInit } from '@angular/core';
import {
  FormArray,
  FormBuilder,
  FormControl,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Actions, ofActionDispatched, Store } from '@ngxs/store';
import { TranslatePipe } from '@ngx-translate/core';
import { AccordionItemComponent } from '../../../shared/components/accordion/accordion-item/accordion-item.component';
import { take } from 'rxjs';
import { EmailTemplate } from '../../../core/services/api.service';
import { TemplatesActions } from '../store/templates.actions';
import { TemplatesState } from '../store/templates.state';
import { ChipFieldComponent } from '../../../shared/components/fields/chip-field/chip-field.component';
import { PageHeaderComponent } from '../../../shared/components/layout/page-header/page-header.component';
import { LoadingButtonComponent } from '../../../shared/components/buttons/loading-button/loading-button.component';
import { HtmlBodyFieldComponent } from '../../../shared/components/fields/html-body-field/html-body-field.component';
import { TextFieldComponent } from '../../../shared/components/fields/text-field/text-field.component';
import { SelectFieldComponent } from '../../../shared/components/fields/select-field/select-field.component';
import { TextareaFieldComponent } from '../../../shared/components/fields/textarea-field/textarea-field.component';
import { ConfirmService } from '../../../core/services/confirm.service';
import { CanDeactivateComponent } from '../../../core/guards/can-deactivate-form.guard';

@Component({
  selector: 'app-template-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    AccordionItemComponent,
    ChipFieldComponent,
    PageHeaderComponent,
    LoadingButtonComponent,
    HtmlBodyFieldComponent,
    TextFieldComponent,
    SelectFieldComponent,
    TextareaFieldComponent,
  ],
  templateUrl: './template-form.component.html',
})
/**
 * Page component for creating and editing email templates.
 *
 * Operates in two modes determined by the presence of a resolved {@code template} in the route
 * data: **create** mode (new template) and **edit** mode (existing template). On successful save
 * the user is navigated back to {@code /templates}. Implements {@link CanDeactivateComponent} to
 * warn about unsaved changes before leaving the page.
 */
export class TemplateFormComponent implements OnInit, CanDeactivateComponent {
  /** BCP 47 locale codes offered in the locale selector. */
  localeOptions = ['', 'en', 'de'];
  /** Set to {@code true} after a successful save to suppress the unsaved-changes guard. */
  saved = false;
  private readonly route = inject(ActivatedRoute);
  /** The template being edited, or {@code undefined} when creating a new template. */
  readonly template = this.route.snapshot.data['template'] as EmailTemplate | undefined;
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  form = this.fb.group({
    name: ['', Validators.required],
    locale: [''],
    tags: this.fb.array<FormControl<string | null>>([]),
    tagInput: [''],
    subject: ['', Validators.required],
    html: ['', Validators.required],
    plain: [''],
  });
  private readonly store = inject(Store);
  loading = this.store.selectSignal(TemplatesState.loading);
  private readonly actions$ = inject(Actions);
  private readonly confirm = inject(ConfirmService);

  /** Returns {@code true} when the form is operating in edit mode. */
  get isEdit(): boolean {
    return !!this.template;
  }

  /** Patches the form with the existing template data when in edit mode. */
  ngOnInit(): void {
    if (this.isEdit && this.template) {
      this.form.patchValue({
        name: this.template.name,
        locale: this.template.locale,
        subject: this.template.subject ?? '',
        html: this.template.html ?? '',
        plain: this.template.plain ?? '',
      });
      const tagsArray = this.form.get('tags') as FormArray;
      (this.template.tags ?? []).forEach((tag) => tagsArray.push(new FormControl(tag)));
    }
  }

  /** Validates the form and dispatches a create or update action depending on the mode. */
  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const payload: EmailTemplate = {
      name: v.name!,
      locale: v.locale!,
      tags: (this.form.get('tags') as FormArray).value.filter(Boolean),
      subject: v.subject || undefined,
      html: v.html || undefined,
      plain: v.plain || undefined,
    };

    if (this.isEdit) {
      this.store.dispatch(new TemplatesActions.UpdateTemplate(this.template!.id!, payload));
    } else {
      this.store.dispatch(new TemplatesActions.CreateTemplate(payload));
    }

    this.actions$
      .pipe(
        ofActionDispatched(
          TemplatesActions.CreateTemplateSuccess,
          TemplatesActions.UpdateTemplateSuccess,
        ),
        take(1),
      )
      .subscribe(() => {
        this.saved = true;
        this.router.navigate(['/templates']);
      });
  }

  /** Navigates back to the templates list without saving. */
  cancel(): void {
    this.router.navigate(['/templates']);
  }

  accordionKey = (key: string) => (this.template?.id ? `template:${this.template.id}:${key}` : key);

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
}
