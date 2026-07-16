import type { Locator, Page } from '@playwright/test'
import { createProduct, createStockBatch, setStaple } from './api.js'
import { expect, test } from './fixtures.js'

const productPath = (pantryId: string, productId: string) =>
  `/pantries/${pantryId}/products/${productId}`

const waitForApiResponse = (page: Page, method: string, path: string) =>
  page.waitForResponse(
    (response) =>
      response.request().method() === method && new URL(response.url()).pathname === `/api${path}`,
  )

async function openProductAction(page: Page, name: string): Promise<Locator> {
  await page.getByRole('button', { name: 'Действия' }).click()
  return page.getByRole('menuitem', { name, exact: true })
}

async function openRenameDialog(page: Page): Promise<Locator> {
  await (await openProductAction(page, 'Переименовать')).click()
  return page.getByRole('dialog', { name: 'Переименовать товар' })
}

function batchWithoutExpiry(page: Page): Locator {
  return page
    .getByText('Срок годности не указан', { exact: true })
    .locator('..')
    .locator('..')
    .locator('..')
}

async function swipeBatchLeft(page: Page, batch: Locator) {
  const box = await batch.boundingBox()
  if (!box) throw new Error('Stock batch is not visible')

  const startX = box.x + box.width * 0.75
  const y = box.y + box.height / 2
  await page.mouse.move(startX, y)
  await page.mouse.down()
  await page.mouse.move(startX - 96, y, { steps: 6 })
  await page.mouse.up()
}

test('E2E-014: user edits a product name and brand', async ({ api, page, registerUser }) => {
  const [pantry] = await registerUser()
  const product = await createProduct(api, pantry.id, {
    name: 'Rice',
    brand: null,
  })
  const renamePath = `${productPath(pantry.id, product.id)}/name`

  await page.goto(productPath(pantry.id, product.id))
  await expect(page.getByRole('heading', { name: 'Rice' })).toBeVisible()

  let dialog = await openRenameDialog(page)
  const nameInput = dialog.getByRole('textbox', { name: 'Название' })
  const brandInput = dialog.getByRole('textbox', { name: 'Бренд' })
  const saveButton = dialog.getByRole('button', { name: 'Сохранить' })

  await expect(saveButton).toBeDisabled()
  await nameInput.clear()
  await expect(saveButton).toBeDisabled()
  await nameInput.fill('  Brown rice  ')
  await brandInput.fill('  North Mill  ')

  let responsePromise = waitForApiResponse(page, 'PATCH', renamePath)
  await saveButton.click()
  expect((await responsePromise).ok()).toBe(true)
  await expect(dialog).toBeHidden()
  await expect(page.getByRole('heading', { name: 'Brown rice' })).toBeVisible()
  await expect(page.getByText('North Mill', { exact: true })).toBeVisible()

  dialog = await openRenameDialog(page)
  await expect(dialog.getByRole('textbox', { name: 'Название' })).toHaveValue('Brown rice')
  await expect(dialog.getByRole('textbox', { name: 'Бренд' })).toHaveValue('North Mill')
  await dialog.getByRole('textbox', { name: 'Бренд' }).fill('  South Mill  ')

  responsePromise = waitForApiResponse(page, 'PATCH', renamePath)
  await dialog.getByRole('button', { name: 'Сохранить' }).click()
  expect((await responsePromise).ok()).toBe(true)
  await expect(dialog).toBeHidden()
  await expect(page.getByText('North Mill', { exact: true })).toHaveCount(0)
  await expect(page.getByText('South Mill', { exact: true })).toBeVisible()

  dialog = await openRenameDialog(page)
  await dialog.getByRole('textbox', { name: 'Бренд' }).fill('   ')

  responsePromise = waitForApiResponse(page, 'PATCH', renamePath)
  await dialog.getByRole('button', { name: 'Сохранить' }).click()
  expect((await responsePromise).ok()).toBe(true)
  await expect(dialog).toBeHidden()
  await expect(page.getByText('South Mill', { exact: true })).toHaveCount(0)
})

test('E2E-015: user toggles a depleted product in the shopping list', async ({
  api,
  page,
  registerUser,
}) => {
  const [pantry] = await registerUser()
  const product = await createProduct(api, pantry.id, {
    name: 'Shopping list oats',
    brand: null,
  })
  const staplePath = `${productPath(pantry.id, product.id)}/staple`

  await page.goto(productPath(pantry.id, product.id))
  const makeStaple = await openProductAction(page, 'Сделать постоянным')
  let responsePromise = waitForApiResponse(page, 'PATCH', staplePath)
  await makeStaple.click()
  expect((await responsePromise).ok()).toBe(true)

  await page.getByRole('link', { name: '← Товары' }).click()
  await page.getByRole('tab', { name: 'Список покупок' }).click()
  const shoppingList = page.getByRole('tabpanel')
  await expect(shoppingList.getByText('Shopping list oats', { exact: true })).toBeVisible()
  await shoppingList.getByText('Shopping list oats', { exact: true }).click()

  const removeStaple = await openProductAction(page, 'Убрать из постоянных')
  responsePromise = waitForApiResponse(page, 'PATCH', staplePath)
  await removeStaple.click()
  expect((await responsePromise).ok()).toBe(true)

  await page.getByRole('link', { name: '← Товары' }).click()
  await page.getByRole('tab', { name: 'Список покупок' }).click()
  await expect(page.getByRole('tabpanel').getByText('Shopping list oats', { exact: true })).toHaveCount(
    0,
  )
  await expect(
    page.getByText('Нет постоянных товаров, которые нужно купить.', { exact: true }),
  ).toBeVisible()
})

test('E2E-016: swipe writes off exactly one item from a stock batch', async ({
  api,
  page,
  registerUser,
}) => {
  const [pantry] = await registerUser()
  const product = await createProduct(api, pantry.id, {
    name: 'Swipe coffee',
    brand: null,
  })
  const stockItem = await createStockBatch(api, pantry.id, product.id, {
    quantity: 3,
    expiresAt: null,
  })

  await page.goto(productPath(pantry.id, product.id))
  await expect(page.getByText('3 шт', { exact: true })).toHaveCount(2)
  const batch = batchWithoutExpiry(page)
  await expect(batch).toBeVisible()

  const responsePromise = waitForApiResponse(
    page,
    'POST',
    `/pantries/${pantry.id}/stock-items/${stockItem.id}/write-off`,
  )
  await swipeBatchLeft(page, batch)
  const response = await responsePromise
  expect(response.ok()).toBe(true)
  expect(response.request().postDataJSON()).toEqual({ quantity: 1 })

  await expect(page.getByText('2 шт', { exact: true })).toHaveCount(2)
  await expect(batchWithoutExpiry(page)).toBeVisible()
})

test('E2E-017: writing off the last item depletes but keeps the product', async ({
  api,
  page,
  registerUser,
}) => {
  const [pantry] = await registerUser()
  const product = await createProduct(api, pantry.id, {
    name: 'Last staple pasta',
    brand: null,
  })
  await setStaple(api, pantry.id, product.id, true)
  const stockItem = await createStockBatch(api, pantry.id, product.id, {
    quantity: 1,
    expiresAt: null,
  })

  await page.goto(productPath(pantry.id, product.id))
  const responsePromise = waitForApiResponse(
    page,
    'POST',
    `/pantries/${pantry.id}/stock-items/${stockItem.id}/write-off`,
  )
  await swipeBatchLeft(page, batchWithoutExpiry(page))
  const response = await responsePromise
  expect(response.ok()).toBe(true)
  expect(response.request().postDataJSON()).toEqual({ quantity: 1 })

  await expect(page.getByText('0 шт', { exact: true })).toBeVisible()
  await expect(page.getByText('Срок годности не указан', { exact: true })).toHaveCount(0)
  await expect(
    page.getByText('Партий нет — весь запас израсходован.', { exact: true }),
  ).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Last staple pasta' })).toBeVisible()

  await page.getByRole('link', { name: '← Товары' }).click()
  await expect(
    page.getByRole('tabpanel').getByText('Last staple pasta', { exact: true }),
  ).toBeVisible()
  await page.getByRole('tab', { name: 'Список покупок' }).click()
  await expect(
    page.getByRole('tabpanel').getByText('Last staple pasta', { exact: true }),
  ).toBeVisible()
})

test('E2E-018: user deletes a product with its stock', async ({ api, page, registerUser }) => {
  const [pantry] = await registerUser()
  const product = await createProduct(api, pantry.id, {
    name: 'Delete stocked lentils',
    brand: 'Field Jar',
  })
  await createStockBatch(api, pantry.id, product.id, {
    quantity: 2,
    expiresAt: null,
  })

  await page.goto(productPath(pantry.id, product.id))
  await (await openProductAction(page, 'Удалить товар')).click()
  const dialog = page.getByRole('dialog', { name: 'Удалить «Delete stocked lentils»?' })
  await expect(
    dialog.getByText(/Товар и все его партии будут удалены навсегда/),
  ).toBeVisible()
  await expect(dialog.getByText(/Действие необратимо/)).toBeVisible()

  const responsePromise = waitForApiResponse(page, 'DELETE', productPath(pantry.id, product.id))
  await dialog.getByRole('button', { name: 'Удалить', exact: true }).click()
  expect((await responsePromise).ok()).toBe(true)

  await expect(page).toHaveURL(new RegExp(`/pantries/${pantry.id}$`))
  await expect(
    page.getByRole('tabpanel').getByText('Delete stocked lentils', { exact: true }),
  ).toHaveCount(0)
})
