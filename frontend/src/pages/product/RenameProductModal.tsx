import { Button, Modal, Stack, TextInput } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useState } from 'react'
import { describeApiError } from '../../api/http.ts'
import { useRenameProduct } from '../../api/products.ts'
import type { ProductResponse } from '../../api/types.ts'

export function RenameProductModal({
  pantryId,
  product,
  onClose,
}: {
  pantryId: string
  product: ProductResponse
  onClose: () => void
}) {
  const renameProduct = useRenameProduct(pantryId, product.id)
  const [name, setName] = useState(product.name)
  const [brand, setBrand] = useState(product.brand ?? '')

  const unchanged = name.trim() === product.name && (brand.trim() || null) === product.brand

  const rename = () =>
    renameProduct.mutate(
      { name: name.trim(), brand: brand.trim() || null },
      {
        onSuccess: onClose,
        onError: (error) =>
          notifications.show({
            color: 'red',
            title: 'Не получилось',
            message: describeApiError(error),
          }),
      },
    )

  return (
    <Modal opened onClose={onClose} title="Переименовать товар" centered>
      <Stack gap="md">
        <TextInput
          label="Название"
          value={name}
          onChange={(event) => setName(event.currentTarget.value)}
          data-autofocus
        />
        <TextInput
          label="Бренд"
          placeholder="Необязательно"
          value={brand}
          onChange={(event) => setBrand(event.currentTarget.value)}
        />
        <Button
          onClick={rename}
          disabled={!name.trim() || unchanged}
          loading={renameProduct.isPending}
          fullWidth
        >
          Сохранить
        </Button>
      </Stack>
    </Modal>
  )
}
