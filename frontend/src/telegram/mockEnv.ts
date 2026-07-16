import { mockTelegramEnv } from '@telegram-apps/sdk-react'

declare global {
  interface Window {
    __PANTRY_E2E_TELEGRAM_INIT_DATA__?: string
  }
}

function initDataForCurrentMode(): string | undefined {
  if (import.meta.env.MODE === 'e2e') return window.__PANTRY_E2E_TELEGRAM_INIT_DATA__
  if (import.meta.env.DEV) return import.meta.env.VITE_DEBUG_INIT_DATA
  return undefined
}

export function mockTelegramEnvForDev() {
  const initData = initDataForCurrentMode()
  if (!initData) return

  try {
    mockTelegramEnv({
      launchParams: {
        tgWebAppData: initData,
        tgWebAppVersion: '8',
        tgWebAppPlatform: 'tdesktop',
        tgWebAppThemeParams: {},
      },
      resetPostMessage: true,
    })
  } catch (error) {
    console.warn('VITE_DEBUG_INIT_DATA отклонён SDK, мок Telegram не применён', error)
  }
}
