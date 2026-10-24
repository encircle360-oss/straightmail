import { expect, test } from './fixtures';
import { mockInfoStatus, mockTemplatesList } from './support/api-mocks';

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/dashboard');
    await page.waitForSelector('.dashboard', { state: 'visible' });
  });

  test('should show API health status as operational when API is up', async ({ page }) => {
    await mockInfoStatus(page, 'up');
    await page.reload();
    await page
      .waitForSelector('.status-badge--up', { state: 'visible', timeout: 10000 })
      .catch(() => null);
    await expect(page.locator('.status-badge--up')).toBeVisible();
  });

  test('should show API health status as down when API returns error', async ({ page }) => {
    await mockInfoStatus(page, 'down');
    await page.reload();
    await page
      .waitForSelector('.status-badge--down', { state: 'visible', timeout: 10000 })
      .catch(() => null);
    await expect(page.locator('.status-badge--down')).toBeVisible();
  });

  test('should navigate to templates via sidebar link', async ({ page }) => {
    await page.click('a[href="/templates"]');
    await expect(page).toHaveURL(/.*templates/);
  });

  test('should navigate to send via action card', async ({ page }) => {
    // Mock the templates resolver so Angular Router can complete navigation to /send
    await mockTemplatesList(page);
    await page.locator('a.action-card[href="/send"]').click();
    await expect(page).toHaveURL(/.*send/);
  });
});
