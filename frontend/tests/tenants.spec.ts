import { expect, test } from './fixtures';

// Every API call is proxied through the app origin: /api → http://localhost:50003
const API_BASE = 'http://localhost:4299/api';

const mockTenantData = [
  {
    slug: 'default',
    displayName: 'Default',
    smtpTls: false,
    smtpSsl: false,
    hasApiKey: true,
    active: true,
  },
  {
    slug: 'test-corp',
    displayName: 'Test Corp',
    smtpTls: false,
    smtpSsl: false,
    hasApiKey: false,
    active: true,
  },
];

async function setupTenantListMock(
  page: import('@playwright/test').Page,
  tenants = mockTenantData,
) {
  await page.route(`${API_BASE}/v1/tenants`, async (route) => {
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
}

test.describe('Tenant Management', () => {
  test.beforeEach(async ({ page }) => {
    await setupTenantListMock(page);
    await page.goto('/tenants');
    await page.waitForSelector('.table-wrapper', { state: 'visible' });
  });

  test('should display tenant list', async ({ page }) => {
    await expect(page.locator('table.table')).toBeVisible();
    await expect(page.getByRole('cell', { name: 'Default', exact: true })).toBeVisible();
    await expect(page.getByRole('cell', { name: 'Test Corp', exact: true })).toBeVisible();
  });

  test('should navigate to create form', async ({ page }) => {
    await page.getByRole('button', { name: 'New Tenant' }).click();
    await expect(page).toHaveURL(/.*tenants\/new/);
  });

  test('should create a new tenant and navigate back', async ({ page }) => {
    const newTenant = {
      slug: 'new-corp',
      displayName: 'New Corp',
      smtpTls: false,
      smtpSsl: false,
      hasApiKey: false,
      active: true,
    };

    await page.route(`${API_BASE}/v1/tenants`, async (route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(newTenant),
        });
      } else {
        await route.fallback();
      }
    });

    await page.goto('/tenants/new');
    await page.waitForSelector('form', { state: 'visible' });

    // Fill slug (only shown on create)
    await page.getByPlaceholder('e.g. acme-corp').fill('new-corp');
    // Fill display name
    await page.locator('input[name="displayName"]').fill('New Corp');

    await page.getByRole('button', { name: 'Save' }).click();

    await expect(page).toHaveURL(/.*tenants/);
  });

  test('should pre-fill edit form with existing tenant data', async ({ page }) => {
    const tenant = mockTenantData[0];

    await page.route(`${API_BASE}/v1/tenants/${tenant.slug}`, async (route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(tenant),
        });
      } else {
        await route.fallback();
      }
    });

    await page.goto(`/tenants/${tenant.slug}/edit`);
    await page.waitForSelector('form', { state: 'visible' });

    await expect(page.locator('input[name="displayName"]')).toHaveValue(tenant.displayName);
  });

  test('should delete a non-default tenant after confirmation', async ({ page }) => {
    // 'test-corp' is not 'default' so it has a delete button
    await page.route(`${API_BASE}/v1/tenants/test-corp`, async (route) => {
      if (route.request().method() === 'DELETE') {
        await route.fulfill({ status: 204 });
      } else {
        await route.fallback();
      }
    });

    // The delete button is only present for non-default tenants (second row = test-corp)
    await page.locator('table.table tbody tr').nth(1).locator('.btn-outline-danger').click();

    // Confirm the ngbModal dialog
    await expect(page.locator('.modal-dialog')).toBeVisible();
    await page.locator('.modal-footer button').last().click();
  });
});
