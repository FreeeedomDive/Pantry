import { expect, test } from './fixtures.js'

type ShareAction = {
  source: 'telegram' | 'window.open'
  value: string
  target?: string
}

const SHARE_TEXT = 'Присоединяйся к моему инвентарю в Pantry'
const TELEGRAM_HOST = /^(?:t\.me|telegram\.me|(?:.+\.)?telegram\.org)$/
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i

function observeShareActions() {
  const actions: ShareAction[] = []
  const observedWindow = window as typeof window & {
    __PANTRY_E2E_SHARE_ACTIONS__: ShareAction[]
    TelegramWebviewProxy?: {
      postEvent?: (eventType: string, eventData?: string) => void
    }
  }
  const existingProxy = observedWindow.TelegramWebviewProxy

  observedWindow.__PANTRY_E2E_SHARE_ACTIONS__ = actions
  observedWindow.TelegramWebviewProxy = {
    ...existingProxy,
    postEvent(eventType, eventData) {
      if (eventType === 'web_app_open_tg_link') {
        const data = eventData ? (JSON.parse(eventData) as { path_full?: unknown }) : {}
        actions.push({
          source: 'telegram',
          value: typeof data.path_full === 'string' ? data.path_full : '',
        })
      }
      existingProxy?.postEvent?.(eventType, eventData)
    },
  }
  window.open = (url, target) => {
    actions.push({ source: 'window.open', value: String(url ?? ''), target })
    return null
  }
}

test('E2E-026: owner creates and shares an invitation link', async ({
  page,
  registerUser,
}) => {
  const unexpectedTelegramRequests: string[] = []
  await page.context().route(
    (url) => TELEGRAM_HOST.test(url.hostname),
    async (route) => {
      unexpectedTelegramRequests.push(route.request().url())
      await route.abort('blockedbyclient')
    },
  )
  await page.addInitScript(observeShareActions)

  const [pantry] = await registerUser()
  await page.goto(`/pantries/${pantry.id}/members`)
  await expect(
    page.getByRole('heading', { name: `${pantry.name} — участники` }),
  ).toBeVisible()

  await page.getByRole('button', { name: 'Пригласить по ссылке' }).click()

  const linkText = page.getByText(/^https:\/\/t\.me\/pantry_e2e\?start=[0-9a-f-]+$/i)
  await expect(linkText).toBeVisible()
  const inviteLink = (await linkText.textContent())?.trim()
  if (!inviteLink) throw new Error('The generated invitation link is empty')

  const inviteUrl = new URL(inviteLink)
  const token = inviteUrl.searchParams.get('start')
  expect(token).toMatch(UUID)
  expect(inviteLink).toBe(`https://t.me/pantry_e2e?start=${token}`)
  expect([...inviteUrl.searchParams.keys()]).toEqual(['start'])
  await expect(
    page.getByText(
      /^Ссылка действует до \d{1,2} [а-я]+ \d{4} г\. — получатель нажмёт Start у бота и попадёт в инвентарь\.$/,
    ),
  ).toBeVisible()

  let canReadClipboard = true
  try {
    await page.context().grantPermissions(['clipboard-read', 'clipboard-write'], {
      origin: new URL(page.url()).origin,
    })
  } catch {
    canReadClipboard = false
  }

  await page.getByRole('button', { name: 'Скопировать' }).click()
  await expect(page.getByRole('button', { name: 'Скопировано' })).toBeVisible()
  if (canReadClipboard) {
    const clipboardText = await page.evaluate(() => navigator.clipboard.readText())
    expect(clipboardText).toBe(inviteLink)
  }

  await page.getByRole('button', { name: 'Поделиться' }).click()
  const shareActions = await page.evaluate(() => {
    const observedWindow = window as typeof window & {
      __PANTRY_E2E_SHARE_ACTIONS__?: ShareAction[]
    }
    return observedWindow.__PANTRY_E2E_SHARE_ACTIONS__ ?? []
  })
  expect(shareActions).toHaveLength(1)

  const expectedShareUrl = `https://t.me/share/url?url=${encodeURIComponent(inviteLink)}&text=${encodeURIComponent(SHARE_TEXT)}`
  const action = shareActions[0]
  const actualShareUrl =
    action.source === 'telegram' ? `https://t.me${action.value}` : action.value
  expect(actualShareUrl).toBe(expectedShareUrl)
  expect(new URL(actualShareUrl).searchParams.get('url')).toBe(inviteLink)
  expect(new URL(actualShareUrl).searchParams.get('text')).toBe(SHARE_TEXT)
  if (action.source === 'window.open') expect(action.target).toBe('_blank')

  expect(
    unexpectedTelegramRequests,
    'Sharing must be delegated to Telegram without making an external network request',
  ).toEqual([])
})
