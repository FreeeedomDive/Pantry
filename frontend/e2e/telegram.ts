import { createHash, createHmac, randomBytes } from 'node:crypto'

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

export function createE2eTelegramIdentity(key: string): E2eTelegramIdentity {
  const digest = createHash('sha256').update(key).digest()
  const identityIndex = digest.readUIntBE(0, 6)
  const usernameSuffix = digest.subarray(0, 8).toString('hex')

  return {
    id: E2E_USER_ID_BASE + identityIndex,
    username: `pantry_e2e_${usernameSuffix}`,
    firstName: 'E2E',
    languageCode: 'en',
  }
}

export function createE2eInitData(
  identity: E2eTelegramIdentity,
  { authDate = Math.floor(Date.now() / 1_000) }: { authDate?: number } = {},
): string {
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
    auth_date: String(authDate),
    query_id: `AAE-e2e-${identity.id}-${randomBytes(8).toString('hex')}`,
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
