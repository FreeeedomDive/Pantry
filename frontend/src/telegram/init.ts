import { backButton, init, initData, miniApp, viewport } from '@telegram-apps/sdk-react'
import { mockTelegramEnvForDev } from './mockEnv'

export function initTelegram() {
  mockTelegramEnvForDev()
  try {
    init()
    initData.restore()
    if (miniApp.mountSync.isAvailable()) miniApp.mountSync()
    if (backButton.mount.isAvailable()) backButton.mount()
    if (viewport.mount.isAvailable()) {
      void viewport.mount().then(() => {
        if (viewport.expand.isAvailable()) viewport.expand()
      })
    }
    if (miniApp.ready.isAvailable()) miniApp.ready()
  } catch (error) {
    console.warn('Telegram SDK не инициализирован, работаем без интеграции', error)
  }
}
