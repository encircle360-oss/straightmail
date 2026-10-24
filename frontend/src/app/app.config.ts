import {
  ApplicationConfig,
  inject,
  isDevMode,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter, Router } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';
import { provideStore, Store } from '@ngxs/store';
import { withNgxsLoggerPlugin } from '@ngxs/logger-plugin';
import { withNgxsReduxDevtoolsPlugin } from '@ngxs/devtools-plugin';
import {
  LOCAL_STORAGE_ENGINE,
  SESSION_STORAGE_ENGINE,
  withNgxsStoragePlugin,
} from '@ngxs/storage-plugin';
import { provideAuth } from 'angular-auth-oidc-client';

import { routes } from './app.routes';
import { ToastState } from './store/toast/toast.state';
import { SendState } from './pages/send/store/send.state';
import { RenderState } from './pages/render/store/render.state';
import { TemplatesState } from './pages/templates/store/templates.state';
import { AuthState } from './store/auth/auth.state';
import { CheckAuth } from './store/auth/auth.actions';
import { TenantState } from './store/tenant/tenant.state';
import { UiState } from './store/ui/ui.state';
import { ApiKeyState } from './store/api-key/api-key.state';
import { TenantActions } from './store/tenant/tenant.actions';
import { DashboardState } from './pages/dashboard/store/dashboard.state';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { apiKeyInterceptor } from './core/interceptors/api-key.interceptor';
import { environment } from '../environments/environment';
import { progressInterceptor } from 'ngx-progressbar/http';

/**
 * Application initializer that verifies the auth session on startup and then pre-loads the tenant list.
 *
 * - {@code oidc}: dispatches {@link CheckAuth} first, then — if authenticated — {@link TenantActions.LoadTenants}.
 * - {@code api-key}: loads tenants immediately if an API key is already stored in {@code SESSION_STORAGE}.
 * - {@code none}: attempts to load tenants directly; failure is swallowed so the app starts regardless.
 *
 * On OIDC error, navigates to {@code /unauthorized}.
 */
export function initializeApp() {
  const store = inject(Store);

  if (environment.authMode === 'none') {
    // No-auth mode: load tenants directly (database profile active), ignore failure if not available
    return store.dispatch(new TenantActions.LoadTenants()).pipe(catchError(() => of(null)));
  }

  if (environment.authMode === 'api-key') {
    // API-key mode: load tenants if API key is stored
    const apiKey = store.selectSnapshot(ApiKeyState.apiKey);
    return apiKey
      ? store.dispatch(new TenantActions.LoadTenants()).pipe(catchError(() => of(null)))
      : Promise.resolve();
  }

  // OIDC mode: authenticate first, then load tenants sequentially
  const router = inject(Router);
  return store.dispatch(new CheckAuth()).pipe(
    switchMap(() => {
      const isAuthenticated = store.selectSnapshot(AuthState.isAuthenticated);
      return isAuthenticated
        ? store.dispatch(new TenantActions.LoadTenants()).pipe(catchError(() => of(null)))
        : of(null);
    }),
    catchError(() => {
      router.navigateByUrl('/unauthorized');
      return of(null);
    }),
  );
}

const ngxsProviders = [
  provideStore(
    [
      ToastState,
      SendState,
      RenderState,
      TemplatesState,
      AuthState,
      UiState,
      TenantState,
      ApiKeyState,
      DashboardState,
    ],
    {
      developmentMode: isDevMode(),
      // NGXS compiles selector property getters with new Function(), which a Content-Security-Policy
      // without 'unsafe-eval' blocks — and the app then fails to bootstrap. This opts into the safe
      // accessor so script-src can stay strict.
      compatibility: { strictContentSecurityPolicy: true },
    },
    withNgxsLoggerPlugin({ disabled: !isDevMode() }),
    withNgxsReduxDevtoolsPlugin({ disabled: !isDevMode() }),
    withNgxsStoragePlugin({
      keys: [
        { key: 'auth', engine: LOCAL_STORAGE_ENGINE },
        { key: 'apiKey', engine: SESSION_STORAGE_ENGINE },
        { key: 'tenant', engine: SESSION_STORAGE_ENGINE },
        { key: 'render.formValues', engine: LOCAL_STORAGE_ENGINE },
        { key: 'render.result', engine: LOCAL_STORAGE_ENGINE },
        { key: 'send.formById', engine: LOCAL_STORAGE_ENGINE },
        { key: 'send.formInline', engine: LOCAL_STORAGE_ENGINE },
        { key: 'send.activeTab', engine: LOCAL_STORAGE_ENGINE },
        { key: 'ui', engine: LOCAL_STORAGE_ENGINE },
      ],
    }),
  ),
];

const translateProviders = [
  provideTranslateService({ lang: 'en', fallbackLang: 'en' }),
  provideTranslateHttpLoader({ prefix: './assets/i18n/', suffix: '.json' }),
];

const interceptors = {
  oidc: [authInterceptor, progressInterceptor],
  'api-key': [apiKeyInterceptor, progressInterceptor],
  none: [progressInterceptor],
} as const;

/**
 * Root Angular application configuration factory.
 *
 * Must be called as a function (not evaluated as a constant) so that
 * {@code environment.authMode} and {@code environment.oidc.authority} are read
 * **after** {@code window.__runtimeConfig} has been populated by the pre-bootstrap
 * fetch in {@code main.ts}. Static module evaluation happens before any async
 * code runs, so a constant would always capture the fallback values.
 *
 * Registers all providers including the router, HTTP client (with the interceptor matching
 * {@code environment.authMode}), NGXS store with storage persistence, ngx-translate, OIDC client,
 * and the application initializer ({@link initializeApp}).
 */
export function createAppConfig(): ApplicationConfig {
  const authProviders = [
    provideAuth({
      config:
        environment.authMode === 'oidc'
          ? {
              authority: environment.oidc.authority,
              redirectUrl: environment.oidc.redirectUrl,
              postLogoutRedirectUri: environment.oidc.postLogoutRedirectUri,
              clientId: environment.oidc.clientId,
              scope: environment.oidc.scope,
              responseType: environment.oidc.responseType,
              silentRenew: environment.oidc.silentRenew,
              useRefreshToken: environment.oidc.useRefreshToken,
              renewTimeBeforeTokenExpiresInSeconds:
                environment.oidc.renewTimeBeforeTokenExpiresInSeconds,
              autoUserInfo: environment.oidc.autoUserInfo,
              logLevel: environment.oidc.logLevel,
              secureRoutes: [],
              ignoreNonceAfterRefresh: true,
            }
          : {
              // Dummy config when OIDC is not active
              authority: 'https://localhost',
              redirectUrl: window.location.origin,
              postLogoutRedirectUri: window.location.origin,
              clientId: 'dummy',
              scope: 'openid',
              responseType: 'code',
            },
    }),
  ];

  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideRouter(routes),
      provideHttpClient(
        withInterceptors([...(interceptors[environment.authMode] ?? interceptors['oidc'])]),
      ),
      provideAnimations(),
      ...ngxsProviders,
      ...translateProviders,
      ...authProviders,
      provideAppInitializer(initializeApp),
    ],
  };
}
