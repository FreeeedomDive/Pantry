import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './http'
import type { DraftLineInput, DraftResponse, UpdateDraftRequest } from './types'

const draftKey = (pantryId: string, draftId: string) => ['pantries', pantryId, 'drafts', draftId]

export function useDraft(pantryId: string, draftId: string) {
  return useQuery({
    queryKey: draftKey(pantryId, draftId),
    queryFn: () => api.get<DraftResponse>(`/pantries/${pantryId}/drafts/${draftId}`),
  })
}

export function useUpdateDraft(pantryId: string, draftId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (lines: DraftLineInput[]) =>
      api.put<DraftResponse>(`/pantries/${pantryId}/drafts/${draftId}`, {
        lines,
      } satisfies UpdateDraftRequest),
    onSuccess: (draft) => {
      queryClient.setQueryData(draftKey(pantryId, draftId), draft)
    },
    onError: () => {
      void queryClient.invalidateQueries({ queryKey: draftKey(pantryId, draftId) })
    },
  })
}

export function useConfirmDraft(pantryId: string, draftId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => api.post<void>(`/pantries/${pantryId}/drafts/${draftId}/confirm`),
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: ['pantries', pantryId] })
    },
  })
}

export function useMoveDraft(pantryId: string, draftId: string) {
  return useMutation({
    mutationFn: (targetPantryId: string) =>
      api.post<void>(`/pantries/${pantryId}/drafts/${draftId}/move`, { pantryId: targetPantryId }),
  })
}
