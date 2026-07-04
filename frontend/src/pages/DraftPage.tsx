import { Alert, Anchor, Button, Container, Stack, Text, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { useConfirmDraft, useDraft, useUpdateDraft } from '../api/drafts'
import { ApiError, describeApiError } from '../api/http'
import { useProducts } from '../api/products'
import { closeApp } from '../telegram/close'
import type {
  DraftLineInput,
  DraftLineResponse,
  DraftResponse,
  ProductResponse,
} from '../api/types'
import { ErrorState, LoadingState } from '../ui/states'
import { DraftLineCard } from './draft/DraftLineCard'
import { EditLineModal } from './draft/EditLineModal'
import { MoveDraftModal } from './draft/MoveDraftModal'
import { resolveLine, willBeApplied } from './draft/lineStatus'

function toInput(line: DraftLineResponse): DraftLineInput {
  return {
    rawText: line.rawText,
    action: line.action,
    productId: line.productId,
    proposedName: line.proposedName,
    proposedBrand: line.proposedBrand,
    quantity: line.quantity,
    expiresAt: line.expiresAt,
  }
}

function notifyDraftError(error: unknown) {
  notifications.show({
    color: 'red',
    title: 'Не получилось',
    message:
      error instanceof ApiError && error.status === 409
        ? 'Черновик уже подтверждён или ещё не готов'
        : describeApiError(error),
  })
}

export function DraftPage() {
  const { pantryId, draftId } = useParams<{ pantryId: string; draftId: string }>()
  const navigate = useNavigate()
  const draft = useDraft(pantryId!, draftId!)
  const products = useProducts(pantryId!)
  const updateDraft = useUpdateDraft(pantryId!, draftId!)
  const confirmDraft = useConfirmDraft(pantryId!, draftId!)
  const [editingIndex, setEditingIndex] = useState<number | null>(null)
  const [moving, setMoving] = useState(false)

  const saveLine = (draftData: DraftResponse, index: number, input: DraftLineInput) => {
    const lines = draftData.lines.map((line, i) => (i === index ? input : toInput(line)))
    updateDraft.mutate(lines, {
      onSuccess: () => setEditingIndex(null),
      onError: notifyDraftError,
    })
  }

  const confirm = () => {
    confirmDraft.mutate(undefined, {
      onSuccess: () =>
        notifications.show({
          color: 'teal',
          title: 'Поступление подтверждено',
          message: 'Позиции начислены в инвентарь',
        }),
      onError: notifyDraftError,
    })
  }

  const onMoved = () => {
    notifications.show({
      color: 'blue',
      title: 'Переношу',
      message: 'Сопоставляю в другом инвентаре — бот пришлёт новую ссылку на черновик.',
    })
    closeApp(() => navigate(`/pantries/${pantryId}`))
  }

  return (
    <Container size="xs" py="md">
      <Stack gap="md">
        <Anchor component={Link} to={`/pantries/${pantryId}`} size="sm">
          ← К инвентарю
        </Anchor>
        <Title order={2}>Черновик поступления</Title>

        {draft.isPending && <LoadingState />}
        {draft.isError &&
          (draft.error instanceof ApiError && draft.error.status === 404 ? (
            <Alert color="gray" title="Черновик не найден">
              Возможно, он был удалён. Отправьте боту новый чек.
            </Alert>
          ) : (
            <ErrorState error={draft.error} onRetry={() => draft.refetch()} />
          ))}

        {draft.isSuccess && draft.data.status === 'CONFIRMED' && (
          <Alert color="teal" title="Черновик уже подтверждён">
            <Stack gap="sm">
              <Text size="sm">Позиции начислены в инвентарь.</Text>
              <Anchor component={Link} to={`/pantries/${pantryId}`} size="sm">
                Посмотреть товары →
              </Anchor>
            </Stack>
          </Alert>
        )}
        {draft.isSuccess &&
          (draft.data.status === 'EXTRACTED' || draft.data.status === 'MATCHING') && (
            <Alert color="blue" title="Сопоставляю с каталогом">
              <Stack gap="sm">
                <Text size="sm">Черновик ещё готовится. Обновите через несколько секунд.</Text>
                <Button variant="light" size="xs" onClick={() => draft.refetch()}>
                  Обновить
                </Button>
              </Stack>
            </Alert>
          )}
        {draft.isSuccess && draft.data.status === 'FAILED' && (
          <Alert color="red" title="Не удалось обработать чек">
            Отправьте боту фото ещё раз.
          </Alert>
        )}

        {draft.isSuccess && draft.data.status === 'READY' && products.isPending && <LoadingState />}
        {draft.isSuccess && draft.data.status === 'READY' && products.isError && (
          <ErrorState error={products.error} onRetry={() => products.refetch()} />
        )}
        {draft.isSuccess && draft.data.status === 'READY' && products.isSuccess && (
          <DraftEditor
            draft={draft.data}
            products={products.data}
            editingIndex={editingIndex}
            onEdit={setEditingIndex}
            onSaveLine={saveLine}
            saving={updateDraft.isPending}
            onConfirm={confirm}
            confirming={confirmDraft.isPending}
            onRequestMove={() => setMoving(true)}
          />
        )}
      </Stack>
      {moving && (
        <MoveDraftModal
          pantryId={pantryId!}
          draftId={draftId!}
          onClose={() => setMoving(false)}
          onMoved={onMoved}
        />
      )}
    </Container>
  )
}

function DraftEditor({
  draft,
  products,
  editingIndex,
  onEdit,
  onSaveLine,
  saving,
  onConfirm,
  confirming,
  onRequestMove,
}: {
  draft: DraftResponse
  products: ProductResponse[]
  editingIndex: number | null
  onEdit: (index: number | null) => void
  onSaveLine: (draft: DraftResponse, index: number, input: DraftLineInput) => void
  saving: boolean
  onConfirm: () => void
  confirming: boolean
  onRequestMove: () => void
}) {
  const resolutions = draft.lines.map((line) => resolveLine(line, products))
  const appliedCount = resolutions.filter(willBeApplied).length
  const skippedCount = draft.lines.length - appliedCount
  const editedLine = editingIndex === null ? null : draft.lines[editingIndex]

  return (
    <>
      <Text size="sm" c="dimmed">
        Нажмите на строку, чтобы отредактировать её перед подтверждением.
      </Text>
      {draft.lines.map((line, index) => (
        <DraftLineCard key={line.id} line={line} products={products} onEdit={() => onEdit(index)} />
      ))}

      {skippedCount > 0 && (
        <Alert color="yellow">
          {skippedCount} из {draft.lines.length} строк будут пропущены — выберите для них
          существующий товар или создайте новый.
        </Alert>
      )}
      <Button
        size="lg"
        fullWidth
        onClick={onConfirm}
        loading={confirming}
        disabled={saving || appliedCount === 0}
      >
        Начислить {appliedCount} {pluralizeLines(appliedCount)}
      </Button>
      <Button variant="subtle" size="sm" onClick={onRequestMove}>
        Перенести в другой инвентарь
      </Button>

      {editedLine && (
        <EditLineModal
          key={editedLine.id}
          line={editedLine}
          products={products}
          saving={saving}
          onSave={(input) => onSaveLine(draft, editingIndex!, input)}
          onClose={() => onEdit(null)}
        />
      )}
    </>
  )
}

function pluralizeLines(count: number): string {
  const mod10 = count % 10
  const mod100 = count % 100
  if (mod10 === 1 && mod100 !== 11) return 'позицию'
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return 'позиции'
  return 'позиций'
}
