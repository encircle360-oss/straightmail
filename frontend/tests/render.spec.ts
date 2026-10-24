import { expect, test } from './fixtures';
import { mockRender, mockTemplatesList } from './support/api-mocks';

test.describe('Live Rendering', () => {
  test.beforeEach(async ({ page }) => {
    await mockTemplatesList(page);
    await mockRender(page, '<h1>Hello Test</h1>', 'Hello Test plain');
    await page.goto('/render');
    await page.waitForSelector('select.form-select', { state: 'visible' });
    await page.waitForSelector('option[value="1"]', { state: 'attached' });
  });

  test('should show render preview after submitting', async ({ page }) => {
    await page.locator('select.form-select').selectOption('1');
    await page.locator('button[type="submit"]').click();

    // Preview iframe appears after successful render
    const iframe = page.frameLocator('iframe.render-iframe');
    await expect(iframe.locator('h1')).toContainText('Hello Test');
  });

  test('should display plain text preview', async ({ page }) => {
    await page.locator('select.form-select').selectOption('1');
    await page.locator('button[type="submit"]').click();

    // Wait for render result tabs
    await page.waitForSelector('ul[ngbNav]', { state: 'visible' });

    // Click the "Plain Text" tab
    await page.getByRole('tab', { name: 'Plain Text' }).click();

    await expect(page.locator('pre.render-plain')).toContainText('Hello Test plain');
  });
});
