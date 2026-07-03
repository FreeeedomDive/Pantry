import crypto from 'node:crypto'
import { writeFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const botToken = process.argv[2]
if (!botToken) {
  console.error('Usage: node scripts/generate-init-data.mjs <bot-token> [telegram-user-id]')
  process.exit(1)
}
const userId = Number(process.argv[3] ?? 7777777)

const params = {
  auth_date: String(Math.floor(Date.now() / 1000)),
  query_id: 'AAE-dev-local',
  signature: 'unchecked-in-dev',
  user: JSON.stringify({ id: userId, first_name: 'Dev', username: 'dev_local', language_code: 'ru' }),
}

const dataCheckString = Object.keys(params)
  .sort()
  .map((key) => `${key}=${params[key]}`)
  .join('\n')
const secret = crypto.createHmac('sha256', 'WebAppData').update(botToken).digest()
const hash = crypto.createHmac('sha256', secret).update(dataCheckString).digest('hex')
const initData = new URLSearchParams({ ...params, hash }).toString()

const envFile = join(dirname(fileURLToPath(import.meta.url)), '..', '.env.local')
writeFileSync(envFile, `VITE_DEBUG_INIT_DATA="${initData}"\n`)
console.log(`written ${envFile} for telegram user ${userId}`)
