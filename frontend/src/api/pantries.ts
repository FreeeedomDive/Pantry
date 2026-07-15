import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './http'
import type { InviteResponse, PantryMemberResponse, PantryResponse } from './types'

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

export function useCreatePantry() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => api.post<PantryResponse>('/pantries', { name }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['pantries'] })
    },
  })
}

export function useRenamePantry(pantryId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (name: string) => api.patch<PantryResponse>(`/pantries/${pantryId}`, { name }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['pantries'] })
    },
  })
}

export function useSetDefaultPantry() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (pantryId: string) => api.post<void>(`/pantries/${pantryId}/default`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['pantries'] })
    },
  })
}

export function useLeaveOrDeletePantry() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (pantryId: string) => api.delete(`/pantries/${pantryId}`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['pantries'] })
    },
  })
}

export function usePantryMembers(pantryId: string) {
  return useQuery({
    queryKey: ['pantries', pantryId, 'members'],
    queryFn: () => api.get<PantryMemberResponse[]>(`/pantries/${pantryId}/members`),
  })
}

export function useKickPantryMember(pantryId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (userId: string) => api.delete(`/pantries/${pantryId}/members/${userId}`),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['pantries', pantryId, 'members'] })
    },
  })
}

export function useCreateInvite(pantryId: string) {
  return useMutation({
    mutationFn: () => api.post<InviteResponse>(`/pantries/${pantryId}/invites`),
  })
}
