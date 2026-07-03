import { useQuery } from '@tanstack/react-query'
import { api } from './http'
import type { ProductBalanceResponse, ProductResponse, StockItemResponse } from './types'

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
