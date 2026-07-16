import type { APIRequestContext, BrowserContext, Page } from '@playwright/test'
import type { PantryResponse } from '../src/api/types.js'
import { addPantryMember } from './api.js'
import type { E2eTelegramIdentity } from './telegram.js'

export interface E2eUser {
  identity: E2eTelegramIdentity
  api: APIRequestContext
  context: BrowserContext
  page: Page
  listPantries: () => Promise<PantryResponse[]>
}

export interface OwnerMemberUsers {
  ownerPantryId: string
  owner: E2eUser
  member: E2eUser
  outsider?: E2eUser
  identities: {
    owner: E2eTelegramIdentity
    member: E2eTelegramIdentity
    outsider?: E2eTelegramIdentity
  }
}

export interface OwnerMemberUsersOptions {
  includeOutsider?: boolean
}

function requirePantry(
  description: string,
  pantries: PantryResponse[],
  predicate: (pantry: PantryResponse) => boolean,
): PantryResponse {
  const pantry = pantries.find(predicate)
  if (!pantry) {
    throw new Error(`${description}. Received pantries: ${JSON.stringify(pantries)}`)
  }
  return pantry
}

export async function setupOwnerMemberUsers(
  createUser: () => Promise<E2eUser>,
  options: OwnerMemberUsersOptions = {},
): Promise<OwnerMemberUsers> {
  const owner = await createUser()
  const ownerPantry = requirePantry(
    'Owner Default pantry with OWNER role was not created',
    await owner.listPantries(),
    (pantry) => pantry.name === 'Default' && pantry.role === 'OWNER' && pantry.isDefault,
  )

  const member = await createUser()
  const memberDefault = requirePantry(
    'Member Default pantry with OWNER role was not created',
    await member.listPantries(),
    (pantry) => pantry.name === 'Default' && pantry.role === 'OWNER' && pantry.isDefault,
  )

  await addPantryMember(owner.api, ownerPantry.id, { telegramUserId: member.identity.id })

  requirePantry(
    'Owner no longer sees the shared pantry with OWNER role',
    await owner.listPantries(),
    (pantry) => pantry.id === ownerPantry.id && pantry.role === 'OWNER',
  )
  const memberPantries = await member.listPantries()
  requirePantry(
    'Member no longer sees their own Default pantry',
    memberPantries,
    (pantry) => pantry.id === memberDefault.id && pantry.role === 'OWNER' && pantry.isDefault,
  )
  requirePantry(
    'Member does not see the shared pantry with MEMBER role',
    memberPantries,
    (pantry) => pantry.id === ownerPantry.id && pantry.role === 'MEMBER',
  )

  const outsider = options.includeOutsider ? await createUser() : undefined
  if (outsider) {
    const outsiderPantries = await outsider.listPantries()
    requirePantry(
      'Outsider Default pantry with OWNER role was not created',
      outsiderPantries,
      (pantry) => pantry.name === 'Default' && pantry.role === 'OWNER' && pantry.isDefault,
    )
    if (outsiderPantries.some((pantry) => pantry.id === ownerPantry.id)) {
      throw new Error(
        `Outsider unexpectedly sees owner pantry ${ownerPantry.id}. Received pantries: ${JSON.stringify(outsiderPantries)}`,
      )
    }
  }

  return {
    ownerPantryId: ownerPantry.id,
    owner,
    member,
    outsider,
    identities: {
      owner: owner.identity,
      member: member.identity,
      outsider: outsider?.identity,
    },
  }
}
