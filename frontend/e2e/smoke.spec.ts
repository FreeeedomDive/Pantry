import { expect, test } from './fixtures.js'

test('new signed Telegram user can open the application', async ({ identity, page }, testInfo) => {
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
    const firstPantriesRequest = page.waitForRequest((request) =>
      request.url().endsWith('/api/pantries'),
    )
    await page.goto('/')

    const authorization = (await firstPantriesRequest).headers().authorization
    expect(authorization).toBeDefined()
    const initData = new URLSearchParams(authorization?.replace(/^tma /, ''))
    expect(JSON.parse(initData.get('user') ?? '{}')).toMatchObject({
      id: identity.id,
      username: identity.username,
    })

    await expect(page.getByRole('heading', { name: 'Мои инвентари' })).toBeVisible()
    await expect(page.getByText('Default', { exact: true })).toBeVisible()

    const reloadedPantriesRequest = page.waitForRequest((request) =>
      request.url().endsWith('/api/pantries'),
    )
    await page.reload()
    expect((await reloadedPantriesRequest).headers().authorization).toBe(authorization)
  } finally {
    await testInfo.attach('browser-console.txt', {
      body: consoleMessages.join('\n'),
      contentType: 'text/plain',
    })
  }
})
