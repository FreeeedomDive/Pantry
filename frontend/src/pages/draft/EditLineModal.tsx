import { Button, Modal, NumberInput, Select, Stack, Text, TextInput } from '@mantine/core'
import { DatePickerInput } from '@mantine/dates'
import { useState } from 'react'
import type {
  DraftLineAction,
  DraftLineInput,
  DraftLineResponse,
  ProductResponse,
} from '../../api/types'

const actionOptions = [
  { value: 'MATCH', label: 'Существующий товар' },
  { value: 'CREATE', label: 'Создать новый товар' },
  { value: 'UNSURE', label: 'Пропустить (не решено)' },
]

export function EditLineModal({
  line,
  products,
  saving,
  onSave,
  onClose,
}: {
  line: DraftLineResponse
  products: ProductResponse[]
  saving: boolean
  onSave: (input: DraftLineInput) => void
  onClose: () => void
}) {
  const [action, setAction] = useState<DraftLineAction>(line.action)
  const [productId, setProductId] = useState<string | null>(line.productId)
  const [name, setName] = useState(line.proposedName ?? line.rawText)
  const [brand, setBrand] = useState(line.proposedBrand ?? '')
  const [quantity, setQuantity] = useState<string | number>(line.quantity)
  const [expiresAt, setExpiresAt] = useState<string | null>(line.expiresAt)

  const parsedQuantity = typeof quantity === 'number' ? quantity : Number.parseInt(quantity, 10)
  const quantityValid = Number.isInteger(parsedQuantity) && parsedQuantity >= 0
  const valid =
    quantityValid &&
    (action === 'UNSURE' ||
      (action === 'MATCH' && productId !== null) ||
      (action === 'CREATE' && name.trim().length > 0))

  const save = () =>
    onSave({
      rawText: line.rawText,
      action,
      productId: action === 'MATCH' ? productId : null,
      proposedName: action === 'CREATE' ? name.trim() : null,
      proposedBrand: action === 'CREATE' && brand.trim() ? brand.trim() : null,
      quantity: parsedQuantity,
      expiresAt: action === 'UNSURE' ? null : expiresAt,
    })

  return (
    <Modal opened onClose={onClose} title="Позиция чека" centered>
      <Stack gap="md">
        <Text size="sm" c="dimmed">
          В чеке: {line.rawText}
        </Text>
        <Select
          label="Действие"
          data={actionOptions}
          value={action}
          onChange={(value) => value && setAction(value as DraftLineAction)}
          allowDeselect={false}
        />
        {action === 'MATCH' && (
          <Select
            label="Товар"
            placeholder="Выберите товар"
            data={products.map((product) => ({
              value: product.id,
              label: product.brand ? `${product.name} · ${product.brand}` : product.name,
            }))}
            value={productId}
            onChange={setProductId}
            searchable
            nothingFoundMessage="Ничего не найдено"
          />
        )}
        {action === 'CREATE' && (
          <>
            <TextInput
              label="Название"
              value={name}
              onChange={(event) => setName(event.currentTarget.value)}
              required
            />
            <TextInput
              label="Бренд"
              value={brand}
              onChange={(event) => setBrand(event.currentTarget.value)}
            />
          </>
        )}
        {action === 'UNSURE' && (
          <Text size="sm" c="yellow">
            Строка будет пропущена при подтверждении
          </Text>
        )}
        <NumberInput
          label="Количество, шт"
          value={quantity}
          onChange={setQuantity}
          min={0}
          step={1}
          allowDecimal={false}
        />
        {action !== 'UNSURE' && (
          <DatePickerInput
            label="Годен до"
            placeholder="Не указан"
            value={expiresAt}
            onChange={setExpiresAt}
            valueFormat="D MMMM YYYY"
            clearable
          />
        )}
        <Button onClick={save} disabled={!valid} loading={saving} fullWidth>
          Сохранить
        </Button>
      </Stack>
    </Modal>
  )
}
