import { readFileSync } from 'fs';
import { resolve } from 'path';
import { Page } from '@playwright/test';
import { EmailTemplate } from '../../src/app/core/services/api.service';

// App dev server URL; every API call is proxied: /api → http://localhost:50003
const APP_BASE = 'http://localhost:4299';

export const defaultTemplates: EmailTemplate[] = [
  {
    id: '1',
    name: 'Test Template',
    locale: 'en',
    tags: ['test'],
    subject: 'Hello',
    html: '<h1>Hello</h1>',
    plain: 'Hello',
  },
];

export const defaultTenants = [
  {
    slug: 'default',
    displayName: 'Default',
    smtpTls: false,
    smtpSsl: false,
    hasApiKey: true,
    active: true,
    editable: true,
  },
];

export const mockTemplatesList = (page: Page, templates = defaultTemplates) =>
  page.route(`${APP_BASE}/api/v1/templates*`, async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          content: templates,
          totalElements: templates.length,
          totalPages: 1,
          number: 0,
          size: 20,
        }),
      });
    } else {
      await route.fallback();
    }
  });

export const mockTemplateGet = (page: Page, template = defaultTemplates[0]) =>
  page.route(`${APP_BASE}/api/v1/templates/${template.id}`, async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(template),
      });
    } else {
      await route.fallback();
    }
  });

export const mockTemplateCreate = (page: Page, created = { ...defaultTemplates[0], id: '99' }) =>
  page.route(`${APP_BASE}/api/v1/templates`, async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify(created),
      });
    } else {
      await route.fallback();
    }
  });

export const mockTemplateUpdate = (page: Page, id: string, updated = defaultTemplates[0]) =>
  page.route(`${APP_BASE}/api/v1/templates/${id}`, async (route) => {
    if (route.request().method() === 'PUT') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(updated),
      });
    } else {
      await route.fallback();
    }
  });

export const mockTemplateDelete = (page: Page, id: string) =>
  page.route(`${APP_BASE}/api/v1/templates/${id}`, async (route) => {
    if (route.request().method() === 'DELETE') {
      await route.fulfill({ status: 204 });
    } else {
      await route.fallback();
    }
  });

export const mockApiInfo = (page: Page) =>
  page.route(`${APP_BASE}/api/v1/info`, async (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        application: 'straightmail',
        status: 'running',
        authMode: 'api-key',
        // Same value the backend's /v1/info serves by default; main.ts puts this whole
        // response into window.__runtimeConfig, so it is what environment.apiUrl resolves to.
        apiUrl: '/api',
        oidcAuthority: '',
      }),
    }),
  );

export const mockInfoStatus = (page: Page, status: 'up' | 'down') =>
  page.route(`${APP_BASE}/api/v1/info`, async (route) =>
    route.fulfill({ status: status === 'up' ? 200 : 503 }),
  );

export const mockSendEmail = (page: Page) =>
  page.route(`${APP_BASE}/api/v1/email*`, async (route) => route.fulfill({ status: 200 }));

export const mockSendEmailError = (page: Page) =>
  page.route(`${APP_BASE}/api/v1/email*`, async (route) =>
    route.fulfill({
      status: 500,
      contentType: 'application/json',
      body: JSON.stringify({ message: 'Server Error' }),
    }),
  );

export const mockRender = (page: Page, html = '<h1>Hello</h1>', plain = 'Hello') =>
  page.route(`${APP_BASE}/api/v1/render`, async (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ html, plain }),
    }),
  );

export const mockTranslations = (page: Page, lang = 'en') => {
  const filePath = resolve(__dirname, `../../public/assets/i18n/${lang}.json`);
  const body = readFileSync(filePath, 'utf-8');
  return page.route(`${APP_BASE}/assets/i18n/${lang}.json`, (route) =>
    route.fulfill({ status: 200, contentType: 'application/json', body }),
  );
};

export const mockTenants = (page: Page, tenants = defaultTenants) =>
  page.route(`${APP_BASE}/api/v1/tenants/me`, async (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(tenants),
    }),
  );

export const mockTenantList = (page: Page, tenants = defaultTenants) =>
  page.route(`${APP_BASE}/api/v1/tenants`, async (route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(tenants),
      });
    } else {
      await route.fallback();
    }
  });

export const mockTenantCreate = (
  page: Page,
  created = {
    slug: 'new-tenant',
    displayName: 'New Tenant',
    smtpTls: false,
    smtpSsl: false,
    hasApiKey: false,
    active: true,
  },
) =>
  page.route(`${APP_BASE}/api/v1/tenants`, async (route) => {
    if (route.request().method() === 'POST') {
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify(created),
      });
    } else {
      await route.fallback();
    }
  });
