import { expect, test as base } from '@playwright/test'
import { randomUUID } from 'node:crypto'
import type { APIRequestContext, BrowserContext, Page } from '@playwright/test'
import type { PantryResponse } from '../src/api/types.js'
import { listPantries } from './api.js'
import {
  createE2eInitData,
  createE2eTelegramIdentity,
  type E2eTelegramIdentity,
} from './telegram.js'
import {
  setupOwnerMemberUsers,
  type E2eUser,
  type OwnerMemberUsers,
  type OwnerMemberUsersOptions,
} from './users.js'

declare global {
  interface Window {
    __PANTRY_E2E_TELEGRAM_INIT_DATA__?: string
  }
}

type E2eFixtures = {
  identity: E2eTelegramIdentity
  initData: string
  api: APIRequestContext
  registerUser: () => Promise<PantryResponse[]>
  createAdditionalUser: () => Promise<AdditionalUser>
  createOwnerMemberUsers: (options?: OwnerMemberUsersOptions) => Promise<OwnerMemberUsers>
  page: Page
}

export type AdditionalUser = E2eUser

const runId = randomUUID()

function setRuntimeInitData(runtimeInitData: string) {
  window.__PANTRY_E2E_TELEGRAM_INIT_DATA__ = runtimeInitData
}

export const test = base.extend<E2eFixtures>({
  // The identity has no fixture dependencies; Playwright still requires the first parameter.
  // eslint-disable-next-line no-empty-pattern
  identity: async ({}, fixtureUse, testInfo) => {
    const attemptKey = [
      runId,
      testInfo.workerIndex,
      testInfo.project.name,
      testInfo.testId,
      testInfo.retry,
    ].join(':')
    await fixtureUse(createE2eTelegramIdentity(attemptKey))
  },

  initData: async ({ identity }, fixtureUse) => {
    await fixtureUse(createE2eInitData(identity))
  },

  api: async ({ baseURL, initData, playwright }, fixtureUse) => {
    const api = await playwright.request.newContext({
      baseURL,
      extraHTTPHeaders: { Authorization: `tma ${initData}` },
    })
    try {
      await fixtureUse(api)
    } finally {
      await api.dispose()
    }
  },

  registerUser: async ({ api }, fixtureUse) => {
    await fixtureUse(() => listPantries(api))
  },

  createAdditionalUser: async (
    { baseURL, browser, playwright },
    fixtureUse,
    testInfo,
  ) => {
    const users: AdditionalUser[] = []
    let sequence = 0

    try {
      await fixtureUse(async () => {
        const identity = createE2eTelegramIdentity(
          [
            runId,
            testInfo.workerIndex,
            testInfo.project.name,
            testInfo.testId,
            testInfo.retry,
            'additional',
            sequence++,
          ].join(':'),
        )
        const initData = createE2eInitData(identity)
        const api = await playwright.request.newContext({
          baseURL,
          extraHTTPHeaders: { Authorization: `tma ${initData}` },
        })
        let context: BrowserContext | undefined

        try {
          context = await browser.newContext()
          await context.addInitScript(setRuntimeInitData, initData)
          const page = await context.newPage()
          const user: AdditionalUser = {
            identity,
            api,
            context,
            page,
            listPantries: () => listPantries(api),
          }
          users.push(user)
          return user
        } catch (error) {
          await Promise.all([context?.close(), api.dispose()])
          throw error
        }
      })
    } finally {
      await Promise.all(
        users.map(({ api, context }) => Promise.all([context.close(), api.dispose()])),
      )
    }
  },

  createOwnerMemberUsers: async ({ createAdditionalUser }, fixtureUse) => {
    await fixtureUse((options) => setupOwnerMemberUsers(createAdditionalUser, options))
  },

  page: async ({ page, initData }, fixtureUse) => {
    await page.addInitScript(setRuntimeInitData, initData)
    await fixtureUse(page)
  },
})

export { expect }
