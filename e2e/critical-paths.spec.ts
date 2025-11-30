import { test, expect } from '@playwright/test';

// ==================== USER REGISTRATION AND LOGIN ====================

test.describe('User Authentication', () => {
  test('should register a new user and login', async ({ page }) => {
    // Navigate to registration page
    await page.goto('http://localhost:4200/register');

    // Fill registration form
    await page.fill('input[name="username"]', 'testoperator');
    await page.fill('input[name="email"]', 'testop@example.com');
    await page.fill('input[name="callsign"]', 'W1TEST');
    await page.fill('input[name="password"]', 'Test123!@#');
    await page.fill('input[name="confirmPassword"]', 'Test123!@#');
    await page.check('input[name="termsAccepted"]');

    // Submit registration
    await page.click('button[type="submit"]');

    // Should redirect to login
    await expect(page).toHaveURL(/.*login/);
    await expect(page.locator('text=Registration successful')).toBeVisible();

    // Login with new credentials
    await page.fill('input[name="username"]', 'testoperator');
    await page.fill('input[name="password"]', 'Test123!@#');
    await page.click('button[type="submit"]');

    // Should redirect to dashboard
    await expect(page).toHaveURL(/.*dashboard/);
    await expect(page.locator('text=Welcome, testoperator')).toBeVisible();
  });

  test('should handle login with invalid credentials', async ({ page }) => {
    await page.goto('http://localhost:4200/login');

    await page.fill('input[name="username"]', 'invalid');
    await page.fill('input[name="password"]', 'wrongpass');
    await page.click('button[type="submit"]');

    await expect(page.locator('text=Invalid credentials')).toBeVisible();
  });

  test('should logout successfully', async ({ page }) => {
    // Login first
    await page.goto('http://localhost:4200/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');

    await expect(page).toHaveURL(/.*dashboard/);

    // Logout
    await page.click('button[aria-label="User menu"]');
    await page.click('text=Logout');

    // Should redirect to login
    await expect(page).toHaveURL(/.*login/);
  });
});

// ==================== LOG FIRST QSO ====================

test.describe('First QSO Workflow', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('http://localhost:4200/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL(/.*dashboard/);
  });

  test('should create a log and log first QSO', async ({ page }) => {
    // Create new log
    await page.click('button:has-text("New Log")');
    await page.fill('input[name="logName"]', 'Field Day 2025');
    await page.selectOption('select[name="contestCode"]', 'ARRL-FD');
    await page.click('button:has-text("Create Log")');

    await expect(page.locator('text=Field Day 2025')).toBeVisible();

    // Log first QSO
    await page.click('button:has-text("Log QSO")');

    await page.fill('input[name="callsign"]', 'W1AW');
    await page.fill('input[name="frequencyKhz"]', '14250');
    await page.selectOption('select[name="mode"]', 'SSB');
    await page.fill('input[name="rstSent"]', '59');
    await page.fill('input[name="rstRcvd"]', '59');

    // Submit QSO
    await page.click('button:has-text("Log QSO")');

    await expect(page.locator('text=QSO logged successfully')).toBeVisible();
    await expect(page.locator('text=W1AW')).toBeVisible();
  });

  test('should log multiple QSOs in succession', async ({ page }) => {
    await page.click('button:has-text("Log QSO")');

    const qsos = [
      { callsign: 'W1AW', freq: '14250', mode: 'SSB' },
      { callsign: 'K2ABC', freq: '14255', mode: 'SSB' },
      { callsign: 'N3XYZ', freq: '14260', mode: 'SSB' }
    ];

    for (const qso of qsos) {
      await page.fill('input[name="callsign"]', qso.callsign);
      await page.fill('input[name="frequencyKhz"]', qso.freq);
      await page.selectOption('select[name="mode"]', qso.mode);
      await page.fill('input[name="rstSent"]', '59');
      await page.fill('input[name="rstRcvd"]', '59');

      await page.click('button:has-text("Log QSO")');
      await expect(page.locator(`text=${qso.callsign}`)).toBeVisible();

      // Form should reset for next QSO
      await expect(page.locator('input[name="callsign"]')).toHaveValue('');
    }

    // Should have 3 QSOs in the log
    const qsoRows = page.locator('tr.qso-row');
    await expect(qsoRows).toHaveCount(3);
  });

  test('should handle duplicate QSO warning', async ({ page }) => {
    await page.click('button:has-text("Log QSO")');

    // Log first QSO
    await page.fill('input[name="callsign"]', 'W1AW');
    await page.fill('input[name="frequencyKhz"]', '14250');
    await page.selectOption('select[name="mode"]', 'SSB');
    await page.fill('input[name="rstSent"]', '59');
    await page.fill('input[name="rstRcvd"]', '59');
    await page.click('button:has-text("Log QSO")');

    await expect(page.locator('text=QSO logged successfully')).toBeVisible();

    // Attempt to log duplicate
    await page.fill('input[name="callsign"]', 'W1AW');
    await page.fill('input[name="frequencyKhz"]', '14250');
    await page.selectOption('select[name="mode"]', 'SSB');
    await page.fill('input[name="rstSent"]', '59');
    await page.fill('input[name="rstRcvd"]', '59');
    await page.click('button:has-text("Log QSO")');

    await expect(page.locator('text=Possible duplicate QSO')).toBeVisible();
  });
});

// ==================== MULTI-USER LOG COLLABORATION ====================

test.describe('Log Collaboration', () => {
  test('should invite user to log and accept invitation', async ({ page, context }) => {
    // User 1 logs in
    await page.goto('http://localhost:4200/login');
    await page.fill('input[name="username"]', 'user1');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');

    // Create log and invite user 2
    await page.click('button:has-text("New Log")');
    await page.fill('input[name="logName"]', 'Shared Log');
    await page.click('button:has-text("Create Log")');

    await page.click('button:has-text("Invite Participants")');
    await page.fill('input[name="username"]', 'user2');
    await page.selectOption('select[name="role"]', 'STATION');
    await page.click('button:has-text("Send Invitation")');

    await expect(page.locator('text=Invitation sent')).toBeVisible();

    // Open new page for user 2
    const page2 = await context.newPage();
    await page2.goto('http://localhost:4200/login');
    await page2.fill('input[name="username"]', 'user2');
    await page2.fill('input[name="password"]', 'password');
    await page2.click('button[type="submit"]');

    // User 2 sees invitation
    await page2.click('button[aria-label="Invitations"]');
    await expect(page2.locator('text=Shared Log')).toBeVisible();

    // Accept invitation
    await page2.click('button:has-text("Accept")');
    await expect(page2.locator('text=Invitation accepted')).toBeVisible();

    // User 2 can now see the shared log
    await expect(page2.locator('text=Shared Log')).toBeVisible();
  });
});

// ==================== LOG FREEZE WORKFLOW ====================

test.describe('Log Freeze', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:4200/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');
  });

  test('should freeze log and prevent modifications', async ({ page }) => {
    // Select log
    await page.click('text=Test Log');

    // Log a QSO
    await page.click('button:has-text("Log QSO")');
    await page.fill('input[name="callsign"]', 'W1AW');
    await page.fill('input[name="frequencyKhz"]', '14250');
    await page.selectOption('select[name="mode"]', 'SSB');
    await page.fill('input[name="rstSent"]', '59');
    await page.fill('input[name="rstRcvd"]', '59');
    await page.click('button:has-text("Log QSO")');

    // Freeze log
    await page.click('button:has-text("Freeze Log")');
    await page.click('button:has-text("Confirm")');

    await expect(page.locator('text=Log frozen successfully')).toBeVisible();

    // Verify QSO entry is disabled
    await expect(page.locator('button:has-text("Log QSO")')).toBeDisabled();

    // Verify edit/delete buttons are disabled
    await expect(page.locator('button.edit-qso')).toBeDisabled();
    await expect(page.locator('button.delete-qso')).toBeDisabled();
  });

  test('should unfreeze log and allow modifications', async ({ page }) => {
    await page.click('text=Frozen Log');

    // Verify log is frozen
    await expect(page.locator('text=This log is frozen')).toBeVisible();

    // Unfreeze log
    await page.click('button:has-text("Unfreeze Log")');
    await page.click('button:has-text("Confirm")');

    await expect(page.locator('text=Log unfrozen successfully')).toBeVisible();

    // Verify QSO entry is enabled
    await expect(page.locator('button:has-text("Log QSO")')).not.toBeDisabled();
  });
});

// ==================== ADIF IMPORT/EXPORT ====================

test.describe('ADIF Import/Export', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:4200/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');
  });

  test('should import ADIF file', async ({ page }) => {
    await page.click('text=Test Log');
    await page.click('button:has-text("Import")');

    // Upload ADIF file
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles('test-data/sample.adi');

    await page.click('button:has-text("Import ADIF")');

    await expect(page.locator('text=10 QSOs imported successfully')).toBeVisible();
  });

  test('should export log as ADIF', async ({ page }) => {
    await page.click('text=Test Log');
    await page.click('button:has-text("Export")');
    await page.click('button:has-text("ADIF")');

    // File should download
    const downloadPromise = page.waitForEvent('download');
    await page.click('button:has-text("Download")');
    const download = await downloadPromise;

    expect(download.suggestedFilename()).toContain('.adi');
  });

  test('should export log as Cabrillo', async ({ page }) => {
    await page.click('text=Field Day Log');
    await page.click('button:has-text("Export")');
    await page.click('button:has-text("Cabrillo")');

    const downloadPromise = page.waitForEvent('download');
    await page.click('button:has-text("Download")');
    const download = await downloadPromise;

    expect(download.suggestedFilename()).toContain('.cbr');
  });
});

// ==================== RIG CONTROL INTEGRATION ====================

test.describe('Rig Control', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:4200/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');
  });

  test('should connect to rig and populate frequency', async ({ page }) => {
    await page.click('text=Test Log');
    await page.click('button:has-text("Rig Control")');

    // Enter rig connection details
    await page.fill('input[name="rigAddress"]', 'localhost');
    await page.fill('input[name="rigPort"]', '4532');
    await page.click('button:has-text("Connect")');

    await expect(page.locator('text=Connected to rig')).toBeVisible();

    // Rig data should populate QSO form
    await page.click('button:has-text("Log QSO")');

    // Frequency should be auto-populated from rig
    const frequency = await page.locator('input[name="frequencyKhz"]').inputValue();
    expect(parseInt(frequency)).toBeGreaterThan(0);

    // Mode should be auto-populated from rig
    const mode = await page.locator('select[name="mode"]').inputValue();
    expect(mode).toBeTruthy();
  });
});

// ==================== SEARCH AND FILTER ====================

test.describe('Search and Filter', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:4200/login');
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="password"]', 'password');
    await page.click('button[type="submit"]');
    await page.click('text=Test Log');
  });

  test('should search QSOs by callsign', async ({ page }) => {
    await page.fill('input[placeholder="Search callsign"]', 'W1AW');
    await page.press('input[placeholder="Search callsign"]', 'Enter');

    const results = page.locator('tr.qso-row');
    const count = await results.count();

    for (let i = 0; i < count; i++) {
      const row = results.nth(i);
      await expect(row.locator('td.callsign')).toContainText('W1AW');
    }
  });

  test('should filter QSOs by band', async ({ page }) => {
    await page.selectOption('select[name="bandFilter"]', '20m');

    const results = page.locator('tr.qso-row');
    const count = await results.count();

    for (let i = 0; i < count; i++) {
      const row = results.nth(i);
      await expect(row.locator('td.band')).toContainText('20m');
    }
  });

  test('should filter QSOs by date range', async ({ page }) => {
    await page.fill('input[name="startDate"]', '2025-01-01');
    await page.fill('input[name="endDate"]', '2025-01-31');
    await page.click('button:has-text("Apply Filter")');

    const results = page.locator('tr.qso-row');
    expect(await results.count()).toBeGreaterThan(0);
  });
});

// ==================== ACCESSIBILITY ====================

test.describe('Accessibility', () => {
  test('should be navigable by keyboard', async ({ page }) => {
    await page.goto('http://localhost:4200/login');

    await page.keyboard.press('Tab'); // Focus username
    await page.keyboard.type('testuser');
    await page.keyboard.press('Tab'); // Focus password
    await page.keyboard.type('password');
    await page.keyboard.press('Enter'); // Submit form

    await expect(page).toHaveURL(/.*dashboard/);
  });

  test('should have proper ARIA labels', async ({ page }) => {
    await page.goto('http://localhost:4200/login');

    const usernameLabel = await page.locator('input[name="username"]').getAttribute('aria-label');
    const passwordLabel = await page.locator('input[name="password"]').getAttribute('aria-label');

    expect(usernameLabel).toBeTruthy();
    expect(passwordLabel).toBeTruthy();
  });
});
