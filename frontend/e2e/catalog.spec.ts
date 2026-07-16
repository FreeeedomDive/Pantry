import type { PantryResponse } from '../src/api/types.js'
import { createProduct, createStockBatch, setStaple } from './api.js'
import { expect, test } from './fixtures.js'

const dateFormatUtc = new Intl.DateTimeFormat('ru-RU', {
  day: 'numeric',
  month: 'long',
  year: 'numeric',
  timeZone: 'UTC',
})

async function defaultPantry(
  registerUser: () => Promise<PantryResponse[]>,
): Promise<PantryResponse> {
  const pantry = (await registerUser()).find(({ isDefault }) => isDefault)
  if (!pantry) throw new Error('Registered E2E user has no default pantry')
  return pantry
}

test.use({ timezoneId: 'UTC' })

test('E2E-010: empty inventory shows both catalog empty states', async ({
  page,
  registerUser,
}) => {
  const pantry = await defaultPantry(registerUser)

  await page.goto(`/pantries/${pantry.id}`)

  await expect(page.getByRole('heading', { name: pantry.name, exact: true })).toBeVisible()
  await expect(
    page.getByText(
      'Товаров пока нет. Отправьте боту фото чека и подтвердите черновик поступления — товары появятся здесь.',
      { exact: true },
    ),
  ).toBeVisible()

  await page.getByRole('tab', { name: 'Список покупок', exact: true }).click()
  await expect(
    page.getByText('Нет постоянных товаров, которые нужно купить.', { exact: true }),
  ).toBeVisible()
})

test('E2E-011: catalog shows product balances and opens a product', async ({
  api,
  page,
  registerUser,
}) => {
  const pantry = await defaultPantry(registerUser)
  const [coffee, rice, salt, sugar] = await Promise.all([
    createProduct(api, pantry.id, { name: 'Кофе', brand: 'North Star' }),
    createProduct(api, pantry.id, { name: 'Рис', brand: null }),
    createProduct(api, pantry.id, { name: 'Соль', brand: 'Sea Crystal' }),
    createProduct(api, pantry.id, { name: 'Сахар', brand: null }),
  ])
  await Promise.all([
    createStockBatch(api, pantry.id, coffee.id, { quantity: 2, expiresAt: null }),
    createStockBatch(api, pantry.id, coffee.id, { quantity: 3, expiresAt: null }),
    createStockBatch(api, pantry.id, rice.id, { quantity: 1, expiresAt: null }),
  ])

  await page.goto(`/pantries/${pantry.id}`)

  const catalog = page.getByRole('tabpanel', { name: 'Все товары', exact: true })
  const productLinks = catalog.getByRole('link')
  await expect(productLinks).toHaveCount(4)

  const coffeeLink = catalog.getByRole('link', { name: /^Кофе/ })
  const riceLink = catalog.getByRole('link', { name: /^Рис/ })
  const saltLink = catalog.getByRole('link', { name: /^Соль/ })
  const sugarLink = catalog.getByRole('link', { name: /^Сахар/ })
  await expect(coffeeLink).toHaveAccessibleName('Кофе North Star 5 шт')
  await expect(riceLink).toHaveAccessibleName('Рис 1 шт')
  await expect(saltLink).toHaveAccessibleName('Соль Sea Crystal 0 шт')
  await expect(sugarLink).toHaveAccessibleName('Сахар 0 шт')

  const cardTexts = await productLinks.allInnerTexts()
  const cardIndex = (productName: string) => {
    const index = cardTexts.findIndex((text) => text.includes(productName))
    if (index === -1) throw new Error(`Product card not found: ${productName}`)
    return index
  }
  const positiveIndexes = [cardIndex(coffee.name), cardIndex(rice.name)]
  const zeroIndexes = [cardIndex(salt.name), cardIndex(sugar.name)]
  expect(Math.max(...positiveIndexes)).toBeLessThan(Math.min(...zeroIndexes))

  await coffeeLink.click()
  await expect(page).toHaveURL(`/pantries/${pantry.id}/products/${coffee.id}`)
  await expect(page.getByRole('heading', { name: coffee.name, exact: true })).toBeVisible()
})

test('E2E-012: shopping list contains only depleted staple products', async ({
  api,
  page,
  registerUser,
}) => {
  const pantry = await defaultPantry(registerUser)
  const [stapleZero, staplePositive, nonStapleZero, nonStaplePositive] = await Promise.all([
    createProduct(api, pantry.id, { name: 'Постоянный без запаса', brand: null }),
    createProduct(api, pantry.id, { name: 'Постоянный с запасом', brand: null }),
    createProduct(api, pantry.id, { name: 'Обычный без запаса', brand: null }),
    createProduct(api, pantry.id, { name: 'Обычный с запасом', brand: null }),
  ])
  await Promise.all([
    setStaple(api, pantry.id, stapleZero.id, true),
    setStaple(api, pantry.id, staplePositive.id, true),
    setStaple(api, pantry.id, nonStapleZero.id, false),
    setStaple(api, pantry.id, nonStaplePositive.id, false),
    createStockBatch(api, pantry.id, staplePositive.id, { quantity: 2, expiresAt: null }),
    createStockBatch(api, pantry.id, nonStaplePositive.id, { quantity: 3, expiresAt: null }),
  ])

  await page.goto(`/pantries/${pantry.id}`)

  const catalog = page.getByRole('tabpanel', { name: 'Все товары', exact: true })
  await expect(catalog.getByRole('link')).toHaveCount(4)

  await page.getByRole('tab', { name: 'Список покупок', exact: true }).click()
  const shoppingList = page.getByRole('tabpanel', {
    name: 'Список покупок',
    exact: true,
  })
  await expect(shoppingList.getByRole('link')).toHaveCount(1)
  await expect(
    shoppingList.getByRole('link', { name: 'Постоянный без запаса 0 шт', exact: true }),
  ).toBeVisible()
})

test('E2E-013: product page describes stock batches', async ({
  api,
  page,
  registerUser,
}) => {
  const pantry = await defaultPantry(registerUser)
  const product = await createProduct(api, pantry.id, {
    name: 'Йогурт',
    brand: 'Молочная ферма',
  })
  const [expiringBatch, batchWithoutExpiration] = await Promise.all([
    createStockBatch(api, pantry.id, product.id, {
      quantity: 4,
      expiresAt: '2031-06-15',
    }),
    createStockBatch(api, pantry.id, product.id, { quantity: 7, expiresAt: null }),
  ])
  if (!expiringBatch.expiresAt) throw new Error('Expected the stock batch to have an expiration')

  await page.goto(`/pantries/${pantry.id}/products/${product.id}`)

  await expect(page.getByRole('heading', { name: product.name, exact: true })).toBeVisible()
  await expect(page.getByText(product.brand!, { exact: true })).toBeVisible()
  await expect(page.getByText('11 шт', { exact: true })).toBeVisible()
  await expect(page.getByText('4 шт', { exact: true })).toBeVisible()
  await expect(page.getByText('7 шт', { exact: true })).toBeVisible()
  await expect(
    page.getByText(`Годен до ${dateFormatUtc.format(new Date(expiringBatch.expiresAt))}`, {
      exact: true,
    }),
  ).toBeVisible()
  await expect(page.getByText('Срок годности не указан', { exact: true })).toHaveCount(1)

  const purchaseLabels = [expiringBatch, batchWithoutExpiration].map(
    ({ purchasedAt }) => `Куплено ${dateFormatUtc.format(new Date(purchasedAt))}`,
  )
  for (const label of new Set(purchaseLabels)) {
    await expect(page.getByText(label, { exact: true })).toHaveCount(
      purchaseLabels.filter((candidate) => candidate === label).length,
    )
  }
})
