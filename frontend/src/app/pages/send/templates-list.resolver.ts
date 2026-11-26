import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { map } from 'rxjs';
import { ApiService, EmailTemplate } from '../../core/services/api.service';

/**
 * Route resolver that pre-fetches the full list of email templates (up to 1 000 items) for the
 * send and render pages. The resolved data is available as {@code route.data['templates']}.
 */
export const templatesListResolver: ResolveFn<EmailTemplate[]> = () =>
  inject(ApiService)
    .getTemplates(0, 1000)
    .pipe(map((p) => p.content));
