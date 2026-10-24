import { expect, test } from './fixtures';
import {
  defaultTemplates,
  mockTemplateCreate,
  mockTemplateDelete,
  mockTemplateGet,
  mockTemplatesList,
  mockTemplateUpdate,
} from './support/api-mocks';

test.describe('Template Lifecycle', () => {
  test.beforeEach(async ({ page }) => {
    await mockTemplatesList(page);
    await page.goto('/templates');
    // Wait for the table or empty state to appear
    await page.waitForSelector('.table-wrapper', { state: 'visible' });
  });

  test('should display existing templates', async ({ page }) => {
    await expect(page.locator('table.table')).toContainText('Test Template');
  });

  test('should navigate to new template form', async ({ page }) => {
    await page.click('button:has-text("New Template")');
    await expect(page).toHaveURL(/.*templates\/new/);
  });

  test('should navigate to edit form when clicking edit button', async ({ page }) => {
    // Mock the template resolver so the edit form can load
    await mockTemplateGet(page, defaultTemplates[0]);
    // Edit button (pencil) is the first .btn-outline-secondary in the actions column
    await page.locator('table.table tbody tr').first().locator('.btn-outline-secondary').click();
    await expect(page).toHaveURL(/.*templates\/1\/edit/);
  });

  test('should filter templates by tag', async ({ page }) => {
    // Tag filter buttons appear when templates have tags
    const tagButton = page.locator('.tag-filter button:has-text("test")');
    if (await tagButton.isVisible()) {
      await tagButton.click();
      await expect(page.locator('table.table')).toContainText('Test Template');
    }
  });
});

test.describe('Template Create', () => {
  test('should navigate to create form and save', async ({ page }) => {
    await mockTemplatesList(page);
    await mockTemplateCreate(page);

    await page.goto('/templates/new');
    await page.waitForSelector('form[novalidate]', { state: 'visible' });

    // The name field is the first text input (app-text-field for "Name")
    await page.locator('input').first().fill('My New Template');

    await page.getByRole('button', { name: 'Save' }).click();

    await expect(page.locator('.toast-container')).toBeVisible();
  });
});

test.describe('Template Edit', () => {
  test('should pre-fill form with existing template data', async ({ page }) => {
    await mockTemplateGet(page, defaultTemplates[0]);
    await mockTemplatesList(page);

    await page.goto('/templates/1/edit');
    await page.waitForSelector('form[novalidate]', { state: 'visible' });

    // The name input should contain "Test Template"
    await expect(page.locator('input').first()).toHaveValue('Test Template');
  });

  test('should update a template and show success toast', async ({ page }) => {
    await mockTemplateGet(page, defaultTemplates[0]);
    await mockTemplateUpdate(page, '1');
    await mockTemplatesList(page);

    await page.goto('/templates/1/edit');
    await page.waitForSelector('form[novalidate]', { state: 'visible' });

    await page.locator('input').first().fill('Updated Template');
    await page.getByRole('button', { name: 'Save' }).click();

    await expect(page.locator('.toast-container')).toBeVisible();
  });
});

test.describe('Git Branch Badge', () => {
  test('should show branch badge for GIT source templates', async ({ page }) => {
    await mockTemplatesList(page, [
      {
        id: 'welcome',
        name: 'welcome',
        locale: 'en',
        tags: ['source:git'],
        source: 'GIT',
        editable: false,
        gitBranch: 'main',
      },
    ]);
    await page.goto('/templates');
    await page.waitForSelector('.table-wrapper', { state: 'visible' });

    await expect(page.locator('table.table .git-branch-badge')).toBeVisible();
    await expect(page.locator('table.table .git-branch-badge')).toContainText('main');
  });

  test('should not show branch badge for DATABASE templates', async ({ page }) => {
    await mockTemplatesList(page, [
      {
        id: '1',
        name: 'Test Template',
        locale: 'en',
        tags: [],
        source: 'DATABASE',
        editable: true,
      },
    ]);
    await page.goto('/templates');
    await page.waitForSelector('.table-wrapper', { state: 'visible' });

    await expect(page.locator('table.table .git-branch-badge')).not.toBeVisible();
  });

  test('should show two rows for same template in two branches', async ({ page }) => {
    await mockTemplatesList(page, [
      {
        id: 'welcome',
        name: 'welcome',
        locale: 'en',
        tags: ['source:git'],
        source: 'GIT',
        editable: false,
        gitBranch: 'main',
      },
      {
        id: 'welcome',
        name: 'welcome',
        locale: 'en',
        tags: ['source:git'],
        source: 'GIT',
        editable: false,
        gitBranch: 'develop',
      },
    ]);
    await page.goto('/templates');
    await page.waitForSelector('.table-wrapper', { state: 'visible' });

    await expect(page.locator('table.table .git-branch-badge')).toHaveCount(2);
  });
});

test.describe('Template Delete', () => {
  test('should open confirm dialog when clicking delete', async ({ page }) => {
    await mockTemplatesList(page);
    await mockTemplateDelete(page, '1');

    await page.goto('/templates');
    await page.waitForSelector('.table-wrapper', { state: 'visible' });

    // Click the delete button (trash icon) in the first row
    await page.locator('table.table tbody tr').first().locator('.btn-outline-danger').click();

    // ngbModal confirm dialog appears
    await expect(page.locator('.modal-dialog')).toBeVisible();

    // Click the confirm button (last button in modal-footer)
    await page.locator('.modal-footer button').last().click();

    await expect(page.locator('.toast-container')).toBeVisible();
  });
});
