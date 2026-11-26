import { expect, test as base } from '@playwright/test';
import { mockApiInfo, mockTenants, mockTranslations } from './support/api-mocks';

export const test = base.extend<{ autoAuth: void }>({
  autoAuth: [
    async ({ page }, use) => {
      // Pre-seed storage so guards find the correct state when Angular bootstraps.
      // apiKey and tenant use SESSION_STORAGE_ENGINE; auth uses LOCAL_STORAGE_ENGINE.
      await page.addInitScript(() => {
        sessionStorage.setItem('apiKey', JSON.stringify({ apiKey: 'e2e-test-key' }));
        sessionStorage.setItem(
          'tenant',
          JSON.stringify({
            tenants: [
              {
                slug: 'default',
                displayName: 'Default',
                smtpTls: false,
                smtpSsl: false,
                hasApiKey: true,
                active: true,
                editable: true,
              },
            ],
            selectedTenantId: 'default',
          }),
        );
        localStorage.setItem(
          'auth',
          JSON.stringify({
            isAuthenticated: false,
            userData: null,
            accessToken: null,
            roles: ['ADMIN'],
          }),
        );
        localStorage.setItem(
          'ui',
          JSON.stringify({
            language: 'en',
            accordionStatus: {},
            tenantsViewMode: 'list',
            templatesViewMode: 'list',
          }),
        );
      });
      // Mock /v1/info so main.ts can populate __runtimeConfig before bootstrap.
      await mockApiInfo(page);
      // Serve translations from the actual file so ngx-translate renders real text.
      await mockTranslations(page);
      // Mock the tenants endpoint called by the loadTenants app-initializer.
      await mockTenants(page);
      await use();
    },
    { auto: true },
  ],
});

export { expect };
