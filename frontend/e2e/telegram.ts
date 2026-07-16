import { createHmac } from 'node:crypto'

export const E2E_TELEGRAM_BOT_TOKEN = '123456:e2e-test-token'
const E2E_USER_ID_BASE = 900_000_000

export function createE2eInitData(parallelIndex = 0): string {
  const userId = E2E_USER_ID_BASE + parallelIndex
  const fields = {
    auth_date: String(Math.floor(Date.now() / 1_000)),
    query_id: `AAE-e2e-${parallelIndex}`,
    signature: 'e2e-unchecked-by-backend',
    user: JSON.stringify({
      id: userId,
      first_name: 'E2E',
      username: `pantry_e2e_${parallelIndex}`,
      language_code: 'en',
    }),
  }
  const dataCheckString = Object.entries(fields)
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, value]) => `${key}=${value}`)
    .join('\n')
  const secret = createHmac('sha256', 'WebAppData').update(E2E_TELEGRAM_BOT_TOKEN).digest()
  const hash = createHmac('sha256', secret).update(dataCheckString).digest('hex')

  return new URLSearchParams({ ...fields, hash }).toString()
}
