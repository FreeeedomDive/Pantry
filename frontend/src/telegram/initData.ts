import { retrieveRawInitData } from '@telegram-apps/sdk-react'

let initDataFromLaunch: string | undefined

function readInitData(): string | undefined {
  try {
    return retrieveRawInitData()
  } catch {
    return undefined
  }
}

export function rememberInitData() {
  initDataFromLaunch = readInitData()
}

export function currentInitData(): string | undefined {
  return initDataFromLaunch ?? readInitData()
}
