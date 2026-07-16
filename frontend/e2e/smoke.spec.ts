import { expect, test } from '@playwright/test'

test('new signed Telegram user can open the application', async ({ page }, testInfo) => {
  const consoleMessages: string[] = []
  page.on('console', (message) => consoleMessages.push(`[${message.type()}] ${message.text()}`))
  page.on('pageerror', (error) => consoleMessages.push(`[pageerror] ${error.message}`))
  page.on('request', (request) => {
    if (request.url().endsWith('/api/pantries')) {
      consoleMessages.push(
        `[request] authorization=${request.headers().authorization ?? 'missing'}`,
      )
    }
  })

  try {
    await page.goto('/')

    await expect(page.getByRole('heading', { name: 'Мои инвентари' })).toBeVisible()
    await expect(page.getByText('Default', { exact: true })).toBeVisible()
  } finally {
    await testInfo.attach('browser-console.txt', {
      body: consoleMessages.join('\n'),
      contentType: 'text/plain',
    })
  }
})
