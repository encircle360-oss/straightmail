import { expect, test } from './fixtures';

test.describe('Auth Flow', () => {
  test('should load the app and redirect to dashboard', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/.*dashboard/);
    await page.waitForSelector('.dashboard', { state: 'visible' });
    await expect(page.locator('h1.page-title')).toContainText(/Dashboard/i);
  });

  test('sessionStorage should contain apiKey after fixture setup', async ({ page }) => {
    await page.goto('/');
    const apiKey = await page.evaluate(() => {
      const raw = sessionStorage.getItem('apiKey');
      return raw ? JSON.parse(raw) : null;
    });
    expect(apiKey).not.toBeNull();
    expect(apiKey.apiKey).toBeTruthy();
  });

  test('sessionStorage should contain tenantId after fixture setup', async ({ page }) => {
    await page.goto('/');
    const tenant = await page.evaluate(() => {
      const raw = sessionStorage.getItem('tenant');
      return raw ? JSON.parse(raw) : null;
    });
    expect(tenant).not.toBeNull();
    expect(tenant.selectedTenantId).toBeTruthy();
  });

  test('unauthenticated access redirects to api-key-login', async ({ context }) => {
    // Use a clean page (no autoAuth initScript) to test the guard without a seeded API key.
    const cleanPage = await context.newPage();
    await cleanPage.goto('/templates');
    await expect(cleanPage).toHaveURL(/.*api-key-login/);
    await cleanPage.close();
  });
});
