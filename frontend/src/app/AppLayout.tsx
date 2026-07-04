import { backButton } from '@telegram-apps/sdk-react'
import { useEffect } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'

function parentPath(pathname: string): string | null {
  if (/^\/pantries\/[^/]+\/members$/.test(pathname)) return '/'
  const nestedMatch = pathname.match(/^(\/pantries\/[^/]+)\/(products\/[^/]+|drafts\/[^/]+)$/)
  if (nestedMatch) return nestedMatch[1]
  if (/^\/pantries\/[^/]+$/.test(pathname)) return '/'
  return null
}

export function AppLayout() {
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const parent = parentPath(pathname)

  useEffect(() => {
    if (!backButton.isMounted()) return
    if (parent === null) {
      backButton.hide()
      return
    }
    backButton.show()
    return backButton.onClick(() => navigate(parent))
  }, [parent, navigate])

  return <Outlet />
}
