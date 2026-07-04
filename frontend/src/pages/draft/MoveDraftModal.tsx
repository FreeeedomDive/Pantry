import { Alert, Button, Modal, Select, Stack } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import { useMoveDraft } from '../../api/drafts'
import { describeApiError } from '../../api/http'
import { usePantries } from '../../api/pantries'

export function MoveDraftModal({
  pantryId,
  draftId,
  onClose,
  onMoved,
}: {
  pantryId: string
  draftId: string
  onClose: () => void
  onMoved: () => void
}) {
  const pantries = usePantries()
  const moveDraft = useMoveDraft(pantryId, draftId)
  const [targetId, setTargetId] = useState<string | null>(null)

  const options = (pantries.data ?? [])
    .filter((pantry) => pantry.id !== pantryId)
    .map((pantry) => ({ value: pantry.id, label: pantry.name }))

  const move = () => {
    if (!targetId) return
    moveDraft.mutate(targetId, {
      onSuccess: onMoved,
      onError: (error) =>
        notifications.show({
          color: 'red',
          title: 'Не получилось',
          message: describeApiError(error),
        }),
    })
  }

  return (
    <Modal opened onClose={onClose} title="Перенести в другой инвентарь" centered>
      <Stack gap="md">
        <Alert color="yellow">
          Позиции будут сопоставлены заново по каталогу выбранного инвентаря. Ручные правки в этом
          черновике не сохранятся.
        </Alert>
        <Select
          label="Инвентарь"
          placeholder={options.length ? 'Выберите инвентарь' : 'Других инвентарей нет'}
          data={options}
          value={targetId}
          onChange={setTargetId}
          disabled={!options.length}
          searchable
        />
        <Button onClick={move} disabled={!targetId} loading={moveDraft.isPending} fullWidth>
          Перенести и сопоставить заново
        </Button>
      </Stack>
    </Modal>
  )
}
