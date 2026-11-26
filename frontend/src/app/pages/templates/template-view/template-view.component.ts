import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { FormArray, FormBuilder, FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { EmailTemplate } from '../../../core/services/api.service';
import { AccordionItemComponent } from '../../../shared/components/accordion/accordion-item/accordion-item.component';
import { HtmlBodyFieldComponent } from '../../../shared/components/fields/html-body-field/html-body-field.component';
import { TextareaFieldComponent } from '../../../shared/components/fields/textarea-field/textarea-field.component';
import { TextFieldComponent } from '../../../shared/components/fields/text-field/text-field.component';
import { SelectFieldComponent } from '../../../shared/components/fields/select-field/select-field.component';
import { ChipFieldComponent } from '../../../shared/components/fields/chip-field/chip-field.component';
import { PageHeaderComponent } from '../../../shared/components/layout/page-header/page-header.component';

@Component({
  selector: 'app-template-view',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    TranslatePipe,
    AccordionItemComponent,
    HtmlBodyFieldComponent,
    TextareaFieldComponent,
    TextFieldComponent,
    SelectFieldComponent,
    ChipFieldComponent,
    PageHeaderComponent,
  ],
  templateUrl: './template-view.component.html',
})
/**
 * Readonly page component for inspecting GIT- and FILE-sourced email templates.
 *
 * Reuses the same field layout as {@link TemplateFormComponent} but with all controls disabled.
 * No save or delete actions are available — the component provides a single "Close" button that
 * navigates back to the templates list.
 */
export class TemplateViewComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  /** The resolved template to display. */
  readonly template = this.route.snapshot.data['template'] as EmailTemplate;

  /** BCP 47 locale codes used by the locale selector. */
  readonly localeOptions = ['en', 'de'];

  form = this.fb.group({
    name: [''],
    locale: [''],
    tags: this.fb.array<FormControl<string | null>>([]),
    tagInput: [''],
    subject: [''],
    html: [''],
    plain: [''],
  });

  /** Returns a Bootstrap badge CSS class based on the template source. */
  get sourceBadgeClass(): string {
    switch (this.template.source) {
      case 'DATABASE':
        return 'bg-success';
      case 'GIT':
        return 'bg-info text-dark';
      default:
        return 'bg-secondary';
    }
  }

  /** Returns the Bootstrap Icon class representing the template's source origin. */
  get sourceIcon(): string {
    switch (this.template.source) {
      case 'DATABASE':
        return 'bi-database-fill';
      case 'GIT':
        return 'bi-git';
      default:
        return 'bi-file-earmark-text';
    }
  }

  /** Returns a translation key for the template source label. */
  get sourceKey(): string {
    switch (this.template.source) {
      case 'DATABASE':
        return 'templates.source_database';
      case 'GIT':
        return 'templates.source_git';
      default:
        return 'templates.source_file';
    }
  }

  accordionKey = (key: string) => `template-view:${this.template.id}:${key}`;

  /**
   * Populates the disabled form with the resolved template data.
   *
   * All controls are disabled immediately after patching so the field components render in their
   * visual disabled state without exposing editable inputs to the user.
   */
  ngOnInit(): void {
    if (this.template) {
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
    this.form.disable();
  }

  /** Navigates back to the templates list. */
  close(): void {
    this.router.navigate(['/templates']);
  }
}
