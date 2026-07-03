import { backButton } from '@telegram-apps/sdk-react'
import { useEffect } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router'

function parentPath(pathname: string): string | null {
  const productMatch = pathname.match(/^(\/pantries\/[^/]+)\/products\/[^/]+$/)
  if (productMatch) return productMatch[1]
  const draftMatch = pathname.match(/^(\/pantries\/[^/]+)\/drafts\/[^/]+$/)
  if (draftMatch) return draftMatch[1]
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
