import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './http'
import type {
  ProductBalanceResponse,
  ProductResponse,
  StockItemResponse,
  WriteOffStockRequest,
} from './types'

export function useProducts(pantryId: string) {
  return useQuery({
    queryKey: ['pantries', pantryId, 'products'],
    queryFn: () => api.get<ProductResponse[]>(`/pantries/${pantryId}/products`),
  })
}

export function usePantryBalance(pantryId: string) {
  return useQuery({
    queryKey: ['pantries', pantryId, 'balance'],
    queryFn: () => api.get<ProductBalanceResponse[]>(`/pantries/${pantryId}/balance`),
  })
}

export function useProductBalance(pantryId: string, productId: string) {
  return useQuery({
    queryKey: ['pantries', pantryId, 'balance'],
    queryFn: () => api.get<ProductBalanceResponse[]>(`/pantries/${pantryId}/balance`),
    select: (balances) => balances.find((balance) => balance.product.id === productId),
  })
}

export function useProductStock(pantryId: string, productId: string) {
  return useQuery({
    queryKey: ['pantries', pantryId, 'products', productId, 'stock'],
    queryFn: () =>
      api.get<StockItemResponse[]>(`/pantries/${pantryId}/products/${productId}/stock`),
  })
}

export function useRemoveProduct(pantryId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (productId: string) => api.delete(`/pantries/${pantryId}/products/${productId}`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['pantries', pantryId, 'balance'] })
      void queryClient.invalidateQueries({
        queryKey: ['pantries', pantryId, 'products'],
        refetchType: 'none',
      })
    },
  })
}

export function useWriteOffStockItem(pantryId: string, productId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ stockItemId, quantity }: { stockItemId: string; quantity: number }) => {
      const body: WriteOffStockRequest = { quantity }
      return api.post<void>(`/pantries/${pantryId}/stock-items/${stockItemId}/write-off`, body)
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: ['pantries', pantryId, 'products', productId, 'stock'],
      })
      void queryClient.invalidateQueries({ queryKey: ['pantries', pantryId, 'balance'] })
    },
  })
}
