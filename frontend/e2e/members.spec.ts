import type { Locator, Page } from '@playwright/test'
import { expect, test } from './fixtures.js'

function pantryCardByRole(page: Page, role: 'Владелец' | 'Совладелец'): Locator {
  return page
    .getByRole('link')
    .filter({ has: page.getByText(role, { exact: true }) })
}

async function openPantryActions(card: Locator) {
  await card.getByRole('button', { name: 'Действия' }).click()
}

const waitForApiResponse = (page: Page, method: string, path: string) =>
  page.waitForResponse(
    (response) =>
      response.request().method() === method && new URL(response.url()).pathname === `/api${path}`,
  )

test('E2E-023: member sees no owner-only inventory actions', async ({
  createOwnerMemberUsers,
}) => {
  const { identities, member, owner, ownerPantryId } = await createOwnerMemberUsers()
  const memberPage = member.page
  const ownerPage = owner.page

  await ownerPage.goto(`/pantries/${ownerPantryId}/members`)
  await expect(
    ownerPage.getByText(`Telegram ID ${identities.owner.id} (вы)`, { exact: true }),
  ).toBeVisible()
  await expect(
    ownerPage.getByText(`Telegram ID ${identities.member.id}`, { exact: true }),
  ).toBeVisible()

  await memberPage.goto('/')
  const sharedCard = pantryCardByRole(memberPage, 'Совладелец')
  await expect(sharedCard).toBeVisible()
  await expect(sharedCard).toHaveAttribute('href', `/pantries/${ownerPantryId}`)
  await expect(sharedCard.getByText('По умолчанию', { exact: true })).toHaveCount(0)

  await openPantryActions(sharedCard)
  await expect(
    memberPage.getByRole('menuitem', { name: 'Переименовать', exact: true }),
  ).toHaveCount(0)
  await expect(
    memberPage.getByRole('menuitem', {
      name: 'Сделать инвентарём по умолчанию',
      exact: true,
    }),
  ).toBeVisible()
  await expect(
    memberPage.getByRole('menuitem', { name: 'Покинуть', exact: true }),
  ).toBeVisible()
  await memberPage.getByRole('menuitem', { name: 'Участники', exact: true }).click()

  await expect(memberPage).toHaveURL(`/pantries/${ownerPantryId}/members`)
  await expect(
    memberPage.getByText(`Telegram ID ${identities.member.id} (вы)`, { exact: true }),
  ).toBeVisible()
  await expect(
    memberPage.getByText(`Telegram ID ${identities.owner.id}`, { exact: true }),
  ).toBeVisible()
  await expect(memberPage.getByRole('button', { name: 'Пригласить по ссылке' })).toHaveCount(0)
  await expect(memberPage.getByRole('button', { name: 'Исключить', exact: true })).toHaveCount(0)
})

test('E2E-024: owner removes another member', async ({ createOwnerMemberUsers }) => {
  const { identities, member, owner, ownerPantryId } = await createOwnerMemberUsers()
  const memberPage = member.page
  const ownerPage = owner.page
  const membersPath = `/pantries/${ownerPantryId}/members`

  await memberPage.goto(membersPath)
  await expect(
    memberPage.getByText(`Telegram ID ${identities.member.id} (вы)`, { exact: true }),
  ).toBeVisible()

  await ownerPage.goto(membersPath)
  await expect(
    ownerPage.getByText(`Telegram ID ${identities.owner.id} (вы)`, { exact: true }),
  ).toBeVisible()
  await expect(
    ownerPage.getByText(`Telegram ID ${identities.member.id}`, { exact: true }),
  ).toBeVisible()

  const kickButton = ownerPage.getByRole('button', { name: 'Исключить', exact: true })
  await expect(kickButton).toHaveCount(1)
  await kickButton.click()

  const dialog = ownerPage.getByRole('dialog', {
    name: `Исключить пользователя ${identities.member.id}?`,
  })
  await expect(
    dialog.getByText(
      'Пользователь потеряет доступ к товарам и остаткам этого инвентаря. Пригласить его снова можно будет по новой ссылке.',
      { exact: true },
    ),
  ).toBeVisible()

  const kickResponsePromise = ownerPage.waitForResponse(
    (response) =>
      response.request().method() === 'DELETE' &&
      new URL(response.url()).pathname.startsWith(
        `/api/pantries/${ownerPantryId}/members/`,
      ),
  )
  await dialog.getByRole('button', { name: 'Исключить', exact: true }).click()
  expect((await kickResponsePromise).ok()).toBe(true)

  await expect(dialog).toBeHidden()
  await expect(
    ownerPage.getByText(`Telegram ID ${identities.member.id}`, { exact: false }),
  ).toHaveCount(0)
  await expect(
    ownerPage.getByText(`Telegram ID ${identities.owner.id} (вы)`, { exact: true }),
  ).toBeVisible()
  await expect(ownerPage.getByRole('button', { name: 'Исключить', exact: true })).toHaveCount(0)

  await memberPage.reload()
  await expect(memberPage.getByText('Нет доступа к этому инвентарю.', { exact: true })).toBeVisible()
  await expect(
    memberPage.getByText(`Telegram ID ${identities.owner.id}`, { exact: false }),
  ).toHaveCount(0)

  await ownerPage.getByRole('link', { name: '← Инвентари' }).click()
  const ownerCard = pantryCardByRole(ownerPage, 'Владелец')
  await expect(ownerCard).toHaveAttribute('href', `/pantries/${ownerPantryId}`)
  await expect(ownerCard).toBeVisible()
})

test('E2E-025: member leaves a shared inventory', async ({ createOwnerMemberUsers }) => {
  const { identities, member, owner, ownerPantryId } = await createOwnerMemberUsers()
  const memberPage = member.page
  const ownerPage = owner.page
  const membersPath = `/pantries/${ownerPantryId}/members`

  await ownerPage.goto(membersPath)
  await expect(
    ownerPage.getByText(`Telegram ID ${identities.owner.id} (вы)`, { exact: true }),
  ).toBeVisible()
  await expect(
    ownerPage.getByText(`Telegram ID ${identities.member.id}`, { exact: true }),
  ).toBeVisible()

  await memberPage.goto('/')
  const ownCard = pantryCardByRole(memberPage, 'Владелец')
  const sharedCard = pantryCardByRole(memberPage, 'Совладелец')
  await expect(ownCard.getByText('По умолчанию', { exact: true })).toBeVisible()
  await expect(sharedCard.getByText('По умолчанию', { exact: true })).toHaveCount(0)

  await openPantryActions(sharedCard)
  const setDefaultResponsePromise = waitForApiResponse(
    memberPage,
    'POST',
    `/pantries/${ownerPantryId}/default`,
  )
  await memberPage
    .getByRole('menuitem', {
      name: 'Сделать инвентарём по умолчанию',
      exact: true,
    })
    .click()
  expect((await setDefaultResponsePromise).ok()).toBe(true)
  await expect(sharedCard.getByText('По умолчанию', { exact: true })).toBeVisible()
  await expect(ownCard.getByText('По умолчанию', { exact: true })).toHaveCount(0)

  await openPantryActions(sharedCard)
  await memberPage.getByRole('menuitem', { name: 'Покинуть', exact: true }).click()
  const dialog = memberPage.getByRole('dialog', { name: 'Покинуть «Default»?' })
  await expect(
    dialog.getByText(
      'Вы потеряете доступ к товарам и остаткам этого инвентаря. Владелец сможет пригласить вас снова.',
      { exact: true },
    ),
  ).toBeVisible()

  const leaveResponsePromise = waitForApiResponse(
    memberPage,
    'DELETE',
    `/pantries/${ownerPantryId}`,
  )
  await dialog.getByRole('button', { name: 'Покинуть', exact: true }).click()
  expect((await leaveResponsePromise).ok()).toBe(true)

  await expect(dialog).toBeHidden()
  await expect(sharedCard).toHaveCount(0)
  await expect(ownCard).toBeVisible()
  await expect(ownCard.getByText('По умолчанию', { exact: true })).toBeVisible()

  await ownerPage.reload()
  await expect(
    ownerPage.getByText(`Telegram ID ${identities.member.id}`, { exact: false }),
  ).toHaveCount(0)
  await expect(
    ownerPage.getByText(`Telegram ID ${identities.owner.id} (вы)`, { exact: true }),
  ).toBeVisible()

  await ownerPage.getByRole('link', { name: '← Инвентари' }).click()
  await expect(pantryCardByRole(ownerPage, 'Владелец')).toHaveAttribute(
    'href',
    `/pantries/${ownerPantryId}`,
  )
})
