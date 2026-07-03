import { useQuery } from '@tanstack/react-query'
import { api } from './http'
import type { PantryResponse } from './types'

export function usePantries() {
  return useQuery({
    queryKey: ['pantries'],
    queryFn: () => api.get<PantryResponse[]>('/pantries'),
  })
}

export function usePantry(pantryId: string) {
  return useQuery({
    queryKey: ['pantries'],
    queryFn: () => api.get<PantryResponse[]>('/pantries'),
    select: (pantries) => pantries.find((pantry) => pantry.id === pantryId),
  })
}
