import '@mantine/core/styles.css'
import '@mantine/dates/styles.css'
import '@mantine/notifications/styles.css'
import 'dayjs/locale/ru'

import { MantineProvider } from '@mantine/core'
import { DatesProvider } from '@mantine/dates'
import { Notifications } from '@mantine/notifications'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { miniApp, useSignal } from '@telegram-apps/sdk-react'
import { RouterProvider } from 'react-router'
import { ApiError } from '../api/http'
import { router } from './router'
import { theme } from './theme'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: (failureCount, error) => {
        if (error instanceof ApiError && error.status >= 400 && error.status < 500) return false
        return failureCount < 1
      },
      refetchOnWindowFocus: false,
    },
  },
})

function useTelegramColorScheme(): 'light' | 'dark' | undefined {
  const isMounted = useSignal(miniApp.isMounted)
  const isDark = useSignal(miniApp.isDark)
  if (!isMounted) return undefined
  return isDark ? 'dark' : 'light'
}

export function App() {
  const telegramColorScheme = useTelegramColorScheme()

  return (
    <MantineProvider theme={theme} forceColorScheme={telegramColorScheme} defaultColorScheme="auto">
      <DatesProvider settings={{ locale: 'ru' }}>
        <Notifications position="top-center" />
        <QueryClientProvider client={queryClient}>
          <RouterProvider router={router} />
        </QueryClientProvider>
      </DatesProvider>
    </MantineProvider>
  )
}
