import { expect, test } from './fixtures.js'

test('E2E-001: new signed Telegram user can open the application', async (
  { identity, initData, page },
  testInfo,
) => {
  const diagnostics: string[] = []
  page.on('console', (message) => {
    const location = message.location().url
    diagnostics.push(
      `[console:${message.type()}] ${location ? new URL(location).pathname : '<unknown>'}`,
    )
  })
  page.on('pageerror', (error) => diagnostics.push(`[pageerror] ${error.name}`))
  page.on('request', (request) => {
    if (new URL(request.url()).pathname === '/api/pantries') {
      diagnostics.push(
        `[request] ${request.method()} /api/pantries authorization=${request.headers().authorization?.startsWith('tma ') ? 'tma-present' : 'missing'}`,
      )
    }
  })
  page.on('response', (response) => {
    if (new URL(response.url()).pathname === '/api/pantries') {
      diagnostics.push(`[response] ${response.status()} /api/pantries`)
    }
  })

  try {
    const firstPantriesResponse = page.waitForResponse(
      (response) =>
        response.request().method() === 'GET' &&
        new URL(response.url()).pathname === '/api/pantries',
    )
    await page.goto('/')

    const response = await firstPantriesResponse
    const authorization = response.request().headers().authorization
    expect(authorization).toBe(`tma ${initData}`)
    expect(response.status()).toBe(200)

    const authorizationData = new URLSearchParams(authorization.replace(/^tma /, ''))
    expect(JSON.parse(authorizationData.get('user') ?? '{}')).toMatchObject({
      id: identity.id,
      username: identity.username,
    })

    await expect(page.getByRole('heading', { name: 'Мои инвентари' })).toBeVisible()
    const defaultPantry = page.getByRole('link', { name: /Default/ })
    await expect(defaultPantry).toHaveCount(1)
    await expect(defaultPantry).toContainText('Владелец')
    await expect(defaultPantry).toContainText('По умолчанию')
  } finally {
    await testInfo.attach('browser-diagnostics.txt', {
      body: diagnostics.join('\n'),
      contentType: 'text/plain',
    })
  }
})
