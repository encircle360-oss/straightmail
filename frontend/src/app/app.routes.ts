import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { templateResolver } from './pages/templates/template.resolver';
import { canDeactivateForm } from './core/guards/can-deactivate-form.guard';
import { templatesListResolver } from './pages/send/templates-list.resolver';
import { tenantResolver, tenantViewResolver } from './pages/tenants/tenant.resolver';

/**
 * Application route configuration.
 *
 * All authenticated routes are nested under the root {@link LayoutComponent} and protected by
 * {@link authGuard}. Tenant-admin routes additionally require the {@link adminGuard}. All page
 * components are lazy-loaded via dynamic imports. Data is pre-fetched via route resolvers where
 * needed. A wildcard route renders the {@link NotFoundComponent}.
 */
export const routes: Routes = [
  {
    path: 'unauthorized',
    loadComponent: () =>
      import('./pages/unauthorized/unauthorized.component').then((m) => m.UnauthorizedComponent),
  },
  {
    path: 'api-key-login',
    loadComponent: () =>
      import('./pages/api-key-login/api-key-login.component').then((m) => m.ApiKeyLoginComponent),
  },
  {
    path: '',
    loadComponent: () => import('./layout/layout.component').then((m) => m.LayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/dashboard.component').then((m) => m.DashboardComponent),
      },
      {
        path: 'send',
        resolve: { templates: templatesListResolver },
        loadComponent: () => import('./pages/send/send.component').then((m) => m.SendComponent),
      },
      {
        path: 'render',
        resolve: { templates: templatesListResolver },
        loadComponent: () =>
          import('./pages/render/render.component').then((m) => m.RenderComponent),
      },
      {
        path: 'templates/new',
        canDeactivate: [canDeactivateForm],
        loadComponent: () =>
          import('./pages/templates/template-form/template-form.component').then(
            (m) => m.TemplateFormComponent,
          ),
      },
      {
        path: 'templates/:id/view',
        resolve: { template: templateResolver },
        loadComponent: () =>
          import('./pages/templates/template-view/template-view.component').then(
            (m) => m.TemplateViewComponent,
          ),
      },
      {
        path: 'templates/:id/edit',
        resolve: { template: templateResolver },
        canDeactivate: [canDeactivateForm],
        loadComponent: () =>
          import('./pages/templates/template-form/template-form.component').then(
            (m) => m.TemplateFormComponent,
          ),
      },
      {
        path: 'templates',
        loadComponent: () =>
          import('./pages/templates/templates.component').then((m) => m.TemplatesComponent),
      },
      {
        path: 'tenants/new',
        canActivate: [adminGuard],
        canDeactivate: [canDeactivateForm],
        loadComponent: () =>
          import('./pages/tenants/tenant-form/tenant-form.component').then(
            (m) => m.TenantFormComponent,
          ),
      },
      {
        path: 'tenants/:slug/view',
        canActivate: [adminGuard],
        resolve: { tenant: tenantViewResolver },
        loadComponent: () =>
          import('./pages/tenants/tenant-view/tenant-view.component').then(
            (m) => m.TenantViewComponent,
          ),
      },
      {
        path: 'tenants/:slug/edit',
        canActivate: [adminGuard],
        canDeactivate: [canDeactivateForm],
        resolve: { tenant: tenantResolver },
        loadComponent: () =>
          import('./pages/tenants/tenant-form/tenant-form.component').then(
            (m) => m.TenantFormComponent,
          ),
      },
      {
        path: 'tenants',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./pages/tenants/tenants.component').then((m) => m.TenantsComponent),
      },
    ],
  },
  {
    path: '**',
    loadComponent: () =>
      import('./pages/not-found/not-found.component').then((m) => m.NotFoundComponent),
  },
];
