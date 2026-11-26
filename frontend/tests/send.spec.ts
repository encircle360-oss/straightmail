import { expect, test } from './fixtures';
import { mockSendEmail, mockSendEmailError, mockTemplatesList } from './support/api-mocks';

test.describe('Email Sending', () => {
  test.beforeEach(async ({ page }) => {
    await mockTemplatesList(page);
    await page.goto('/send');
    // Wait for Angular to finish rendering the nav tabs
    await page.waitForSelector('ul[ngbNav].nav-tabs', { state: 'visible' });
  });

  test('should load templates into the template select', async ({ page }) => {
    await expect(page.locator('select.form-select option[value="1"]')).toBeAttached();
  });

  test('should send email by template and show success toast', async ({ page }) => {
    // Add recipient via chip-field
    await page.getByPlaceholder('email@your-domain.tld').fill('recipient@your-domain.tld');
    await page.locator('.input-group button:has-text("Add")').first().click();

    // Fill sender
    await page.getByPlaceholder('sender@your-domain.tld').fill('sender@your-domain.tld');

    // Select template
    await page
      .locator('select.form-select')
      .filter({ has: page.locator('option[value="1"]') })
      .selectOption('1');

    // Mock send API and submit
    await mockSendEmail(page);
    await page.locator('app-page-header button').last().click();

    await expect(page.locator('.toast-container')).toBeVisible();
  });

  test('should switch to inline tab and send inline email', async ({ page }) => {
    // Click the "Inline Template" tab by text
    await page.locator('button.nav-link', { hasText: 'Inline Template' }).click();

    // Wait for the inline form to appear
    await page.waitForSelector('form[novalidate]', { state: 'visible', timeout: 5000 });

    // Set the emailTemplate control via Angular's debug API (bypasses the CodeMirror editor)
    await page.evaluate(() => {
      const el = document.querySelector('app-send');
      const comp = (window as any).ng?.getComponent?.(el);
      comp?.inlineForm?.get('emailTemplate')?.setValue('<h1>Test</h1>');
      if (el) (window as any).ng?.applyChanges?.(el);
    });

    // Fill recipient
    await page.getByPlaceholder('email@your-domain.tld').fill('inline@your-domain.tld');
    await page.locator('.input-group button:has-text("Add")').first().click();

    // Fill sender and subject
    await page.getByPlaceholder('sender@your-domain.tld').fill('sender@your-domain.tld');
    await page.getByPlaceholder('Email subject').fill('Hello World');

    // Mock and submit
    await mockSendEmail(page);
    await page.locator('app-page-header button').last().click();

    await expect(page.locator('.toast-container')).toBeVisible();
  });

  test('should show error toast when API returns error', async ({ page }) => {
    // Add required fields
    await page.getByPlaceholder('email@your-domain.tld').fill('recipient@your-domain.tld');
    await page.locator('.input-group button:has-text("Add")').first().click();
    await page.getByPlaceholder('sender@your-domain.tld').fill('sender@your-domain.tld');
    await page
      .locator('select.form-select')
      .filter({ has: page.locator('option[value="1"]') })
      .selectOption('1');

    await mockSendEmailError(page);
    await page.locator('app-page-header button').last().click();

    await expect(page.locator('.toast-container')).toBeVisible();
  });
});
