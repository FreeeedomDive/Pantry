import { createHmac } from 'node:crypto'

export const E2E_TELEGRAM_BOT_TOKEN = '123456:e2e-test-token'
const E2E_USER_ID_BASE = 900_000_000

export interface E2eTelegramIdentity {
  id: number
  username: string
  firstName: string
  lastName?: string
  languageCode?: string
  isPremium?: boolean
  allowsWriteToPm?: boolean
  photoUrl?: string
}

export function createE2eTelegramIdentity(index = 0): E2eTelegramIdentity {
  return {
    id: E2E_USER_ID_BASE + index,
    username: `pantry_e2e_${index}`,
    firstName: 'E2E',
    languageCode: 'en',
  }
}

export function createE2eInitData(identity: E2eTelegramIdentity): string {
  const user = {
    id: identity.id,
    first_name: identity.firstName,
    username: identity.username,
    ...(identity.lastName === undefined ? {} : { last_name: identity.lastName }),
    ...(identity.languageCode === undefined ? {} : { language_code: identity.languageCode }),
    ...(identity.isPremium === undefined ? {} : { is_premium: identity.isPremium }),
    ...(identity.allowsWriteToPm === undefined
      ? {}
      : { allows_write_to_pm: identity.allowsWriteToPm }),
    ...(identity.photoUrl === undefined ? {} : { photo_url: identity.photoUrl }),
  }
  const fields = {
    auth_date: String(Math.floor(Date.now() / 1_000)),
    query_id: `AAE-e2e-${identity.id}`,
    signature: 'e2e-unchecked-by-backend',
    user: JSON.stringify(user),
  }
  const dataCheckString = Object.entries(fields)
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, value]) => `${key}=${value}`)
    .join('\n')
  const secret = createHmac('sha256', 'WebAppData').update(E2E_TELEGRAM_BOT_TOKEN).digest()
  const hash = createHmac('sha256', secret).update(dataCheckString).digest('hex')

  return new URLSearchParams({ ...fields, hash }).toString()
}
