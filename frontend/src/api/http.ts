import { retrieveRawInitData } from '@telegram-apps/sdk-react'

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

export function describeApiError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Не удалось авторизоваться. Откройте приложение из Telegram.'
    if (error.status === 403) return 'Нет доступа к этому инвентарю.'
    return error.message
  }
  return 'Не удалось выполнить запрос. Проверьте соединение.'
}

function rawInitData(): string | undefined {
  try {
    return retrieveRawInitData()
  } catch {
    return undefined
  }
}

async function errorMessage(response: Response): Promise<string> {
  const fallback = `HTTP ${response.status}`
  try {
    const body: unknown = await response.json()
    if (body && typeof body === 'object' && 'message' in body && typeof body.message === 'string') {
      return body.message || fallback
    }
  } catch {
    // тело не JSON — используем fallback
  }
  return fallback
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const initData = rawInitData()
  const headers = new Headers(init.headers)
  if (initData) headers.set('Authorization', `tma ${initData}`)
  if (init.body) headers.set('Content-Type', 'application/json')

  const response = await fetch(`/api${path}`, { ...init, headers })
  if (!response.ok) throw new ApiError(response.status, await errorMessage(response))

  const text = await response.text()
  return (text ? JSON.parse(text) : undefined) as T
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, {
      method: 'POST',
      body: body === undefined ? undefined : JSON.stringify(body),
    }),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  patch: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path: string) => request<void>(path, { method: 'DELETE' }),
}
