import { miniApp } from '@telegram-apps/sdk-react'

export function closeApp(fallback: () => void) {
  if (miniApp.close.isAvailable()) miniApp.close()
  else fallback()
}
