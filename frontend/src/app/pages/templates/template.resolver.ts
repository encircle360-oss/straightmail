import { inject } from '@angular/core';
import { ResolveFn, Router } from '@angular/router';
import { catchError, EMPTY } from 'rxjs';
import { ApiService, EmailTemplate } from '../../core/services/api.service';

/**
 * Route resolver that fetches a single {@link EmailTemplate} by the {@code id} route parameter.
 *
 * On any fetch error the user is redirected to {@code /templates} and the navigation is cancelled
 * (returns {@code EMPTY}).
 */
export const templateResolver: ResolveFn<EmailTemplate> = (route) => {
  const api = inject(ApiService);
  const router = inject(Router);
  return api.getTemplate(route.paramMap.get('id')!).pipe(
    catchError(() => {
      router.navigate(['/templates']);
      return EMPTY;
    }),
  );
};
