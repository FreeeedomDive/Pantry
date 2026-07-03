import { createBrowserRouter } from 'react-router'
import { DraftPage } from '../pages/DraftPage'
import { PantriesPage } from '../pages/PantriesPage'
import { PantryPage } from '../pages/PantryPage'
import { ProductPage } from '../pages/ProductPage'
import { AppLayout } from './AppLayout'

export const router = createBrowserRouter([
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <PantriesPage /> },
      { path: '/pantries/:pantryId', element: <PantryPage /> },
      { path: '/pantries/:pantryId/products/:productId', element: <ProductPage /> },
      { path: '/pantries/:pantryId/drafts/:draftId', element: <DraftPage /> },
    ],
  },
])
