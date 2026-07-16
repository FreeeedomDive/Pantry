import { expect, test as base } from '@playwright/test'
import type { APIRequestContext, Page } from '@playwright/test'
import {
  createE2eInitData,
  createE2eTelegramIdentity,
  type E2eTelegramIdentity,
} from './telegram.js'

declare global {
  interface Window {
    __PANTRY_E2E_TELEGRAM_INIT_DATA__?: string
  }
}

type E2eFixtures = {
  identity: E2eTelegramIdentity
  api: APIRequestContext
  page: Page
}

let identitySequence = 0

function identityIndex(workerIndex: number, retry: number): number {
  const sequence = identitySequence++
  return workerIndex * 1_000_000 + retry * 100_000 + sequence
}

export const test = base.extend<E2eFixtures>({
  identity: async ({}, use, testInfo) => {
    await use(createE2eTelegramIdentity(identityIndex(testInfo.workerIndex, testInfo.retry)))
  },

  api: async ({ baseURL, identity, playwright }, use) => {
    const api = await playwright.request.newContext({
      baseURL,
      extraHTTPHeaders: { Authorization: `tma ${createE2eInitData(identity)}` },
    })
    await use(api)
    await api.dispose()
  },

  page: async ({ page, identity }, use) => {
    const initData = createE2eInitData(identity)
    await page.addInitScript((runtimeInitData) => {
      window.__PANTRY_E2E_TELEGRAM_INIT_DATA__ = runtimeInitData
    }, initData)
    await use(page)
  },
})

export { expect }
