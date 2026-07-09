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
        if (viewport.bindCssVars.isAvailable()) viewport.bindCssVars()
        if (viewport.expand.isAvailable()) viewport.expand()
        if (viewport.requestFullscreen.isAvailable()) void viewport.requestFullscreen()
      })
    }
    if (miniApp.ready.isAvailable()) miniApp.ready()
  } catch (error) {
    console.warn('Telegram SDK не инициализирован, работаем без интеграции', error)
  }
}
