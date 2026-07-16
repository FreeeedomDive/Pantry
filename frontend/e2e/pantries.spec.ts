import type { Page } from '@playwright/test'
import { createPantry } from './api.js'
import { expect, test } from './fixtures.js'

function pantryCard(page: Page, name: string) {
  return page
    .getByRole('link')
    .filter({ has: page.getByText(name, { exact: true }) })
}

async function openPantryActions(page: Page, name: string) {
  await pantryCard(page, name).getByRole('button', { name: 'Действия' }).click()
}

test('E2E-004: owner creates a named inventory', async ({ page, registerUser }) => {
  await registerUser()
  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'Мои инвентари' })).toBeVisible()
  await page.getByRole('button', { name: 'Создать' }).click()

  const dialog = page.getByRole('dialog', { name: 'Новый инвентарь' })
  const nameInput = dialog.getByRole('textbox', { name: 'Название' })
  const createButton = dialog.getByRole('button', { name: 'Создать' })

  await expect(createButton).toBeDisabled()
  await nameInput.fill('   ')
  await expect(createButton).toBeDisabled()

  await nameInput.fill('  Запасы на даче  ')
  await expect(createButton).toBeEnabled()
  await createButton.click()

  await expect(page).toHaveURL(/\/pantries\/[^/?#]+$/)
  await expect(page.getByRole('heading', { name: 'Запасы на даче' })).toBeVisible()

  await page.getByRole('link', { name: '← Инвентари' }).click()
  await expect(page).toHaveURL('/')

  const defaultCard = pantryCard(page, 'Default')
  const createdCard = pantryCard(page, 'Запасы на даче')
  await expect(defaultCard).toBeVisible()
  await expect(createdCard).toBeVisible()
  await expect(defaultCard.getByText('По умолчанию', { exact: true })).toBeVisible()
  await expect(createdCard.getByText('По умолчанию', { exact: true })).toHaveCount(0)
})

test('E2E-005: owner renames an inventory', async ({ api, page, registerUser }) => {
  await registerUser()
  const originalName = 'Кладовая'
  const renamedName = 'Домашние запасы'
  await createPantry(api, originalName)
  await page.goto('/')

  await openPantryActions(page, originalName)
  await page.getByRole('menuitem', { name: 'Переименовать', exact: true }).click()

  const dialog = page.getByRole('dialog', { name: 'Переименовать инвентарь' })
  const nameInput = dialog.getByRole('textbox', { name: 'Название' })
  const saveButton = dialog.getByRole('button', { name: 'Сохранить' })

  await expect(nameInput).toHaveValue(originalName)
  await expect(saveButton).toBeDisabled()
  await nameInput.fill('   ')
  await expect(saveButton).toBeDisabled()
  await nameInput.fill(`  ${originalName}  `)
  await expect(saveButton).toBeDisabled()

  await nameInput.fill(`  ${renamedName}  `)
  await expect(saveButton).toBeEnabled()
  await saveButton.click()

  await expect(dialog).toBeHidden()
  await expect(pantryCard(page, renamedName)).toBeVisible()
  await expect(page.getByText(originalName, { exact: true })).toHaveCount(0)
})

test('E2E-006: user changes the default inventory', async ({ api, page, registerUser }) => {
  await registerUser()
  const selectedName = 'Основной склад'
  await createPantry(api, selectedName)
  await page.goto('/')

  const defaultCard = pantryCard(page, 'Default')
  const selectedCard = pantryCard(page, selectedName)
  await expect(defaultCard.getByText('По умолчанию', { exact: true })).toBeVisible()
  await expect(selectedCard.getByText('По умолчанию', { exact: true })).toHaveCount(0)

  await openPantryActions(page, selectedName)
  await page
    .getByRole('menuitem', {
      name: 'Сделать инвентарём по умолчанию',
      exact: true,
    })
    .click()

  await expect(selectedCard.getByText('По умолчанию', { exact: true })).toBeVisible()
  await expect(defaultCard.getByText('По умолчанию', { exact: true })).toHaveCount(0)

  await openPantryActions(page, selectedName)
  await expect(page.getByRole('menuitem', { name: 'Переименовать', exact: true })).toBeVisible()
  await expect(
    page.getByRole('menuitem', {
      name: 'Сделать инвентарём по умолчанию',
      exact: true,
    }),
  ).toHaveCount(0)
})

test('E2E-007: owner cannot delete the last owned inventory', async ({ page, registerUser }) => {
  const [defaultPantry] = await registerUser()
  await page.goto('/')

  await openPantryActions(page, defaultPantry.name)
  await page.getByRole('menuitem', { name: 'Удалить', exact: true }).click()

  const dialog = page.getByRole('dialog', { name: `Удалить «${defaultPantry.name}»?` })
  await expect(dialog.getByText(/Действие необратимо\./)).toBeVisible()

  const deleteResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'DELETE' &&
      response.url().endsWith(`/api/pantries/${defaultPantry.id}`),
  )
  await dialog.getByRole('button', { name: 'Удалить', exact: true }).click()
  const deleteResponse = await deleteResponsePromise

  expect(deleteResponse.status()).toBe(409)
  await expect(
    page.getByText(
      'Нельзя удалить последний инвентарь, владельцем которого вы являетесь — сначала создайте другой.',
      { exact: true },
    ),
  ).toBeVisible()
  await expect(dialog).toBeVisible()
  await expect(pantryCard(page, defaultPantry.name)).toBeVisible()
})

test('E2E-008: owner deletes an additional inventory', async ({ api, page, registerUser }) => {
  await registerUser()
  const additionalPantry = await createPantry(api, 'Временный склад')
  await page.goto('/')

  await openPantryActions(page, additionalPantry.name)
  await page.getByRole('menuitem', { name: 'Удалить', exact: true }).click()

  const dialog = page.getByRole('dialog', {
    name: `Удалить «${additionalPantry.name}»?`,
  })
  await expect(dialog.getByText(/Действие необратимо\./)).toBeVisible()

  const deleteResponsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'DELETE' &&
      response.url().endsWith(`/api/pantries/${additionalPantry.id}`),
  )
  await dialog.getByRole('button', { name: 'Удалить', exact: true }).click()
  const deleteResponse = await deleteResponsePromise

  expect(deleteResponse.ok()).toBe(true)
  await expect(dialog).toBeHidden()
  await expect(pantryCard(page, additionalPantry.name)).toHaveCount(0)
  await expect(pantryCard(page, 'Default')).toBeVisible()
})

test('E2E-009: owner opens the inventory member list', async ({ identity, page, registerUser }) => {
  const [defaultPantry] = await registerUser()
  await page.goto('/')

  await openPantryActions(page, defaultPantry.name)
  await page.getByRole('menuitem', { name: 'Участники', exact: true }).click()

  await expect(page).toHaveURL(`/pantries/${defaultPantry.id}/members`)
  await expect(
    page.getByRole('heading', { name: `${defaultPantry.name} — участники` }),
  ).toBeVisible()
  await expect(
    page.getByText(`Telegram ID ${identity.id} (вы)`, { exact: true }),
  ).toBeVisible()
  await expect(page.getByText('Владелец', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Пригласить по ссылке' })).toBeEnabled()
  await expect(page.getByRole('button', { name: 'Скопировать' })).toHaveCount(0)
  await expect(page.getByRole('button', { name: 'Поделиться' })).toHaveCount(0)
})
