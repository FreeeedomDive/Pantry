import type { Page } from '@playwright/test'
import type { PantryResponse } from '../src/api/types.js'
import { expect, test } from './fixtures.js'
import { createE2eInitData } from './telegram.js'

const AUTHORIZATION_ERROR = 'Не удалось авторизоваться. Откройте приложение из Telegram.'

function setRuntimeInitData(runtimeInitData: string) {
  window.__PANTRY_E2E_TELEGRAM_INIT_DATA__ = runtimeInitData
}

function isPantriesResponse(response: { url(): string; request(): { method(): string } }) {
  return (
    response.request().method() === 'GET' &&
    new URL(response.url()).pathname === '/api/pantries'
  )
}

async function expectDefaultPantry(page: Page) {
  const defaultPantry = page.getByRole('link', { name: /Default/ })
  await expect(defaultPantry).toHaveCount(1)
  await expect(defaultPantry).toContainText('Владелец')
  await expect(defaultPantry).toContainText('По умолчанию')
}

async function expectAuthorizationFailure(page: Page, expectedAuthorization?: string) {
  const responsePromise = page.waitForResponse(isPantriesResponse)
  await page.goto('/')
  const response = await responsePromise

  expect(response.status()).toBe(401)
  expect(response.request().headers().authorization).toBe(expectedAuthorization)

  const alert = page.getByRole('alert')
  await expect(alert).toBeVisible()
  await expect(alert.getByText(AUTHORIZATION_ERROR, { exact: true })).toBeVisible()
  await expect(alert.getByRole('button', { name: 'Повторить', exact: true })).toBeVisible()
  await expect(page.getByRole('link')).toHaveCount(0)
}

test('E2E-002: returning user keeps the existing default inventory', async ({
  context,
  initData,
  page,
}) => {
  await context.addInitScript(setRuntimeInitData, initData)
  const firstResponsePromise = page.waitForResponse(isPantriesResponse)
  await page.goto('/')
  const firstResponse = await firstResponsePromise
  const firstPantries = (await firstResponse.json()) as PantryResponse[]

  expect(firstResponse.status()).toBe(200)
  expect(firstResponse.request().headers().authorization).toBe(`tma ${initData}`)
  expect(firstPantries).toEqual([
    expect.objectContaining({ name: 'Default', role: 'OWNER', isDefault: true }),
  ])
  await expectDefaultPantry(page)

  const reloadResponsePromise = page.waitForResponse(isPantriesResponse)
  await page.reload()
  const reloadResponse = await reloadResponsePromise

  expect(reloadResponse.status()).toBe(200)
  expect(reloadResponse.request().headers().authorization).toBe(`tma ${initData}`)
  expect((await reloadResponse.json()) as PantryResponse[]).toEqual(firstPantries)
  await expectDefaultPantry(page)

  await page.close()
  const reopenedPage = await context.newPage()
  const reopenResponsePromise = reopenedPage.waitForResponse(isPantriesResponse)
  await reopenedPage.goto('/')
  const reopenResponse = await reopenResponsePromise

  expect(reopenResponse.status()).toBe(200)
  expect(reopenResponse.request().headers().authorization).toBe(`tma ${initData}`)
  expect((await reopenResponse.json()) as PantryResponse[]).toEqual(firstPantries)
  await expectDefaultPantry(reopenedPage)
})

test('E2E-003: missing Telegram initData is explained to the user', async ({ browser }) => {
  const context = await browser.newContext()
  try {
    const page = await context.newPage()
    await expectAuthorizationFailure(page)
  } finally {
    await context.close()
  }
})

test('E2E-003: invalidly signed Telegram initData is explained to the user', async ({
  browser,
  identity,
}) => {
  const initData = new URLSearchParams(createE2eInitData(identity))
  const validHash = initData.get('hash')
  expect(validHash).not.toBeNull()
  initData.set('hash', `${validHash?.startsWith('0') ? '1' : '0'}${validHash?.slice(1)}`)
  const invalidInitData = initData.toString()
  const context = await browser.newContext()
  try {
    await context.addInitScript(setRuntimeInitData, invalidInitData)
    const page = await context.newPage()
    await expectAuthorizationFailure(page, `tma ${invalidInitData}`)
  } finally {
    await context.close()
  }
})

test('E2E-003: expired signed Telegram initData is explained to the user', async ({
  browser,
  identity,
}) => {
  const expiredInitData = createE2eInitData(identity, {
    authDate: Math.floor(Date.now() / 1_000) - 86_401,
  })
  const context = await browser.newContext()
  try {
    await context.addInitScript(setRuntimeInitData, expiredInitData)
    const page = await context.newPage()
    await expectAuthorizationFailure(page, `tma ${expiredInitData}`)
  } finally {
    await context.close()
  }
})
