import type { Page, Route } from '@playwright/test'
import { createPantry, createProduct } from './api.js'
import { expect, test } from './fixtures.js'

test('E2E-019: application routes survive direct opening and reload', async ({
  api,
  identity,
  page,
  registerUser,
}) => {
  await registerUser()
  const pantry = await createPantry(api, 'Route-safe pantry')
  const product = await createProduct(api, pantry.id, {
    name: 'Route-safe product',
    brand: 'Route-safe brand',
  })

  const routes: Array<{
    path: string
    parentName: string
    parentPath: string
    expectData: () => Promise<void>
  }> = [
    {
      path: `/pantries/${pantry.id}`,
      parentName: '← Инвентари',
      parentPath: '/',
      expectData: async () => {
        await expect(page.getByRole('heading', { name: pantry.name })).toBeVisible()
        await expect(page.getByText(product.name, { exact: true })).toBeVisible()
      },
    },
    {
      path: `/pantries/${pantry.id}/products/${product.id}`,
      parentName: '← Товары',
      parentPath: `/pantries/${pantry.id}`,
      expectData: async () => {
        await expect(page.getByRole('heading', { name: product.name })).toBeVisible()
        await expect(page.getByText(product.brand!, { exact: true })).toBeVisible()
      },
    },
    {
      path: `/pantries/${pantry.id}/members`,
      parentName: '← Инвентари',
      parentPath: '/',
      expectData: async () => {
        await expect(
          page.getByRole('heading', { name: `${pantry.name} — участники` }),
        ).toBeVisible()
        await expect(
          page.getByText(`Telegram ID ${identity.id} (вы)`, { exact: true }),
        ).toBeVisible()
      },
    },
  ]

  for (const route of routes) {
    await test.step(`direct open and reload ${route.path}`, async () => {
      const directResponse = await page.goto(route.path)
      expect(directResponse?.status()).toBe(200)
      await route.expectData()
      await expect(page.getByRole('link', { name: route.parentName })).toHaveAttribute(
        'href',
        route.parentPath,
      )

      const reloadResponse = await page.reload()
      expect(reloadResponse?.status()).toBe(200)
      await route.expectData()
      await expect(page.getByRole('link', { name: route.parentName })).toHaveAttribute(
        'href',
        route.parentPath,
      )
    })
  }
})

test('E2E-020: query failures exhaust automatic retries and recover on manual retry', async ({
  api,
  page,
  registerUser,
}) => {
  await registerUser()
  const pantry = await createPantry(api, 'Retry pantry')
  const product = await createProduct(api, pantry.id, {
    name: 'Real product after retry',
    brand: null,
  })
  const balanceUrl = `**/api/pantries/${pantry.id}/balance`
  const failures: Array<{
    name: string
    expectedRequests: number
    message: string
    status?: number
  }> = [
    {
      name: 'network failure',
      expectedRequests: 2,
      message: 'Не удалось выполнить запрос. Проверьте соединение.',
    },
    {
      name: '5xx response',
      expectedRequests: 2,
      message: 'Temporary server failure',
      status: 503,
    },
    {
      name: '4xx response',
      expectedRequests: 1,
      message: 'Rejected client request',
      status: 400,
    },
  ]

  for (const failure of failures) {
    await test.step(failure.name, async () => {
      let requestCount = 0
      const failBalance = async (route: Route) => {
        requestCount += 1
        if (failure.status === undefined) {
          await route.abort('failed')
          return
        }
        await route.fulfill({
          status: failure.status,
          contentType: 'application/json',
          body: JSON.stringify({ message: failure.message }),
        })
      }

      await page.route(balanceUrl, failBalance)
      await page.goto(`/pantries/${pantry.id}`)

      await expect(page.getByText(failure.message, { exact: true })).toBeVisible()
      expect(requestCount).toBe(failure.expectedRequests)

      await page.unroute(balanceUrl, failBalance)
      await page.getByRole('button', { name: 'Повторить' }).click()
      await expect(page.getByText(product.name, { exact: true })).toBeVisible()
      await expect(page.getByText(failure.message, { exact: true })).toHaveCount(0)
    })
  }
})

test('E2E-021: failed create and rename mutations preserve their forms and recover', async ({
  page,
  registerUser,
}) => {
  await registerUser()
  await page.goto('/')
  await expect(page.getByRole('heading', { name: 'Мои инвентари' })).toBeVisible()

  const createdName = 'Recovered create'
  const createFailureMessage = 'Create failed temporarily'
  await page.getByRole('button', { name: 'Создать' }).click()
  const createDialog = page.getByRole('dialog', { name: 'Новый инвентарь' })
  const createName = createDialog.getByRole('textbox', { name: 'Название' })
  await createName.fill(createdName)

  let createRequests = 0
  const failCreate = async (route: Route) => {
    createRequests += 1
    await route.fulfill({
      status: 503,
      contentType: 'application/json',
      body: JSON.stringify({ message: createFailureMessage }),
    })
  }
  const createUrl = '**/api/pantries'
  await page.route(createUrl, failCreate)
  await createDialog.getByRole('button', { name: 'Создать' }).click()

  await expect(page.getByText(createFailureMessage, { exact: true })).toBeVisible()
  await expect(page.getByText('Не получилось', { exact: true })).toBeVisible()
  await expect(createDialog).toBeVisible()
  await expect(createName).toHaveValue(createdName)
  expect(createRequests).toBe(1)

  await page.unroute(createUrl, failCreate)
  await createDialog.getByRole('button', { name: 'Создать' }).click()
  await expect(page).toHaveURL(/\/pantries\/[^/]+$/)
  await expect(page.getByRole('heading', { name: createdName })).toBeVisible()

  const pantryId = new URL(page.url()).pathname.split('/').at(-1)
  if (!pantryId) throw new Error('Created pantry URL did not contain an id')

  await page.goto('/')
  const pantryCard = page.getByRole('link', { name: new RegExp(createdName) })
  await pantryCard.getByRole('button', { name: 'Действия' }).click()
  await page.getByRole('menuitem', { name: 'Переименовать' }).click()

  const renamedName = 'Recovered rename'
  const renameFailureMessage = 'Rename failed temporarily'
  const renameDialog = page.getByRole('dialog', { name: 'Переименовать инвентарь' })
  const renameName = renameDialog.getByRole('textbox', { name: 'Название' })
  await renameName.fill(renamedName)

  let renameRequests = 0
  const failRename = async (route: Route) => {
    renameRequests += 1
    await route.fulfill({
      status: 503,
      contentType: 'application/json',
      body: JSON.stringify({ message: renameFailureMessage }),
    })
  }
  const renameUrl = `**/api/pantries/${pantryId}`
  await page.route(renameUrl, failRename)
  await renameDialog.getByRole('button', { name: 'Сохранить' }).click()

  await expect(page.getByText(renameFailureMessage, { exact: true })).toBeVisible()
  await expect(page.getByText('Не получилось', { exact: true })).toBeVisible()
  await expect(renameDialog).toBeVisible()
  await expect(renameName).toHaveValue(renamedName)
  expect(renameRequests).toBe(1)

  await page.unroute(renameUrl, failRename)
  await renameDialog.getByRole('button', { name: 'Сохранить' }).click()
  await expect(renameDialog).toBeHidden()
  await expect(page.getByText(renamedName, { exact: true })).toBeVisible()

  await page.reload()
  await expect(page.getByText(renamedName, { exact: true })).toBeVisible()
  await expect(page.getByText(createdName, { exact: true })).toHaveCount(0)
})

test('E2E-022: real backend denies another user’s pantry routes without leaking data', async ({
  createAdditionalUser,
  page,
  registerUser,
}) => {
  await registerUser()
  const otherUser = await createAdditionalUser()
  await otherUser.listPantries()
  const secretPantry = await createPantry(otherUser.api, 'Other user secret pantry')
  const secretProduct = await createProduct(otherUser.api, secretPantry.id, {
    name: 'Other user secret product',
    brand: 'Other user secret brand',
  })
  const deniedMessage = 'Нет доступа к этому инвентарю.'

  const expectNoProtectedData = async (browserPage: Page) => {
    await expect(browserPage.getByText(secretPantry.name, { exact: true })).toHaveCount(0)
    await expect(browserPage.getByText(secretProduct.name, { exact: true })).toHaveCount(0)
    await expect(browserPage.getByText(secretProduct.brand!, { exact: true })).toHaveCount(0)
    await expect(
      browserPage.getByText(`Telegram ID ${otherUser.identity.id}`, { exact: false }),
    ).toHaveCount(0)
  }

  const protectedRoutes = [
    {
      path: `/pantries/${secretPantry.id}`,
      endpoint: `/api/pantries/${secretPantry.id}/balance`,
    },
    {
      path: `/pantries/${secretPantry.id}/products/${secretProduct.id}`,
      endpoint: `/api/pantries/${secretPantry.id}/products/${secretProduct.id}/stock`,
    },
    {
      path: `/pantries/${secretPantry.id}/members`,
      endpoint: `/api/pantries/${secretPantry.id}/members`,
    },
  ]

  for (const protectedRoute of protectedRoutes) {
    await test.step(`deny ${protectedRoute.path}`, async () => {
      const deniedResponse = page.waitForResponse(
        (response) =>
          response.url().endsWith(protectedRoute.endpoint) &&
          response.request().method() === 'GET',
      )
      await page.goto(protectedRoute.path)

      expect((await deniedResponse).status()).toBe(403)
      await expect(page.getByText(deniedMessage, { exact: true })).toBeVisible()
      await expectNoProtectedData(page)
    })
  }
})
