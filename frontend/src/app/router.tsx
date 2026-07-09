import { createBrowserRouter } from 'react-router'
import { DraftPage } from '../pages/draft/DraftPage.tsx'
import { MembersPage } from '../pages/members/MembersPage.tsx'
import { PantriesPage } from '../pages/pantry/PantriesPage.tsx'
import { PantryPage } from '../pages/pantry/PantryPage.tsx'
import { ProductPage } from '../pages/product/ProductPage.tsx'
import { AppLayout } from './AppLayout'

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <PantriesPage /> },
      { path: '/pantries/:pantryId', element: <PantryPage /> },
      { path: '/pantries/:pantryId/products/:productId', element: <ProductPage /> },
      { path: '/pantries/:pantryId/drafts/:draftId', element: <DraftPage /> },
      { path: '/pantries/:pantryId/members', element: <MembersPage /> },
    ],
  },
])
