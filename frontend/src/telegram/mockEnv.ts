import { isTMA, mockTelegramEnv } from '@telegram-apps/sdk-react'

export function mockTelegramEnvForDev() {
  const initData = import.meta.env.VITE_DEBUG_INIT_DATA
  if (!import.meta.env.DEV || !initData || isTMA()) return

  try {
    mockTelegramEnv({
      launchParams: {
        tgWebAppData: initData,
        tgWebAppVersion: '8',
        tgWebAppPlatform: 'tdesktop',
        tgWebAppThemeParams: {},
      },
    })
  } catch (error) {
    console.warn('VITE_DEBUG_INIT_DATA отклонён SDK, мок Telegram не применён', error)
  }
}
